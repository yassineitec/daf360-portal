package com.daf360.portal.service;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.dto.PaysScope;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
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

    /**
     * @param paysScope countries the user's role may see (V74). Emitted as the `paysIds` +
     *        `paysScopeAll` claims. The single-valued `paysId` claim is kept alongside them
     *        because log-service, payroll and finance all parse these tokens and still read
     *        it — dropping it would break three services that know nothing about scope.
     *        Pass null to omit the scope claims entirely (pre-V74 shape).
     *
     * NO `permissions` claim — deliberately. This token travels as the `daf360_access`
     * cookie, and browsers silently DROP any single cookie over 4096 bytes: with the full
     * permission list inside, the largest roles (Administrateur, Super Admin) could no
     * longer log in at all, and every new permission code made it worse. Permissions now
     * travel only in the `rhToken` that /api/me returns in its body, which every frontend
     * sends as `Authorization: Bearer` (a header, where nginx allows 32k). Every backend
     * already tries the Bearer token before this cookie; the portal itself resolves them
     * from the database (JwtAuthFilter → UserService.getUserInfo). The cookie now only
     * proves WHO the user is — ~700 bytes, whatever the role.
     */
    public String generateAccessToken(Long userId, String azureOid, String email,
                                      Long roleId, Long paysId, PaysScope paysScope) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + props.getJwt().getAccessTokenExpirySeconds() * 1000L);

        var builder = Jwts.builder()
            .subject(String.valueOf(userId))
            .issuer(props.getJwt().getIssuer())
            .issuedAt(now)
            .expiration(expiry)
            .claim("azureOid", azureOid)
            .claim("email", email)
            .claim("roleId", roleId)
            .claim("paysId", paysId);

        if (paysScope != null) {
            builder.claim("paysScopeAll", paysScope.all())
                   .claim("paysIds", paysScope.paysIds());
        }

        return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
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

    /**
     * Same Domain as buildAccessCookie, or the browser never deletes the real cookie: a
     * cookie is identified by name + domain + path, so a Max-Age=0 without the Domain
     * targets a host-only twin that doesn't exist, and the session set WITH COOKIE_DOMAIN
     * survives logout — the user is "connected again" on the next reload.
     */
    public Cookie buildClearAccessCookie() {
        Cookie cookie = new Cookie("daf360_access", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(props.getCookie().isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        if (!props.getCookie().getDomain().isBlank()) {
            cookie.setDomain(props.getCookie().getDomain());
        }
        return cookie;
    }

    public Cookie buildClearRefreshCookie() {
        Cookie cookie = new Cookie("daf360_refresh", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(props.getCookie().isSecure());
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(0);
        // Same reason as buildClearAccessCookie.
        if (!props.getCookie().getDomain().isBlank()) {
            cookie.setDomain(props.getCookie().getDomain());
        }
        return cookie;
    }
}
