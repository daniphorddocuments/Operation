package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.AuditLog;
import com.daniphord.mahanga.Repositories.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/logs")
    public ResponseEntity<?> auditLogs(@RequestParam(name = "limit", defaultValue = "100") int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 250));
        return ResponseEntity.ok(auditLogRepository.findAll(PageRequest.of(0, boundedLimit))
                .stream()
                .map(this::toView)
                .toList());
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        return ResponseEntity.ok(Map.of(
                "totalEntries", auditLogRepository.count(),
                "failedEvents", auditLogRepository.findByStatus("FAILURE").size(),
                "successfulEvents", auditLogRepository.findByStatus("SUCCESS").size()
        ));
    }

    private Map<String, Object> toView(AuditLog log) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("id", log.getId());
        payload.put("userId", log.getUser() == null ? null : log.getUser().getId());
        payload.put("username", log.getUser() == null ? "" : log.getUser().getUsername());
        payload.put("action", log.getAction());
        payload.put("description", log.getDescription());
        payload.put("entityType", log.getEntityType());
        payload.put("entityId", log.getEntityId());
        payload.put("ipAddress", log.getIpAddress());
        payload.put("status", log.getStatus());
        payload.put("details", log.getDetails() == null ? "" : log.getDetails());
        payload.put("timestamp", log.getTimestamp() == null ? "" : log.getTimestamp().toString());
        return payload;
    }
}
