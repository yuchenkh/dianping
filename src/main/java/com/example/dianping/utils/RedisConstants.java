package com.example.dianping.utils;

/**
 * Redis 相关常量。最后编辑于：2022-4-18。
 * @author yuchen
 */
public class RedisConstants {

    // 手机登录验证码相关
    public static final String PIN_KEY_PREFIX = "dp:login:pin:";
    public static final Long PIN_TTL = 2L;

    // 用户 token 相关
    public static final String USER_TOKEN_PREFIX = "dp:login:token:";
    public static final Long USER_TOKEN_TTL = 30L;

    // 商户信息缓存相关
    public static final String SHOP_KEY_PREFIX = "dp:cache:shop:";
    public static final Long SHOP_CACHE_TTL = 30L;

    // 商户类别相关
    public static final String SHOP_TYPES_KEY = "dp:cache:shop-types";
}
