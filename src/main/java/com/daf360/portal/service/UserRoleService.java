package com.daf360.portal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads roles for a user from your database.
 *
 * PLACEHOLDER IMPLEMENTATION — returns empty list until you connect your DB.
 *
 * To integrate with your existing Timesheet role service:
 *   Option A) Inject your existing repository here.
 *   Option B) Call the Timesheet REST API via FeignClient.
 *   Option C) Move this to daf360-shared-lib and share across all apps.
 *
 * The OID is the Azure AD Object ID from the 'oid' JWT claim.
 * Store this in your users table as azure_oid (VARCHAR 36).
 */
@Slf4j
@Service
public class UserRoleService {

    /**
     * Return the list of role names for the given user.
     * Role names should be plain strings: "EMPLOYEE", "MANAGER", "HR", etc.
     * Spring Security will prepend "ROLE_" automatically.
     *
     * @param azureOid the Azure AD Object ID from the JWT 'oid' claim
     * @return list of role name strings
     */
    public List<String> getRoles(String azureOid) {
        // TODO: replace with actual DB query
        // Example:
        //   return userRoleRepository.findRoleNamesByAzureOid(azureOid);
        log.debug("Loading roles for OID: {} (stub — returning empty list)", azureOid);
        return List.of();
    }
}
