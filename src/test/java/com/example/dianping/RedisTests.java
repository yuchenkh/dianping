package com.example.dianping;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.dianping.entity.Shop;
import com.example.dianping.service.impl.ShopServiceImpl;
import com.example.dianping.utils.HotKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class RedisTests {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ShopServiceImpl service;


    // 删除 Redis 中不存在的 key 会报错吗？
    // 不会，会返回 false
    @Test
    void redisTest() {
        Boolean result = redisTemplate.delete("123");
        System.out.println(result);
    }

    // 查询不存在的 key
    @Test
    void queryKeyNotExisted() {
        String s = redisTemplate.opsForValue().get("aahahha");
        System.out.println(s);
    }

    @Test
    void cacheHotKey() throws InterruptedException {
        service.cacheShopAsHotkey(1L, 10L);
    }

    @Test
    void getHotKey() {
        String s = redisTemplate.opsForValue().get("dp:hotkey:shop:1");
        HotKey hotKey = JSONUtil.toBean(s, HotKey.class);
        JSONObject json = (JSONObject) hotKey.getData();
        Shop shop = JSONUtil.toBean(json, Shop.class);
        System.out.println(shop);
    }
}
