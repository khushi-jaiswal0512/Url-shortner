package com.urlshortener.config;

import com.urlshortener.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiting filter using Bucket4j token-bucket algorithm.
 * <p>
 * Limits:
 * - POST /api/v1/urls       → 10 requests/minute per IP
 * - GET  /{shortCode}       → 100 requests/minute per IP
 * - All other endpoints     → no limit
 * <p>
 * Returns HTTP 429 with JSON error body when limit is exceeded.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.create-requests-per-minute:10}")
    private int createLimit;

    @Value("${app.rate-limit.redirect-requests-per-minute:100}")
    private int redirectLimit;

    private final Map<String, Bucket> createBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> redirectBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Rate limit: POST /api/v1/urls
        if ("POST".equalsIgnoreCase(method) && "/api/v1/urls".equals(path)) {
            Bucket bucket = createBuckets.computeIfAbsent(clientIp, this::newCreateBucket);
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP {} on POST /api/v1/urls", clientIp);
                writeRateLimitResponse(response);
                return;
            }
        }

        // Rate limit: GET /{shortCode} (single path segment, not /api/*)
        if ("GET".equalsIgnoreCase(method)
                && !path.startsWith("/api/")
                && !path.startsWith("/swagger")
                && !path.startsWith("/actuator")
                && !path.startsWith("/v3/api-docs")
                && !path.equals("/favicon.ico")
                && path.length() > 1) {
            Bucket bucket = redirectBuckets.computeIfAbsent(clientIp, this::newRedirectBucket);
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP {} on GET {}", clientIp, path);
                writeRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket newCreateBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(createLimit, Refill.greedy(createLimit, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket newRedirectBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(redirectLimit, Refill.greedy(redirectLimit, Duration.ofMinutes(1))))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("""
                {
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Rate limit exceeded. Please try again later.",
                  "timestamp": "%s"
                }
                """.formatted(java.time.LocalDateTime.now()));
    }
}
