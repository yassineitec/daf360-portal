package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Getter @Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {

    // The audit_log table uses a SQL SEQUENCE named audit_log_SEQ (not IDENTITY)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_gen")
    @SequenceGenerator(
        name = "audit_log_gen",
        sequenceName = "audit_log_SEQ",
        allocationSize = 50
    )
    private Long id;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    // userId is VARCHAR in the schema (not BIGINT) — store as String
    @Column(name = "userId", length = 50)
    private String userId;

    @Column(name = "ipAddress", length = 50)
    private String ipAddress;

    // The column is named `timestamp`, not `createdAt`
    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "entityId", length = 100)
    private String entityId;

    @Column(name = "entityType", length = 100)
    private String entityType;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "pays_id")
    private Long paysId;
}
