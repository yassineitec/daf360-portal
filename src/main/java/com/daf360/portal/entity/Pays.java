package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Pays")
public class Pays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iso_code", length = 10)
    private String isoCode;

    @Column(name = "french_label", length = 255)
    private String frenchLabel;
}
