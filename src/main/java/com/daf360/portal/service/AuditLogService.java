package com.daf360.portal.service;

import com.daf360.portal.entity.AuditLog;
import com.daf360.portal.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // userId is String (VARCHAR in schema) — never Long
    @Async
    public void log(String action, String module, String userId, String ipAddress, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setModule(module);
            entry.setUserId(userId);
            entry.setIpAddress(ipAddress);
            // Column is named `timestamp`, not `createdAt`
            entry.setTimestamp(OffsetDateTime.now());
            entry.setStatus("SUCCESS");
            if (details != null) {
                entry.setEntityType("USER");
                entry.setEntityId(details);
            }
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit failure must NEVER break the main flow
            log.error("Audit log write failed: action={}, userId={}, error={}",
                action, userId, e.getMessage());
        }
    }
}
