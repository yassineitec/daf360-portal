package com.daf360.portal.repository;

import com.daf360.portal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByAzureOid(String azureOid);

    Optional<User> findByEmail(String email);

    // Looks up by portal UUID refresh token (the `refresh_token` column, field:
    // refreshToken)
    Optional<User> findByRefreshToken(String refreshToken);

    Page<User> findByIsActiveTrueOrIsActiveIsNull(Pageable pageable);

    /**
     * L'annuaire : le personnel visible par l'appelant, embauche la plus récente d'abord.
     *
     * <h3>Ce que la liste écarte, et pourquoi</h3>
     *
     * <b>Les comptes qui ne sont pas des personnes</b> — {@code u.isEmployee}. C'est le
     * prédicat de V96, et l'annuaire est son consommateur le plus évident : « TimeSheet
     * TUN », « TimeSheet Egypte », « test test » et une vingtaine d'utilisateurs de
     * démonstration s'affichaient au milieu du personnel. Écrit exactement comme
     * {@code UserScope.realPeople} côté rh-service — {@code = true}, sans tolérance au
     * null : la colonne est NOT NULL DEFAULT 1 (V96), et deux écritures différentes du
     * même prédicat finiraient par faire diverger l'annuaire de la liste des profils.
     *
     * <b>Les autres pays</b> — la portée du rôle, résolue par le portail lui-même quand il
     * signe le jeton. L'annuaire n'avait AUCUN filtre pays : chacun voyait le personnel de
     * toutes les entités.
     *
     * <b>Ceux qui ne sont pas dans l'effectif</b> — un profil en `PRE_ONBOARDING`,
     * `OFFBOARDING`, `TERMINATED` ou `ARCHIVED` n'est pas dans l'annuaire : il est dans un
     * autre état, avec son propre écran côté RH.
     *
     * <h3>Pourquoi un NOT EXISTS et non une jointure sur les trois statuts</h3>
     *
     * C'est le point délicat, et l'inverse de ce qu'on ferait spontanément. Exiger un
     * profil dont le statut est ACTIVE / ON_LEAVE / ON_MISSION reviendrait à exiger un
     * profil — et {@code UserScope} (rh-service) documente pourquoi c'est faux : sur cette
     * base, 155 des 258 comptes actifs n'ont pas de ligne `employee_profiles`, et
     * l'immense majorité sont de VRAIES personnes — le DRH, le PDG, le responsable IT, des
     * assistantes RH, une vingtaine de chefs de chantier, une centaine de collaborateurs
     * dont le dossier RH n'est simplement pas rempli. L'absence de profil veut dire « le
     * dossier RH est incomplet », jamais « ce n'est pas une personne ». Cette jointure
     * aurait donc effacé la direction de l'entreprise de son propre annuaire.
     *
     * On écarte donc ceux dont on SAIT qu'ils sont ailleurs, et on garde ceux dont on ne
     * sait rien. `is_employee` porte déjà la question « est-ce une personne ? », et c'est
     * la bonne colonne pour ça.
     */
    @Query("""
                SELECT u FROM User u
                WHERE (u.isActive = true OR u.isActive IS NULL)
                  AND u.isEmployee = true
                  AND (:scopeAll = true OR u.paysId IN (:paysIds))
                  AND NOT EXISTS (
                        SELECT 1
                        FROM EmployeeProfile x
                        WHERE x.userId = u.id
                          AND (x.deleted IS NULL OR x.deleted = false)
                          AND x.lifecycleStatus IN ('PRE_ONBOARDING', 'OFFBOARDING',
                                                     'TERMINATED', 'ARCHIVED')
                  )
                  AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
                  AND (:departmentId IS NULL OR EXISTS (
                        SELECT 1
                        FROM EmployeeProfile p
                        WHERE p.userId = u.id
                          AND p.department.id = :departmentId
                  ))
                ORDER BY
                CASE WHEN (
                    SELECT MAX(p.hireDate)
                    FROM EmployeeProfile p
                    WHERE p.userId = u.id
                ) IS NULL THEN 1 ELSE 0 END,
                (
                    SELECT MAX(p.hireDate)
                    FROM EmployeeProfile p
                    WHERE p.userId = u.id
                ) DESC,
                u.id DESC
            """)
    Page<User> search(
            @Param("search") String search,
            @Param("departmentId") Long departmentId,
            @Param("scopeAll") boolean scopeAll,
            @Param("paysIds") java.util.Collection<Long> paysIds,
            Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = null WHERE u.id = :userId")
    void clearRefreshToken(@Param("userId") Long userId);
}
