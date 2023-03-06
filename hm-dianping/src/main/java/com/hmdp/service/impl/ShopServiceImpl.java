package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) throws InterruptedException {
        // 缓存穿透
//        Shop shop = queryWithPassThrough(id);

        // 缓存击穿
        // 互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        // 使用工具类来实现查询
        Shop shop = cacheClient.queryWithLogicExpire(CACHE_SHOP_KEY,id, Shop.class, this::getById,20L,TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

//    public Shop queryWithMutex(Long id) throws InterruptedException {
//        String key = "cache:shop:" + id;
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(shopJson)) {
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        if (shopJson != null) {  // 说明我们获取到的key为""空字符串，是我们手动为了防止缓存击穿而创建的
//            return null;
//        }
//        String lockKey = "lock:shop:" + id;
//        // 获取互斥锁
//        boolean islock = tryLock(lockKey);
//        if (!islock) {
//            // 失败，则休眠并重试
//            Thread.sleep(50);
//            return queryWithMutex(id);
//        }
//        // 判断是否获取成功
//
//        // 成功、根据id 查询数据库
//        Shop shop = getById(id);
//        //  模拟重建演示
//        Thread.sleep(200);
//        if (shop == null) {
//            stringRedisTemplate.opsForValue().set(key,"",5,TimeUnit.MINUTES);
//            return null;
//        }
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),30, TimeUnit.MINUTES);
//        unlock(lockKey);
//        return shop;
//    }
//
//    public Shop queryWithPassThrough(Long id) {
//        String key = "cache:shop:" + id;
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(shopJson)) {
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        if (shopJson != null) {
//            return null;
//        }
//        Shop shop = getById(id);
//        if (shop == null) {
//            stringRedisTemplate.opsForValue().set(key,"",5,TimeUnit.MINUTES);
//            return null;
//        }
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),30, TimeUnit.MINUTES);
//        return shop;
//    }

    /**
     *采取更新数据库就删除缓存的解决方式
     * 如果更新了数据库的同时也更新缓存，但是更新缓存之后可能之后不会有查询操作，因此会导致浪费
     * 所以我们更新数据库之后就删除缓存，之后查询的时候再把数据添加到缓存中去。
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
