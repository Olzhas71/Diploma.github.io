package com.parking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP token-bucket rate limiter for {@code /auth/*} endpoints — protects
 * login/register from brute-force without an external dependency.
 *
 * Defaults: 10 requests / 60 s per IP. Configurable via:
 *   {@code app.security.rate-limit.auth-requests-per-window}
 *   {@code app.security.rate-limit.auth-window-seconds}
 *
 * In-memory only — fine for a single instance. For multi-instance deployments
 * swap the {@link #buckets} map for a Redis-backed counter.
 */
@Slf4j
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final int permits;
    private final Duration window;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.security.rate-limit.auth-requests-per-window:10}") int permits,
            @Value("${app.security.rate-limit.auth-window-seconds:60}") int windowSeconds) {
        this.permits = permits;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only rate-limit the auth endpoints. Context path is stripped from servletPath.
        String path = request.getServletPath();
        return !path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String key = clientKey(request);
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            Bucket b = existing == null ? new Bucket(permits, Instant.now().plus(window)) : existing;
            return b.tryAcquire(permits, window);
        });
        long remaining = bucket.remaining;
        response.setHeader("X-RateLimit-Limit",     String.valueOf(permits));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
        response.setHeader("X-RateLimit-Reset",     String.valueOf(bucket.resetAt.getEpochSecond()));
        if (remaining < 0) {
            log.warn("Rate limit hit for {} on {}", key, request.getServletPath());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","message":"Too many auth attempts; slow down."}""");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private static final class Bucket {
        final long remaining;
        final Instant resetAt;

        Bucket(long remaining, Instant resetAt) {
            this.remaining = remaining;
            this.resetAt = resetAt;
        }

        Bucket tryAcquire(int permits, Duration window) {
            Instant now = Instant.now();
            if (now.isAfter(resetAt)) {
                return new Bucket(permits - 1, now.plus(window));
            }
            return new Bucket(remaining - 1, resetAt);
        }
    }
}
