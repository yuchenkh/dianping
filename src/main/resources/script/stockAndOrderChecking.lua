---
--- 用于优惠券秒杀业务。检查优惠券库存以及用户是否已经购买过该优惠券，如果符合购买资格，则修改 Redis 中的库存信息和该优惠券的买家信息，并向 stream 中添加创建新订单的任务供专门的消费者线程处理。
--- Created by yuchen.
--- DateTime: 6/26/22 12:04 PM
---

--- keys: 库存 key、购买用户列表 key、任务队列 key
--- args：订单 ID、优惠券 ID、用户 ID

local stockKey = KEYS[1]
local buyerListKey = KEYS[2]
local queueKey = KEYS[3]
local orderId = ARGV[1]
local voucherId = ARGV[2]
local userId = ARGV[3]


-- 检查库存
if (tonumber(redis.call('GET', stockKey)) <= 0) then
    return 1
end

-- 检查用户是否购买过该优惠券
if (redis.call('SISMEMBER', buyerListKey, userId) == 1) then
    return 2
end

-- 修改信息
redis.call('INCRBY', stockKey, -1)
redis.call('SADD', buyerListKey, userId)

-- 向 stream 添加订单
redis.call('XADD', queueKey, "*", "id", orderId, "voucherId", voucherId, "userId", userId)

return 0