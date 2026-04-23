package com.daf360.portal.config;

import com.daf360.portal.security.AzureOAuth2SuccessHandler;
import com.daf360.portal.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Central Spring Security configuration for the DAF360 Portal.
 *
 * Two authentication mechanisms co-exist:
 *  A) oauth2Login — for the browser-based Microsoft 365 redirect flow
 *  B) JwtAuthFilter — for stateless Angular API calls (Bearer token)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AzureOAuth2SuccessHandler successHandler;
    private final JwtAuthFilter             jwtAuthFilter;
    private final AppProperties             appProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/oauth2/**", "/login/**"))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/public/**",
                    "/actuator/health",
                    "/oauth2/**",
                    "/login/**",
                    "/login-error",
                    "/api/auth/login",
                    "/api/auth/status",
                    "/error"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // ── OAuth2 login for browser-based flow ───────────────────────
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/azure")
                .successHandler(successHandler)
                .failureUrl("/login-error")
            )

            // ── JWT filter for stateless Angular API calls ────────────────
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ── 401 (not 302) for unauthenticated /api calls ──────────────
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    req -> req.getRequestURI().startsWith("/api/")
                )
            )

            // ── Session: stateless for API, minimal for OAuth2 flow ───────
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) ->
                    res.setStatus(HttpStatus.OK.value()))
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        List<String> origins = new ArrayList<>();
        origins.add("http://localhost:4200");
        origins.addAll(appProperties.getAllowedOrigins());

        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
