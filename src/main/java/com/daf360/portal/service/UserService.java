package com.daf360.portal.service;

import com.daf360.portal.dto.MeResponse;
import com.daf360.portal.entity.Pays;
import com.daf360.portal.entity.Role;
import com.daf360.portal.entity.User;
import com.daf360.portal.repository.EmployeeProfileRepository;
import com.daf360.portal.repository.PaysRepository;
import com.daf360.portal.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PaysRepository paysRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    @Cacheable(value = "userInfo", key = "#userId")
    @Transactional(readOnly = true)
    public MeResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        String isoCode = paysRepository.findById(user.getPaysId())
            .map(Pays::getIsoCode)
            .orElse(null);

        Role role = user.getRole();
        List<String> permissions = role != null ? role.getPermissions() : List.of();

        String photoUrl = employeeProfileRepository.findByUserId(userId)
            .map(ep -> ep.getPhotoUrl())
            .orElse(null);

        return MeResponse.builder()
            .userId(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .azureUpn(user.getAzureUpn())
            .roleId(role != null ? role.getId() : null)
            .roleName(role != null ? role.getFrenchName() : null)
            .permissions(permissions)
            .paysId(user.getPaysId())
            .isoCode(isoCode)
            .employeeId(user.getEmployeeId())
            .photoUrl(photoUrl)
            .build();
        // Sensitive fields intentionally omitted:
        // password, refreshToken, azureOid, ms365AccessToken, ms365RefreshToken
    }
}
