package ru.netology.cloudService.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.netology.cloudService.service.TokenBlacklistService;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "auth-token";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String header = request.getHeader(HEADER);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String token = normalize(header);

        if (token != null && !tokenBlacklistService.isRevoked(token) && jwtService.isValid(token)) {
            Claims claims = jwtService.parse(token);
            Long userId = claims.get("userId", Long.class);
            var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }

    private String normalize(String header) {
        if (header == null) return null;
        String t = header.trim();
        if (t.isEmpty()) return null;
        if (t.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return t.substring(BEARER.length()).trim();
        }
        return t;
    }
}