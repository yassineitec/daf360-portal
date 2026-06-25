package com.daf360.portal.dto;

import lombok.Data;

@Data
public class PortalEmployeeDto {
    private Long   id;
    private String matricule;
    private String firstName;
    private String lastName;
    private String email;
    private String position;
    private String grade;
    private String discipline;
    private String nogLevel;
    private String phone;
    private String status;
    private String contractType;
    private String hireDate;
    private Long departmentId;
    private String department;
    private String photoUrl;
    private String country;
}
