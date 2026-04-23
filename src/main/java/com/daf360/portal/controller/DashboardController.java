package com.daf360.portal.controller;

import com.daf360.portal.config.PortalProperties;
import com.daf360.portal.dto.PortalUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Protected dashboard – requires authentication.
 * Shows user info and links to the other apps.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final PortalProperties portalProperties;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal PortalUser user,
                            Model model) {

        model.addAttribute("userName",    user.getDisplayName());
        model.addAttribute("email",       user.getEmail());
        model.addAttribute("azureOid",    user.getAzureOid());

        // App links – pulled from application.yml
        model.addAttribute("rhUrl",        portalProperties.getRhUrl());
        model.addAttribute("billingUrl",   portalProperties.getBillingUrl());
        model.addAttribute("timesheetUrl", portalProperties.getTimesheetUrl());

        return "dashboard"; // templates/dashboard.html
    }
}
