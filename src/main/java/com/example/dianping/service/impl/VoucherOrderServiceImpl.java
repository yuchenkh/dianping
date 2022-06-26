package com.example.dianping.service.impl;

import com.example.dianping.dto.Result;
import com.example.dianping.entity.VoucherOrder;
import com.example.dianping.mapper.VoucherOrderMapper;
import com.example.dianping.service.ILimitedVoucherService;
import com.example.dianping.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.utils.UIDGenerator;
import com.example.dianping.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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
     * 待创建的订单队列
     */
    private static final BlockingQueue<VoucherOrder> ORDERS = new ArrayBlockingQueue<>(1 << 20);

    /**
     * 线程池，其实只有一个线程负责所有订单的创建。
     */
    private static final ExecutorService ORDER_HANDLER = Executors.newSingleThreadExecutor();

    /**
     * 在依赖完成注入之后进行初始化，向线程池提交任务，不断从任务队列中领取待创建的订单，添加至 MySQL 中。
     */
    @PostConstruct
    private void init() {
        ORDER_HANDLER.submit(() -> {
           while (true) {
               try {
                   VoucherOrder order = ORDERS.take();
                   saveOrder(order);
               } catch (InterruptedException e) {
                   log.error("从任务队列中取待添加的订单时被打断。");
               }
           }
        });
    }

    static {
        CHECKING_SCRIPT = new DefaultRedisScript<>();
        CHECKING_SCRIPT.setLocation(new ClassPathResource("script/stockAndOrderChecking.lua"));      // 导入 Lua 脚本
        CHECKING_SCRIPT.setResultType(Long.class);
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
        Long result = redisTemplate.execute(CHECKING_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString());                // 在 Redis 中执行 Lua 脚本能够保证脚本中操作的原子性，因此之前的同步机制这里不再需要了
        log.debug("使用 Lua 脚本在 Redis 中进行查询，判断用户是否具有购买资格。");
        assert result != null;
        if (result.intValue() == 1) {
            log.debug("有用户用户尝试下单 {} 号优惠券，但是库存不足", voucherId);
            return Result.fail("优惠券已被抢光");
        }
        if (result.intValue() == 2) {
            return Result.fail("请勿重复下单");
        }
        long orderId = uidGenerator.nextId("order");        // 订单号
        VoucherOrder newOrder = new VoucherOrder(orderId, userId, voucherId);       // 订单实体，代表要创建的订单
        try {
            ORDERS.put(newOrder);        // 将修改 MySQL 创建订单的任务加入任务队列
            log.debug("用户 {} 购买优惠券 {} 成功，订单号：{}。将订单加入队列等待另一线程异步完成添加。", userId, voucherId, orderId);
        } catch (InterruptedException e) {
            log.error("将创建订单任务加入至任务队列时被中断。");
        }

        return Result.ok(orderId);          // 直接返回响应，告知用户购买成功，提供订单号
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
            log.error("MySQL 中已经存在用户 {} 购买优惠券 {} 的订单，Redis 和 MySQL 的数据出现不一致", userId, voucherId);           // 按理说是不会出现这种情况的
            return;
        }
        boolean result = limitedVoucherService.update()         // 减库存
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)         // 乐观锁的思想
                .update();
        if (!result) {
            log.error("MySQL 中显示库存不足，Redis 和 MySQL 中的数据出现不一致");
            return;
        }
        save(order);
    }
}
