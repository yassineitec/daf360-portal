package com.daf360.portal.controller;

import com.daf360.portal.dto.EmployeePageResponse;
import com.daf360.portal.dto.PortalEmployeeDto;
import com.daf360.portal.entity.EmployeeProfile;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.EmployeeProfileRepository;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class DirectoryController {

    private final UserRepository          userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final PaysRepository          paysRepository;

@GetMapping("/employees")
public EmployeePageResponse listEmployees(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(defaultValue = "fr") String lang
) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());

    Page<User> usersPage =
            userRepository.search(search, departmentId, pageable);

    List<PortalEmployeeDto> content = usersPage.getContent()
            .stream()
            .map(u -> toDto(u, profileRepository.findByUserId(u.getId()).orElse(null), lang))
            .toList();

    return new EmployeePageResponse(
            content,
            usersPage.getTotalElements(),
            usersPage.getTotalPages(),
            usersPage.getNumber(),
            usersPage.getSize()
    );
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

    private PortalEmployeeDto toDto(User u, EmployeeProfile profile, String lang) {
        boolean en = "en".equalsIgnoreCase(lang);
        PortalEmployeeDto dto = new PortalEmployeeDto();
        dto.setId(u.getId());
        dto.setMatricule(u.getEmployeeId() != null ? u.getEmployeeId() : "USR-" + u.getId());

        String[] parts = splitName(u.getFullName());
        dto.setFirstName(parts[0]);
        dto.setLastName(parts[1]);

        dto.setEmail(u.getEmail());
        dto.setPosition(u.getRole() != null ? u.getRole().getFrenchName() : null);
        dto.setGrade(profile != null && profile.getGrade() != null
                ? (en ? profile.getGrade().getLabelEn() : profile.getGrade().getLabelFr()) : null);
        dto.setDiscipline(profile != null && profile.getDiscipline() != null
                ? profile.getDiscipline().getLabelFr() : null);
        dto.setNogLevel(profile != null && profile.getNogLevel() != null
                ? profile.getNogLevel().getLabelFr() : null);
        dto.setDepartment(profile != null && profile.getDepartment() != null
                ? (en ? profile.getDepartment().getLabelEn() : profile.getDepartment().getLabelFr())
                : null);
        dto.setStaffType(staffTypeLabel(profile != null ? profile.getStaffType() : null, en));
        dto.setPhone(profile != null ? profile.getPhone() : null);
        dto.setStatus(Boolean.FALSE.equals(u.getIsActive()) ? "INACTIVE" : "ACTIVE");
        dto.setContractType(profile != null && profile.getContractType() != null
                ? profile.getContractType() : "CDI");
        dto.setHireDate(profile != null && profile.getHireDate() != null
                ? profile.getHireDate().toString() : null);
        dto.setDepartmentId(profile != null && profile.getDepartment() != null
                ? profile.getDepartment().getId() : null);
        dto.setPhotoUrl(profile != null ? profile.getPhotoUrl() : null);
        dto.setCountry(paysRepository.findById(u.getPaysId())
                .map(p -> p.getFrenchLabel())
                .orElse(null));
        dto.setGender(profile != null ? profile.getGender() : null);
        return dto;
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        int i = fullName.indexOf(' ');
        if (i < 0) return new String[]{fullName, ""};
        return new String[]{fullName.substring(0, i), fullName.substring(i + 1)};
    }

    /** Maps the staff_type code (TECHNICAL / OPERATIONS_SUPPORT) to a localized label. */
    private String staffTypeLabel(String code, boolean en) {
        if (code == null) return null;
        return switch (code) {
            case "TECHNICAL"          -> en ? "Technical" : "Technique";
            case "OPERATIONS_SUPPORT" -> en ? "Operations Support" : "Support opérations";
            default -> code;
        };
    }
}
