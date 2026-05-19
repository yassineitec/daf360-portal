package com.daf360.portal.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// Safe user info — NEVER include password, refreshToken, azureOid, ms365* tokens
@Data
@Builder
public class MeResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String azureUpn;
    private Long roleId;
    private String roleName;
    private List<String> permissions;
    private Long paysId;
    private String isoCode;
    private String employeeId;
}
