package com.daf360.portal.controller;

import com.daf360.portal.entity.User;
import com.daf360.portal.repository.UserRepository;
import com.daf360.portal.service.JwtTokenService;
import com.daf360.portal.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MOCK ONLY — activated only when profile = mock.
 * Allows testing Angular integration before Azure AD credentials are available.
 *
 * Looks up the user by email in the DB and issues a proper RS256 JWT
 * (same structure as the production OAuth2 flow) with real permissions from RolePermissions.
 *
 * Usage:
 *   GET http://localhost:8080/api/auth/mock-login?email=test@company.com
 *   Returns a JWT cookie (daf360_access) and JSON with userId/email/permissions.
 *
 * REMOVE or disable this controller before going to production.
 */
@Profile("mock")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MockLoginController {

    private final JwtTokenService jwtTokenService;
    private final UserRepository  userRepository;
    private final UserSyncService userSyncService;

    @GetMapping("/mock-login")
    public ResponseEntity<?> mockLogin(
            @RequestParam(defaultValue = "test@daf360.com") String email,
            jakarta.servlet.http.HttpServletResponse response) {

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404)
                .body(Map.of("error", "User not found for email: " + email));
        }

        List<String> permissions = userSyncService.extractPermissions(user);

        String accessJwt = jwtTokenService.generateAccessToken(
            user.getId(),
            user.getAzureOid() != null ? user.getAzureOid() : "mock-oid-" + user.getId(),
            user.getEmail(),
            user.getRole() != null ? user.getRole().getId() : null,
            user.getPaysId(),
            permissions,
            userSyncService.extractPaysScope(user)
        );

        response.addCookie(jwtTokenService.buildAccessCookie(accessJwt));

        return ResponseEntity.ok(Map.of(
            "userId",      user.getId(),
            "email",       user.getEmail(),
            "roleName",    user.getRole() != null ? user.getRole().getFrenchName() : null,
            "permissions", permissions,
            "token",       accessJwt
        ));
    }
}
