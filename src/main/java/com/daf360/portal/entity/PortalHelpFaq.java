package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * A question / answer typed by an administrator in the shell's « Aide & guides » page
 * (« Gérer les questions »), shown by the help assistant and the help page next to the FAQ
 * shipped with the app. Company-wide. French is required; English and Arabic fall back to it.
 *
 * <p>Own table, not the RH `help_faq`: both services share the DAF360_HR database, and the
 * pages of the portal (`home`, `self-service`, `notifications`) are not RH pages.
 * Table created by docs/sql/2026-11-16_portal_help_faq.sql (or by `ddl-auto: update` in local).
 */
@Entity
@Table(name = "portal_help_faq")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalHelpFaq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Guide key of the page the question is about (shell `GUIDE_PAGES.guideKey`, e.g. "home"). */
    @Column(name = "page_key", nullable = false, length = 80, columnDefinition = "nvarchar(80)")
    private String pageKey;

    @Column(name = "question_fr", nullable = false, length = 300, columnDefinition = "nvarchar(300)")
    private String questionFr;

    @Column(name = "answer_fr", nullable = false, length = 2000, columnDefinition = "nvarchar(2000)")
    private String answerFr;

    @Column(name = "question_en", length = 300, columnDefinition = "nvarchar(300)")
    private String questionEn;

    @Column(name = "answer_en", length = 2000, columnDefinition = "nvarchar(2000)")
    private String answerEn;

    @Column(name = "question_ar", length = 300, columnDefinition = "nvarchar(300)")
    private String questionAr;

    @Column(name = "answer_ar", length = 2000, columnDefinition = "nvarchar(2000)")
    private String answerAr;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    /** Published: shown by the assistant and the help page. Inactive = kept but hidden. */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetimeoffset(6)")
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at", columnDefinition = "datetimeoffset(6)")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
