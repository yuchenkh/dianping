package com.example.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.example.dianping.dto.Result;
import com.example.dianping.entity.VoucherOrder;
import com.example.dianping.mapper.VoucherOrderMapper;
import com.example.dianping.service.ILimitedVoucherService;
import com.example.dianping.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.utils.RedisConstants;
import com.example.dianping.utils.UIDGenerator;
import com.example.dianping.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@AllArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    /**
     * 秒杀券信息相关服务
     */
    private final ILimitedVoucherService limitedVoucherService;

    /**
     * 全局唯一 ID 生成器
     */
    private final UIDGenerator uidGenerator;

    /**
     * RedisTemplate
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * Lua 脚本，用于判断用户是否有购买某张秒杀优惠券资格
     */
    private static final DefaultRedisScript<Long> CHECKING_SCRIPT;

    /**
     * 线程池，共有 5 条线程同时处理待创建的订单。
     */
    private static final ExecutorService ORDER_HANDLER = Executors.newFixedThreadPool(5);

    /**
     * 在依赖完成注入之后进行初始化，向线程池提交任务，不断从 Redis 的 stream 中领取待创建的订单，添加至 MySQL 中。
     */
    @PostConstruct
    private void init() {
        for (int i = 0; i < 5; i++) {           // 向线程池提交 5 个任务，即使用 5 个线程处理待创建的订单
            ORDER_HANDLER.submit(new OrderHandling());
        }
    }

    static {
        CHECKING_SCRIPT = new DefaultRedisScript<>();
        CHECKING_SCRIPT.setLocation(new ClassPathResource("script/stockAndOrderChecking.lua"));      // 导入 Lua 脚本
        CHECKING_SCRIPT.setResultType(Long.class);
    }

    /**
     * 处理订单创建的任务。
     */
    private class OrderHandling implements Runnable {

        @Override
        public void run() {
            while (true) {
                // 从 Redis 的 stream 中取一条消息
                // XREADGROUP GROUP order-handler alice COUNT 1 BLOCK 2000 STREAMS orders >
                try {
                    List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                            Consumer.from("order-handler", Thread.currentThread().getName()),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(RedisConstants.ORDER_QUEUE_KEY, ReadOffset.lastConsumed())
                    );
                    if (!CollectionUtil.isEmpty(records)) {
                        MapRecord<String, Object, Object> entry = records.get(0);
                        RecordId id = entry.getId();        // stream 中的消息的 ID
                        Map<Object, Object> message = entry.getValue();    // 消息的内容
                        VoucherOrder order = BeanUtil.fillBeanWithMap(message, new VoucherOrder(), true);
                        saveOrder(order);       // 调用方法，保存订单至 MySQL 并修改库存信息
                        redisTemplate.opsForStream().acknowledge(RedisConstants.ORDER_QUEUE_KEY, "order-handler", id);      // 向 Redis 确认消息已经处理
                    }
                } catch (Exception e) {
                    log.error("订单处理失败");
                    errorHandler();
                }
            }
        }
    }


    /**
     * <p>下单秒杀券。</p>
     * <p>方法首先使用 Lua 脚本在 Redis 中查询优惠券的库存以及该用户是否是第一次购买该优惠券。
     * 如果满足购买条件，则生成订单 ID，将创建订单的任务加入至任务队列并告知用户下单成功，在 MySQL 中创建订单的任务将由 {@link ExecutorService} 中的线程异步完成。</p>
     * <p>分析一下这个方法的涉及到的共享「状态」：Redis 中的库存信息、优惠券的购买人信息。都存在 "check-then-act" 的竞态条件。但是放在 Lua 脚本中就避免了线程安全性问题。</p>
     *
     * @param voucherId 优惠券 ID
     * @return          购买结果，下单成功则返回订单号，否则提示购买失败
     */
    @Override
    public Result purchaseLimitedVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        Long orderId = uidGenerator.nextId("order");        // 订单号，先创建但不一定用户有购买资格
        Long result = redisTemplate.execute(CHECKING_SCRIPT,
                Arrays.asList(
                        RedisConstants.STOCK_KEY_PREFIX + voucherId,
                        RedisConstants.BUYER_LIST_KEY_PREFIX + voucherId,
                        RedisConstants.ORDER_QUEUE_KEY),
                orderId.toString(),
                voucherId.toString(),
                userId.toString());                // 在 Redis 中执行 Lua 脚本能够保证脚本中操作的原子性，因此之前的同步机制这里不再需要了
        assert result != null;
        if (result.intValue() == 1) {
            log.debug("库存不足，用户 {} 购买 {} 号优惠券失败。", userId, voucherId);
            return Result.fail("优惠券已被抢光");
        }
        if (result.intValue() == 2) {
            log.debug("用户 {} 已经购买过 {} 号优惠券，下单失败。", userId, voucherId);
            return Result.fail("请勿重复下单");
        }
        log.debug("用户 {} 购买优惠券 {} 成功，订单号：{}。将该订单加入消息队列，等待创建。", userId, voucherId, orderId);
        return Result.ok(orderId);          // 直接返回订单号，告知用户购买成功
    }


    /**
     * 将订单保存至 MySQL。
     * @param order 订单
     */
    @Transactional
    public void saveOrder(VoucherOrder order) {
        Long userId = order.getUserId();
        Long voucherId = order.getVoucherId();
        Long count = query().eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if (count > 0) {
            log.error("MySQL 中已经存在用户 {} 购买优惠券 {} 的订单，Redis 和 MySQL 的数据出现不一致。", userId, voucherId);           // 按理说是不会出现这种情况的
            return;
        }
        boolean result = limitedVoucherService.update()         // 减库存
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)         // 乐观锁的思想
                .update();
        if (!result) {
            log.error("MySQL 中显示库存不足，Redis 和 MySQL 中的数据出现不一致。");
            return;
        }
        save(order);
        log.debug("订单 {} 成功写入 MySQL，并扣减库存。", order.getId());
    }


    /**
     * 订单处理失败时调用。
     */
    private void errorHandler() {
        while (true) {
            // 从 Redis 的 stream 中读取 pending 消息
            // XREADGROUP GROUP order-handler alice COUNT 1 BLOCK 2000 STREAMS orders 0
            try {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                        Consumer.from("order-handler", Thread.currentThread().getName()),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(RedisConstants.ORDER_QUEUE_KEY, ReadOffset.from("0"))
                );
                if (CollectionUtil.isEmpty(records)) {          // 该消费者已经没有 pending 的消息了
                    break;
                }
                MapRecord<String, Object, Object> entry = records.get(0);
                RecordId id = entry.getId();        // stream 中的消息的 ID
                Map<Object, Object> message = entry.getValue();    // 消息的内容
                VoucherOrder order = BeanUtil.fillBeanWithMap(message, new VoucherOrder(), true);
                saveOrder(order);       // 调用方法，保存订单至 MySQL 并修改库存信息
                redisTemplate.opsForStream().acknowledge(RedisConstants.ORDER_QUEUE_KEY, "order-handler", id);      // 向 Redis 确认消息已经处理
            } catch (Exception e) {
                log.error("pending 订单处理失败");
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
