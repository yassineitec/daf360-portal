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
    private long defaultPaysId = 1L;

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
}
