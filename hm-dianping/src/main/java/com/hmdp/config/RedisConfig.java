package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 20179
 */
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

    private final String AGREEMENT = "redis://";
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 设置单节点的redis地址
        config.useSingleServer().setAddress(AGREEMENT + host + ":" + port);
        return Redisson.create(config);
    }
}
