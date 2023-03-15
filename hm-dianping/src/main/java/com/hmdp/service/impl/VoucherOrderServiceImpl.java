package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.OptionalLong;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    @Autowired
    private RedissonClient redissonClient;

    private static ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            while (true){
                // 获取队列中的订单信息
                VoucherOrder voucherOrder = orderTasks.take();
                handlerVoucherOrder(voucherOrder);
                // 创建订单
            }
        }
    }
    public void handlerVoucherOrder(VoucherOrder voucherOrder) {
        save(voucherOrder);
    }
    @Override
    public Result seckillVoucher(Long voucherId) throws InterruptedException {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        System.out.println(userId);
        System.out.println(voucherId);
        Long orderId = redisIdWorker.nextId("order");
        //  执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                orderId.toString()
        );
        //  判断是否为0
        // 不为0 没有购买资格
        assert result != null;
        int r = result.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 为0 有购买资格，将信息保存到队列中
        // todo 保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //  放入队列当中
        orderTasks.add(voucherOrder);
        // 返回订单id
        return Result.ok(orderId);
    }

    @Override
    public Result createVoucherOrder(Long voucherId) {
        return null;
    }


//    @Override
//    public Result seckillVoucher(Long voucherId) throws InterruptedException {
//        // 查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 判断秒杀是否开始结束
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            // 尚未开始
//            return Result.fail("秒杀尚未开始");
//        }
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            // 秒杀已经结束
//            return Result.fail("秒杀已经结束");
//        }
//        // 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足!");
//        }
//        Long userId = UserHolder.getUser().getId();
//        // 创建锁对象
//        SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, "order" + userId);
//        RLock lock = redissonClient.getLock("order" + userId);
//        boolean isLock = lock.tryLock(1,TimeUnit.SECONDS);
//        if (!isLock) {
//            // 锁失败，返回或重试
//            return Result.fail("一个人只允许下一单");
//        }
//
//        // 获取代理对象(事务)
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    @Override
//    @Transactional
//    public Result createVoucherOrder(Long voucherId) {
//
//        Long userId = UserHolder.getUser().getId();
//
//        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//
//        if (count > 0) {
//            return Result.fail("已下单");
//        }
//
//        boolean success = seckillVoucherService.update().
//                setSql("stock = stock - 1").
//                eq("voucher_id", voucherId).
//                gt("stock", 0).
//                update();
//
//        if (!success) {
//            return Result.fail("库存不足!");
//        }
//
//        // 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 订单id
//
//        Long order = redisIdWorker.nextId("order");
//
//        voucherOrder.setId(order);
//        voucherOrder.setUserId(userId);
//        // 代金券id
//        voucherOrder.setVoucherId(voucherId);
//
//        save(voucherOrder);
//
//        // 返回订单id
//        return Result.ok(order);
//    }

}
