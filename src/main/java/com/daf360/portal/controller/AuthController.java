package com.daf360.portal.controller;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.UserRepository;
import com.daf360.portal.service.AuditLogService;
import com.daf360.portal.service.JwtTokenService;
import com.daf360.portal.service.UserSyncService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppProperties props;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final UserSyncService userSyncService;
    private final AuditLogService auditLogService;

    /** Redirect browser to Azure AD login */
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/azure");
    }

    /** Status health-check — no auth required */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "daf360-portal"));
    }

    /**
     * Refresh: validates portal refresh cookie, issues new access JWT.
     * Rotates refresh token on every call to prevent replay attacks.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "daf360_refresh").orElse(null);
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No refresh token"));
        }

        // findByRefreshToken looks up the `refresh_token` column (portal UUID)
        User user = userRepository.findByRefreshToken(refreshToken).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            clearAuthCookies(response);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }

        List<String> permissions = userSyncService.extractPermissions(user);
        String newAccessJwt = jwtTokenService.generateAccessToken(
            user.getId(), user.getAzureOid(), user.getEmail(),
            user.getRole() != null ? user.getRole().getId() : null,
            user.getPaysId(), permissions
        );

        // Rotate refresh token
        String newRefresh = jwtTokenService.generateRefreshToken();
        user.setRefreshToken(newRefresh);
        userRepository.save(user);

        response.addCookie(jwtTokenService.buildAccessCookie(newAccessJwt));
        response.addCookie(jwtTokenService.buildRefreshCookie(newRefresh));

        return ResponseEntity.ok(Map.of("refreshed", true));
    }

    /** Logout: clear cookies, invalidate refresh token in DB, write audit */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        extractCookie(request, "daf360_access").ifPresent(token -> {
            try {
                Claims claims = jwtTokenService.parseToken(token);
                Long userId = Long.parseLong(claims.getSubject());

                // Clear `refresh_token` column — UUID can no longer be replayed
                userRepository.clearRefreshToken(userId);

                String ip = getClientIp(request);
                auditLogService.log("LOGOUT", "PORTAL", String.valueOf(userId), ip, null);
            } catch (Exception e) {
                log.debug("Logout — could not parse token: {}", e.getMessage());
            }
        });

        clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("loggedOut", true));
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addCookie(jwtTokenService.buildClearAccessCookie());
        response.addCookie(jwtTokenService.buildClearRefreshCookie());
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
            .filter(c -> name.equals(c.getName()))
            .map(Cookie::getValue)
            .filter(v -> v != null && !v.isBlank())
            .findFirst();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
