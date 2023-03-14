-- 参数列表
-- 优惠券id
local voucherId = ARGV[1]

-- 用户id
local userId = ARGV[2]
-- 订单id
local orderId = ARGV[3]

-- 2.数据key
-- 2.1库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2订单Key
local orderKey = 'seckill:order:' .. voucherId

-- 3.脚本业务
-- 3.1判断库存是否充足 get stockKey
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 3.2 库存不足
    return 1
end
-- 3.2 判断下单用户是否下单SISMEMBER orderKey userId
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 3.3 存在，说明是重复下单
    return 2
end
-- 3.4扣库存 incrby stockKey -1
redis.call("incrby", stockKey, -1)
-- 3.5下单(保存客户) sadd orderKey userId
redis.call('sadd', orderKey, userId)
-- 3.6 发送消息到消息队列中 ，XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd', 'stream.order', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
return 0