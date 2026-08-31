package io.autoflow.inventory.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class InventoryConfiguration {
    @Bean(destroyMethod = "shutdown")
    RedissonClient redissonClient(@Value("${spring.data.redis.host}") String host,
                                  @Value("${spring.data.redis.port}") int port) {
        var config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }

    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}

