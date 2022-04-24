package com.example.dianping.service;

import com.example.dianping.dto.Result;
import com.example.dianping.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addLimitedVoucher(Voucher voucher);
}
