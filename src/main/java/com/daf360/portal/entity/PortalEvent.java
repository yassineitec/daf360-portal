package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "portal_events")
public class PortalEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType; // "COMPANY_EVENT", "ANNOUNCEMENT", "OTHER"

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "pays_id")
    private Long paysId; // null = visible to all countries

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
