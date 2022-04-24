package com.example.dianping.controller;

import com.example.dianping.dto.Result;
import com.example.dianping.service.IVoucherOrderService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 涉及优惠券的下单等操作。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/buy-voucher")
public class VoucherOrderController {

    private final IVoucherOrderService orderService;

    /**
     * 购买秒杀券
     * @param voucherId 优惠券 ID
     * @return          购买结果
     */
    @PostMapping("/limited/{id}")
    public Result purchaseLimitedVoucher(@PathVariable("id") Long voucherId) {
        return orderService.purchaseLimitedVoucher(voucherId);
    }
}
