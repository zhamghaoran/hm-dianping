
-- 获取锁中的线程标识
local id = redis.call('get',KEYS[1])
if (id == ARGV[1]) then
    -- 释放锁
    return redis.call('del',key)
end
return 0