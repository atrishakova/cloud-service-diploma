package ru.netology.cloudService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.netology.cloudService.service.AuthService;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "auth-token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Предварительные запросы CORS пропускаем напрямую
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 2. /login не требует токена
        if (path.endsWith("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String raw = request.getHeader("auth-token");
        String token = normalize(raw);

        Optional<Long> userId = token != null ? authService.resolveUserId(token) : Optional.empty();

        if (userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Unauthorized\",\"id\":401}");
            return;
        }

        request.setAttribute("userId", userId.get());
        filterChain.doFilter(request, response);
    }

    /** Поддерживает и «чистый» токен, и «Bearer <token>». */
    private String normalize(String header) {
        if (header == null) return null;
        String trimmed = header.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return trimmed.substring(BEARER_PREFIX.length()).trim();
        }
        return trimmed;
    }
}