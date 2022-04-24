package com.example.dianping.utils;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用于 Redis 缓存读写的工具类。最后编辑于：2022-4-21。
 */
@Slf4j
@Component
@AllArgsConstructor
public class RedisCacheClient {

    private final StringRedisTemplate redisTemplate;

    /**
     * 将任意 Java 对象以 JSON 字符串形式保存到 Redis，并设置过期时间。
     * @param key       键
     * @param value     值，可以是任意 Java 对象
     * @param timeout   过期时间
     * @param unit      过期时间单位
     */
    public void set(String key, Object value, Long timeout, TimeUnit unit) {
        String jsonStr = JSONUtil.toJsonStr(value);
        redisTemplate.opsForValue().set(key, jsonStr, timeout, unit);
    }

    /**
     * 将任意 Java 对象作为热点 key 以 JSON 字符串形式保存到 Redis，并设置逻辑过期时间。
     * @param key       键
     * @param value     值，可以是任意 Java 对象
     * @param timeout   过期时间
     * @param unit      过期时间单位
     */
    public void setHotKey(String key, Object value, Long timeout, TimeUnit unit) {
        HotKey obj = new HotKey(value, LocalDateTime.now().plusSeconds(unit.toSeconds(timeout)));
        String jsonStr = JSONUtil.toJsonStr(obj);
        redisTemplate.opsForValue().set(key, jsonStr);
    }


    /**
     * 从 Redis 中获取 key 并将其转换为相应 Java 对象返回。
     * @param key           键
     * @param targetClass   希望转换成的类
     * @return              对应的 Java 对象
     * @param <T>           类型参数
     */
    public <T> T get(String key, Class<T> targetClass) {
        String json = redisTemplate.opsForValue().get(key);
        JSONUtil.isTypeJSONArray(json);
        return JSONUtil.toBean(json, targetClass);
    }
}
