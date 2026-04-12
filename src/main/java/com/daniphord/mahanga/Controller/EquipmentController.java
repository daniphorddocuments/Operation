package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.HydrantLocation;
import com.daniphord.mahanga.Model.HydrantReport;
import com.daniphord.mahanga.Model.MaintenanceLog;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.EquipmentManagementService;
import com.daniphord.mahanga.Service.RoleAccessService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentManagementService equipmentManagementService;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;

    public EquipmentController(
            EquipmentManagementService equipmentManagementService,
            UserRepository userRepository,
            RoleAccessService roleAccessService
    ) {
        this.equipmentManagementService = equipmentManagementService;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping
    public ResponseEntity<?> listEquipment(HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canManageEquipment(currentUser) && !roleAccessService.canReviewOperationalApprovals(currentUser)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(equipmentManagementService.visibleEquipment(currentUser).stream()
                .map(this::toEquipmentRecord)
                .toList());
    }

    @PostMapping
    public ResponseEntity<?> registerEquipment(@RequestBody Equipment equipment, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canManageEquipment(currentUser)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Action not allowed for your role"));
        }
        try {
            return ResponseEntity.ok(toEquipmentRecord(equipmentManagementService.registerEquipment(equipment, currentUser)));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/{equipmentId}/decision")
    public ResponseEntity<?> reviewEquipment(
            @PathVariable Long equipmentId,
            @RequestBody Map<String, Object> payload,
            HttpSession session
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canReviewOperationalApprovals(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            boolean approve = Boolean.TRUE.equals(payload.get("approve"));
            String comment = payload.get("comment") == null ? "" : payload.get("comment").toString();
            return ResponseEntity.ok(toEquipmentRecord(equipmentManagementService.reviewEquipment(equipmentId, approve, comment, currentUser)));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/{equipmentId}/resubmit")
    public ResponseEntity<?> resubmitEquipment(@PathVariable Long equipmentId, @RequestBody Equipment equipment, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canManageEquipment(currentUser)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Action not allowed for your role"));
        }
        try {
            return ResponseEntity.ok(toEquipmentRecord(equipmentManagementService.resubmitEquipment(equipmentId, equipment, currentUser)));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/{equipmentId}/maintenance")
    public ResponseEntity<?> addMaintenanceLog(@PathVariable Long equipmentId, @RequestBody MaintenanceLog maintenanceLog, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canManageEquipment(currentUser)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Action not allowed for your role"));
        }
        try {
            MaintenanceLog savedLog = equipmentManagementService.addMaintenanceLog(equipmentId, maintenanceLog, currentUser);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", savedLog.getId());
            payload.put("serviceDate", savedLog.getServiceDate() == null ? "" : savedLog.getServiceDate().toString());
            payload.put("notes", safe(savedLog.getNotes()));
            payload.put("servicedBy", safe(savedLog.getServicedBy()));
            payload.put("nextServiceDate", savedLog.getNextServiceDate() == null ? "" : savedLog.getNextServiceDate().toString());
            payload.put("equipment", savedLog.getEquipment() == null ? null : toEquipmentRecord(savedLog.getEquipment()));
            return ResponseEntity.ok(payload);
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/hydrants")
    public ResponseEntity<?> listHydrantReports(HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canManageHydrants(currentUser) && !roleAccessService.canReviewOperationalApprovals(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(equipmentManagementService.getHydrantReports(currentUser).stream()
                .map(this::toHydrantRecord)
                .toList());
    }

    @PostMapping("/hydrants")
    public ResponseEntity<?> registerHydrantReport(@RequestBody Map<String, Object> payload, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canManageHydrants(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            HydrantReport report = new HydrantReport();
            report.setWorking(readInt(payload.get("working")));
            report.setNotWorking(readInt(payload.get("notWorking")));
            report.setLowPressure(readInt(payload.get("lowPressure")));
            report.setRemarks(payload.get("remarks") == null ? null : payload.get("remarks").toString());
            Object locationsObject = payload.get("locations");
            if (locationsObject instanceof List<?> locations) {
                for (Object item : locations) {
                    if (!(item instanceof Map<?, ?> locationMap)) {
                        continue;
                    }
                    HydrantLocation location = new HydrantLocation();
                    location.setHydrantReport(report);
                    location.setName(readString(locationMap.get("name")));
                    location.setStatus(readString(locationMap.get("status")));
                    location.setPressure(readString(locationMap.get("pressure")));
                    report.getLocations().add(location);
                }
            }
            return ResponseEntity.ok(toHydrantRecord(equipmentManagementService.registerHydrantReport(report, currentUser)));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/hydrants/{hydrantReportId}/decision")
    public ResponseEntity<?> reviewHydrant(
            @PathVariable Long hydrantReportId,
            @RequestBody Map<String, Object> payload,
            HttpSession session
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canReviewOperationalApprovals(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            boolean approve = Boolean.TRUE.equals(payload.get("approve"));
            String comment = payload.get("comment") == null ? "" : payload.get("comment").toString();
            return ResponseEntity.ok(toHydrantRecord(equipmentManagementService.reviewHydrant(hydrantReportId, approve, comment, currentUser)));
        } catch (Exception exception) {
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

    private Integer readInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String normalized = value.toString().trim();
        return normalized.isBlank() ? 0 : Integer.parseInt(normalized);
    }

    private String readString(Object value) {
        return value == null ? "" : value.toString();
    }

    private Map<String, Object> toEquipmentRecord(Equipment equipment) {
        Map<String, Object> record = new LinkedHashMap<>();
        String normalizedType = normalizedEquipmentType(equipment);
        record.put("id", equipment.getId());
        record.put("name", safe(equipment.getName()));
        record.put("type", normalizedType);
        record.put("typeLabel", equipmentTypeLabel(normalizedType));
        record.put("serialNumber", safe(equipment.getSerialNumber()));
        record.put("conditionStatus", safe(equipment.getConditionStatus()));
        record.put("operationalStatus", safe(equipment.getOperationalStatus()));
        record.put("purchaseDate", equipment.getPurchaseDate() == null ? "" : equipment.getPurchaseDate().toString());
        record.put("maintenanceDueDate", equipment.getMaintenanceDueDate() == null ? "" : equipment.getMaintenanceDueDate().toString());
        record.put("lastServicedAt", equipment.getLastServicedAt() == null ? "" : equipment.getLastServicedAt().toString());
        record.put("createdAt", equipment.getCreatedAt() == null ? "" : equipment.getCreatedAt().toString());
        record.put("maintenanceRequired", Boolean.TRUE.equals(equipment.getMaintenanceRequired()));
        record.put("quantityInStore", valueOrZero(equipment.getQuantityInStore()));
        record.put("station", equipment.getStation() == null ? "" : safe(equipment.getStation().getName()));
        record.put("createdBy", equipment.getCreatedBy() == null
                ? ""
                : !safe(equipment.getCreatedBy().getFullName()).isBlank() ? safe(equipment.getCreatedBy().getFullName()) : safe(equipment.getCreatedBy().getUsername()));
        record.put("createdById", equipment.getCreatedBy() == null ? null : equipment.getCreatedBy().getId());
        record.put("approvalStatus", safe(equipment.getApprovalStatus()));
        record.put("approvalCurrentLevel", safe(equipment.getApprovalCurrentLevel()));
        record.put("approvalLastComment", safe(equipment.getApprovalLastComment()));
        return record;
    }

    private Map<String, Object> toHydrantRecord(HydrantReport report) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", report.getId());
        record.put("region", report.getRegion() == null ? "" : safe(report.getRegion().getName()));
        record.put("district", report.getDistrict() == null ? "" : safe(report.getDistrict().getName()));
        record.put("station", report.getStation() == null ? "" : safe(report.getStation().getName()));
        record.put("totalHydrants", valueOrZero(report.getTotalHydrants()));
        record.put("working", valueOrZero(report.getWorking()));
        record.put("notWorking", valueOrZero(report.getNotWorking()));
        record.put("lowPressure", valueOrZero(report.getLowPressure()));
        record.put("remarks", safe(report.getRemarks()));
        record.put("approvalStatus", safe(report.getApprovalStatus()));
        record.put("approvalCurrentLevel", safe(report.getApprovalCurrentLevel()));
        record.put("approvalLastComment", safe(report.getApprovalLastComment()));
        record.put("createdAt", report.getCreatedAt() == null ? "" : report.getCreatedAt().toString());
        record.put("createdById", report.getCreatedBy() == null ? null : report.getCreatedBy().getId());
        record.put("locations", report.getLocations().stream()
                .map(location -> Map.of(
                        "name", safe(location.getName()),
                        "status", safe(location.getStatus()),
                        "pressure", safe(location.getPressure())
                ))
                .toList());
        return record;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizedEquipmentType(Equipment equipment) {
        String type = safe(equipment.getType()).trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return type.isBlank() ? "GENERAL" : type;
    }

    private String equipmentTypeLabel(String type) {
        return switch (safe(type)) {
            case "FIRE_TENDER" -> "Fire Tender";
            case "COMMAND_CAR" -> "Command Car";
            case "MANAGEMENT_CAR" -> "Management Car";
            case "HAZMAT_CAR" -> "Hazmat Car";
            case "AMBULANCE" -> "Ambulance";
            case "RESCUE_EQUIPMENT" -> "Rescue Equipment";
            case "FIRE_FIGHTING_EQUIPMENT" -> "Fire Fighting Equipment";
            case "FIRE_FIGHTING_CHEMICALS" -> "Fire Fighting Chemicals";
            case "BA" -> "BA";
            default -> safe(type).replace('_', ' ').trim();
        };
    }
}
