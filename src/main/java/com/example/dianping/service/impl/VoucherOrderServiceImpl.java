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
import com.example.dianping.utils.locking.RedisLockUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 下单秒杀券
     * @param voucherId 优惠券 ID
     * @return          购买结果，下单成功则返回订单号，否则提示购买失败
     */
    @Override
    public Result purchaseLimitedVoucher(Long voucherId) {
        // 查询秒杀券的库存、开放购买时间以及结束购买时间
        LimitedVoucher voucher = limitedVoucherService.getById(voucherId);
        Long userId = UserHolder.getUser().getId();
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        } else if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("优惠券购买还未开放");
        } else if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("优惠券活动已结束");
        } else if (voucher.getStock() < 1) {
            log.debug("{} 号用户尝试下单，但是库存不足", userId);
            return Result.fail("优惠券已抢光");
        }

        // 查询当前用户之前有没有购买过这张优惠券，然后尝试减库存并下单
        // 不同的线程对于 MySQL 中 "userId 用户购买 vouchId 优惠券的订单数" 这个数据是共享的，可能会出现「线程干扰」，必须使用同步 (Synchronization)
        return createOrder(voucherId, userId);
    }


    /**
     * 指定用户和优惠券，创建订单。需先查询该用户对该优惠券的购买记录，如未购买过，则减去库存并下单，否则下单失败。
     * @param voucherId 优惠券 ID
     * @param userId    用户 ID
     */
    @Transactional
    public Result createOrder(Long voucherId, Long userId) {

        // 获取分布式锁，让相同 userId 和 voucherId 的线程串行执行下面的逻辑
        String key = LOCK_KEY_PREFIX + "order:v" + voucherId + ":u" + userId;         // 针对于「查询某一用户对某一优惠券所下过的订单」这一操作，申请的锁的 key
        String lockValue = RedisLockUtil.tryLock(key, 100);
        if (lockValue == null) {
            return Result.fail("请勿重复下单");          // 获取锁失败则说明此时此刻还有一个线程（对应该用户）在对同一优惠券下单
        }
        log.debug("当前线程获取分布式锁成功，在 Redis 中的 key 是：{}，value 是 {}", key, lockValue);

        try {
            // 在 MySQL 查询该用户购买该优惠券的订单数，这一步骤是判断用户能否去尝试减库存的关卡。
            Long count = query().eq("user_id", userId)
                    .eq("voucher_id", voucherId)
                    .count();
            if (count > 0) {
                return Result.fail("该优惠券限一人购买一张，您已购买过");
            }
            // 减库存
            boolean result = limitedVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)         // 乐观锁的思想
                    .update();
            if (!result) {
                log.debug("{} 号用户尝试下单，但是库存不足", userId);
                return Result.fail("库存不足");
            }

            // 在 MySQL 中创建订单
            VoucherOrder order = new VoucherOrder();
            Long uid = uidGenerator.nextId("order");        // 订单分配到的 ID
            order.setId(uid);
            order.setVoucherId(voucherId);
            order.setPayTime(LocalDateTime.now());
            order.setUserId(userId);          // 用户信息到当前线程关联的 UserHolder 中取
            save(order);
            log.debug("用户 {} 成功购买了一张 {} 号优惠券，订单号：{}", userId, voucherId, uid);

            return Result.ok(uid);
        } finally {
            RedisLockUtil.releaseLock(key, lockValue);
        }
    }
}
