package com.daf360.portal.controller;

import com.daf360.portal.dto.MeResponse;
import com.daf360.portal.dto.PortalUser;
import com.daf360.portal.repository.UserRepository;
import com.daf360.portal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof String s) {
            return Long.valueOf(s);
        }
        // Fallback: session still holds the OAuth2 PortalUser (JWT cookie not yet sent)
        if (principal instanceof PortalUser portalUser) {
            return userRepository.findByAzureOid(portalUser.getAzureOid())
                .map(u -> u.getId())
                .orElse(null);
        }
        return null;
    }
}
