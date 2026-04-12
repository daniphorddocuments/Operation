package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.EquipmentReportItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentReportItemRepository extends JpaRepository<EquipmentReportItem, Long> {
    List<EquipmentReportItem> findByEquipmentReportId(Long equipmentReportId);
}
