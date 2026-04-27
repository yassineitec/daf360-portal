package com.daf360.portal.security;

import com.daf360.portal.dto.PortalUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import java.io.IOException;

/**
 * Runs immediately after a successful Azure AD login.
 *
 * Current behaviour:
 *  - Logs the login event
 *  - Redirects to /dashboard (or to the originally requested URL)
 *
 * Future (SSO Phase 2):
 *  - Generate a short-lived JWT for cross-app navigation
 *  - Store it in an HttpOnly cookie
 */
@Slf4j
@Component
public class LoginSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    public LoginSuccessHandler() {
        // Default redirect if no saved request exists
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(false); // respect saved URL
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof PortalUser user) {
            log.info("LOGIN SUCCESS | user={} | oid={} | ip={}",
                user.getPreferredUsername(),
                user.getAzureOid(),
                request.getRemoteAddr());
        }

        // ── SSO Phase 2 – uncomment when ready ──────────────────
        // String ssoToken = jwtService.generatePortalToken(user);
        // Cookie ssoCookie = new Cookie("daf360-sso", ssoToken);
        // ssoCookie.setHttpOnly(true);
        // ssoCookie.setSecure(true);
        // ssoCookie.setPath("/");
        // ssoCookie.setMaxAge((int) portalProperties.getJwt().getExpirationSeconds());
        // response.addCookie(ssoCookie);
        // ────────────────────────────────────────────────────────

        // Redirect to dashboard (or originally requested URL)
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
