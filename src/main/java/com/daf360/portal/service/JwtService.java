package com.daf360.portal.service;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.dto.AuthUserDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Generates and validates the internal JWT token that the Portal issues
 * to Angular and other microservices after a successful Microsoft 365 login.
 *
 * The token contains:
 *  - sub   : Azure AD Object ID (OID) — permanent user identifier
 *  - email : user email
 *  - name  : display name
 *  - roles : list of roles loaded from your database
 *  - iss   : "daf360-portal"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;

    /**
     * Generate a signed JWT for the given user.
     * This token is what Angular stores and sends to ALL microservices.
     */
    public String generateToken(AuthUserDto user) {
        Instant now    = Instant.now();
        Instant expiry = now.plusSeconds(appProperties.getJwtExpirySeconds());

        return Jwts.builder()
            .subject(user.getOid())
            .issuer("daf360-portal")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("email",  user.getEmail())
            .claim("name",   user.getName())
            .claim("roles",  user.getRoles())
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Validate and parse a token — used by the portal itself
     * to verify tokens it issued (e.g. for the /api/auth/me endpoint).
     */
    public io.jsonwebtoken.Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private SecretKey getSigningKey() {
        // Ensure the secret is at least 256 bits (32 ASCII chars)
        byte[] keyBytes = appProperties.getJwtSecret()
            .getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
