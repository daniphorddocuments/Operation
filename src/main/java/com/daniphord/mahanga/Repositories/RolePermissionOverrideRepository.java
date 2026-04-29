package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.RolePermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionOverrideRepository extends JpaRepository<RolePermissionOverride, Long> {

    List<RolePermissionOverride> findByRoleKeyOrderByActionKeyAsc(String roleKey);

    Optional<RolePermissionOverride> findByRoleKeyAndActionKey(String roleKey, String actionKey);

    void deleteByRoleKeyAndActionKey(String roleKey, String actionKey);
}
