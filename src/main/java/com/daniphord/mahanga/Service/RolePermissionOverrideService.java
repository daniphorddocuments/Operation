package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.RolePermissionOverride;
import com.daniphord.mahanga.Repositories.RolePermissionOverrideRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RolePermissionOverrideService {

    private final RolePermissionOverrideRepository repository;

    public RolePermissionOverrideService(RolePermissionOverrideRepository repository) {
        this.repository = repository;
    }

    public Map<String, Boolean> overridesForRole(String roleKey) {
        String normalizedRole = OperationRole.normalizeRole(roleKey);
        Map<String, Boolean> values = new LinkedHashMap<>();
        repository.findByRoleKeyOrderByActionKeyAsc(normalizedRole).forEach(item ->
                values.put(item.getActionKey(), Boolean.TRUE.equals(item.getEnabled()))
        );
        return values;
    }

    public Set<String> applyOverrides(String roleKey, Set<String> baseActions) {
        Set<String> effective = new LinkedHashSet<>(baseActions == null ? Set.of() : baseActions);
        overridesForRole(roleKey).forEach((actionKey, enabled) -> {
            if (enabled) {
                effective.add(actionKey);
            } else {
                effective.remove(actionKey);
            }
        });
        return effective;
    }

    @Transactional
    public void replaceRolePermissions(String roleKey, Set<String> enabledActions, Set<String> supportedActions) {
        String normalizedRole = OperationRole.normalizeRole(roleKey);
        Set<String> selected = new LinkedHashSet<>(enabledActions == null ? Set.of() : enabledActions);
        Set<String> allowedCatalog = new LinkedHashSet<>(supportedActions == null ? Set.of() : supportedActions);
        if (!OperationRole.ALL_FROMS_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Unsupported role");
        }
        if (!allowedCatalog.containsAll(selected)) {
            throw new IllegalArgumentException("Unsupported permission found in request");
        }

        List<RolePermissionOverride> existing = repository.findByRoleKeyOrderByActionKeyAsc(normalizedRole);
        Map<String, RolePermissionOverride> byAction = new LinkedHashMap<>();
        existing.forEach(item -> byAction.put(item.getActionKey(), item));

        for (String actionKey : allowedCatalog) {
            boolean shouldEnable = selected.contains(actionKey);
            RolePermissionOverride override = byAction.get(actionKey);
            if (override == null) {
                override = new RolePermissionOverride();
                override.setRoleKey(normalizedRole);
                override.setActionKey(actionKey);
            }
            override.setEnabled(shouldEnable);
            repository.save(override);
        }
    }
}
