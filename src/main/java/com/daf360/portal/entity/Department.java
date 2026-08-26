package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "departments")
public class Department implements Localized {

    @Id
    private Long id;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "label_en", length = 255)
    private String labelEn;

    @Column(name = "label_fr", length = 255)
    private String labelFr;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "pays_id")
    private Long paysId;
}
