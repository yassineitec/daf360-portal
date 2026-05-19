package com.daf360.portal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Cookie cookie = new Cookie();
    private Apps apps = new Apps();
    private long defaultPaysId = 1L;

    /** Frontend (portal) URL — used for post-login redirects. */
    private String frontendUrl = "http://localhost:4200";

    // ── Legacy helpers kept for backward-compat with older service classes ────

    /** Shortcut: HMAC secret used by the old JwtService (pre-RSA). */
    private String jwtSecret = "daf360-change-me-in-production-32chars";

    /** Shortcut: access-token expiry seconds (delegates to jwt.accessTokenExpirySeconds). */
    public long getJwtExpirySeconds() {
        return jwt.getAccessTokenExpirySeconds();
    }

    /** Shortcut: all allowed CORS origins as a flat list (delegates to cors.*). */
    public java.util.List<String> getAllowedOrigins() {
        return java.util.List.of(
            cors.getPortalOrigin(),
            cors.getHrOrigin(),
            cors.getFactuOrigin(),
            cors.getTimesheetOrigin()
        );
    }

    @Data
    public static class Jwt {
        private String privateKeyPath;
        private String publicKeyPath;
        private long accessTokenExpirySeconds = 3600;
        private long refreshTokenExpirySeconds = 604800;
        private String issuer = "daf360-portal";
    }

    @Data
    public static class Cors {
        private String portalOrigin    = "http://localhost:4200";
        private String hrOrigin        = "http://localhost:4201";
        private String factuOrigin     = "http://localhost:4202";
        private String timesheetOrigin = "http://localhost:4203";
    }

    @Data
    public static class Cookie {
        private boolean secure = false;
        private String sameSite = "Strict";
        private String domain = "";
    }

    @Data
    public static class Apps {
        private String rhUrl          = "http://localhost:4201";
        private String billingUrl     = "http://localhost:4202";
        private String timesheetUrl   = "http://localhost:4203";
    }
}
