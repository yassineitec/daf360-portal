package com.daf360.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body returned to Angular after successful OAuth2 login.
 * Contains the JWT token and basic user info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private String tokenType = "Bearer";
    private long   expiresIn;   // seconds
    private AuthUserDto user;
}
