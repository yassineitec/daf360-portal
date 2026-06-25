package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "employee_profiles")
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "pays_id")
    private Long paysId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "passport_number", length = 50)
    private String passportNumber;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "personal_email", length = 255)
    private String personalEmail;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "emergency_contact_name", length = 255)
    private String emergencyContactName;

    @Column(name = "emergency_contact_relation", length = 100)
    private String emergencyContactRelation;

    @Column(name = "emergency_contact_phone", length = 50)
    private String emergencyContactPhone;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "contract_type", length = 50)
    private String contractType;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Column(name = "is_on_probation")
    private Boolean isOnProbation;

    @Column(name = "staff_type", length = 50)
    private String staffType;

    @Column(name = "regime_template_id")
    private Long regimeTemplateId;

    @Column(name = "regime_start_date")
    private LocalDate regimeStartDate;

    @Column(name = "regime_end_date")
    private LocalDate regimeEndDate;

    @Column(name = "regime_reason", length = 500)
    private String regimeReason;

    @Column(name = "iban", length = 50)
    private String iban;

    @Column(name = "bank_account_number", length = 100)
    private String bankAccountNumber;

    @Column(name = "rib", length = 50)
    private String rib;

    @Column(name = "social_security_number", length = 50)
    private String socialSecurityNumber;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "lifecycle_status", length = 50)
    private String lifecycleStatus;

    @Column(name = "lifecycle_status_code", length = 50)
    private String lifecycleStatusCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "cnss_number", length = 50)
    private String cnssNumber;

    @Column(name = "cnss_affiliation_date")
    private LocalDate cnssAffiliationDate;

    @Column(name = "marital_status", length = 50)
    private String maritalStatus;

    @Column(name = "number_of_children")
    private Integer numberOfChildren;

    @Column(name = "personal_address", length = 500)
    private String personalAddress;

    @Column(name = "home_address", length = 500)
    private String homeAddress;

    @Column(name = "candidate_id")
    private Long candidateId;

    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    @Column(name = "cin_city", length = 100)
    private String cinCity;

    @Column(name = "cin_date")
    private LocalDate cinDate;

    @Column(name = "nationality_id")
    private Long nationalityId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "discipline_id")
    private Discipline discipline;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grade_id")
    private Grade grade;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nog_level_id")
    private NogLevel nogLevel;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "salaire_net_candidat", precision = 18, scale = 2)
    private BigDecimal salaireNetCandidat;

    @Column(name = "salaire_net_rh", precision = 18, scale = 2)
    private BigDecimal salaireNetRh;

    @Column(name = "direct_technical_manager_id")
    private Long directTechnicalManagerId;

    @Column(name = "direct_hierarchical_manager_id")
    private Long directHierarchicalManagerId;

    @Column(name = "current_contract_id")
    private Long currentContractId;
}
