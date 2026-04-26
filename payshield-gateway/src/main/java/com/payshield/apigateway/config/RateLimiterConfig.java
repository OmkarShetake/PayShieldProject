package com.payshield.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory sliding-window rate limiter (no Redis required).
 *
 * Limits per client IP:
 *  - /api/auth/**  → 20 requests per minute  (brute-force protection)
 *  - all others    → 200 requests per minute
 *
 * Uses a simple token-bucket approach with per-IP counters reset every minute.
 */
@Component
@Slf4j
public class RateLimiterConfig implements GlobalFilter, Ordered {

    private static final int AUTH_LIMIT    = 20;
    private static final int DEFAULT_LIMIT = 200;
    private static final long WINDOW_MS    = 60_000L; // 1 minute

    private record Counter(AtomicInteger count, AtomicLong windowStart) {}

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Skip rate limiting for OPTIONS preflight
        if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String ip = getClientIp(exchange);
        String path = exchange.getRequest().getPath().value();
        int limit = path.startsWith("/api/auth") ? AUTH_LIMIT : DEFAULT_LIMIT;

        String key = ip + ":" + (path.startsWith("/api/auth") ? "auth" : "api");
        Counter counter = counters.computeIfAbsent(key,
                k -> new Counter(new AtomicInteger(0), new AtomicLong(System.currentTimeMillis())));

        long now = System.currentTimeMillis();
        // Reset window if expired
        if (now - counter.windowStart().get() > WINDOW_MS) {
            counter.windowStart().set(now);
            counter.count().set(0);
        }

        int current = counter.count().incrementAndGet();
        if (current > limit) {
            log.warn("Rate limit exceeded for IP={} path={} count={}/{}", ip, path, current, limit);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            return exchange.getResponse().setComplete();
        }

        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(limit - current));
        return chain.filter(exchange);
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() { return -50; } // run before JwtAuthFilter
}
