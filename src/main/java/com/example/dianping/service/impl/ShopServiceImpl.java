package com.example.dianping.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.dianping.dto.Result;
import com.example.dianping.entity.Shop;
import com.example.dianping.mapper.ShopMapper;
import com.example.dianping.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static com.example.dianping.utils.RedisConstants.SHOP_CACHE_TTL;
import static com.example.dianping.utils.RedisConstants.SHOP_KEY_PREFIX;

/**
 * 商户相关服务实现类。最后编辑于：2022-4-19。
 * @author yuchen
 */
@Service
@AllArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 根据 ID 查询商户信息。
     * @param id    商户 ID
     * @return      相应的 {@link Shop} 对象
     */
    @Override
    public Shop shopById(long id) {
        // 优先在 Redis 中查询
        String key = SHOP_KEY_PREFIX + id;
        String shopJson = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotEmpty(shopJson)) {
            log.debug("商户 " + id + " 在 Redis 中命中。");
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // Redis 没有缓存，则去 MySQL 中查询
        Shop shop = getById(id);
        if (shop != null) {
            log.debug("商户 " + id + " 从 MySQL 中查询，并写入 Redis。");
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));         // 将商户信息缓存至 Redis
            redisTemplate.expire(key, Duration.ofMinutes(SHOP_CACHE_TTL));            // 设置缓存过期时间
        }

        return shop;
    }

    /**
     * 修改某一商户的信息。
     * @param shop    代表期望的修改结果的 {@link Shop} 实体，会根据其中的商户 ID 来决定修改哪一条 MySQL 记录。
     * @return        操作结果
     */
    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("未指定商户 ID");
        }

        updateById(shop);
        redisTemplate.delete(SHOP_KEY_PREFIX + id);
        log.debug("修改商户 " + id + " 信息，并删除 Redis 中的相应记录。");
        return Result.ok();
    }
}
