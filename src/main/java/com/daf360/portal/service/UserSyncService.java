package com.daf360.portal.service;

import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final UserRepository userRepository;

    @CacheEvict(value = "userInfo", key = "#result.id")
    @Transactional
    public User syncUser(OidcIdToken idToken, String ms365AccessToken, String ms365RefreshToken) {
        String azureOid = extractClaim(idToken, "oid");
        String email    = extractClaim(idToken, StandardClaimNames.EMAIL);
        String fullName = extractClaim(idToken, StandardClaimNames.NAME);
        String upn      = extractClaim(idToken, "preferred_username");
        if (upn == null) upn = email;

        User user = resolveUser(azureOid, email);

        // Fix 2: reject deactivated accounts before issuing any token
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Login rejected — account is inactive for email={}", maskEmail(email));
            throw new DisabledException("Account disabled: " + maskEmail(email));
        }

        user.setAzureOid(azureOid);
        user.setAzureUpn(upn);
        user.setFullName(fullName);
        user.setMs365AccessToken(ms365AccessToken);
        user.setMs365RefreshToken(ms365RefreshToken);
        user.setTokenExpiresAt(LocalDateTime.now().plusSeconds(3600));
        user.setPassword(null);

        return userRepository.save(user);
    }

    private User resolveUser(String azureOid, String email) {
        Optional<User> byOid = userRepository.findByAzureOid(azureOid);
        if (byOid.isPresent()) return byOid.get();

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) return byEmail.get();

        log.warn("Login rejected — no user found for email={}", maskEmail(email));
        throw new UsernameNotFoundException("User not provisioned: " + maskEmail(email));
    }

    /**
     * Collects all permissions for a user's role, including permissions inherited
     * from subordinate roles in the hierarchy (same logic as timeSheetBack).
     * Deduplicates automatically. Returns empty list if role is null or soft-deleted.
     */
    public List<String> extractPermissions(User user) {
        Role role = user.getRole();
        // Fix 5: ignore soft-deleted roles
        if (role == null || Boolean.TRUE.equals(role.getDeleted())) {
            return List.of();
        }
        // Fix 3 + 4: recursive collection + deduplication via LinkedHashSet
        Set<String> collected = new LinkedHashSet<>();
        collectPermissions(role, collected, new HashSet<>());
        return new ArrayList<>(collected);
    }

    private void collectPermissions(Role role, Set<String> perms, Set<Long> visited) {
        if (role == null || Boolean.TRUE.equals(role.getDeleted())) return;
        // Prevent infinite loops in circular hierarchies
        if (role.getId() != null && !visited.add(role.getId())) return;

        if (role.getPermissions() != null) {
            perms.addAll(role.getPermissions());
        }
        if (role.getSubordinateRoles() != null) {
            for (Role sub : role.getSubordinateRoles()) {
                collectPermissions(sub, perms, visited);
            }
        }
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
