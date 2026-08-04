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

    /**
     * Anniversary anchors for the calendar: only the four columns the birthday feed
     * actually needs, joined to the user's name in ONE query.
     *
     * Replaces `findAll()` + a `userRepository.findById` per profile. That loaded every
     * employee_profiles row (with photo_url, national_id, passport_number and the rest)
     * into memory and then issued one extra query per profile — hundreds of round-trips
     * to render one month of a calendar.
     *
     * Rows with neither anchor date are dropped in SQL; the month filter cannot be done
     * here because a recurring anniversary has to be matched on month/day, not on the
     * stored year (see BirthdayService).
     */
    @Query("""
            SELECT p.id, p.paysId, p.dateOfBirth, p.hireDate, u.fullName
            FROM EmployeeProfile p
            LEFT JOIN User u ON u.id = p.userId
            WHERE (:paysId IS NULL OR p.paysId = :paysId)
              AND (p.dateOfBirth IS NOT NULL OR p.hireDate IS NOT NULL)
            """)
    List<Object[]> findAnniversaryAnchors(@Param("paysId") Long paysId);
}
