package com.example.dianping.service.impl;

import com.example.dianping.dto.Result;
import com.example.dianping.entity.LimitedVoucher;
import com.example.dianping.entity.VoucherOrder;
import com.example.dianping.mapper.VoucherOrderMapper;
import com.example.dianping.service.ILimitedVoucherService;
import com.example.dianping.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.utils.UIDGenerator;
import com.example.dianping.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.example.dianping.utils.RedisConstants.LOCK_KEY_PREFIX;

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
     * Redisson Client
     */
    private final RedissonClient redissonClient;

    /**
     * 下单秒杀券
     * @param voucherId 优惠券 ID
     * @return          购买结果，下单成功则返回订单号，否则提示购买失败
     */
    @Override
    public Result purchaseLimitedVoucher(Long voucherId) {
        LimitedVoucher voucher = limitedVoucherService.getById(voucherId);              // 查询该秒杀券的库存、开放购买时间以及结束购买时间
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        } else if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("优惠券购买还未开放");
        } else if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("优惠券活动已结束");
        } else if (voucher.getStock() < 1) {
            log.debug("有用户用户尝试下单 {} 号优惠券，但是库存不足", voucherId);
            return Result.fail("优惠券已抢光");
        }
        return createOrder(voucherId);          // 尝试下单
    }


    /**
     * <p>提供优惠券 ID，创建订单。</p>
     * <p>该方法会先确定当前线程对应的用户，然后在 MySQL 中查询该用户对该优惠券的购买记录，如未购买过则减去库存并下单，否则下单失败。</p>
     * <p>MySQL 中 "用户 U1 购买优惠券 V1 的订单数" 这个数据由多个线程共享，而秒杀券同一用户仅限购买一张，
     * 为防止同一用户使用多线程购买同一优惠券单的线程安全性问题，下单操作必须有适当的同步机制。
     * 这里我们使用 Redisson 提供的分布式锁。</p>
     *
     * @param voucherId 优惠券 ID
     * @return 表示创建订单成功与否的 {@link Result} 实例
     */
    @Transactional
    public Result createOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 获取分布式锁，让相同 userId 和 voucherId 的线程串行执行下面的逻辑
        String lockName = LOCK_KEY_PREFIX + "order:v" + voucherId + ":u" + userId;         // 针对于「查询某一用户对某一优惠券所下过的订单」这一操作，申请的锁的 key
        RLock lock = redissonClient.getLock(lockName);       // 获取对应于这个用户和这个优惠券的组合的锁（锁机制由 Redisson 提供）
        boolean acquired = lock.tryLock();          // 以默认方式尝试获取锁（如果锁目前被其他线程持有则立即返回 false）
        if (!acquired) {
            log.debug("当前线程获取分布式锁 (Redisson) 失败，锁名称：{}", lockName);
            return Result.fail("请勿重复下单");          // 获取锁失败则说明此时此刻还有一个线程（对应该用户）在对同一优惠券下单
        }
        log.debug("当前线程获取分布式锁 (Redisson) 成功，锁名称：{}", lockName);
        try {
            Long count = query().eq("user_id", userId)      // 在 MySQL 查询该用户购买该优惠券的订单数，这一步骤是判断用户能否去尝试减库存的关卡。
                    .eq("voucher_id", voucherId)
                    .count();
            if (count > 0) {
                return Result.fail("该优惠券限一人购买一张，您已购买过");
            }
            boolean result = limitedVoucherService.update()         // 减库存
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)         // 乐观锁的思想
                    .update();
            if (!result) {
                log.debug("{} 号用户尝试下单，但是库存不足", userId);
                return Result.fail("库存不足");
            }
            VoucherOrder order = new VoucherOrder();            // 在 MySQL 中创建订单
            Long uid = uidGenerator.nextId("order");        // 订单分配到的 ID
            order.setId(uid);
            order.setVoucherId(voucherId);
            order.setPayTime(LocalDateTime.now());
            order.setUserId(userId);          // 用户信息到当前线程关联的 UserHolder 中取
            save(order);
            log.debug("用户 {} 成功购买了一张 {} 号优惠券，订单号：{}", userId, voucherId, uid);
            return Result.ok(uid);
        } finally {
            lock.unlock();          // 释放锁
        }
    }
}
