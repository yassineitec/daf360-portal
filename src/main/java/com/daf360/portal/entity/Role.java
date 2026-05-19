package com.daf360.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "parent_role_id")
    private Long parentRoleId;

    // RolePermissions table has columns: role_id (FK) + permission (varchar string)
    // This is NOT a join to a Permission entity — it is a simple string collection
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "RolePermissions",
        joinColumns = @JoinColumn(name = "role_id")
    )
    @Column(name = "permission")
    private List<String> permissions = new ArrayList<>();
}
