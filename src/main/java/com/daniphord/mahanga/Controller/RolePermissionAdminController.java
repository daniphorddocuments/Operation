package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.AuditService;
import com.daniphord.mahanga.Service.RoleAccessService;
import com.daniphord.mahanga.Service.RolePermissionOverrideService;
import com.daniphord.mahanga.Service.RoleResponsibilityService;
import com.daniphord.mahanga.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/role-permissions")
public class RolePermissionAdminController {

    private final RoleResponsibilityService roleResponsibilityService;
    private final RolePermissionOverrideService rolePermissionOverrideService;
    private final RoleAccessService roleAccessService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public RolePermissionAdminController(
            RoleResponsibilityService roleResponsibilityService,
            RolePermissionOverrideService rolePermissionOverrideService,
            RoleAccessService roleAccessService,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.roleResponsibilityService = roleResponsibilityService;
        this.rolePermissionOverrideService = rolePermissionOverrideService;
        this.roleAccessService = roleAccessService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<?> definitions(HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canManageRolePermissions(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(Map.of(
                "roles", roleResponsibilityService.rolePermissionDefinitions(),
                "supportedActions", roleResponsibilityService.actionDefinitions().stream().map(action -> Map.of(
                        "key", action.key(),
                        "label", action.label(),
                        "description", action.description()
                )).toList()
        ));
    }

    @PutMapping("/{roleKey}")
    public ResponseEntity<?> updateRolePermissions(
            @PathVariable String roleKey,
            @RequestBody Map<String, Object> payload,
            HttpSession session,
            HttpServletRequest request
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canManageRolePermissions(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            Set<String> enabledActions = new LinkedHashSet<>();
            Object rawActions = payload.get("enabledActions");
            if (rawActions instanceof List<?> list) {
                list.stream()
                        .map(item -> item == null ? null : item.toString())
                        .filter(action -> action != null && roleResponsibilityService.isSupportedAction(action))
                        .forEach(enabledActions::add);
            }
            rolePermissionOverrideService.replaceRolePermissions(roleKey, enabledActions, roleResponsibilityService.supportedActions());
            auditService.logAction(
                    currentUser,
                    "ROLE_PERMISSIONS_UPDATED",
                    "Updated role permissions for " + roleKey,
                    "RolePermission",
                    null,
                    RequestUtil.getClientIpAddress(request)
            );
            return ResponseEntity.ok(roleResponsibilityService.rolePermissionDefinition(roleKey));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }
}
