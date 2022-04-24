package com.example.dianping.utils;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 基于 Redis 的全局唯一 ID 生成器。创建于：2022-4-23。
 * @author yuchen
 */
@Component
@AllArgsConstructor
public class UIDGenerator {

    /**
     * 设定的起始时间（2022 年 1 月 1 日 00:00，Unix time）
     */
    private static final long BEGIN_EPOCH_TIME = 1640966400L;

    /**
     * UID 中「序号」占的位数
     */
    private static final int SERIAL_NUM_BITS = 32;

    private final StringRedisTemplate redisTemplate;

    /**
     * 获取某一类实体的下一个全局唯一 ID。
     * @param entityName    类名，用于组成 Redis 中的 key
     * @return              全局唯一 ID
     */
    public long nextId(String entityName) {
        // 时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowEpochTime = now.toEpochSecond(ZoneOffset.ofHours(8));       // 将当前时间转换为 Unix time (epoch time)
        long timeDiff = nowEpochTime - BEGIN_EPOCH_TIME;

        // 序号
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));      // 将当前时间转换为"年月日"的字符串形式
        String key = "dp:serial-number:" + entityName + ":" + date;             // Redis 中的 key，其值代表这类对象在当天的创建个数
        Long serialNumber = redisTemplate.opsForValue().increment(key);         // 将 key 的值加 1

        return timeDiff << SERIAL_NUM_BITS | serialNumber;
    }

    public static void main(String[] args) {
        long nowEpochTimestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
        System.out.println(nowEpochTimestamp);

        long beginEpochTimestamp = LocalDateTime.of(2022, 1, 1, 0, 0).toEpochSecond(ZoneOffset.ofHours(8));
        System.out.println(beginEpochTimestamp);

    }
}
