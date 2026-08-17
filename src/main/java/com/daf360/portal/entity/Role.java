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

    /**
     * OWN | LIST | ALL — how paysScope below is read (V74__role_pays_scope.sql).
     * Plain String rather than an enum: the portal only branches on it, and a value written
     * by a future rh-service version must not break token minting here.
     */
    @Column(name = "pays_scope_mode", length = 10)
    private String paysScopeMode;

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

    /**
     * Countries this role may see data from (V74__role_pays_scope.sql). Empty means
     * "not configured" and falls back to the user's own Users.pays_id; it is ignored
     * entirely when showAll is true. EAGER for the same reason as permissions —
     * UserSyncService walks the role tree outside any open session.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "RolePaysScope",
        joinColumns = @JoinColumn(name = "role_id")
    )
    @Column(name = "pays_id")
    private List<Long> paysScope = new ArrayList<>();
}
