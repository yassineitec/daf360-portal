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
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
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

    private static final HttpSessionRequestCache REQUEST_CACHE = new HttpSessionRequestCache();

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
        String oid   = oidcUser.getClaimAsString("oid");
        String email = oidcUser.getClaimAsString("preferred_username");
        String name  = oidcUser.getClaimAsString("name");

        log.info("Azure AD login success — OID: {}, email: {}", oid, email);

        // ── 2. Load roles ─────────────────────────────────────────────────
        List<String> roles = userRoleService.getRoles(oid);

        // ── 3. Sign the internal JWT ──────────────────────────────────────
        String token = jwtService.generateToken(
            AuthUserDto.builder().oid(oid).email(email).name(name).roles(roles).build());

        // ── 4. Choose redirect target ─────────────────────────────────────
        // If login was triggered by a cross-app SSO request (e.g. /sso/rh),
        // send the JWT directly to that app's callback rather than the portal.
        String callbackBase = resolveCallbackBase(request, response);

        clearAuthenticationAttributes(request);
        log.debug("Post-login redirect → {}/auth/callback", callbackBase);
        response.sendRedirect(callbackBase + "/auth/callback?token=" + token);
    }

    /**
     * Checks if the login was originally triggered by an /sso/* saved request
     * and returns the matching app's frontend URL. Falls back to the portal URL.
     */
    private String resolveCallbackBase(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = REQUEST_CACHE.getRequest(request, response);
        if (saved != null) {
            String savedUrl = saved.getRedirectUrl();
            if (savedUrl != null && savedUrl.contains("/sso/rh")) {
                REQUEST_CACHE.removeRequest(request, response);
                return appProperties.getApps().getRhUrl();
            }
        }
        return appProperties.getFrontendUrl();
    }
}
