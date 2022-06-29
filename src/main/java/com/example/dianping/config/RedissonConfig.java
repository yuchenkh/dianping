package com.example.dianping.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 的相关配置。创建于：2022-6-23。
 * @author yuchen
 */
@Configuration
public class RedissonConfig {

//    @Value("${my.redisson.redis-address}")
//    private String address = "redis://101.35.16.16:6379";
//
//    @Value("${my.redisson.redis-password}")
//    private String password = "yc8234636";
//
//    @Bean
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        config.useSingleServer()
//                .setAddress(address)
//                .setPassword(password);
//
//        return Redisson.create(config);
//    }
}
