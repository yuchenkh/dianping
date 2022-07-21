package com.example.dianping.utils;

/**
 * Redis 相关常量。最后编辑于：2022-4-25。
 *
 * @author yuchen
 */
public interface RedisConstants {

    // 手机登录验证码相关
    String PIN_KEY_PREFIX = "dp:login:pin:";
    Long PIN_TTL = 2L;

    // 已登录用户相关
    String LOGGED_IN_USER_PREFIX = "dp:logged-in-user:";
    Long LOGGED_IN_USER_TTL = 30L;

    // 商户信息缓存相关
    String SHOP_KEY_PREFIX = "dp:cache:shop:";
    String SHOP_LOCK_KEY_PREFIX = "dp:lock:shop:";      // 写入商户信息的缓存的锁
    String HOT_KEY_PREFIX = "dp:hotkey:shop:";
    Long SHOP_CACHE_TTL = 30L;
    Long NULL_CACHE_TTL = 2L;
    Long SHOP_LOCK_TTL = 1L;

    // 商户类别相关
    String SHOP_TYPES_KEY = "dp:cache:shop-types";

    // 分布式锁相关
    String LOCK_KEY_PREFIX = "dp:lock:";

    // 秒杀优惠券相关
    String STOCK_KEY_PREFIX = "dp:voucher:stock:";
    String BUYER_LIST_KEY_PREFIX = "dp:voucher:buyer-list:";
    String ORDER_QUEUE_KEY = "dp:voucher:orders";

    // 点赞相关
    String BLOG_LIKED_BY_KEY = "dp:blog:liked-by:";

    // 关注列表相关
    String FOLLOWING_SET_KEY = "dp:user:following:";

}
