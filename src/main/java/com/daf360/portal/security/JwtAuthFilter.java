package com.daf360.portal.security;

import com.daf360.portal.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    /**
     * Skip JWT processing for OAuth2 paths.
     *
     * The OAuth2 authorization flow (/oauth2/authorization/**) and callback
     * (/login/oauth2/code/**) rely on session-based CSRF state management.
     * If the JWT filter sets the SecurityContext on those paths it interferes
     * with Spring Security's state validation, causing login?error=oauth2.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/oauth2/")
            || uri.startsWith("/login/oauth2/")
            || uri.startsWith("/auth/callback");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        extractToken(request).ifPresent(token -> {
            try {
                Claims claims = jwtTokenService.parseToken(token);
                List<SimpleGrantedAuthority> authorities = extractAuthorities(claims);

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                log.debug("JWT rejected for {}: {}", request.getRequestURI(), e.getMessage());
                SecurityContextHolder.clearContext();
            }
        });

        chain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        // 1. HttpOnly cookie (primary path for browser clients)
        if (request.getCookies() != null) {
            Optional<String> cookie = Arrays.stream(request.getCookies())
                .filter(c -> "daf360_access".equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
            if (cookie.isPresent()) return cookie;
        }

        // 2. Authorization: Bearer <token> (service-to-service calls)
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object perms = claims.get("permissions");
        if (perms instanceof List<?> list) {
            return list.stream()
                .map(p -> new SimpleGrantedAuthority(p.toString()))
                .toList();
        }
        return List.of();
    }
}
