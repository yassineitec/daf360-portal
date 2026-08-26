package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "disciplines")
public class Discipline implements Localized {

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

    @Column(name = "pays_id")
    private Long paysId;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
