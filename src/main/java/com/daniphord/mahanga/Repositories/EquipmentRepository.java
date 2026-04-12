package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByOperationalStatusIgnoreCase(String operationalStatus);
    List<Equipment> findByMaintenanceDueDateBefore(LocalDate date);
    Optional<Equipment> findFirstBySerialNumberIgnoreCase(String serialNumber);
}
