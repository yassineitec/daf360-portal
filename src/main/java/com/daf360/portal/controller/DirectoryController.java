package com.daf360.portal.controller;

import com.daf360.portal.dto.EmployeePageResponse;
import com.daf360.portal.dto.PortalEmployeeDto;
import com.daf360.portal.entity.EmployeeProfile;
import com.daf360.portal.entity.Localized;
import com.daf360.portal.entity.Pays;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.EmployeeProfileRepository;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class DirectoryController {

        private final UserRepository userRepository;
        private final EmployeeProfileRepository profileRepository;
        private final PaysRepository paysRepository;

        @GetMapping("/employees")
        public EmployeePageResponse listEmployees(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) Long departmentId,
                        @RequestParam(defaultValue = "fr") String lang) {

                // Unsorted on purpose: `search` carries its own ORDER BY (newest hire
                // first, see UserRepository). A Sort here is appended after it, so it
                // could only ever act as a tiebreaker behind the unique u.id.
                Pageable pageable = PageRequest.of(page, size);

                Page<User> usersPage = userRepository.search(search, departmentId, pageable);

                List<PortalEmployeeDto> content = usersPage.getContent()
                                .stream()
                                .map(u -> toDto(u, profileRepository.findByUserId(u.getId()).orElse(null), lang))
                                .toList();

                return new EmployeePageResponse(
                                content,
                                usersPage.getTotalElements(),
                                usersPage.getTotalPages(),
                                usersPage.getNumber(),
                                usersPage.getSize());
        }

        @GetMapping("/employees/recent")
        @PreAuthorize("isAuthenticated()")
        public List<PortalEmployeeDto> recentEmployees(
                        @RequestParam(defaultValue = "30") int days,
                        @RequestParam(required = false) Long paysId,
                        @RequestParam(defaultValue = "fr") String lang) {
                LocalDate since = LocalDate.now().minusDays(days);
                return profileRepository.findRecentHires(since, paysId).stream()
                                .map(profile -> userRepository.findById(profile.getUserId())
                                                .map(user -> toDto(user, profile, lang))
                                                .orElse(null))
                                .filter(dto -> dto != null)
                                .toList();
        }

        // ── helpers ────────────────────────────────────────────────────────────────

        /**
         * @param maybeProfile the user's HR profile, or {@code null} — a user can exist
         *                     with no {@code employee_profiles} row at all (MS365
         *                     provisioning creates the account before HR fills the
         *                     profile), so this is a normal case, not an edge case.
         */
        private PortalEmployeeDto toDto(User u, EmployeeProfile maybeProfile, String lang) {
                boolean en = "en".equalsIgnoreCase(lang);

                // Substitute an all-null profile rather than guarding each field on
                // `maybeProfile != null`. Ten hand-written guards used to say the same
                // thing and a single missing one 500'd the whole directory page — which
                // stayed invisible until a search happened to match a profile-less user.
                // With no null in scope, that class of bug cannot come back, and a field
                // added below needs no guard to be safe.
                EmployeeProfile profile = maybeProfile != null ? maybeProfile : new EmployeeProfile();

                // One lookup, not two — and `findById` rejects a null id outright, so a
                // user with no pays_id must not reach it.
                Pays pays = u.getPaysId() != null
                                ? paysRepository.findById(u.getPaysId()).orElse(null)
                                : null;

                PortalEmployeeDto dto = new PortalEmployeeDto();
                dto.setId(u.getId());
                dto.setMatricule(u.getEmployeeId() != null ? u.getEmployeeId() : "USR-" + u.getId());

                String[] parts = splitName(u.getFullName());
                dto.setFirstName(parts[0]);
                dto.setLastName(parts[1]);
                dto.setEmail(u.getEmail());
                dto.setPosition(u.getRole() != null ? u.getRole().getFrenchName() : null);
                dto.setGrade(label(profile.getGrade(), en));
                // Discipline and NOG level stay French-only, as they were — the labels
                // the UI shows are not this fix's business to change.
                dto.setDiscipline(labelFr(profile.getDiscipline()));
                dto.setNogLevel(labelFr(profile.getNogLevel()));
                dto.setDepartment(label(profile.getDepartment(), en));
                dto.setStaffType(staffTypeLabel(profile.getStaffType(), en));
                dto.setPhone(profile.getPhone());
                dto.setStatus(Boolean.FALSE.equals(u.getIsActive()) ? "INACTIVE" : "ACTIVE");
                dto.setContractType(profile.getContractType() != null ? profile.getContractType() : "CDI");
                dto.setHireDate(profile.getHireDate() != null ? profile.getHireDate().toString() : null);
                dto.setDepartmentId(profile.getDepartment() != null ? profile.getDepartment().getId() : null);
                dto.setPhotoUrl(profile.getPhotoUrl());
                dto.setCountry(pays != null ? pays.getFrenchLabel() : null);
                dto.setCountryIsoCode(pays != null ? pays.getIsoCode() : null);
                dto.setGender(profile.getGender());
                return dto;
        }

        /** Localized label of a reference row, null-safe in both the row and the label. */
        private static String label(Localized ref, boolean en) {
                if (ref == null)
                        return null;
                return en ? ref.getLabelEn() : ref.getLabelFr();
        }

        /** French label of a reference row, null-safe. */
        private static String labelFr(Localized ref) {
                return ref != null ? ref.getLabelFr() : null;
        }

        private String[] splitName(String fullName) {
                if (fullName == null || fullName.isBlank())
                        return new String[] { "", "" };
                int i = fullName.indexOf(' ');
                if (i < 0)
                        return new String[] { fullName, "" };
                return new String[] { fullName.substring(0, i), fullName.substring(i + 1) };
        }

        /**
         * Maps the staff_type code (TECHNICAL / OPERATIONS_SUPPORT) to a localized
         * label.
         */
        private String staffTypeLabel(String code, boolean en) {
                if (code == null)
                        return null;
                return switch (code) {
                        case "TECHNICAL" -> en ? "Technical" : "Technique";
                        case "OPERATIONS_SUPPORT" -> en ? "Operations Support" : "Support opérations";
                        default -> code;
                };
        }
}
