package com.daf360.portal.service;

import com.daf360.portal.dto.MeResponse;
import com.daf360.portal.entity.Pays;
import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PaysRepository paysRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, paysRepository);
    }

    private User buildUser(Long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setFullName("Alice Martin");
        u.setEmail("alice@corp.com");
        u.setAzureUpn("alice@corp.onmicrosoft.com");
        u.setAzureOid("oid-secret");
        u.setPassword("hashed-secret");
        u.setRefreshToken("refresh-uuid-secret");
        u.setMs365AccessToken("ms-access-secret");
        u.setMs365RefreshToken("ms-refresh-secret");
        u.setEmployeeId("EMP001");
        u.setPaysId(1L);
        u.setRole(role);
        return u;
    }

    @Test
    void getUserInfo_returnsCorrectPublicFields() {
        Role role = new Role();
        role.setId(2L);
        role.setFrenchName("RH Manager");
        role.setPermissions(List.of("RH_READ", "GET_USERS"));

        Pays pays = new Pays();
        pays.setId(1L);
        pays.setIsoCode("TN");

        when(userRepository.findById(7L)).thenReturn(Optional.of(buildUser(7L, role)));
        when(paysRepository.findById(1L)).thenReturn(Optional.of(pays));

        MeResponse response = userService.getUserInfo(7L);

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getFullName()).isEqualTo("Alice Martin");
        assertThat(response.getEmail()).isEqualTo("alice@corp.com");
        assertThat(response.getAzureUpn()).isEqualTo("alice@corp.onmicrosoft.com");
        assertThat(response.getRoleId()).isEqualTo(2L);
        assertThat(response.getRoleName()).isEqualTo("RH Manager");
        assertThat(response.getPermissions()).containsExactlyInAnyOrder("RH_READ", "GET_USERS");
        assertThat(response.getPaysId()).isEqualTo(1L);
        assertThat(response.getIsoCode()).isEqualTo("TN");
        assertThat(response.getEmployeeId()).isEqualTo("EMP001");
    }

    @Test
    void getUserInfo_neverExposesSensitiveFields() throws Exception {
        Role role = new Role();
        role.setId(1L);
        role.setFrenchName("Collaborateur");
        role.setPermissions(List.of());

        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, role)));
        when(paysRepository.findById(1L)).thenReturn(Optional.empty());

        MeResponse response = userService.getUserInfo(1L);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);

        assertThat(json).doesNotContain("hashed-secret");
        assertThat(json).doesNotContain("refresh-uuid-secret");
        assertThat(json).doesNotContain("ms-access-secret");
        assertThat(json).doesNotContain("ms-refresh-secret");
        assertThat(json).doesNotContain("oid-secret");
    }

    @Test
    void getUserInfo_userNotFound_throwsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserInfo(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void getUserInfo_noRole_returnsNullRoleFieldsAndEmptyPermissions() {
        User user = buildUser(3L, null);
        user.setRole(null);

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(paysRepository.findById(1L)).thenReturn(Optional.empty());

        MeResponse response = userService.getUserInfo(3L);

        assertThat(response.getRoleId()).isNull();
        assertThat(response.getRoleName()).isNull();
        assertThat(response.getPermissions()).isEmpty();
        assertThat(response.getIsoCode()).isNull();
    }
}
