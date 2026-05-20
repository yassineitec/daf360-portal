package com.daf360.portal.controller;

import com.daf360.portal.dto.MeResponse;
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

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof String principalStr)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        MeResponse response = userService.getUserInfo(Long.valueOf(principalStr));
        return ResponseEntity.ok(response);
    }
}
