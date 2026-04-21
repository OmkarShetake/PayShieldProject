package com.payshield.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway access log — logs every request entering PayShield.
 * Format: [METHOD] /path → SERVICE → STATUS in Xms
 */
@Component
@Slf4j
public class AccessLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest  req   = exchange.getRequest();
        String method = req.getMethod() != null ? req.getMethod().name() : "?";
        String path   = req.getURI().getPath();
        long   start  = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse res      = exchange.getResponse();
            long duration = System.currentTimeMillis() - start;
            int  status   = res.getStatusCode() != null
                    ? res.getStatusCode().value() : 0;

            // Skip actuator noise
            if (path.contains("/actuator")) return;

            if (status >= 500) {
                log.error("GATEWAY | {} {} → {} in {}ms", method, path, status, duration);
            } else if (status >= 400) {
                log.warn ("GATEWAY | {} {} → {} in {}ms", method, path, status, duration);
            } else {
                log.info ("GATEWAY | {} {} → {} in {}ms", method, path, status, duration);
            }
        }));
    }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
