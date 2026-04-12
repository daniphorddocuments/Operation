package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.MaintenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {
    List<MaintenanceLog> findByEquipmentId(Long equipmentId);
}
