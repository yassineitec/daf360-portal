package com.daf360.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the authenticated user.
 * Built from the Azure AD OIDC claims after successful login.
 * Returned by /api/auth/me and embedded in the JWT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserDto {

    /** Azure AD Object ID — permanent, unique, use as DB foreign key */
    private String oid;

    /** User's email (preferred_username claim) */
    private String email;

    /** User's display name */
    private String name;

    /** Roles loaded from your database — NOT from Azure AD */
    private List<String> roles;

    /** The signed JWT token to send to Angular (only in /api/auth/login response) */
    private String token;
}
