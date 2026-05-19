package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "fullName", length = 255)
    private String fullName;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "isActive")
    private Boolean isActive = true;

    @Column(name = "azure_oid", length = 100)
    private String azureOid;

    @Column(name = "azure_upn", length = 255)
    private String azureUpn;

    @Column(name = "ms365_access_token", columnDefinition = "nvarchar(max)")
    private String ms365AccessToken;

    @Column(name = "ms365_refresh_token", length = 500)
    private String ms365RefreshToken;

    // Portal UUID refresh token — separate from MS365 refresh token
    @Column(name = "refresh_token", length = 36)
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    // employee_id is VARCHAR(30) in the schema, not a Long
    @Column(name = "employee_id", length = 30)
    private String employeeId;

    @Column(name = "manager_id")
    private Long managerId;

    // pays_id is NOT NULL in the schema — always set before saving a new user
    @Column(name = "pays_id", nullable = false)
    private Long paysId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleId")
    private Role role;
}
