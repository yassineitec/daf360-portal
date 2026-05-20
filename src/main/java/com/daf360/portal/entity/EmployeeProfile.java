package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "employee_profiles")
public class EmployeeProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "pays_id")
    private Long paysId;
}
