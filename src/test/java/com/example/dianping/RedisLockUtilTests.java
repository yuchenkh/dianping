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
        System.out.println(RedisLockUtil.tryLock("test-key", 100));
    }

    @Test
    void releaseLock() {
        RedisLockUtil.releaseLock("test-key", "dae696c739e84be29be5ab12271ed60f-1");
    }
}
