package com.daf360.portal.service;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.dto.MeResponse;
import com.daf360.portal.entity.Pays;
import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.EmployeeProfileRepository;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PaysRepository paysRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserSyncService userSyncService;
    private final AppProperties appProperties;

    @Cacheable(value = "userInfo", key = "#userId")
    @Transactional(readOnly = true)
    public MeResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        String isoCode = paysRepository.findById(user.getPaysId())
            .map(Pays::getIsoCode)
            .orElse(null);

        Role role = user.getRole();
        // Use recursive extractPermissions to match exactly what the JWT contains,
        // including permissions inherited from subordinate roles in the hierarchy.
        List<String> permissions = userSyncService.extractPermissions(user);

        String photoUrl = employeeProfileRepository.findByUserId(userId)
            .map(ep -> ep.getPhotoUrl())
            .orElse(null);

        // HMAC token for rh-service / microservice calls — sent in response body so Angular
        // can use it as Authorization: Bearer without relying on cross-port cookie delivery.
        String rhToken = buildRhToken(user.getId(), user.getAzureUpn(), user.getEmail(),
                role != null ? role.getId() : null, user.getPaysId(), permissions);

        return MeResponse.builder()
            .userId(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .azureUpn(user.getAzureUpn())
            .roleId(role != null ? role.getId() : null)
            .roleName(role != null ? role.getFrenchName() : null)
            .permissions(permissions)
            .paysId(user.getPaysId())
            .isoCode(isoCode)
            .employeeId(user.getEmployeeId())
            .photoUrl(photoUrl)
            .rhToken(rhToken)
            .build();
        // Sensitive fields intentionally omitted:
        // password, refreshToken, azureOid, ms365AccessToken, ms365RefreshToken
    }

    /** HMAC-HS256 token carrying the same claims as the portal's RS256 token.
     *  Microservices share the JWT_SECRET to validate this without needing the RSA key. */
    private String buildRhToken(Long userId, String azureUpn, String email,
                                 Long roleId, Long paysId, List<String> permissions) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime()
                + appProperties.getJwt().getAccessTokenExpirySeconds() * 1000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(appProperties.getJwt().getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .claim("azureOid",    azureUpn)
                .claim("email",       email)
                .claim("roleId",      roleId)
                .claim("paysId",      paysId)
                .claim("permissions", permissions)
                .signWith(Keys.hmacShaKeyFor(
                        appProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
