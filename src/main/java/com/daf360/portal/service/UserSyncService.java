package com.daf360.portal.service;

import com.daf360.portal.config.AppProperties;
import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.RoleRepository;
import com.daf360.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private static final String DEFAULT_ROLE = "Collaborateur";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AppProperties props;

    @CacheEvict(value = "userInfo", key = "#result.id")
    @Transactional
    public User syncUser(OidcIdToken idToken, String ms365AccessToken, String ms365RefreshToken) {
        String azureOid = extractClaim(idToken, "oid");
        String email    = extractClaim(idToken, StandardClaimNames.EMAIL);
        String fullName = extractClaim(idToken, StandardClaimNames.NAME);
        String upn      = extractClaim(idToken, "preferred_username");
        if (upn == null) upn = email;

        User user = resolveUser(azureOid, email, upn);

        user.setAzureOid(azureOid);
        user.setAzureUpn(upn);
        user.setFullName(fullName);
        user.setMs365AccessToken(ms365AccessToken);
        user.setMs365RefreshToken(ms365RefreshToken);
        user.setTokenExpiresAt(LocalDateTime.now().plusSeconds(3600));
        user.setPassword(null);

        return userRepository.save(user);
    }

    private User resolveUser(String azureOid, String email, String upn) {
        Optional<User> byOid = userRepository.findByAzureOid(azureOid);
        if (byOid.isPresent()) {
            return byOid.get();
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        return createNewUser(email, azureOid, upn);
    }

    private User createNewUser(String email, String azureOid, String upn) {
        log.info("Creating new portal user for email={}", maskEmail(email));
        Role defaultRole = roleRepository.findByFrenchName(DEFAULT_ROLE)
            .orElseThrow(() -> new IllegalStateException(
                "Default role '" + DEFAULT_ROLE + "' not found in Roles table"
            ));

        User user = new User();
        user.setEmail(email);
        user.setUsername(upn != null ? upn : email);
        user.setPaysId(props.getDefaultPaysId());
        user.setIsActive(true);
        user.setRole(defaultRole);
        return user;
    }

    // permissions is List<String> from @ElementCollection — return directly
    public List<String> extractPermissions(User user) {
        if (user.getRole() == null || user.getRole().getPermissions() == null) {
            return List.of();
        }
        return user.getRole().getPermissions();
    }

    private String extractClaim(OidcIdToken token, String claim) {
        Object value = token.getClaims().get(claim);
        return value != null ? value.toString() : null;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
