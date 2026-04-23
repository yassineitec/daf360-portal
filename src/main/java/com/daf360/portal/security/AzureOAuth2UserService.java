package com.daf360.portal.security;

import com.daf360.portal.dto.PortalUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Custom OIDC user service for Azure AD.
 *
 * Responsibilities:
 *  1. Load the user from Azure AD via the default OidcUserService
 *  2. Extract relevant claims (oid, email, name, preferred_username)
 *  3. Wrap in our PortalUser which holds the Azure Object ID
 *     (used later to load roles from our database)
 *
 * When Azure credentials are NOT yet configured, this service
 * will never be called – requests will fail at the redirect step.
 */
@Slf4j
@Service
public class AzureOAuth2UserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {

        // 1. Load user from Azure AD using parent service
        OidcUser oidcUser = super.loadUser(userRequest);

        // 2. Extract Azure AD claims
        String oid               = oidcUser.getAttribute("oid");
        String preferredUsername = oidcUser.getAttribute("preferred_username");
        String name              = oidcUser.getFullName();
        String email             = oidcUser.getEmail();

        log.info("User authenticated via Azure AD: oid={}, username={}",
            oid, preferredUsername);

        // 3. Return PortalUser wrapping the OIDC user
        //    In Phase 2, this is where you load roles from your DB:
        //    List<String> roles = roleService.getRoles(oid);
        return new PortalUser(oidcUser, oid, preferredUsername, name, email);
    }
}
