package com.daf360.portal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strongly-typed binding for all custom 'app.*' properties in application.yml.
 * Keeps application.yml as the single source of truth for configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Allowed CORS origins (Angular prod URL + other apps) */
    private List<String> allowedOrigins = List.of();

    /** Secret key used to sign internal JWT tokens (min 256 bits / 32 chars) */
    private String jwtSecret;

    /** JWT expiry in seconds (default 3600 = 1 hour) */
    private long jwtExpirySeconds = 3600;

    /** URLs of the other apps to redirect to after SSO */
    private Apps apps = new Apps();

    @Data
    public static class Apps {
        private String rhUrl      = "http://localhost:4201";
        private String billingUrl = "http://localhost:4202";
        private String timesheetUrl = "http://localhost:4203";
    }

    /** Frontend URL — where Angular is hosted */
    private String frontendUrl = "http://localhost:4200";
}
