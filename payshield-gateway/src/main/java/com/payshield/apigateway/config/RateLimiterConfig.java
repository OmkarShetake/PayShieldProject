package com.payshield.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Redis-backed rate limiting for the API Gateway.
 *
 * Limits:
 *  - Auth endpoints (login/register): 20 req/s — brute-force protection
 *  - All other endpoints: 100 req/s per IP
 *
 * Uses token bucket algorithm via Redis.
 * Key is resolved from X-Forwarded-For or remote address.
 */
@Configuration
public class RateLimiterConfig {

    /** Rate limiter for auth endpoints — stricter to prevent brute force */
    @Bean
    public RedisRateLimiter authRateLimiter() {
        // replenishRate=20 (tokens/sec), burstCapacity=30, requestedTokens=1
        return new RedisRateLimiter(20, 30, 1);
    }

    /** Rate limiter for general API endpoints */
    @Bean
    public RedisRateLimiter apiRateLimiter() {
        // replenishRate=100 (tokens/sec), burstCapacity=150, requestedTokens=1
        return new RedisRateLimiter(100, 150, 1);
    }

    /** Resolve rate limit key from client IP */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
            }
            // Use only the first IP if X-Forwarded-For has a chain
            return Mono.just(ip.split(",")[0].trim());
        };
    }
}
