package com.example.dianping.service;

import com.example.dianping.dto.Result;
import com.example.dianping.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 优惠券购买服务类。
 * @author yuchen
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    // 下单秒杀券
    Result purchaseLimitedVoucher(Long voucherId);
}
