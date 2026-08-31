package io.autoflow.order.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@EnableScheduling
public class OrderConfiguration {
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    RedisCacheManagerBuilderCustomizer cacheCustomizer(ObjectMapper objectMapper) {
        var serializer = new GenericJackson2JsonRedisSerializer(objectMapper.copy()
            .activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY));
        var config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl((key, value) -> Duration.ofSeconds(ThreadLocalRandom.current().nextLong(540, 661)))
            .prefixCacheNameWith("autoflow:order:")
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();
        return builder -> builder.cacheDefaults(config);
    }
}
