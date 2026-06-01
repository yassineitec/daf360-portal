package com.daf360.portal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter @Setter
@Entity
@Table(name = "Roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "frenchName", nullable = false, length = 100)
    private String frenchName;

    @Column(name = "englishName", length = 100)
    private String englishName;

    @Column(name = "showAll")
    private Boolean showAll;

    @Column(name = "deleted")
    private Boolean deleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    @JsonIgnore
    private Role parentRole;

    // Children in the hierarchy — used to collect inherited permissions recursively
    @OneToMany(mappedBy = "parentRole", fetch = FetchType.EAGER)
    @JsonIgnore
    private Set<Role> subordinateRoles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "RolePermissions",
        joinColumns = @JoinColumn(name = "role_id")
    )
    @Column(name = "permission")
    private List<String> permissions = new ArrayList<>();
}
