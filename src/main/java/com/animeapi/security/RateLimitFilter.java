package com.animeapi.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra brute force no endpoint de autenticação.
 * Limita a 10 tentativas por minuto por IP.
 *
 * IMPORTANTE: adicione ao pom.xml:
 * <dependency>
 *   <groupId>com.bucket4j</groupId>
 *   <artifactId>bucket4j-core</artifactId>
 *   <version>8.7.0</version>
 * </dependency>
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    // Mapa IP → bucket de tokens
    // Em produção com múltiplas instâncias, substitua por Bucket4j + Redis
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        // 10 requisições por minuto, recarrega gradualmente
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket getBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> createBucket());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        // Aplica rate limit apenas nas rotas de autenticação
        if (request.getRequestURI().startsWith("/api/auth/")) {
            String ip = getClientIp(request);
            Bucket bucket = getBucket(ip);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP: {}", ip);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"status\":429,\"message\":\"Too many requests. Try again in a minute.\"}"
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Extrai o IP real do cliente, considerando proxies reversos (Nginx, Cloudflare, etc.)
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For pode conter múltiplos IPs: "client, proxy1, proxy2"
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}