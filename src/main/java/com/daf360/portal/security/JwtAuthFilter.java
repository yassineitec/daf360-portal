package com.daf360.portal.security;

import com.daf360.portal.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads the internal JWT from the Authorization: Bearer <token> header
 * on every request to /api/** endpoints.
 *
 * When Angular sends the JWT (obtained after OAuth2 login), this filter:
 *  1. Extracts and validates the token
 *  2. Loads the user's roles from the token claims
 *  3. Sets the SecurityContext so @PreAuthorize annotations work
 *
 * Note: This filter handles the PORTAL's own JWT tokens (signed by JwtService).
 * It is separate from Azure AD validation — the portal signs its own tokens
 * that it issues to Angular.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Skip if no token present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtService.isTokenValid(token)) {
                Claims claims = jwtService.parseToken(token);

                // Build Spring authorities from the roles claim
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                        .collect(Collectors.toList());

                // Set authentication in the security context
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),   // principal = OID
                        null,
                        authorities
                    );
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("JWT authenticated: OID={}", claims.getSubject());
            }
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            // Do not set authentication — request continues unauthenticated
        }

        filterChain.doFilter(request, response);
    }

    /** Only apply this filter to /api/** paths */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
