package com.example.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.dto.Result;
import com.example.dianping.entity.LimitedVoucher;
import com.example.dianping.entity.Voucher;
import com.example.dianping.mapper.VoucherMapper;
import com.example.dianping.service.ILimitedVoucherService;
import com.example.dianping.service.IVoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ILimitedVoucherService limitedVoucherService;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addLimitedVoucher(Voucher voucher) {
        // 保存到 Voucher 表
        save(voucher);

        // 附加信息保存到 LimitedVoucher 表
        LimitedVoucher limitedVoucher = new LimitedVoucher();
        limitedVoucher.setVoucherId(voucher.getId());
        limitedVoucher.setStock(voucher.getStock());
        limitedVoucher.setBeginTime(voucher.getBeginTime());
        limitedVoucher.setEndTime(voucher.getEndTime());
        limitedVoucherService.save(limitedVoucher);
    }
}
