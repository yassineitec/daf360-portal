package com.daf360.portal.service;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.dto.PaysScope;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();

        AppProperties props = new AppProperties();
        props.getJwt().setAccessTokenExpirySeconds(3600);
        props.getJwt().setRefreshTokenExpirySeconds(604800);
        props.getJwt().setIssuer("daf360-portal");

        jwtTokenService = new JwtTokenService(props, keyPair.getPrivate(), keyPair.getPublic());
    }

    @Test
    void generateAccessToken_returnsValidJwt() {
        String token = jwtTokenService.generateAccessToken(
            1L, "oid-123", "user@example.com", 2L, 1L, List.of("RH_READ", "RH_WRITE"),
            PaysScope.of(List.of(179L, 53L))
        );

        assertThat(token).isNotBlank();
        Claims claims = jwtTokenService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("azureOid", String.class)).isEqualTo("oid-123");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("roleId", Long.class)).isEqualTo(2L);
        assertThat(claims.get("paysId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("permissions")).isInstanceOf(List.class);
        // The single-valued paysId claim above must survive alongside the scope: log-service,
        // payroll and finance still read it and know nothing about paysIds.
        assertThat(claims.get("paysScopeAll", Boolean.class)).isFalse();
        // Jackson deserialises the JSON array back as Integers, not Longs
        assertThat((List<Object>) claims.get("paysIds", List.class))
                .containsExactlyInAnyOrder(179, 53);
    }

    @Test
    void generateAccessToken_showAllRole_emitsUnrestrictedScope() {
        String token = jwtTokenService.generateAccessToken(
            1L, "oid", "e@e.com", 1L, 1L, List.of(), PaysScope.unrestricted()
        );

        Claims claims = jwtTokenService.parseToken(token);
        assertThat(claims.get("paysScopeAll", Boolean.class)).isTrue();
        assertThat((List<?>) claims.get("paysIds", List.class)).isEmpty();
    }

    @Test
    void generateAccessToken_nullScope_omitsScopeClaimsEntirely() {
        String token = jwtTokenService.generateAccessToken(
            1L, "oid", "e@e.com", 1L, 1L, List.of(), null
        );

        Claims claims = jwtTokenService.parseToken(token);
        assertThat(claims.get("paysScopeAll")).isNull();
        assertThat(claims.get("paysIds")).isNull();
        // Pre-V74 token shape is still produceable — consumers must treat absence as "unknown".
        assertThat(claims.get("paysId", Long.class)).isEqualTo(1L);
    }

    @Test
    void parseToken_expiredToken_throwsException() throws Exception {
        AppProperties shortExpiry = new AppProperties();
        shortExpiry.getJwt().setAccessTokenExpirySeconds(-1);
        shortExpiry.getJwt().setIssuer("daf360-portal");
        JwtTokenService shortService = new JwtTokenService(
            shortExpiry, keyPair.getPrivate(), keyPair.getPublic()
        );

        String token = shortService.generateAccessToken(
            1L, "oid", "e@e.com", 1L, 1L, List.of(), PaysScope.unrestricted());

        assertThatThrownBy(() -> jwtTokenService.parseToken(token))
            .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void parseToken_tamperedToken_throwsException() {
        String token = jwtTokenService.generateAccessToken(
            1L, "oid", "e@e.com", 1L, 1L, List.of(), PaysScope.unrestricted());
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

        assertThatThrownBy(() -> jwtTokenService.parseToken(tampered))
            .isInstanceOf(io.jsonwebtoken.security.SecurityException.class);
    }

    @Test
    void generateRefreshToken_returnsUuid() {
        String refresh = jwtTokenService.generateRefreshToken();
        assertThat(refresh).matches("[0-9a-f\\-]{36}");
    }
}
