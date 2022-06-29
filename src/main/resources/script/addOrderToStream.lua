---
--- 将待创建的订单任务加入 Redis 的 stream。
--- Created by yuchen.
--- DateTime: 6/28/22 9:55 AM
---

local key = KEYS[1]
local orderId = ARGV[1]
local voucherId = ARGV[2]
local userId = ARGV[3]

redis.call('XADD', key, "*", "order-id", orderId, "voucher-id", voucherId, "user-id", userId)