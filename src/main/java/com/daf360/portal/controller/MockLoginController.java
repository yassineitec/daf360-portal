package com.daf360.portal.controller;

import com.daf360.portal.dto.AuthUserDto;
import com.daf360.portal.dto.LoginResponseDto;
import com.daf360.portal.service.JwtService;
import com.daf360.portal.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MOCK ONLY — activated only when profile = mock.
 * Allows testing Angular integration before Azure AD credentials are available.
 *
 * Usage:
 *   GET http://localhost:8080/api/auth/mock-login?email=test@company.com
 *   Returns a JWT token you can paste into Angular to test authenticated flows.
 *
 * REMOVE or disable this controller before going to production.
 */
@Profile("mock")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MockLoginController {

    private final JwtService    jwtService;
    private final AppProperties appProperties;

    @GetMapping("/mock-login")
    public ResponseEntity<LoginResponseDto> mockLogin(
            @RequestParam(defaultValue = "test@daf360.com") String email,
            @RequestParam(defaultValue = "Test User")       String name,
            @RequestParam(defaultValue = "EMPLOYEE")        String role) {

        AuthUserDto user = AuthUserDto.builder()
            .oid("mock-oid-" + email.hashCode())
            .email(email)
            .name(name)
            .roles(List.of(role))
            .build();

        String token = jwtService.generateToken(user);
        user.setToken(token);

        return ResponseEntity.ok(LoginResponseDto.builder()
            .token(token)
            .tokenType("Bearer")
            .expiresIn(appProperties.getJwtExpirySeconds())
            .user(user)
            .build());
    }
}
