package com.daf360.portal.dto;

import java.util.List;

public record EmployeePageResponse(
        List<PortalEmployeeDto> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {}