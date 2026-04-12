package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.AuditLog;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Service for logging all system actions
 * Provides security and compliance tracking
 */
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logAction(User user, String action, String description, String entityType, Long entityId, String ipAddress) {
        AuditLog log = new AuditLog(user, action, description, entityType, entityId, ipAddress);
        log.setStatus("SUCCESS");
        auditLogRepository.save(log);
    }

    public void logFailure(User user, String action, String description, String ipAddress, String details) {
        AuditLog log = new AuditLog(user, action, description, "SYSTEM", null, ipAddress);
        log.setStatus("FAILURE");
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getUserAuditLog(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> getAuditLogByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    public List<AuditLog> getAuditLogByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate);
    }

    public List<AuditLog> getFailedAttempts() {
        return auditLogRepository.findByStatus("FAILURE");
    }

    public List<AuditLog> getAuditLogByIpAddress(String ipAddress) {
        return auditLogRepository.findByIpAddress(ipAddress);
    }
}

