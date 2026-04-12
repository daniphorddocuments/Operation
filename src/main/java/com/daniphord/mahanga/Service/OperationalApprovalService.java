package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Approval;
import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.HydrantReport;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.ApprovalRepository;
import com.daniphord.mahanga.Repositories.EquipmentRepository;
import com.daniphord.mahanga.Repositories.HydrantReportRepository;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class OperationalApprovalService {

    private static final List<String> APPROVAL_CHAIN = List.of(
            OperationRole.STATION_OPERATION_OFFICER,
            OperationRole.STATION_FIRE_OPERATION_OFFICER,
            OperationRole.STATION_FIRE_OFFICER,
            OperationRole.DISTRICT_OPERATION_OFFICER,
            OperationRole.DISTRICT_FIRE_OFFICER,
            OperationRole.REGIONAL_OPERATION_OFFICER,
            OperationRole.COMMISSIONER_OPERATIONS
    );

    private final ApprovalRepository approvalRepository;
    private final IncidentRepository incidentRepository;
    private final EquipmentRepository equipmentRepository;
    private final HydrantReportRepository hydrantReportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public OperationalApprovalService(
            ApprovalRepository approvalRepository,
            IncidentRepository incidentRepository,
            EquipmentRepository equipmentRepository,
            HydrantReportRepository hydrantReportRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.approvalRepository = approvalRepository;
        this.incidentRepository = incidentRepository;
        this.equipmentRepository = equipmentRepository;
        this.hydrantReportRepository = hydrantReportRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Incident initializeIncidentApproval(Incident incident, User initiator) {
        String role = normalizeRole(initiator == null ? null : initiator.getRole());
        LocalDateTime now = LocalDateTime.now();
        String nextRole = nextApprovalRole(role, true);
        if (nextRole == null) {
            incident.setApprovalStatus("APPROVED");
            incident.setApprovalCurrentLevel(role);
            incident.setApprovalLastComment("Registered without additional approval workflow.");
            incident.setApprovalSubmittedAt(now);
            incident.setApprovalUpdatedAt(now);
            Incident saved = incidentRepository.save(incident);
            record(Approval.ApprovalTargetType.INCIDENT_REPORT, saved.getId(), Approval.Decision.APPROVED, role, "Registered without approval workflow.", initiator);
            return saved;
        }

        incident.setApprovalStatus("PENDING");
        incident.setApprovalCurrentLevel(nextRole);
        incident.setApprovalLastComment("Submitted by " + displayRole(role) + " for review.");
        incident.setApprovalSubmittedAt(now);
        incident.setApprovalUpdatedAt(now);
        Incident saved = incidentRepository.save(incident);
        record(Approval.ApprovalTargetType.INCIDENT_REPORT, saved.getId(), Approval.Decision.PENDING, nextRole, "Submitted for approval.", initiator);
        notifyApprovers(scopeFor(saved), nextRole, "Incident awaiting approval", "Incident " + saved.getIncidentNumber() + " is waiting for your approval.");
        return saved;
    }

    public Equipment initializeEquipmentApproval(Equipment equipment, User initiator) {
        String role = normalizeRole(initiator == null ? null : initiator.getRole());
        LocalDateTime now = LocalDateTime.now();
        String nextRole = nextApprovalRole(role, true);
        if (nextRole == null) {
            equipment.setApprovalStatus("APPROVED");
            equipment.setApprovalCurrentLevel(role);
            equipment.setApprovalLastComment("Registered without additional approval workflow.");
            equipment.setApprovalSubmittedAt(now);
            equipment.setApprovalUpdatedAt(now);
            Equipment saved = equipmentRepository.save(equipment);
            record(Approval.ApprovalTargetType.EQUIPMENT_REPORT, saved.getId(), Approval.Decision.APPROVED, role, "Registered without approval workflow.", initiator);
            return saved;
        }

        equipment.setApprovalStatus("PENDING");
        equipment.setApprovalCurrentLevel(nextRole);
        equipment.setApprovalLastComment("Submitted by " + displayRole(role) + " for review.");
        equipment.setApprovalSubmittedAt(now);
        equipment.setApprovalUpdatedAt(now);
        Equipment saved = equipmentRepository.save(equipment);
        record(Approval.ApprovalTargetType.EQUIPMENT_REPORT, saved.getId(), Approval.Decision.PENDING, nextRole, "Submitted for approval.", initiator);
        notifyApprovers(scopeFor(saved), nextRole, "Equipment registration awaiting approval", "Equipment " + saved.getName() + " is waiting for your approval.");
        return saved;
    }

    public HydrantReport initializeHydrantApproval(HydrantReport report, User initiator) {
        String role = normalizeRole(initiator == null ? null : initiator.getRole());
        LocalDateTime now = LocalDateTime.now();
        String nextRole = nextApprovalRole(role, true);
        if (nextRole == null) {
            report.setApprovalStatus("APPROVED");
            report.setApprovalCurrentLevel(role);
            report.setApprovalLastComment("Registered without additional approval workflow.");
            report.setApprovalSubmittedAt(now);
            report.setApprovalUpdatedAt(now);
            HydrantReport saved = hydrantReportRepository.save(report);
            record(Approval.ApprovalTargetType.HYDRANT_REPORT, saved.getId(), Approval.Decision.APPROVED, role, "Registered without approval workflow.", initiator);
            return saved;
        }

        report.setApprovalStatus("PENDING");
        report.setApprovalCurrentLevel(nextRole);
        report.setApprovalLastComment("Submitted by " + displayRole(role) + " for review.");
        report.setApprovalSubmittedAt(now);
        report.setApprovalUpdatedAt(now);
        HydrantReport saved = hydrantReportRepository.save(report);
        record(Approval.ApprovalTargetType.HYDRANT_REPORT, saved.getId(), Approval.Decision.PENDING, nextRole, "Submitted for approval.", initiator);
        notifyApprovers(scopeFor(saved), nextRole, "Hydrant report awaiting approval", "Hydrant report #" + saved.getId() + " is waiting for your approval.");
        return saved;
    }

    public Incident reviewIncident(Long incidentId, boolean approve, String comment, User actor) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        return reviewIncident(incident, approve, comment, actor);
    }

    public Incident reviewIncident(Incident incident, boolean approve, String comment, User actor) {
        validateReview(incident.getApprovalCurrentLevel(), approve, comment, actor);
        String actorRole = normalizeRole(actor == null ? null : actor.getRole());
        LocalDateTime now = LocalDateTime.now();
        if (approve) {
            String nextRole = "SUPER_ADMIN".equals(actorRole) ? null : nextApprovalRole(incident.getApprovalCurrentLevel(), false);
            if (nextRole == null) {
                incident.setApprovalStatus("APPROVED");
                incident.setApprovalCurrentLevel(actorRole);
                incident.setApprovalLastComment(blankToDefault(comment, "Incident approved."));
                incident.setApprovalUpdatedAt(now);
                Incident saved = incidentRepository.save(incident);
                record(Approval.ApprovalTargetType.INCIDENT_REPORT, saved.getId(), Approval.Decision.APPROVED, actorRole, blankToDefault(comment, "Incident approved."), actor);
                notifyOriginator(saved.getCreatedBy(), "Incident approved", "Incident " + saved.getIncidentNumber() + " has been fully approved.");
                return saved;
            }
            incident.setApprovalStatus("PENDING");
            incident.setApprovalCurrentLevel(nextRole);
            incident.setApprovalLastComment(blankToDefault(comment, "Approved and forwarded to " + nextRole + "."));
            incident.setApprovalUpdatedAt(now);
            Incident saved = incidentRepository.save(incident);
            record(Approval.ApprovalTargetType.INCIDENT_REPORT, saved.getId(), Approval.Decision.APPROVED, actorRole, blankToDefault(comment, "Approved and forwarded."), actor);
            notifyApprovers(scopeFor(saved), nextRole, "Incident awaiting approval", "Incident " + saved.getIncidentNumber() + " has been approved and forwarded to your level.");
            return saved;
        }

        incident.setApprovalStatus("DENIED");
        incident.setApprovalCurrentLevel(normalizeRole(incident.getCreatedBy() == null ? null : incident.getCreatedBy().getRole()));
        incident.setApprovalLastComment(comment);
        incident.setApprovalUpdatedAt(now);
        Incident saved = incidentRepository.save(incident);
        record(Approval.ApprovalTargetType.INCIDENT_REPORT, saved.getId(), Approval.Decision.REJECTED, actorRole, comment, actor);
        notifyOriginator(saved.getCreatedBy(), "Incident denied", "Incident " + saved.getIncidentNumber() + " was denied. Review the approval comment and submit again.");
        return saved;
    }

    public Equipment reviewEquipment(Long equipmentId, boolean approve, String comment, User actor) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found"));
        validateReview(equipment.getApprovalCurrentLevel(), approve, comment, actor);
        String actorRole = normalizeRole(actor == null ? null : actor.getRole());
        LocalDateTime now = LocalDateTime.now();
        if (approve) {
            String nextRole = "SUPER_ADMIN".equals(actorRole) ? null : nextApprovalRole(equipment.getApprovalCurrentLevel(), false);
            if (nextRole == null) {
                equipment.setApprovalStatus("APPROVED");
                equipment.setApprovalCurrentLevel(actorRole);
                equipment.setApprovalLastComment(blankToDefault(comment, "Equipment registration approved."));
                equipment.setApprovalUpdatedAt(now);
                Equipment saved = equipmentRepository.save(equipment);
                record(Approval.ApprovalTargetType.EQUIPMENT_REPORT, saved.getId(), Approval.Decision.APPROVED, actorRole, blankToDefault(comment, "Equipment registration approved."), actor);
                notifyOriginator(saved.getCreatedBy(), "Equipment approved", "Equipment " + saved.getName() + " has been fully approved.");
                return saved;
            }
            equipment.setApprovalStatus("PENDING");
            equipment.setApprovalCurrentLevel(nextRole);
            equipment.setApprovalLastComment(blankToDefault(comment, "Approved and forwarded to " + nextRole + "."));
            equipment.setApprovalUpdatedAt(now);
            Equipment saved = equipmentRepository.save(equipment);
            record(Approval.ApprovalTargetType.EQUIPMENT_REPORT, saved.getId(), Approval.Decision.APPROVED, actorRole, blankToDefault(comment, "Approved and forwarded."), actor);
            notifyApprovers(scopeFor(saved), nextRole, "Equipment registration awaiting approval", "Equipment " + saved.getName() + " has been approved and forwarded to your level.");
            return saved;
        }

        equipment.setApprovalStatus("DENIED");
        equipment.setApprovalCurrentLevel(normalizeRole(equipment.getCreatedBy() == null ? null : equipment.getCreatedBy().getRole()));
        equipment.setApprovalLastComment(comment);
        equipment.setApprovalUpdatedAt(now);
        Equipment saved = equipmentRepository.save(equipment);
        record(Approval.ApprovalTargetType.EQUIPMENT_REPORT, saved.getId(), Approval.Decision.REJECTED, actorRole, comment, actor);
        notifyOriginator(saved.getCreatedBy(), "Equipment denied", "Equipment " + saved.getName() + " was denied. Review the approval comment and submit again.");
        return saved;
    }

    public HydrantReport reviewHydrant(Long hydrantReportId, boolean approve, String comment, User actor) {
        HydrantReport report = hydrantReportRepository.findById(hydrantReportId)
                .orElseThrow(() -> new IllegalArgumentException("Hydrant report not found"));
        validateReview(report.getApprovalCurrentLevel(), approve, comment, actor);
        String actorRole = normalizeRole(actor == null ? null : actor.getRole());
        LocalDateTime now = LocalDateTime.now();
        if (approve) {
            String nextRole = "SUPER_ADMIN".equals(actorRole) ? null : nextApprovalRole(report.getApprovalCurrentLevel(), false);
            if (nextRole == null) {
                report.setApprovalStatus("APPROVED");
                report.setApprovalCurrentLevel(actorRole);
                report.setApprovalLastComment(blankToDefault(comment, "Hydrant report approved."));
                report.setApprovalUpdatedAt(now);
                HydrantReport saved = hydrantReportRepository.save(report);
                record(Approval.ApprovalTargetType.HYDRANT_REPORT, saved.getId(), Approval.Decision.APPROVED, actorRole, blankToDefault(comment, "Hydrant report approved."), actor);
                notifyOriginator(saved.getCreatedBy(), "Hydrant report approved", "Hydrant report #" + saved.getId() + " has been fully approved.");
                return saved;
            }
            report.setApprovalStatus("PENDING");
            report.setApprovalCurrentLevel(nextRole);
            report.setApprovalLastComment(blankToDefault(comment, "Approved and forwarded to " + nextRole + "."));
            report.setApprovalUpdatedAt(now);
            HydrantReport saved = hydrantReportRepository.save(report);
            record(Approval.ApprovalTargetType.HYDRANT_REPORT, saved.getId(), Approval.Decision.APPROVED, actorRole, blankToDefault(comment, "Approved and forwarded."), actor);
            notifyApprovers(scopeFor(saved), nextRole, "Hydrant report awaiting approval", "Hydrant report #" + saved.getId() + " has been approved and forwarded to your level.");
            return saved;
        }

        report.setApprovalStatus("DENIED");
        report.setApprovalCurrentLevel(normalizeRole(report.getCreatedBy() == null ? null : report.getCreatedBy().getRole()));
        report.setApprovalLastComment(comment);
        report.setApprovalUpdatedAt(now);
        HydrantReport saved = hydrantReportRepository.save(report);
        record(Approval.ApprovalTargetType.HYDRANT_REPORT, saved.getId(), Approval.Decision.REJECTED, actorRole, comment, actor);
        notifyOriginator(saved.getCreatedBy(), "Hydrant report denied", "Hydrant report #" + saved.getId() + " was denied. Review the approval comment and submit again.");
        return saved;
    }

    public List<Approval> history(Approval.ApprovalTargetType targetType, Long targetId) {
        return approvalRepository.findByTargetTypeAndTargetIdOrderByApprovalLevelAsc(targetType, targetId);
    }

    private void validateReview(String currentLevel, boolean approve, String comment, User actor) {
        String actorRole = normalizeRole(actor == null ? null : actor.getRole());
        String requiredRole = normalizeRole(currentLevel);
        if (!"SUPER_ADMIN".equals(actorRole) && !actorRole.equals(requiredRole)) {
            throw new IllegalStateException("This approval step is assigned to " + requiredRole);
        }
        if (!approve && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("Comment is required when denying a submission");
        }
    }

    private void record(
            Approval.ApprovalTargetType targetType,
            Long targetId,
            Approval.Decision decision,
            String levelRole,
            String comment,
            User actor
    ) {
        Approval approval = new Approval();
        approval.setTargetType(targetType);
        approval.setTargetId(targetId);
        approval.setDecision(decision);
        approval.setApprovalLevel(Math.max(APPROVAL_CHAIN.indexOf(normalizeRole(levelRole)) + 1, 1));
        approval.setComments(comment);
        approval.setActor(actor);
        approvalRepository.save(approval);
    }

    private void notifyApprovers(Scope scope, String role, String title, String message) {
        if (role == null || role.isBlank()) {
            return;
        }
        notificationService.notifyUsers(recipientsForRole(scope, role), title, message, "/dashboard");
    }

    private void notifyOriginator(User user, String title, String message) {
        if (user == null) {
            return;
        }
        notificationService.notifyUsers(List.of(user), title, message, "/dashboard");
    }

    private List<User> recipientsForRole(Scope scope, String role) {
        return userRepository.findAll().stream()
                .filter(user -> role.equals(normalizeRole(user.getRole())))
                .filter(user -> matchesScope(user, scope))
                .toList();
    }

    private boolean matchesScope(User user, Scope scope) {
        String role = normalizeRole(user == null ? null : user.getRole());
        if (scope == null || user == null) {
            return false;
        }
        if (Set.of("SUPER_ADMIN", OperationRole.COMMISSIONER_OPERATIONS).contains(role)) {
            return true;
        }
        if (Set.of(OperationRole.REGIONAL_OPERATION_OFFICER, OperationRole.REGIONAL_FIRE_OFFICER).contains(role)) {
            return user.getStation() != null
                    && user.getStation().getDistrict() != null
                    && user.getStation().getDistrict().getRegion() != null
                    && scope.regionId() != null
                    && scope.regionId().equals(user.getStation().getDistrict().getRegion().getId());
        }
        if (Set.of(OperationRole.DISTRICT_OPERATION_OFFICER).contains(role)) {
            return user.getStation() != null
                    && user.getStation().getDistrict() != null
                    && scope.districtId() != null
                    && scope.districtId().equals(user.getStation().getDistrict().getId());
        }
        if (Set.of(OperationRole.STATION_FIRE_OFFICER, OperationRole.STATION_FIRE_OPERATION_OFFICER).contains(role)) {
            return user.getStation() != null
                    && scope.stationId() != null
                    && scope.stationId().equals(user.getStation().getId());
        }
        return false;
    }

    private Scope scopeFor(Incident incident) {
        Station station = incident.getStation();
        Long stationId = station == null ? null : station.getId();
        Long districtId = incident.getDistrict() != null
                ? incident.getDistrict().getId()
                : station != null && station.getDistrict() != null ? station.getDistrict().getId() : null;
        Region region = incident.getRegion() != null
                ? incident.getRegion()
                : incident.getDistrict() != null ? incident.getDistrict().getRegion()
                : station != null && station.getDistrict() != null ? station.getDistrict().getRegion() : null;
        return new Scope(stationId, districtId, region == null ? null : region.getId());
    }

    private Scope scopeFor(Equipment equipment) {
        Station station = equipment.getStation();
        Long stationId = station == null ? null : station.getId();
        Long districtId = station != null && station.getDistrict() != null ? station.getDistrict().getId() : null;
        Long regionId = station != null && station.getDistrict() != null && station.getDistrict().getRegion() != null
                ? station.getDistrict().getRegion().getId()
                : null;
        return new Scope(stationId, districtId, regionId);
    }

    private Scope scopeFor(HydrantReport report) {
        Station station = report.getStation();
        Long stationId = station == null ? null : station.getId();
        Long districtId = report.getDistrict() != null
                ? report.getDistrict().getId()
                : station != null && station.getDistrict() != null ? station.getDistrict().getId() : null;
        Long regionId = report.getRegion() != null
                ? report.getRegion().getId()
                : report.getDistrict() != null && report.getDistrict().getRegion() != null ? report.getDistrict().getRegion().getId()
                : station != null && station.getDistrict() != null && station.getDistrict().getRegion() != null ? station.getDistrict().getRegion().getId()
                : null;
        return new Scope(stationId, districtId, regionId);
    }

    private String nextRole(String currentRole) {
        int index = APPROVAL_CHAIN.indexOf(normalizeRole(currentRole));
        if (index < 0 || index + 1 >= APPROVAL_CHAIN.size()) {
            return null;
        }
        return APPROVAL_CHAIN.get(index + 1);
    }

    private String nextApprovalRole(String currentRole, boolean initialSubmission) {
        String normalizedRole = normalizeRole(currentRole);
        if (initialSubmission && Set.of(
                OperationRole.STATION_OPERATION_OFFICER,
                OperationRole.STATION_FIRE_OPERATION_OFFICER,
                OperationRole.STATION_FIRE_OFFICER,
                OperationRole.CONTROL_ROOM_ATTENDANT,
                OperationRole.CONTROL_ROOM_OPERATOR,
                OperationRole.OPERATION_OFFICER,
                OperationRole.DEPARTMENT_OFFICER
        ).contains(normalizedRole)) {
            return OperationRole.DISTRICT_OPERATION_OFFICER;
        }
        return nextRole(normalizedRole);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeRole(String role) {
        return OperationRole.normalizeRole(role);
    }

    private String displayRole(String role) {
        return normalizeRole(role).replace('_', ' ').trim();
    }

    private record Scope(Long stationId, Long districtId, Long regionId) {
    }
}
