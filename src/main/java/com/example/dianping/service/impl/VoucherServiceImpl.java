package com.example.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.dto.Result;
import com.example.dianping.entity.LimitedVoucher;
import com.example.dianping.entity.Voucher;
import com.example.dianping.mapper.VoucherMapper;
import com.example.dianping.service.ILimitedVoucherService;
import com.example.dianping.service.IVoucherService;
import com.example.dianping.utils.RedisConstants;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 优惠券相关服务。最后编辑于：2022-6-26。
 * @author yuchen
 */
@Service
@AllArgsConstructor
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    private final ILimitedVoucherService limitedVoucherService;

    private final StringRedisTemplate redisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    /**
     * <p>添加优惠券。</p>
     * <p>会将优惠券信息添加至 MySQL 中的 {@code tb_voucher} 表，如果是秒杀型优惠券还会将库存、秒杀开始及结束时间添加至 {@code tb_limited_voucher} 表。并将该优惠券的库存信息缓存至 Redis 中。</p>
     * @param voucher   {@link Voucher} 对象
     */
    @Override
    @Transactional
    public void addLimitedVoucher(Voucher voucher) {
        save(voucher);      // 保存到 Voucher 表
        Integer type = voucher.getType();
        if (type != null && type == 1) {        // 秒杀券
            LimitedVoucher limitedVoucher = new LimitedVoucher();
            limitedVoucher.setVoucherId(voucher.getId());
            limitedVoucher.setStock(voucher.getStock());
            limitedVoucher.setBeginTime(voucher.getBeginTime());
            limitedVoucher.setEndTime(voucher.getEndTime());
            limitedVoucherService.save(limitedVoucher);
            redisTemplate.opsForValue()
                    .set(RedisConstants.STOCK_KEY_PREFIX + voucher.getId(), voucher.getStock().toString());              // 将秒杀型优惠券的库存信息缓存至 Redis
        }
    }
}
