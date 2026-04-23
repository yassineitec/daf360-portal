package com.daf360.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public controller – no authentication required.
 */
@Controller
public class HomeController {

    /** Public landing page – shows login button */
    @GetMapping("/")
    public String home() {
        return "home";   // templates/home.html
    }

    /**
     * Login page.
     * Spring Security auto-handles the actual OAuth2 redirect from here.
     * This only renders the login button UI.
     */
    @GetMapping("/login")
    public String login() {
        return "login";  // templates/login.html
    }

    /** Simple error page */
    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403"; // templates/error/403.html
    }
}
