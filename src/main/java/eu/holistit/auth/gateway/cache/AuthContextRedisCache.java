package eu.holistit.auth.gateway.cache;

import eu.holistit.auth.core.cache.AuthContextCacheKeys;
import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

public class AuthContextRedisCache {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final HolistitAuthGatewayProperties properties;

    public AuthContextRedisCache(
            ReactiveStringRedisTemplate redisTemplate,
            HolistitAuthGatewayProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public Mono<String> findAuthContextToken(
            String sourceTokenHash,
            Long organizationId,
            Long tenantId) {
        String key = AuthContextCacheKeys.tokenKey(
                properties.redis().tokenPrefix(),
                sourceTokenHash,
                organizationId,
                tenantId);

        return redisTemplate.opsForValue().get(key);
    }
}