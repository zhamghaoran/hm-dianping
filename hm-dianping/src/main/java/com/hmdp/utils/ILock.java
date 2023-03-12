package com.hmdp.utils;

/**
 * @author 20179
 */
public interface ILock {
    /**
     * 尝试获取分布式锁
     * @param timeoutSec 锁持有时间，过期后自动释放
     *
     * @return true 表示获取锁成功，false表示获取锁失败
     */
    boolean tryLock(Long timeoutSec);
    void unlock();
}
