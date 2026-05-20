package com.daf360.portal.service;

import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {

    @Mock UserRepository userRepository;

    private UserSyncService userSyncService;

    @BeforeEach
    void setUp() {
        userSyncService = new UserSyncService(userRepository);
    }

    private OidcIdToken buildToken(String oid, String email, String name, String upn) {
        Map<String, Object> claims = Map.of(
            "oid", oid,
            StandardClaimNames.EMAIL, email,
            StandardClaimNames.NAME, name,
            "preferred_username", upn,
            "sub", oid
        );
        return new OidcIdToken("raw-token", Instant.now(), Instant.now().plusSeconds(3600), claims);
    }

    @Test
    void syncUser_existingUserByOid_updatesTokens() {
        OidcIdToken token = buildToken("oid-abc", "user@corp.com", "Alice Martin", "alice@corp.com");

        User existing = new User();
        existing.setId(5L);
        existing.setAzureOid("oid-abc");
        existing.setEmail("user@corp.com");
        existing.setUsername("user@corp.com");
        existing.setPaysId(1L);

        when(userRepository.findByAzureOid("oid-abc")).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userSyncService.syncUser(token, "Bearer ms365-access", "ms365-refresh");

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getAzureUpn()).isEqualTo("alice@corp.com");
        assertThat(result.getMs365AccessToken()).isEqualTo("Bearer ms365-access");
        assertThat(result.getMs365RefreshToken()).isEqualTo("ms365-refresh");
        assertThat(result.getPassword()).isNull();
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void syncUser_unprovisioned_throwsUsernameNotFoundException() {
        OidcIdToken token = buildToken("oid-new", "new@corp.com", "Bob Dupont", "bob@corp.com");

        when(userRepository.findByAzureOid("oid-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@corp.com")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            UsernameNotFoundException.class,
            () -> userSyncService.syncUser(token, "ms-access", "ms-refresh")
        );
    }

    @Test
    void syncUser_existingUserFoundByEmail_mergesOid() {
        OidcIdToken token = buildToken("oid-xyz", "legacy@corp.com", "Carol", "carol@corp.com");

        User legacy = new User();
        legacy.setId(2L);
        legacy.setEmail("legacy@corp.com");
        legacy.setUsername("legacy@corp.com");
        legacy.setPaysId(1L);
        legacy.setAzureOid(null);

        when(userRepository.findByAzureOid("oid-xyz")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("legacy@corp.com")).thenReturn(Optional.of(legacy));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userSyncService.syncUser(token, "access", "refresh");

        assertThat(result.getAzureOid()).isEqualTo("oid-xyz");
        verify(userRepository, times(1)).save(legacy);
    }

    @Test
    void extractPermissions_returnsRolePermissionStrings() {
        Role role = new Role();
        role.setPermissions(List.of("RH_READ", "PORTAL_READ"));

        User user = new User();
        user.setRole(role);

        List<String> perms = userSyncService.extractPermissions(user);

        assertThat(perms).containsExactlyInAnyOrder("RH_READ", "PORTAL_READ");
    }
}
