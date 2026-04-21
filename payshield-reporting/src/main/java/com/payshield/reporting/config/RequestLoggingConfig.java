package com.payshield.reporting.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every incoming HTTP request with method, URI, status, and duration.
 * Example: POST /api/payments/initiate → 200 in 45ms
 */
@Configuration
@Slf4j
public class RequestLoggingConfig {

    @Bean
    public OncePerRequestFilter requestLoggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain)
                    throws ServletException, IOException {

                long start = System.currentTimeMillis();
                String method = req.getMethod();
                String uri    = req.getRequestURI();

                try {
                    chain.doFilter(req, res);
                } finally {
                    long duration = System.currentTimeMillis() - start;
                    int  status   = res.getStatus();

                    if (uri.contains("/actuator")) {
                        // Skip actuator health-check noise
                        return;
                    }

                    if (status >= 500) {
                        log.error("{} {} → {} in {}ms", method, uri, status, duration);
                    } else if (status >= 400) {
                        log.warn("{} {} → {} in {}ms",  method, uri, status, duration);
                    } else {
                        log.info("{} {} → {} in {}ms",  method, uri, status, duration);
                    }
                }
            }
        };
    }
}
