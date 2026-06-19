package com.daf360.portal.controller;

import com.daf360.portal.dto.PortalDepartmentDto;
import com.daf360.portal.dto.PortalEmployeeDto;
import com.daf360.portal.entity.EmployeeProfile;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.EmployeeProfileRepository;
import com.daf360.portal.repository.RoleRepository;
import com.daf360.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class DirectoryController {

    private final UserRepository          userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final RoleRepository          roleRepository;

    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('GET_USERS')")
    public Map<String, Object> listEmployees(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "18") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        Page<User> usersPage = userRepository.findByIsActiveTrueOrIsActiveIsNull(pageable);

        List<PortalEmployeeDto> content = usersPage.getContent().stream()
                .map(u -> toDto(u, profileRepository.findByUserId(u.getId()).orElse(null)))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       content);
        result.put("totalElements", usersPage.getTotalElements());
        result.put("totalPages",    usersPage.getTotalPages());
        result.put("number",        usersPage.getNumber());
        result.put("size",          usersPage.getSize());
        return result;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('GET_USERS')")
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
        dto.setPhone(null);
        dto.setStatus(Boolean.FALSE.equals(u.getIsActive()) ? "INACTIVE" : "ACTIVE");
        dto.setContractType("CDI");
        dto.setHireDate(profile != null && profile.getHireDate() != null
                ? profile.getHireDate().toString() : null);
        dto.setDepartmentId(u.getRole() != null ? u.getRole().getId() : null);
        dto.setPhotoUrl(profile != null ? profile.getPhotoUrl() : null);
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
