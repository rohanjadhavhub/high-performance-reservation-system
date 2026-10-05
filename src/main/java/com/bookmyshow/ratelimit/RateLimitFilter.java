package com.bookmyshow.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter - implements admission control via Token Bucket.
 *
 * For high-demand show_ids, excess traffic is rejected with 429 Too Many Requests,
 * protecting the Redis locking mechanism from thundering herds.
 *
 * Uses per-show-id buckets for fine-grained control.
 */
@Slf4j
@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Value("${booking.rate-limit.capacity:100}")
    private long capacity;

    @Value("${booking.rate-limit.refill-tokens:10}")
    private long refillTokens;

    @Value("${booking.rate-limit.refill-interval-ms:1000}")
    private long refillIntervalMs;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Only rate-limit reservation and payment endpoints (mutation paths)
        if (path.startsWith("/api/v1/reservations") || path.startsWith("/api/v1/payments")) {
            String bucketKey = resolveBucketKey(httpRequest);
            TokenBucket bucket = buckets.computeIfAbsent(bucketKey,
                k -> new TokenBucket(capacity, refillTokens, refillIntervalMs));

            if (!bucket.tryConsume()) {
                log.warn("Rate limited request: path={}, bucketKey={}", path, bucketKey);
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please try again shortly.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Determine the bucket key for rate limiting.
     * Uses the client IP as a basic key. In production, this would use
     * authenticated user ID or a combination of IP + show_id.
     */
    private String resolveBucketKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
