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
     * Directory listing, newest hire first. The date lives on EmployeeProfile, so it
     * is read through a correlated subquery (MAX, because a user can carry more than
     * one profile row). Users with no profile — or a profile with no hire_date — have
     * nothing to date; the leading CASE sinks them to the bottom instead of letting
     * SQL Server sort NULL first and park them at the top of page 1. `u.id DESC` is
     * the tiebreaker that keeps OFFSET paging deterministic for a shared hire date.
     */
    @Query("""
                SELECT u FROM User u
                WHERE (u.isActive = true OR u.isActive IS NULL)
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
            Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = null WHERE u.id = :userId")
    void clearRefreshToken(@Param("userId") Long userId);
}
