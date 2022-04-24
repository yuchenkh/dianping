package com.example.dianping.controller;

import com.example.dianping.dto.Result;
import com.example.dianping.entity.Voucher;
import com.example.dianping.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 优惠券相关
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    /**
     * 添加普通券，管理端使用。
     * @param voucher 优惠券信息
     * @return 优惠券id
     */
    @PostMapping("/normal")
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 添加秒杀券，管理端使用。
     * @param voucher 优惠券信息，包含秒杀券特有信息（生效失效时间、库存）
     * @return 优惠券id
     */
    @PostMapping("/limited")
    public Result addLimitedVoucher(@RequestBody Voucher voucher) {
        voucherService.addLimitedVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }

}
