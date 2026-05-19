package com.daf360.portal.security;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.UserRepository;
import com.daf360.portal.service.AuditLogService;
import com.daf360.portal.service.JwtTokenService;
import com.daf360.portal.service.UserSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AppProperties props;
    private final UserSyncService userSyncService;
    private final JwtTokenService jwtTokenService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid principal type");
            return;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthToken.getName()
        );

        String ms365AccessToken  = client != null && client.getAccessToken() != null
            ? client.getAccessToken().getTokenValue() : "";
        String ms365RefreshToken = client != null && client.getRefreshToken() != null
            ? client.getRefreshToken().getTokenValue() : "";

        // Sync user — stores ms365_access_token and ms365_refresh_token
        User user = userSyncService.syncUser(oidcUser.getIdToken(), ms365AccessToken, ms365RefreshToken);
        List<String> permissions = userSyncService.extractPermissions(user);

        // Issue portal access JWT (RS256)
        String accessJwt = jwtTokenService.generateAccessToken(
            user.getId(),
            user.getAzureOid(),
            user.getEmail(),
            user.getRole() != null ? user.getRole().getId() : null,
            user.getPaysId(),
            permissions
        );

        // Generate portal refresh token UUID — stored in `refresh_token` column
        String portalRefreshToken = jwtTokenService.generateRefreshToken();
        user.setRefreshToken(portalRefreshToken);
        userRepository.save(user);

        // Set HttpOnly cookies — no token in URL
        response.addCookie(jwtTokenService.buildAccessCookie(accessJwt));
        response.addCookie(jwtTokenService.buildRefreshCookie(portalRefreshToken));

        // Async audit log — never blocks redirect
        String ip = getClientIp(request);
        auditLogService.log("LOGIN", "PORTAL", String.valueOf(user.getId()), ip,
            user.getAzureOid());

        log.info("User {} authenticated via MS365, userId={}", maskEmail(user.getEmail()), user.getId());

        // Redirect Angular — no token in URL (it's in the HttpOnly cookie)
        response.sendRedirect(props.getCors().getPortalOrigin() + "/home");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
