package com.example.dianping.utils;

/**
 * Redis 相关常量。最后编辑于：2022-4-25。
 * @author yuchen
 */
public class RedisConstants {

    // 手机登录验证码相关
    public static final String PIN_KEY_PREFIX = "dp:login:pin:";
    public static final Long PIN_TTL = 2L;

    // 已登录用户相关
    public static final String LOGGED_IN_USER_PREFIX = "dp:logged-in-user:";
    public static final Long LOGGED_IN_USER_TTL = 30L;

    // 商户信息缓存相关
    public static final String SHOP_KEY_PREFIX = "dp:cache:shop:";
    public static final String SHOP_LOCK_KEY_PREFIX = "dp:lock:shop:";      // 写入商户信息的缓存的锁
    public static final String HOT_KEY_PREFIX = "dp:hotkey:shop:";
    public static final Long SHOP_CACHE_TTL = 30L;
    public static final Long NULL_CACHE_TTL = 2L;
    public static final Long SHOP_LOCK_TTL = 1L;

    // 商户类别相关
    public static final String SHOP_TYPES_KEY = "dp:cache:shop-types";
}
