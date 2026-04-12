package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.AuditService;
import com.daniphord.mahanga.Util.OperationRole;
import com.daniphord.mahanga.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import com.daniphord.mahanga.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    // GET all users
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/locked")
    public List<User> getLockedUsers() {
        return userService.getLockedUsers();
    }

    @GetMapping("/security-summary")
    public Map<String, Object> getSecuritySummary() {
        return userService.userSecuritySummary();
    }

    // GET user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody User user,
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String regionId,
            HttpSession session,
            HttpServletRequest request
    ) {
        if (isInvestigationRole(user.getRole()) && !isSuperAdmin(session)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only system admin can register investigators"));
        }
        try {
            User created = userService.createUser(user, parseNullableLong(stationId), parseNullableLong(districtId), parseNullableLong(regionId));
            auditService.logAction(currentUser(session), "USER_CREATED", "Administrator created user account", "User", created.getId(), RequestUtil.getClientIpAddress(request));
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody User updatedUser,
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String regionId,
            HttpSession session,
            HttpServletRequest request
    ) {
        if (isInvestigationRole(updatedUser.getRole()) && !isSuperAdmin(session)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only system admin can update investigators"));
        }
        try {
            User saved = userService.updateUser(id, updatedUser, parseNullableLong(stationId), parseNullableLong(districtId), parseNullableLong(regionId));
            auditService.logAction(currentUser(session), "USER_UPDATED", "Administrator updated user account", "User", saved.getId(), RequestUtil.getClientIpAddress(request));
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            if ("User not found".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        Object currentUserId = session.getAttribute("userId");
        if (currentUserId instanceof Long loggedInId && loggedInId.equals(id)) {
            return ResponseEntity.badRequest().build();
        }
        if (!userService.deleteUser(id)) return ResponseEntity.notFound().build();
        auditService.logAction(currentUser(session), "USER_DELETED", "Administrator deleted user account", "User", id, RequestUtil.getClientIpAddress(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        Object currentUserId = session.getAttribute("userId");
        if (currentUserId instanceof Long loggedInId && loggedInId.equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot lock your own account"));
        }
        try {
            User user = userService.lockUser(id);
            auditService.logFailure(currentUser(session), "USER_LOCKED", "Administrator locked user account", RequestUtil.getClientIpAddress(request), user.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            if ("User not found".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        try {
            User user = userService.unlockUser(id);
            auditService.logAction(currentUser(session), "USER_UNLOCKED", "Administrator unlocked user account", "User", user.getId(), RequestUtil.getClientIpAddress(request));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            if ("User not found".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }

    private boolean isSuperAdmin(HttpSession session) {
        Object role = session.getAttribute("role");
        return OperationRole.SUPER_ADMIN.equals(OperationRole.normalizeRole(role == null ? null : role.toString()));
    }

    private boolean isInvestigationRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return "FIRE_INVESTIGATION_HOD".equals(normalized)
                || "REGIONAL_INVESTIGATION_OFFICER".equals(normalized)
                || "DISTRICT_INVESTIGATION_OFFICER".equals(normalized);
    }

    private Long parseNullableLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid scope identifier");
        }
    }
}
