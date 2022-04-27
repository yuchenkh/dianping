package com.example.dianping.utils.locking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁的工具类。创建于：2022-4-27。
 * @author yuchen
 */
@Component
public class RedisLockUtil {

    private static StringRedisTemplate redisTemplate;       // 静态 field，在静态方法中使用

    @Autowired
    public RedisLockUtil(StringRedisTemplate redisTemplate) {       // 这里是一个小 trick，为了将 bean 作为一个静态 field 自动注入
        RedisLockUtil.redisTemplate = redisTemplate;
    }

    public static boolean tryLock(String key, long timeout) {
        long threadId = Thread.currentThread().getId();
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(threadId), timeout, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(result);
    }


    public static void releaseLock(String key) {
        redisTemplate.delete(key);
    }
}
