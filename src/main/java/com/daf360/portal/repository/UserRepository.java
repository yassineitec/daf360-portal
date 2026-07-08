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

    @Query("""
            SELECT u FROM User u
            WHERE (u.isActive = true OR u.isActive IS NULL)
            AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:departmentId IS NULL OR EXISTS (
                SELECT 1 FROM EmployeeProfile p
                WHERE p.userId = u.id AND p.department.id = :departmentId))
            """)
    Page<User> search(
            @Param("search") String search,
            @Param("departmentId") Long departmentId,
            Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = null WHERE u.id = :userId")
    void clearRefreshToken(@Param("userId") Long userId);
}
