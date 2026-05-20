package com.daf360.portal.repository;

import com.daf360.portal.entity.PortalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PortalEventRepository extends JpaRepository<PortalEvent, Long> {
    @Query("SELECT e FROM PortalEvent e WHERE e.deleted = false " +
           "AND e.eventDate BETWEEN :from AND :to " +
           "AND (:paysId IS NULL OR e.paysId IS NULL OR e.paysId = :paysId) " +
           "ORDER BY e.eventDate ASC")
    List<PortalEvent> findActiveEventsInRange(@Param("from") LocalDate from,
                                               @Param("to") LocalDate to,
                                               @Param("paysId") Long paysId);

    @Query("SELECT e FROM PortalEvent e WHERE e.deleted = false " +
           "AND e.eventDate BETWEEN :from AND :to " +
           "AND (:paysId IS NULL OR e.paysId IS NULL OR e.paysId = :paysId) " +
           "ORDER BY e.eventDate ASC")
    List<PortalEvent> findUpcoming(@Param("from") LocalDate from,
                                    @Param("to") LocalDate to,
                                    @Param("paysId") Long paysId);
}
