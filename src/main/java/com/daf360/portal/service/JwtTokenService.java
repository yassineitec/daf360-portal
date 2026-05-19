package com.daf360.portal.service;

import com.daf360.portal.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class JwtTokenService {

    private final AppProperties props;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtTokenService(AppProperties props, PrivateKey privateKey, PublicKey publicKey) {
        this.props = props;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String generateAccessToken(Long userId, String azureOid, String email,
                                      Long roleId, Long paysId, List<String> permissions) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + props.getJwt().getAccessTokenExpirySeconds() * 1000L);

        return Jwts.builder()
            .subject(String.valueOf(userId))
            .issuer(props.getJwt().getIssuer())
            .issuedAt(now)
            .expiration(expiry)
            .claim("azureOid", azureOid)
            .claim("email", email)
            .claim("roleId", roleId)
            .claim("paysId", paysId)
            .claim("permissions", permissions)
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(publicKey)
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

    public Cookie buildAccessCookie(String token) {
        Cookie cookie = new Cookie("daf360_access", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(props.getCookie().isSecure());
        cookie.setPath("/");
        cookie.setMaxAge((int) props.getJwt().getAccessTokenExpirySeconds());
        if (!props.getCookie().getDomain().isBlank()) {
            cookie.setDomain(props.getCookie().getDomain());
        }
        return cookie;
    }

    public Cookie buildRefreshCookie(String refreshToken) {
        Cookie cookie = new Cookie("daf360_refresh", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(props.getCookie().isSecure());
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge((int) props.getJwt().getRefreshTokenExpirySeconds());
        if (!props.getCookie().getDomain().isBlank()) {
            cookie.setDomain(props.getCookie().getDomain());
        }
        return cookie;
    }

    public Cookie buildClearAccessCookie() {
        Cookie cookie = new Cookie("daf360_access", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(props.getCookie().isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }

    public Cookie buildClearRefreshCookie() {
        Cookie cookie = new Cookie("daf360_refresh", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(props.getCookie().isSecure());
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(0);
        return cookie;
    }
}
