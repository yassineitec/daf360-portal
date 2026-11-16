package com.daf360.portal.repository;

import com.daf360.portal.entity.PortalHelpFaq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortalHelpFaqRepository extends JpaRepository<PortalHelpFaq, Long> {

    List<PortalHelpFaq> findAllByOrderByPageKeyAscOrderIndexAscIdAsc();

    List<PortalHelpFaq> findByIsActiveTrueOrderByPageKeyAscOrderIndexAscIdAsc();
}
