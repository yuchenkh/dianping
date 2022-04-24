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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    @Transactional
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

        // 库存足够，将库存减 1
        boolean result = limitedVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .update();
        if (!result) {
            log.debug("库存更新失败");
            return Result.fail("库存不足");
        }

        // 创建订单
        VoucherOrder order = new VoucherOrder();
        Long uid = uidGenerator.nextId("order");        // 订单分配到的 ID
        order.setId(uid);
        order.setVoucherId(voucherId);
        order.setPayTime(LocalDateTime.now());

        order.setUserId(userId);          // 用户信息到当前线程关联的 UserHolder 中取
        save(order);
        log.debug("用户 {} 成功购买了一张 {} 号优惠券，订单号：{}", userId, voucherId, uid);

        return Result.ok(uid);
    }
}
