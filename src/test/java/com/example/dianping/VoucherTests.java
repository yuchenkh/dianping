package com.example.dianping;

import com.example.dianping.utils.UIDGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class VoucherTests {

    @Autowired
    UIDGenerator uidGenerator;

    // 创建一个有 500 个线程的线程池，用于测试 UID 的生成
    private ExecutorService execService = Executors.newFixedThreadPool(50);

    // 生成一个 UID
    @Test
    void uidGenerate() {
        long id = uidGenerator.nextId("order");
        System.out.println(id);
    }

    @Test
    void uidBatch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(30);
        Runnable task = () -> {
            for (int i = 0; i < 10; i++) {
                System.out.println("UID: " + uidGenerator.nextId("order"));
            }
            latch.countDown();
        };
        long beginTime = System.currentTimeMillis();
        // 将 task 重复提交 30 次，交给线程池中的线程去执行
        for (int i = 0; i < 30; i++) {
            execService.submit(task);
        }
        latch.await();      // 等待 latch 中的线程全部执行完毕
        long endTime = System.currentTimeMillis();
        long timeUsed = endTime - beginTime;
        System.out.println("完成时间：" + timeUsed + "ms");
    }

    @Test
    void currentTime() {
        long l = System.currentTimeMillis();
        System.out.println(l);
    }
}
