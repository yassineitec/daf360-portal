package com.daf360.portal.controller;

import com.daf360.portal.dto.EmployeePageResponse;
import com.daf360.portal.dto.PortalEmployeeDto;
import com.daf360.portal.entity.EmployeeProfile;
import com.daf360.portal.entity.Pays;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.EmployeeProfileRepository;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The directory has to survive rows the HR side has not filled in yet. A user with no
 * `employee_profiles` row is normal — MS365 provisioning creates the account first —
 * and one un-guarded getter on that null profile used to 500 the whole page. It went
 * unnoticed on first load and only appeared once a search matched such a user, so
 * these tests pin the profile-less shapes rather than the happy path.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DirectoryControllerTest {

    @Mock UserRepository            userRepository;
    @Mock EmployeeProfileRepository profileRepository;
    @Mock PaysRepository            paysRepository;

    private DirectoryController controller() {
        return new DirectoryController(userRepository, profileRepository, paysRepository);
    }

    private User user(long id, String fullName, Long paysId) {
        User u = new User();
        u.setId(id);
        u.setFullName(fullName);
        u.setEmail(fullName.replace(' ', '.') + "@itecgroupe.com");
        u.setPaysId(paysId);
        return u;
    }

    private void givenUsers(User... users) {
        Page<User> page = new PageImpl<>(List.of(users));
        when(userRepository.search(any(), any(), any(Pageable.class))).thenReturn(page);
    }

    private void givenPays(long id, String label, String iso) {
        Pays pays = new Pays();
        pays.setId(id);
        pays.setFrenchLabel(label);
        pays.setIsoCode(iso);
        when(paysRepository.findById(id)).thenReturn(Optional.of(pays));
    }

    @Test
    void listEmployees_withNoProfileRow_returnsTheUserWithNullProfileFields() {
        givenUsers(user(1L, "Karim Bensalah", 1L));
        givenPays(1L, "Tunisie", "TN");
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        EmployeePageResponse res = controller().listEmployees(0, 20, "k", null, "fr");

        assertThat(res.content()).hasSize(1);
        PortalEmployeeDto dto = res.content().get(0);
        assertThat(dto.getFirstName()).isEqualTo("Karim");
        assertThat(dto.getLastName()).isEqualTo("Bensalah");
        assertThat(dto.getCountry()).isEqualTo("Tunisie");
        // Everything that lives on the profile comes back empty rather than blowing up.
        assertThat(dto.getGrade()).isNull();
        assertThat(dto.getDiscipline()).isNull();
        assertThat(dto.getNogLevel()).isNull();
        assertThat(dto.getDepartment()).isNull();
        assertThat(dto.getDepartmentId()).isNull();
        assertThat(dto.getPhone()).isNull();
        assertThat(dto.getHireDate()).isNull();
        assertThat(dto.getPhotoUrl()).isNull();
        assertThat(dto.getGender()).isNull();
        // The one profile field with a default keeps it.
        assertThat(dto.getContractType()).isEqualTo("CDI");
    }

    /** The failing request was a search; a profile-less match must not take the page down. */
    @Test
    void listEmployees_mixedPage_keepsBothTheProfiledAndTheProfilelessUser() {
        givenUsers(user(1L, "Karim Bensalah", 1L), user(2L, "Kenza Mrad", 1L));
        givenPays(1L, "Tunisie", "TN");

        EmployeeProfile profile = new EmployeeProfile();
        profile.setUserId(2L);
        profile.setPhone("+216 55 000 000");
        profile.setHireDate(LocalDate.of(2026, 3, 1));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));

        EmployeePageResponse res = controller().listEmployees(0, 20, "k", null, "fr");

        assertThat(res.content()).extracting(PortalEmployeeDto::getLastName)
                .containsExactly("Bensalah", "Mrad");
        assertThat(res.content().get(0).getHireDate()).isNull();
        assertThat(res.content().get(1).getHireDate()).isEqualTo("2026-03-01");
    }

    /** `findById(null)` throws, so a user with no pays_id must never reach the lookup. */
    @Test
    void listEmployees_withNoPaysId_skipsTheCountryLookup() {
        givenUsers(user(3L, "Sans Pays", null));
        when(profileRepository.findByUserId(3L)).thenReturn(Optional.empty());

        EmployeePageResponse res = controller().listEmployees(0, 20, null, null, "fr");

        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).getCountry()).isNull();
        assertThat(res.content().get(0).getCountryIsoCode()).isNull();
        verify(paysRepository, never()).findById(anyLong());
    }

    @Test
    void listEmployees_withNoProfileRow_survivesTheEnglishLocaleToo() {
        givenUsers(user(1L, "Karim Bensalah", 1L));
        givenPays(1L, "Tunisie", "TN");
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        EmployeePageResponse res = controller().listEmployees(0, 20, "k", null, "en");

        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).getGrade()).isNull();
        assertThat(res.content().get(0).getDepartment()).isNull();
    }

    /** A name with no space at all — `splitName` returns an empty last name, not an index crash. */
    @Test
    void listEmployees_withSingleWordName_doesNotThrow() {
        givenUsers(user(4L, "Cher", 1L));
        givenPays(1L, "Tunisie", "TN");
        when(profileRepository.findByUserId(4L)).thenReturn(Optional.empty());

        EmployeePageResponse res = controller().listEmployees(0, 20, null, null, "fr");

        assertThat(res.content().get(0).getFirstName()).isEqualTo("Cher");
        assertThat(res.content().get(0).getLastName()).isEmpty();
    }
}
