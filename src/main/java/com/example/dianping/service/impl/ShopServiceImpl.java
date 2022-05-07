package com.example.dianping.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.dianping.dto.Result;
import com.example.dianping.entity.Shop;
import com.example.dianping.mapper.ShopMapper;
import com.example.dianping.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.utils.HotKey;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.dianping.utils.RedisConstants.*;

/**
 * 商户相关服务实现类。最后编辑于：2022-4-21。
 * @author yuchen
 */
@Service
@AllArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 线程池，用于热点 key 的刷新
     */
    private static final ExecutorService HOT_KEY_REFRESHER = Executors.newFixedThreadPool(5);

    /**
     * 根据 ID 查询商户信息。
     * @param id    商户 ID
     * @return      相应的 {@link Shop} 对象
     */
    @Override
    public Shop shopById(long id) {
        // 这里可以在「互斥锁」和「逻辑过期时间」两种方案之间切换
        return queryShopWithoutBreakdown(id);
    }


    /**
     * 根据 ID 查询商户信息，且采用缓存空值的方式防止缓存穿透。
     * @param id    商户 ID
     * @return      相应的 {@link Shop} 对象
     */
    private Shop queryShopWithoutPenetration(long id) {
        String key = SHOP_KEY_PREFIX + id;
        String shopJson = redisTemplate.opsForValue().get(key);
        // Redis 中有对应缓存
        if (StrUtil.isNotBlank(shopJson)) {
            log.debug("商户 " + id + " 在 Redis 中命中。");
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // Redis 中有记录，但存的是为防止缓存穿透设置的空字符串，说明 Redis 和 MySQL 中都没有该记录
        if (shopJson != null) {
            log.debug("商户 " + id + " 不存在，命中 Redis 中的空值");
            return null;
        }
        // Redis 中没有缓存，去 MySQL 中查询
        Shop shop = getById(id);
        // MySQL 也没有记录，会有缓存穿透的可能
        if (shop == null) {
            log.debug("商户 " + id + " 在 Redis 和 MySQL 均不存在，向 Redis 缓存空值防止缓存穿透。");
            redisTemplate.opsForValue().set(key, "");       // 缓存 null 值
            redisTemplate.expire(key, Duration.ofMinutes(NULL_CACHE_TTL));
            return null;
        }
        // MySQL 中有数据
        log.debug("商户 " + id + " 从 MySQL 中命中，并写入 Redis。");
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));         // 将商户信息缓存至 Redis
        redisTemplate.expire(key, Duration.ofMinutes(SHOP_CACHE_TTL));            // 设置缓存过期时间
        return shop;
    }


    /**
     * 根据 ID 查询商户信息，在防止穿透的基础上使用互斥锁机制防止缓存击穿。
     * @param id    商户 ID
     * @return      相应的 {@link Shop} 对象
     */
    public Shop queryShopWithoutBreakdown(long id) {
        String key = SHOP_KEY_PREFIX + id;
        String shopJson = redisTemplate.opsForValue().get(key);
        // Redis 中有对应缓存
        if (StrUtil.isNotBlank(shopJson)) {
            log.debug("商户 " + id + " 在 Redis 中命中。");
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // Redis 中有记录，但存的是为防止缓存穿透设置的空字符串，说明 Redis 和 MySQL 中都没有该记录
        if (shopJson != null) {
            log.debug("商户 " + id + " 不存在，命中 Redis 中的空值");
            return null;
        }
        // Redis 中没有缓存，尝试获取互斥锁，去 MySQL 中查询并写入缓存
        try {
            boolean gotLock = getLockOnWritingCache(id);
            // 获取锁失败，休眠一段时间后重试
            if (!gotLock) {
                Thread.sleep(50);
                return queryShopWithoutBreakdown(id);
            }
            // 获取锁成功
            Shop shop = getById(id);
            Thread.sleep(200);      // 模拟缓存重建过程中的延时
            // MySQL 也没有记录，会有缓存穿透的可能
            if (shop == null) {
                log.debug("商户 " + id + " 在 Redis 和 MySQL 均不存在，向 Redis 缓存空值防止缓存穿透。");
                redisTemplate.opsForValue().set(key, "");       // 缓存 null 值
                redisTemplate.expire(key, Duration.ofMinutes(NULL_CACHE_TTL));
                return null;
            }
            // MySQL 中有数据
            log.debug("商户 " + id + " 从 MySQL 中命中，并写入 Redis。");
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));         // 将商户信息缓存至 Redis
            redisTemplate.expire(key, Duration.ofMinutes(SHOP_CACHE_TTL));            // 设置缓存过期时间
            return shop;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            releaseLockOnWritingCache(id);          // 最后释放锁
        }
    }


    /**
     * 根据 ID 查询热点商户信息，使用逻辑过期机制防止缓存击穿。
     * 使用这种机制时，需要提前对所有热点 key 进行预热，即：将相关数据从 MySQL 中转换为 {@link Shop} 对象缓存至 Redis。
     * 前端调用时，如果 Redis 没有记录则说明这个不是热点 key，应调用获取常规 key 的接口。
     * @param id    商户 ID
     * @return      相应的 {@link Shop} 对象
     */
    public Shop queryShopWithoutBreakdown2(long id) {
        String key = HOT_KEY_PREFIX + id;
        String json = redisTemplate.opsForValue().get(key);
        // Redis 中没有对应的热点 key 时，直接返回 null
        if (StrUtil.isBlank(json)) {
            log.debug("热点商户 " + id + " 不存在。");
            return null;        // 该热点 key 不存在
        }
        // Redis 中存在该热点 key
        HotKey obj = JSONUtil.toBean(json, HotKey.class);
        Shop shop = JSONUtil.toBean((JSONObject) obj.getData(), Shop.class);
        // 如果热点 key 未过期，则直接返回
        if (obj.getExpireTime().isAfter(LocalDateTime.now())) {
            log.debug("命中热点商户 " + id + " 在 Redis 中的缓存。");
            return shop;
        }
        // 热点 key 已经过期，向线程池提交刷新缓存的任务，自己则直接返回过时的数据
        log.debug("命中热点商户 " + id + " 在 Redis 中的过期缓存，同时尝试获取互斥锁以开启新线程刷新缓存。");
        boolean gotLock = getLockOnWritingCache(id);
        if (gotLock) {
            log.debug("获取互斥锁成功。");
            HOT_KEY_REFRESHER.submit(() -> {
                try {
                    cacheShopAsHotkey(id, 20L);         // 设置热点 key 的逻辑过期时间为 20 秒
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    releaseLockOnWritingCache(id);      // 释放锁
                }
            });
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


    /**
     * 获取向 Redis 写某一商户信息的缓存的锁。用于解决缓存击穿问题。
     * @param id    要写缓存的商户 ID
     * @return      获取锁的结果
     */
    private boolean getLockOnWritingCache(Long id) {
        String key = SHOP_LOCK_KEY_PREFIX + id;
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", SHOP_LOCK_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(result);
    }


    /**
     * 解锁
     * @param id    要写缓存的商户 ID
     */
    private void releaseLockOnWritingCache(Long id) {
        String key = SHOP_LOCK_KEY_PREFIX + id;
        redisTemplate.delete(key);
    }


    /**
     * 将指定商户的信息作为 {@link HotKey} 从 MySQL 中缓存至 Redis
     * @param id    商户 ID，必须存在
     * @param ttl   缓存的逻辑过期时间，以秒为单位
     */
    public void cacheShopAsHotkey(Long id, Long ttl) throws InterruptedException {
        Shop shop = getById(id);
        if (shop != null) {
            String key = HOT_KEY_PREFIX + id;
            HotKey data = new HotKey(shop, LocalDateTime.now().plusSeconds(ttl));
            Thread.sleep(200);          // 模拟缓存重建过程中的延迟
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(data));
            log.debug("从 MySQL 查询记录，完成热点商户 " + id + " 的缓存刷新。");
        }
    }
}
