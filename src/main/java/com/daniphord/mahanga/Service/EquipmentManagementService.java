package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.HydrantReport;
import com.daniphord.mahanga.Model.MaintenanceLog;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.HydrantReportRepository;
import com.daniphord.mahanga.Repositories.EquipmentRepository;
import com.daniphord.mahanga.Repositories.MaintenanceLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EquipmentManagementService {

    private final EquipmentRepository equipmentRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;
    private final HydrantReportRepository hydrantReportRepository;
    private final RoleAccessService roleAccessService;
    private final OperationalApprovalService operationalApprovalService;

    public EquipmentManagementService(
            EquipmentRepository equipmentRepository,
            MaintenanceLogRepository maintenanceLogRepository,
            HydrantReportRepository hydrantReportRepository,
            RoleAccessService roleAccessService,
            OperationalApprovalService operationalApprovalService
    ) {
        this.equipmentRepository = equipmentRepository;
        this.maintenanceLogRepository = maintenanceLogRepository;
        this.hydrantReportRepository = hydrantReportRepository;
        this.roleAccessService = roleAccessService;
        this.operationalApprovalService = operationalApprovalService;
    }

    public List<Equipment> getAllEquipment(User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_MANAGE_EQUIPMENT);
        return roleAccessService.visibleEquipment(currentUser, equipmentRepository.findAll());
    }

    public List<Equipment> visibleEquipment(User currentUser) {
        if (currentUser == null) {
            return List.of();
        }
        return roleAccessService.visibleEquipment(currentUser, equipmentRepository.findAll());
    }

    public Equipment registerEquipment(Equipment equipment, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_MANAGE_EQUIPMENT);
        equipment.setCreatedBy(currentUser);
        prepareEquipmentForSave(equipment, currentUser);
        Equipment saved = equipmentRepository.save(equipment);
        return operationalApprovalService.initializeEquipmentApproval(saved, currentUser);
    }

    public Equipment resubmitEquipment(Long equipmentId, Equipment updates, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_MANAGE_EQUIPMENT);
        Equipment existing = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found"));
        if (existing.getCreatedBy() == null || currentUser == null || !existing.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the original initiator can edit and resubmit this equipment record");
        }
        if (!"DENIED".equalsIgnoreCase(existing.getApprovalStatus())) {
            throw new IllegalStateException("Only denied equipment records can be edited and resubmitted");
        }

        existing.setName(updates.getName());
        existing.setType(updates.getType());
        existing.setSubtype(updates.getSubtype());
        existing.setSerialNumber(updates.getSerialNumber());
        existing.setConditionStatus(updates.getConditionStatus());
        existing.setOperationalStatus(updates.getOperationalStatus());
        existing.setPurchaseDate(updates.getPurchaseDate());
        existing.setMaintenanceDueDate(updates.getMaintenanceDueDate());
        existing.setLastServicedAt(updates.getLastServicedAt());
        existing.setMaintenanceRequired(updates.getMaintenanceRequired());
        existing.setQuantityInStore(updates.getQuantityInStore());
        prepareEquipmentForSave(existing, currentUser);

        Equipment saved = equipmentRepository.save(existing);
        return operationalApprovalService.initializeEquipmentApproval(saved, currentUser);
    }

    public HydrantReport registerHydrantReport(HydrantReport report, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_MANAGE_HYDRANTS);
        report.setCreatedBy(currentUser);
        if (currentUser != null && currentUser.getStation() != null && report.getStation() == null) {
            report.setStation(currentUser.getStation());
        }
        if (report.getDistrict() == null && report.getStation() != null) {
            report.setDistrict(report.getStation().getDistrict());
        }
        if (report.getRegion() == null && report.getDistrict() != null) {
            report.setRegion(report.getDistrict().getRegion());
        }
        HydrantReport saved = hydrantReportRepository.save(report);
        return operationalApprovalService.initializeHydrantApproval(saved, currentUser);
    }

    public MaintenanceLog addMaintenanceLog(Long equipmentId, MaintenanceLog maintenanceLog, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_MANAGE_EQUIPMENT);
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found"));
        equipment.setLastServicedAt(LocalDateTime.now());
        if (maintenanceLog.getNextServiceDate() != null) {
            equipment.setMaintenanceDueDate(maintenanceLog.getNextServiceDate());
        }
        equipment.setOperationalStatus("AVAILABLE");
        equipment.setMaintenanceRequired(false);
        equipmentRepository.save(equipment);
        maintenanceLog.setEquipment(equipment);
        return maintenanceLogRepository.save(maintenanceLog);
    }

    public Equipment reviewEquipment(Long equipmentId, boolean approve, String comment, User currentUser) {
        return operationalApprovalService.reviewEquipment(equipmentId, approve, comment, currentUser);
    }

    public HydrantReport reviewHydrant(Long hydrantReportId, boolean approve, String comment, User currentUser) {
        return operationalApprovalService.reviewHydrant(hydrantReportId, approve, comment, currentUser);
    }

    public List<HydrantReport> getHydrantReports(User currentUser) {
        if (!roleAccessService.canManageHydrants(currentUser) && !roleAccessService.canReviewOperationalApprovals(currentUser)) {
            return List.of();
        }
        return roleAccessService.visibleHydrantReports(currentUser, hydrantReportRepository.findAll());
    }

    public Map<String, Object> dashboardMetrics(User currentUser) {
        List<Equipment> equipment = currentUser == null
                ? List.of()
                : roleAccessService.visibleEquipment(currentUser, equipmentRepository.findAll());
        return Map.of(
                "totalEquipment", equipment.size(),
                "availableUnits", equipment.stream().filter(item -> "AVAILABLE".equalsIgnoreCase(item.getOperationalStatus())).count(),
                "inUseUnits", equipment.stream().filter(item -> "IN_USE".equalsIgnoreCase(item.getOperationalStatus())).count(),
                "maintenanceAlerts", equipment.stream()
                        .filter(item -> item.getMaintenanceDueDate() != null)
                        .filter(item -> !item.getMaintenanceDueDate().isAfter(LocalDate.now().plusDays(7)))
                        .count()
        );
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void prepareEquipmentForSave(Equipment equipment, User currentUser) {
        equipment.setName(clean(equipment.getName()));
        equipment.setType(clean(equipment.getType()));
        equipment.setSubtype(clean(equipment.getSubtype()));
        equipment.setSerialNumber(clean(equipment.getSerialNumber()));
        if (equipment.getSerialNumber() != null) {
            equipmentRepository.findFirstBySerialNumberIgnoreCase(equipment.getSerialNumber())
                    .filter(existing -> equipment.getId() == null || !existing.getId().equals(equipment.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Serial number already exists for equipment " + existing.getName() + ". Edit the existing record or use a different serial number.");
                    });
        }
        equipment.setConditionStatus(defaultIfBlank(equipment.getConditionStatus(), "GOOD"));
        equipment.setOperationalStatus(defaultIfBlank(equipment.getOperationalStatus(), "AVAILABLE"));
        if (currentUser != null && currentUser.getStation() != null && equipment.getStation() == null) {
            equipment.setStation(currentUser.getStation());
        }
        if (equipment.getQuantityInStore() == null || equipment.getQuantityInStore() < 1) {
            equipment.setQuantityInStore(1);
        }
        if (equipment.getMaintenanceRequired() == null) {
            equipment.setMaintenanceRequired(!"IN_USE".equalsIgnoreCase(equipment.getOperationalStatus()));
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned == null ? fallback : cleaned;
    }
}
