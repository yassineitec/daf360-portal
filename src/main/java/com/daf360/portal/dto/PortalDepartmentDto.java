package com.daf360.portal.dto;

import lombok.Data;

@Data
public class PortalDepartmentDto {
    private Long   id;
    private String name;
    private String code;
    private Long   managerId;
    private Long   parentId;
}
