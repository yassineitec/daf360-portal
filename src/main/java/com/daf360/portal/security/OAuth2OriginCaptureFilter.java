package com.daf360.portal.security;

import com.daf360.portal.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * Captures the Referer header when any OAuth2 authorization flow begins
 * (/oauth2/authorization/**) and stores it in the HTTP session as
 * {@link AzureOAuth2SuccessHandler#ORIGIN_KEY}.
 *
 * This allows the success handler to redirect back to the originating app
 * (e.g. daf360-rh-frontend at localhost:4201) instead of always defaulting
 * to the portal shell. Without this, the Referer on the Azure callback
 * request is Azure's domain — not the original app — so the redirect fails.
 *
 * Order = 1 ensures this filter runs before Spring Security processes the
 * /oauth2/authorization/* endpoint and establishes the OAuth2 state.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class OAuth2OriginCaptureFilter extends OncePerRequestFilter {

    private final AppProperties props;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
            String referer = request.getHeader("Referer");
            if (referer != null) {
                String origin = extractOrigin(referer);
                if (origin != null && isAllowedOrigin(origin)) {
                    request.getSession(true)
                           .setAttribute(AzureOAuth2SuccessHandler.ORIGIN_KEY, origin);
                    log.debug("OAuth2 origin captured from Referer: {}", origin);
                }
            }
        }

        chain.doFilter(request, response);
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

    private boolean isAllowedOrigin(String origin) {
        return Set.of(
            props.getCors().getPortalOrigin(),
            props.getCors().getHrOrigin(),
            props.getCors().getFactuOrigin(),
            props.getCors().getTimesheetOrigin()
        ).contains(origin);
    }
}
