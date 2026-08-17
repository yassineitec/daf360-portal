package com.daf360.portal.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// Safe user info — NEVER include password, refreshToken, azureOid, ms365* tokens
@Data
@Builder
public class MeResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String azureUpn;
    private Long roleId;
    private String roleName;
    private List<String> permissions;
    /** The user's OWN country. Still the right default selection for a filter, but no longer
     *  the boundary of what they may see — that is paysIds/paysScopeAll below. */
    private Long paysId;
    private String isoCode;
    /** true = this user's role sees every country; paysIds is then empty and meaningless. */
    private Boolean paysScopeAll;
    /** Countries the user may see when paysScopeAll is false. Never empty in that case. */
    private List<Long> paysIds;
    private String employeeId;
    private String photoUrl;
    /** HMAC-signed JWT for authenticating requests to microservices (rh-service, etc.).
     *  Signed with the shared JWT_SECRET — never transmitted over non-HTTPS in production. */
    private String rhToken;
}
