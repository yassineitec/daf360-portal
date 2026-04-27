package com.daf360.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * Typed binding for the app.portal.* section of application.yml.
 * All values are injected via environment variables with fallback defaults.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.portal")
public class PortalProperties {

    /** URL of the RH application */
    private String rhUrl;

    /** URL of the Billing application */
    private String billingUrl;

    /** URL of the Timesheet application */
    private String timesheetUrl;

    /** JWT settings for SSO token forwarding */
    private Jwt jwt = new Jwt();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationSeconds = 3600;
        private String issuer = "daf360-portal";
    }
}
