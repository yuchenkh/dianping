package com.example.dianping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;

@SpringBootTest
public class LuaScriptTests {

    private static final DefaultRedisScript<Long> CHECKING_SCRIPT;

    @Autowired
    StringRedisTemplate redisTemplate;

    static {
        CHECKING_SCRIPT = new DefaultRedisScript<>();
        CHECKING_SCRIPT.setLocation(new ClassPathResource("script/stockAndOrderChecking.lua"));      // 导入 Lua 脚本
        CHECKING_SCRIPT.setResultType(Long.class);
    }

    @Test
    void stockChecking() {
        Long result = redisTemplate.execute(CHECKING_SCRIPT,
                Arrays.asList("dp:voucher:stock:13", "dp:voucher:buyer-list:13", "dp:voucher:orders"),
                "3010101",
                "13",
                "1106"
        );
        System.out.println(result);
    }
}
