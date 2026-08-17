package com.daf360.portal.security;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.UserRepository;
import com.daf360.portal.service.AuditLogService;
import com.daf360.portal.service.JwtTokenService;
import com.daf360.portal.service.UserSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import java.net.URI;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    /** Session attribute key set before the OAuth2 redirect to remember which app initiated login. */
    public static final String ORIGIN_KEY = "oauth2_origin_app";

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

        User user = userSyncService.syncUser(oidcUser.getIdToken(), ms365AccessToken, ms365RefreshToken);
        List<String> permissions = userSyncService.extractPermissions(user);

        String accessJwt = jwtTokenService.generateAccessToken(
            user.getId(),
            user.getAzureOid(),
            user.getEmail(),
            user.getRole() != null ? user.getRole().getId() : null,
            user.getPaysId(),
            permissions,
            userSyncService.extractPaysScope(user)
        );

        String portalRefreshToken = jwtTokenService.generateRefreshToken();
        user.setRefreshToken(portalRefreshToken);
        userRepository.save(user);

        response.addCookie(jwtTokenService.buildAccessCookie(accessJwt));
        response.addCookie(jwtTokenService.buildRefreshCookie(portalRefreshToken));

        // No second HMAC cookie: it duplicated the permissions claim byte-for-byte and pushed
        // the response headers past Tomcat's limit. Every microservice JwtAuthFilter falls back
        // to this RSA cookie (they sniff the alg header), and /api/me still returns an HMAC
        // rhToken in its body for Bearer-style calls. Requires app.jwt-public-key-path on each
        // service — without it an RS256 token fails validation there.

        String ip = getClientIp(request);
        auditLogService.log("LOGIN", "PORTAL", String.valueOf(user.getId()), ip,
            user.getAzureOid());

        log.info("User {} authenticated via MS365, userId={}", maskEmail(user.getEmail()), user.getId());

        // Determine which app initiated the OAuth2 flow and redirect back there.
        String redirectTo = resolveRedirectTarget(request);

        // Drop the OAuth2 session — JWT cookie is the auth mechanism from this point on.
        request.getSession().invalidate();

        response.sendRedirect(redirectTo);
    }

    /**
     * Resolve the post-login redirect target.
     *
     * Priority:
     * 1. Session attribute stored by the /oauth2/init endpoint or filter (most reliable)
     * 2. Referer header (if it's a known DAF360 app origin)
     * 3. Default: portal shell /auth/callback
     */
    private String resolveRedirectTarget(HttpServletRequest request) {
        // 1. Session attribute (set when a non-shell app initiates OAuth2)
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object stored = session.getAttribute(ORIGIN_KEY);
            if (stored instanceof String origin && isAllowedOrigin(origin)) {
                log.debug("Post-login redirect → stored origin: {}", origin);
                return origin + "/auth/callback";
            }
        }

        // 2. Referer header (browser sends this on the /oauth2/authorization/* request)
        String referer = request.getHeader("Referer");
        if (referer != null) {
            String refererOrigin = extractOrigin(referer);
            if (refererOrigin != null && isAllowedOrigin(refererOrigin)) {
                log.debug("Post-login redirect → referer origin: {}", refererOrigin);
                return refererOrigin + "/auth/callback";
            }
        }

        // 3. Default: portal shell frontend URL
        return props.getFrontendUrl() + "/auth/callback";
    }

    private boolean isAllowedOrigin(String origin) {
        return Set.of(
            props.getCors().getPortalOrigin(),
            props.getCors().getHrOrigin(),
            props.getCors().getFactuOrigin(),
            props.getCors().getTimesheetOrigin()
        ).contains(origin);
    }

    private String extractOrigin(String url) {
        try {
            URI uri = URI.create(url);
            int port = uri.getPort();
            return port > 0
                ? uri.getScheme() + "://" + uri.getHost() + ":" + port
                : uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            return null;
        }
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
