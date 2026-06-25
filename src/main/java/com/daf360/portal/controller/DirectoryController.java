package com.daf360.portal.controller;

import com.daf360.portal.dto.EmployeePageResponse;
import com.daf360.portal.dto.PortalDepartmentDto;
import com.daf360.portal.dto.PortalEmployeeDto;
import com.daf360.portal.entity.EmployeeProfile;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.EmployeeProfileRepository;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.RoleRepository;
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
    private final RoleRepository          roleRepository;
    private final PaysRepository          paysRepository;

@GetMapping("/employees")
public EmployeePageResponse listEmployees(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long departmentId
) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());

    Page<User> usersPage =
            userRepository.search(search, departmentId, pageable);

    List<PortalEmployeeDto> content = usersPage.getContent()
            .stream()
            .map(u -> toDto(u, profileRepository.findByUserId(u.getId()).orElse(null)))
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
            @RequestParam(required = false) Long paysId) {
        LocalDate since = LocalDate.now().minusDays(days);
        return profileRepository.findRecentHires(since, paysId).stream()
                .map(profile -> userRepository.findById(profile.getUserId())
                        .map(user -> toDto(user, profile))
                        .orElse(null))
                .filter(dto -> dto != null)
                .toList();
    }

    @GetMapping("/departments")
    @PreAuthorize("isAuthenticated()")
    public List<PortalDepartmentDto> listDepartments() {
        return roleRepository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .map(r -> {
                    PortalDepartmentDto dto = new PortalDepartmentDto();
                    dto.setId(r.getId());
                    dto.setName(r.getFrenchName());
                    dto.setCode(makeCode(r.getFrenchName()));
                    dto.setManagerId(null);
                    dto.setParentId(null);
                    return dto;
                })
                .toList();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private PortalEmployeeDto toDto(User u, EmployeeProfile profile) {
        PortalEmployeeDto dto = new PortalEmployeeDto();
        dto.setId(u.getId());
        dto.setMatricule(u.getEmployeeId() != null ? u.getEmployeeId() : "USR-" + u.getId());

        String[] parts = splitName(u.getFullName());
        dto.setFirstName(parts[0]);
        dto.setLastName(parts[1]);

        dto.setEmail(u.getEmail());
        dto.setPosition(u.getRole() != null ? u.getRole().getFrenchName() : null);
        dto.setGrade(profile != null && profile.getGrade() != null
                ? profile.getGrade().getLabelFr() : null);
        dto.setDiscipline(profile != null && profile.getDiscipline() != null
                ? profile.getDiscipline().getLabelFr() : null);
        dto.setNogLevel(profile != null && profile.getNogLevel() != null
                ? profile.getNogLevel().getLabelFr() : null);
        dto.setDepartment(profile != null && profile.getDepartment() != null
                ? profile.getDepartment().getLabelFr()
                : (u.getRole() != null ? u.getRole().getFrenchName() : null));
        dto.setPhone(profile != null ? profile.getPhone() : null);
        dto.setStatus(Boolean.FALSE.equals(u.getIsActive()) ? "INACTIVE" : "ACTIVE");
        dto.setContractType(profile != null && profile.getContractType() != null
                ? profile.getContractType() : "CDI");
        dto.setHireDate(profile != null && profile.getHireDate() != null
                ? profile.getHireDate().toString() : null);
        dto.setDepartmentId(profile != null && profile.getDepartment() != null
                ? profile.getDepartment().getId()
                : (u.getRole() != null ? u.getRole().getId() : null));
        dto.setPhotoUrl(profile != null ? profile.getPhotoUrl() : null);
        dto.setCountry(paysRepository.findById(u.getPaysId())
                .map(p -> p.getFrenchLabel())
                .orElse(null));
        return dto;
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        int i = fullName.indexOf(' ');
        if (i < 0) return new String[]{fullName, ""};
        return new String[]{fullName.substring(0, i), fullName.substring(i + 1)};
    }

    /** Derives a short 3-letter code from a role name (e.g. "Administrateur" → "ADM"). */
    private String makeCode(String name) {
        if (name == null || name.isBlank()) return "OTH";
        String letters = name.replaceAll("[^A-Za-zÀ-ÿ]", "").toUpperCase();
        return letters.substring(0, Math.min(3, letters.length()));
    }
}
