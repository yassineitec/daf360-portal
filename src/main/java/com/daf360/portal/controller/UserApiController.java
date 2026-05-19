package com.daf360.portal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserApiController {

    @GetMapping("/public/config")
    public ResponseEntity<Object> publicConfig() {
        return ResponseEntity.ok(Map.of(
            "loginUrl",   "/oauth2/authorization/azure",
            "logoutUrl",  "/logout",
            "appVersion", "1.0.0"
        ));
    }
}
