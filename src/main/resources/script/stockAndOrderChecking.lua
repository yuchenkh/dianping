---
--- 用于优惠券秒杀业务。检查优惠券库存以及用户是否已经购买过该优惠券，如果符合购买资格，则修改 Redis 中的库存信息和该优惠券的买家信息。
--- Created by yuchen.
--- DateTime: 6/26/22 12:04 PM
---
local voucherId = ARGV[1]
local userId = ARGV[2]

-- 检查库存
local stockKey = 'dp:voucher:stock:' .. voucherId
local buyerSetKey = 'dp:voucher:buyer-list:' .. voucherId
local stock = redis.call('GET', stockKey)
if (tonumber(stock) <= 0) then
    return 1
end
-- 检查用户是否购买过该优惠券
local purchasedBefore = redis.call('SISMEMBER', buyerSetKey, userId)
if (purchasedBefore == 1) then
    return 2
end
-- 修改信息
redis.call('INCRBY', stockKey, -1)
redis.call('SADD', buyerSetKey, userId)
return 0