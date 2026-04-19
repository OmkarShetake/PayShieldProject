package com.payshield.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = parseToken(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                // Forward user info to downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Email", username)
                        .header("X-User-Role", role != null ? role : "")
                        .build();

                log.debug("JWT validated for user: {} role: {}", username, role);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                return unauthorized(exchange, "Invalid or expired token");
            }
        };
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        var body = ("{\"status\":401,\"error\":\"" + message + "\"}").getBytes();
        var buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {}
}
