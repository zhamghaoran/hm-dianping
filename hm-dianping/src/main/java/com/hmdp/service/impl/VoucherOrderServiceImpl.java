package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.support.incrementer.SybaseAnywhereMaxValueIncrementer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service

public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Result seckillVoucher(Long voucherId) throws InterruptedException {
        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 判断秒杀是否开始结束
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 秒杀已经结束
            return Result.fail("秒杀已经结束");
        }
        // 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足!");
        }
        Long userId = UserHolder.getUser().getId();
        // 创建锁对象
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, "order" + userId);
        RLock lock = redissonClient.getLock("order" + userId);
        boolean isLock = lock.tryLock(1,TimeUnit.SECONDS);
        if (!isLock) {
            // 锁失败，返回或重试
            return Result.fail("一个人只允许下一单");
        }

        // 获取代理对象(事务)
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId) {

        Long userId = UserHolder.getUser().getId();

        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();

        if (count > 0) {
            return Result.fail("已下单");
        }

        boolean success = seckillVoucherService.update().
                setSql("stock = stock - 1").
                eq("voucher_id", voucherId).
                gt("stock", 0).
                update();

        if (!success) {
            return Result.fail("库存不足!");
        }

        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 订单id

        Long order = redisIdWorker.nextId("order");

        voucherOrder.setId(order);
        voucherOrder.setUserId(userId);
        // 代金券id
        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        // 返回订单id
        return Result.ok(order);
    }

}
