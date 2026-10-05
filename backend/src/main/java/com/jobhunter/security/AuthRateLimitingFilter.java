package com.jobhunter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Defensive Rate Limiting Filter to prevent brute-force and credential stuffing attacks.
 * Limits authentication attempts per remote IP to a safe sliding threshold (15 attempts / minute).
 */
@Component
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS_PER_MINUTE = 15;
    private static final long WINDOW_MS = 60_000L;

    private static class RequestWindow {
        long windowStart;
        AtomicInteger count;

        RequestWindow(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestWindow> ipRateMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod()) && (path.endsWith("/api/auth/login") || path.endsWith("/api/auth/register"))) {
            String clientIp = getClientIp(request);
            long now = System.currentTimeMillis();

            RequestWindow window = ipRateMap.compute(clientIp, (ip, current) -> {
                if (current == null || (now - current.windowStart) > WINDOW_MS) {
                    return new RequestWindow(now);
                } else {
                    current.count.incrementAndGet();
                    return current;
                }
            });

            if (window.count.get() > MAX_ATTEMPTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", "60");

                ApiResponse<Void> apiResponse = ApiResponse.error("Too many authentication attempts. Please wait 60 seconds before trying again.");
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
