package com.example.dianping;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.example.dianping.dto.UserDTO;
import com.example.dianping.entity.User;
import com.example.dianping.service.IUserService;
import com.example.dianping.utils.UIDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.dianping.utils.RedisConstants.LOGGED_IN_USER_PREFIX;
import static com.example.dianping.utils.RedisConstants.LOGGED_IN_USER_TTL;

@SpringBootTest
@Slf4j
public class VoucherTests {

    @Autowired
    UIDGenerator uidGenerator;

    @Autowired
    IUserService userService;

    @Autowired
    StringRedisTemplate redisTemplate;

    // 创建一个有 500 个线程的线程池，用于测试 UID 的生成
    private final ExecutorService execService = Executors.newFixedThreadPool(50);

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

    @Test
    void logging() {
        String user = "yuchen";
        String voucherId = "12";
        log.debug("用户 {} 成功购买 {} 号优惠券一张", user, voucherId);
        log.debug("你好");
    }

    /**
     * 随机生成 1000 个测试用户
     */
    @Test
    void helper() {

        for (int i = 0; i < 1000; i++) {
            DecimalFormat format = new DecimalFormat("0000");
            String phone = "1380000" + format.format(i);
            User user = new User();
            user.setId((long) (8000+ i));
            user.setPhone(phone);
            user.setNickName(RandomUtil.randomString(6));
            userService.save(user);
        }

    }

    /**
     * 生成 1000 个随机用户的 token 并保存至一个文本文件中
     */
    @Test
    void token() throws IOException {

        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(UUID.randomUUID(false).toString());
        }
        Path file = Paths.get("/Users/yuchen/All/work/learn/redis/dianping/src/main/resources/tokens");
        Files.write(file, tokens, StandardCharsets.UTF_8);
    }

    /**
     * 让 1000 个测试用户登录
     */
    @Test
    void login() throws IOException {
        Path file = Paths.get("/Users/yuchen/All/work/learn/redis/dianping/src/main/resources/tokens");
        List<String> tokens = Files.readAllLines(file);
        for (int i = 0; i < 1000; i++) {
            DecimalFormat format = new DecimalFormat("0000");
            String phone = "1380000" + format.format(i);
            User user = userService.query().eq("phone", phone).one();
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            String token = tokens.get(i);
            Map<String, Object> userMap = BeanUtil.beanToMap(
                    userDTO,
                    new HashMap<>(),
                    CopyOptions.create()
                            .ignoreNullValue()
                            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
            redisTemplate.opsForHash().putAll(LOGGED_IN_USER_PREFIX + token, userMap);
            redisTemplate.expire(LOGGED_IN_USER_PREFIX + token, Duration.ofMinutes(LOGGED_IN_USER_TTL));        // 设置用户 token 过期时间
        }
    }
}
