package com.example.dianping.utils.locking;

/**
 * 定义了锁相关的工具类的标准。创建于：2022-4-27。
 * @author yuchen
 */
public interface LockUtil {

    /**
     * 尝试获取分布式锁，提供这个锁在 Redis 中的 key 以及过期时间
     * @param key       代表这把锁的 key
     * @param timeout   key 的过期时间
     * @return          锁是否获取成功
     */
    boolean tryLock(String key, long timeout);

    /**
     * 释放指定 key 所代表的锁
     * @param key   key
     */
    void releaseLock(String key);
}
