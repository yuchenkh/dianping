package com.example.dianping.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 为解决缓存击穿问题所使用的包含逻辑过期时间字段的热点 key。最后编辑于：2022-4-21。
 * @author yuchen 
 */
@Data
@AllArgsConstructor
public class HotKey {

    /**
     * 原数据
     */
    private Object data;

    /**
     * 逻辑过期时间
     */
    private LocalDateTime expireTime;
}
