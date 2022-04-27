package com.example.dianping;

import com.example.dianping.utils.locking.RedisLockUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RedisLockUtilTests {

    @Test
    void currentThread() {
        long id = Thread.currentThread().getId();
        String name = Thread.currentThread().getName();
        System.out.println("ID: " + id);
        System.out.println("Name: " + name);
    }

    @Test
    void getLock() {
        boolean gotLock = RedisLockUtil.tryLock("test-key", 100);
        System.out.println(gotLock);
    }

    @Test
    void releaseLock() {
        RedisLockUtil.releaseLock("test-key");
    }
}
