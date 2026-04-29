package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Model.AuditLog;
import com.daniphord.mahanga.Repositories.AuditLogRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.RoleAccessService;
import com.daniphord.mahanga.Service.SecurityIntelligenceService;
import jakarta.servlet.http.HttpSession;
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
    private final SecurityIntelligenceService securityIntelligenceService;
    private final RoleAccessService roleAccessService;
    private final UserRepository userRepository;

    public AdminAuditController(
            AuditLogRepository auditLogRepository,
            SecurityIntelligenceService securityIntelligenceService,
            RoleAccessService roleAccessService,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.securityIntelligenceService = securityIntelligenceService;
        this.roleAccessService = roleAccessService;
        this.userRepository = userRepository;
    }

    @GetMapping("/logs")
    public ResponseEntity<?> auditLogs(@RequestParam(name = "limit", defaultValue = "100") int limit, HttpSession session) {
        if (!canViewAdminAudit(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        int boundedLimit = Math.max(1, Math.min(limit, 250));
        return ResponseEntity.ok(auditLogRepository.findAll(PageRequest.of(0, boundedLimit))
                .stream()
                .map(this::toView)
                .toList());
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(HttpSession session) {
        if (!canViewAdminAudit(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(Map.of(
                "totalEntries", auditLogRepository.count(),
                "failedEvents", auditLogRepository.findByStatus("FAILURE").size(),
                "successfulEvents", auditLogRepository.findByStatus("SUCCESS").size()
        ));
    }

    @GetMapping("/security/overview")
    public ResponseEntity<?> securityOverview(HttpSession session) {
        if (!canViewAdminAudit(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(securityIntelligenceService.adminOverview());
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

    private boolean canViewAdminAudit(HttpSession session) {
        User user = currentUser(session);
        return roleAccessService.canManageUsers(user)
                || roleAccessService.canManageRolePermissions(user)
                || roleAccessService.canManageSystemSettings(user);
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }
}
