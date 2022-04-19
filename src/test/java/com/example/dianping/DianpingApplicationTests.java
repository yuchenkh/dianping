package com.example.dianping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class DianpingApplicationTests {

    @Autowired
    StringRedisTemplate redisTemplate;


    // 删除 Redis 中不存在的 key 会报错吗？
    // 不会，会返回 false
    @Test
    void redisTest() {
        Boolean result = redisTemplate.delete("123");
        System.out.println(result);
    }
}
