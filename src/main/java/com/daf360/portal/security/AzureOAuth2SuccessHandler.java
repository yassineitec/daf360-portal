package com.daf360.portal.security;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.dto.AuthUserDto;
import com.daf360.portal.service.JwtService;
import com.daf360.portal.service.UserRoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Called by Spring Security after a successful Microsoft 365 login.
 *
 * What it does:
 *  1. Extracts user info from the Azure AD OIDC token (oid, email, name)
 *  2. Loads roles from your database via UserRoleService
 *  3. Signs an internal JWT token containing user info + roles
 *  4. Redirects Angular to: {frontendUrl}/auth/callback?token=<jwt>
 *
 * Angular picks up the token from the URL, stores it in memory (not localStorage),
 * and sends it as Authorization: Bearer <token> on every API call.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService       jwtService;
    private final UserRoleService  userRoleService;
    private final AppProperties    appProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest  request,
            HttpServletResponse response,
            Authentication      authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        // ── 1. Extract claims from Azure AD OIDC token ────────────────────
        String oid   = oidcUser.getClaimAsString("oid");               // Object ID — permanent
        String email = oidcUser.getClaimAsString("preferred_username"); // user@company.com
        String name  = oidcUser.getClaimAsString("name");              // Display name

        log.info("Azure AD login success — OID: {}, email: {}", oid, email);

        // ── 2. Load roles from your database ─────────────────────────────
        List<String> roles = userRoleService.getRoles(oid);
        log.debug("Roles for {}: {}", email, roles);

        // ── 3. Build our internal AuthUserDto ─────────────────────────────
        AuthUserDto user = AuthUserDto.builder()
            .oid(oid)
            .email(email)
            .name(name)
            .roles(roles)
            .build();

        // ── 4. Sign the internal JWT ──────────────────────────────────────
        String token = jwtService.generateToken(user);

        // ── 5. Redirect Angular to the callback URL with the token ────────
        // Angular reads ?token= from the URL, stores it, then removes it from URL
        String redirectUrl = appProperties.getFrontendUrl()
            + "/auth/callback?token=" + token;

        log.debug("Redirecting to Angular callback: {}", appProperties.getFrontendUrl() + "/auth/callback");

        // Clear the authentication session (we are stateless after this)
        clearAuthenticationAttributes(request);

        response.sendRedirect(redirectUrl);
    }
}
