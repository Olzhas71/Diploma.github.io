package com.parking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Wires Redis-backed caching only if a {@link RedisConnectionFactory} is on the
 * classpath and active (i.e. when Redis auto-configuration succeeded). If Redis
 * is excluded — e.g. in the {@code dev-embedded} profile — Spring Boot falls
 * back to its default in-memory {@code ConcurrentMapCacheManager}.
 */
@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer(mapper);
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(json);
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer(mapper);
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(json));
        return RedisCacheManager.builder(cf).cacheDefaults(config).build();
    }
}
