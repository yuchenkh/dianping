package com.example.dianping;

import com.example.dianping.utils.HotKey;
import com.example.dianping.utils.RedisCacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisCacheClientTests {

    @Autowired
    RedisCacheClient client;

    @Test
    void set() {
        Map<String, Integer> blog = new HashMap<>();
        blog.put("shopId", 10);
        blog.put("userId", 3);
        client.set("blog1", blog, 1L, TimeUnit.MINUTES);
    }

    @Test
    void setHotKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("age", 23);
        map.put("id", 1);
        client.setHotKey("hotkey1", map, 30L, TimeUnit.SECONDS);
    }

    @Test
    void get() {
        HotKey ans = client.get("dp:hotkey:shop:1", HotKey.class);
        System.out.println(ans);
    }
}
