package com.daf360.portal.controller;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.dto.AuthUserDto;
import com.daf360.portal.service.JwtService;
import com.daf360.portal.service.UserRoleService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;

/**
 * SSO bridge endpoint: user arrives here already authenticated via the portal
 * OAuth2 session, and this controller issues a fresh JWT then redirects the
 * browser to the target app's /auth/callback?token=…
 *
 * Flow (already logged in to portal):
 *   RH guard → browser navigates to /sso/rh → this controller fires
 *   → JWT generated → redirect to http://localhost:4201/auth/callback?token=…
 *
 * Flow (not yet logged in):
 *   RH guard → browser navigates to /sso/rh → Spring Security saves request
 *   → Azure login → AzureOAuth2SuccessHandler detects saved /sso/rh request
 *   → redirects directly to http://localhost:4201/auth/callback?token=…
 *   (this controller is never reached in that case)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SsoController {

    private final JwtService      jwtService;
    private final UserRoleService userRoleService;
    private final AppProperties   appProperties;

    @GetMapping("/sso/rh")
    public void redirectToRh(Authentication authentication,
                             HttpServletResponse response) throws IOException {
        String token = buildToken(authentication);
        String target = appProperties.getApps().getRhUrl() + "/auth/callback?token=" + token;
        log.info("SSO → RH app for user={}", authentication.getName());
        response.sendRedirect(target);
    }

    private String buildToken(Authentication authentication) {
        OidcUser oidc = (OidcUser) authentication.getPrincipal();
        List<String> roles = userRoleService.getRoles(oidc.getClaimAsString("oid"));
        AuthUserDto user = AuthUserDto.builder()
                .oid(oidc.getClaimAsString("oid"))
                .email(oidc.getClaimAsString("preferred_username"))
                .name(oidc.getClaimAsString("name"))
                .roles(roles)
                .build();
        return jwtService.generateToken(user);
    }
}
