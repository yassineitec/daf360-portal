package com.daf360.portal.controller;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.dto.AuthUserDto;
import com.daf360.portal.service.JwtService;
import com.daf360.portal.service.UserRoleService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API endpoints consumed by the Angular frontend.
 *
 * Flow:
 *  1. Angular calls GET /api/auth/login  →  redirected to Microsoft 365
 *  2. After login, Azure redirects back, SuccessHandler sends token to Angular
 *  3. Angular stores the token and calls GET /api/auth/me to verify
 *  4. Angular calls POST /api/auth/logout when the user logs out
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService      jwtService;
    private final UserRoleService userRoleService;
    private final AppProperties   appProperties;

    /**
     * GET /api/auth/login
     * Angular calls this to initiate login.
     * Returns the Microsoft 365 login URL so Angular can redirect.
     * (Spring Security's /oauth2/authorization/azure is the actual trigger)
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> getLoginUrl(HttpServletRequest request) {
        String baseUrl    = request.getScheme() + "://" + request.getServerName()
            + ":" + request.getServerPort();
        String loginUrl   = baseUrl + "/oauth2/authorization/azure";

        return ResponseEntity.ok(Map.of(
            "loginUrl",   loginUrl,
            "provider",   "Microsoft 365",
            "callbackUrl", appProperties.getFrontendUrl() + "/auth/callback"
        ));
    }

    /**
     * GET /api/auth/me
     * Called by Angular after storing the token to verify authentication.
     * Reads the JWT from the Authorization header and returns user info.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        Claims claims = jwtService.parseToken(token);

        AuthUserDto user = AuthUserDto.builder()
            .oid(claims.getSubject())
            .email(claims.get("email", String.class))
            .name(claims.get("name", String.class))
            .roles(claims.get("roles", java.util.List.class))
            .build();

        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/auth/logout
     * Angular calls this when the user clicks logout.
     * Returns the Microsoft 365 logout URL so Angular can redirect there.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        // Invalidate the Spring session (if any)
        request.getSession(false);

        // Return the Azure AD logout URL
        String tenantId = System.getenv().getOrDefault("AZURE_TENANT_ID", "common");
        String frontendUrl = appProperties.getFrontendUrl();
        String logoutUrl = "https://login.microsoftonline.com/" + tenantId
            + "/oauth2/v2.0/logout?post_logout_redirect_uri="
            + frontendUrl;

        return ResponseEntity.ok(Map.of(
            "logoutUrl", logoutUrl,
            "message",   "Logout successful — redirect to Microsoft to complete sign-out"
        ));
    }

    /**
     * GET /api/auth/status
     * Simple health check for the authentication system.
     * Returns 200 with config status — useful for debugging.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean configured = appProperties.getJwtSecret() != null
            && !appProperties.getJwtSecret().isBlank();

        return ResponseEntity.ok(Map.of(
            "service",    "daf360-portal-auth",
            "configured", configured,
            "provider",   "Microsoft Entra ID (Azure AD)",
            "jwtEnabled", configured
        ));
    }
}
