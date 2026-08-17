package com.daf360.portal.service;

import com.daf360.portal.dto.PaysScope;
import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Country-scope resolution (V74__role_pays_scope.sql).
 *
 * The first two tests are the case that motivated the mode column: a role shared by
 * Tunisian and Egyptian holders must NOT become a cross-country window.
 */
class UserSyncServicePaysScopeTest {

    private static final long TN = 179L;
    private static final long EG = 53L;
    private static final long UAE = 185L;

    private final UserSyncService service =
            new UserSyncService(Mockito.mock(UserRepository.class));

    private User userWith(Long paysId, Role role) {
        User u = new User();
        u.setId(1L);
        u.setPaysId(paysId);
        u.setRole(role);
        return u;
    }

    private Role role(String mode, List<Long> scope) {
        Role r = new Role();
        r.setId(10L);
        r.setFrenchName("Responsable GC");
        r.setPaysScopeMode(mode);
        r.setPaysScope(scope);
        return r;
    }

    @Test
    void ownMode_sharedRole_tunisianHolderSeesOnlyTunisia() {
        PaysScope scope = service.extractPaysScope(userWith(TN, role("OWN", List.of())));

        assertThat(scope.all()).isFalse();
        assertThat(scope.paysIds()).containsExactly(TN);
    }

    @Test
    void ownMode_sameSharedRole_egyptianHolderSeesOnlyEgypt() {
        PaysScope scope = service.extractPaysScope(userWith(EG, role("OWN", List.of())));

        assertThat(scope.all()).isFalse();
        assertThat(scope.paysIds()).containsExactly(EG);
    }

    @Test
    void ownMode_ignoresAnyStoredCountryList() {
        // Rows can survive a LIST -> OWN switch; OWN must not honour them or the shared-role
        // case above would leak. The rows are kept so switching back does not lose them.
        PaysScope scope = service.extractPaysScope(userWith(TN, role("OWN", List.of(TN, EG, UAE))));

        assertThat(scope.paysIds()).containsExactly(TN);
    }

    @Test
    void listMode_everyHolderSeesTheSameCountries() {
        PaysScope tunisian = service.extractPaysScope(userWith(TN, role("LIST", List.of(TN, EG, UAE))));
        PaysScope egyptian = service.extractPaysScope(userWith(EG, role("LIST", List.of(TN, EG, UAE))));

        assertThat(tunisian.paysIds()).containsExactlyInAnyOrder(TN, EG, UAE);
        assertThat(egyptian.paysIds()).containsExactlyInAnyOrder(TN, EG, UAE);
    }

    @Test
    void allMode_isUnrestricted() {
        PaysScope scope = service.extractPaysScope(userWith(TN, role("ALL", List.of())));

        assertThat(scope.all()).isTrue();
        assertThat(scope.paysIds()).isEmpty();
    }

    @Test
    void listMode_withEmptyList_narrowsToOwnCountryRatherThanGrantingAll() {
        PaysScope scope = service.extractPaysScope(userWith(EG, role("LIST", List.of())));

        assertThat(scope.all()).isFalse();
        assertThat(scope.paysIds()).containsExactly(EG);
    }

    @Test
    void nullMode_withLegacyShowAll_stillMeansAll() {
        Role legacy = role(null, List.of());
        legacy.setShowAll(true);

        assertThat(service.extractPaysScope(userWith(TN, legacy)).all()).isTrue();
    }

    @Test
    void nullMode_withoutShowAll_isOwnCountry() {
        // Every pre-V74 role, before the backfill runs.
        PaysScope scope = service.extractPaysScope(userWith(TN, role(null, List.of())));

        assertThat(scope.paysIds()).containsExactly(TN);
    }

    @Test
    void unknownMode_narrowsToOwnCountry() {
        PaysScope scope = service.extractPaysScope(userWith(TN, role("EVERYTHING", List.of(EG))));

        assertThat(scope.paysIds()).containsExactly(TN);
    }

    @Test
    void scopeDoesNotInheritFromSubordinateRoles() {
        Role child = role("LIST", List.of(EG, UAE));
        child.setId(11L);
        Role parent = role("OWN", List.of());
        parent.setId(10L);
        parent.setSubordinateRoles(Set.of(child));

        // Permissions DO inherit downward; country scope deliberately does not.
        PaysScope scope = service.extractPaysScope(userWith(TN, parent));

        assertThat(scope.all()).isFalse();
        assertThat(scope.paysIds()).containsExactly(TN);
    }

    @Test
    void deletedRole_fallsBackToOwnCountry() {
        Role deleted = role("ALL", List.of());
        deleted.setDeleted(true);

        PaysScope scope = service.extractPaysScope(userWith(TN, deleted));

        assertThat(scope.all()).isFalse();
        assertThat(scope.paysIds()).containsExactly(TN);
    }

    @Test
    void noRoleAndNoOwnCountry_isUnrestrictedAsBeforeV74() {
        // Pre-V74 a null paysId claim meant "unscoped"; keep that rather than locking out.
        PaysScope scope = service.extractPaysScope(userWith(null, null));

        assertThat(scope.all()).isTrue();
    }
}
