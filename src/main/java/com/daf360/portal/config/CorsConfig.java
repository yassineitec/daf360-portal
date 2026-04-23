package com.daf360.portal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    // Angular dev server origin – injected from env or defaults to localhost:4200
    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Split comma-separated origins (e.g. "http://localhost:4200,https://portal.daf360.com")
        cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-XSRF-TOKEN",  // CSRF token header for Angular
            "X-Requested-With"
        ));
        cfg.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        cfg.setAllowCredentials(true); // Required to send/receive cookies
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        source.registerCorsConfiguration("/login/**", cfg);
        source.registerCorsConfiguration("/logout", cfg);
        return source;
    }
}
