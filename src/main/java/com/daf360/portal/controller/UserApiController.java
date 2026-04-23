package com.daf360.portal.controller;

import com.daf360.portal.dto.PortalUser;
import com.daf360.portal.dto.UserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API consumed by the Angular frontend.
 *
 * Angular calls GET /api/me to check if the user is authenticated
 * and retrieve their info without a full page reload.
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class UserApiController {

    /**
     * Returns current user info as JSON.
     * Angular calls this on startup to check auth state.
     *
     * Returns 200 with user data if authenticated.
     * Returns 401 automatically if not authenticated (Spring Security).
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(
            @AuthenticationPrincipal PortalUser user) {

        return ResponseEntity.ok(UserInfoResponse.builder()
            .azureOid(user.getAzureOid())
            .displayName(user.getDisplayName())
            .email(user.getEmail())
            .username(user.getPreferredUsername())
            .build());
    }

    /**
     * Public endpoint – Angular calls this to get app URLs
     * without needing to be authenticated.
     */
    @GetMapping("/public/config")
    public ResponseEntity<Object> publicConfig() {
        return ResponseEntity.ok(java.util.Map.of(
            "loginUrl",   "/oauth2/authorization/azure",
            "logoutUrl",  "/logout",
            "appVersion", "1.0.0"
        ));
    }
}
