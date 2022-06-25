package com.example.dianping.utils.locking;

import cn.hutool.core.lang.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁的工具类。创建于：2022-4-27。
 * @author yuchen
 */
@Component
public class RedisLockUtil {

    private static StringRedisTemplate redisTemplate;       // 静态 field，在静态方法中使用

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;       // 执行解锁逻辑的 Lua 脚本

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("script/unlock.lua"));      // 指定脚本所在位置
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    public RedisLockUtil(StringRedisTemplate redisTemplate) {       // 这里是一个小 trick，为了将 bean 作为一个静态 field 自动注入
        RedisLockUtil.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取分布式锁，提供这个锁在 Redis 中的 key 以及过期时间。如果获取锁成功，则生成一个随机的 value 返回，用于解锁时校验。
     * @param key       代表这把锁的 key
     * @param timeout   key 的过期时间
     * @return          锁在 Redis 中的值
     */
    public static String tryLock(String key, long timeout) {
        long threadId = Thread.currentThread().getId();
        String value = UUID.randomUUID().toString(true) + "-" +  threadId;      // key 的值设置为一个 UUID 和当前线程 ID 的拼接字符串
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(result)) {
            return value;
        } else {
            return null;
        }
    }


    /**
     * 使用 Lua 脚本释放指定 key 所代表的锁。
     * 需将 value 与 Redis 中的值比对，如果相同则说明这个锁是自己线程的，可以释放，否则不能释放。
     * @param key   key
     * @param value 线程获取锁时拿到的 value
     */
    public static void releaseLock(String key, String value) {
        // 将解锁的步骤放在一条语句中完成，保证了其原子性
        redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(key),
                value
        );
    }
}
