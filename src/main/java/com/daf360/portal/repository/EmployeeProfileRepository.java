package com.daf360.portal.repository;

import com.daf360.portal.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByUserId(Long userId);

    @Query("""
            SELECT p FROM EmployeeProfile p
            WHERE p.hireDate >= :since
            AND (:paysId IS NULL OR p.paysId = :paysId)
            ORDER BY p.hireDate DESC
            """)
    List<EmployeeProfile> findRecentHires(
            @Param("since") LocalDate since,
            @Param("paysId") Long paysId);
}
