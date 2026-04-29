package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.HydrantReport;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Recommendation;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RoleAccessService {

    private final RoleResponsibilityService roleResponsibilityService;

    public RoleAccessService() {
        this(new RoleResponsibilityService());
    }

    @Autowired
    public RoleAccessService(RoleResponsibilityService roleResponsibilityService) {
        this.roleResponsibilityService = roleResponsibilityService;
    }

    public List<Incident> visibleIncidents(User user, List<Incident> incidents) {
        if (user == null) {
            return List.of();
        }
        String role = normalizeRole(user.getRole());
        return incidents.stream()
                .filter(incident -> canViewIncident(role, user, incident))
                .toList();
    }

    public List<Recommendation> visibleRecommendations(User user, List<Recommendation> recommendations) {
        if (user == null || !canSeeAiRecommendations(user)) {
            return List.of();
        }
        return recommendations.stream()
                .filter(recommendation -> recommendation.getIncident() == null || canViewIncident(normalizeRole(user.getRole()), user, recommendation.getIncident()))
                .toList();
    }

    public List<Station> visibleStations(User user, List<Station> stations) {
        if (user == null) {
            return List.of();
        }
        String role = normalizeRole(user.getRole());
        if (isHeadquartersRole(role) || canAccessCallHandling(user) || canManageSystemSettings(user)) {
            return stations;
        }
        if (isRegionalRole(role)) {
            Region userRegion = userRegion(user);
            return stations.stream()
                    .filter(station -> station.getDistrict() != null && station.getDistrict().getRegion() != null)
                    .filter(station -> userRegion != null && userRegion.getId().equals(station.getDistrict().getRegion().getId()))
                    .toList();
        }
        if (isDistrictRole(role)) {
            District userDistrict = userDistrict(user);
            return stations.stream()
                    .filter(station -> station.getDistrict() != null)
                    .filter(station -> userDistrict != null && userDistrict.getId().equals(station.getDistrict().getId()))
                    .toList();
        }
        if (user.getStation() != null) {
            return stations.stream()
                    .filter(station -> station.getId().equals(user.getStation().getId()))
                    .toList();
        }
        return List.of();
    }

    public List<EmergencyCall> visibleCalls(User user, List<EmergencyCall> calls) {
        if (user == null || !canAccessCallHandling(user)) {
            return List.of();
        }
        return calls;
    }

    public List<Equipment> visibleEquipment(User user, List<Equipment> equipment) {
        if (user == null) {
            return List.of();
        }
        String role = normalizeRole(user.getRole());
        if (isHeadquartersRole(role) || canManageSystemSettings(user) || canManageUsers(user)) {
            return equipment;
        }
        if (isRegionalRole(role)) {
            Region userRegion = userRegion(user);
            return equipment.stream()
                    .filter(item -> item.getStation() != null && item.getStation().getDistrict() != null && item.getStation().getDistrict().getRegion() != null)
                    .filter(item -> userRegion != null && userRegion.getId().equals(item.getStation().getDistrict().getRegion().getId()))
                    .toList();
        }
        if (isDistrictRole(role)) {
            District userDistrict = userDistrict(user);
            return equipment.stream()
                    .filter(item -> item.getStation() != null && item.getStation().getDistrict() != null)
                    .filter(item -> userDistrict != null && userDistrict.getId().equals(item.getStation().getDistrict().getId()))
                    .toList();
        }
        if (user.getStation() != null) {
            return equipment.stream()
                    .filter(item -> item.getStation() != null && user.getStation().getId().equals(item.getStation().getId()))
                    .toList();
        }
        return List.of();
    }

    public List<HydrantReport> visibleHydrantReports(User user, List<HydrantReport> reports) {
        if (user == null) {
            return List.of();
        }
        String role = normalizeRole(user.getRole());
        if (isHeadquartersRole(role) || canManageSystemSettings(user) || canManageUsers(user)) {
            return reports;
        }
        if (isRegionalRole(role)) {
            Region userRegion = userRegion(user);
            return reports.stream()
                    .filter(report -> report.getRegion() != null || report.getDistrict() != null || report.getStation() != null)
                    .filter(report -> {
                        Region region = report.getRegion() != null
                                ? report.getRegion()
                                : report.getDistrict() != null ? report.getDistrict().getRegion()
                                : report.getStation() != null && report.getStation().getDistrict() != null ? report.getStation().getDistrict().getRegion() : null;
                        return userRegion != null && region != null && userRegion.getId().equals(region.getId());
                    })
                    .toList();
        }
        if (isDistrictRole(role)) {
            District userDistrict = userDistrict(user);
            return reports.stream()
                    .filter(report -> {
                        District district = report.getDistrict() != null
                                ? report.getDistrict()
                                : report.getStation() != null ? report.getStation().getDistrict() : null;
                        return userDistrict != null && district != null && userDistrict.getId().equals(district.getId());
                    })
                    .toList();
        }
        if (user.getStation() != null) {
            return reports.stream()
                    .filter(report -> report.getStation() != null && user.getStation().getId().equals(report.getStation().getId()))
                    .toList();
        }
        return List.of();
    }

    public RoleResponsibilityService.RoleWorkspaceDefinition workspaceDefinition(User user) {
        return roleResponsibilityService.definitionFor(user == null ? null : user.getRole());
    }

    public RoleResponsibilityService.WorkspaceSidebar sidebar(User user, RoleResponsibilityService.WorkspacePage page, boolean hasReports) {
        return roleResponsibilityService.sidebarFor(user == null ? null : user.getRole(), page, hasReports);
    }

    public void enforceAction(User user, String action) {
        if (!hasAction(user, action)) {
            throw new IllegalStateException("Action not allowed for your role");
        }
    }

    public boolean hasAction(User user, String action) {
        return roleResponsibilityService.hasAction(user == null ? null : user.getRole(), action);
    }

    public boolean canDispatchTeams(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_DISPATCH_INCIDENTS);
    }

    public boolean canManageEquipment(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_MANAGE_EQUIPMENT);
    }

    public boolean canManageHydrants(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_MANAGE_HYDRANTS);
    }

    public boolean canManageMonthlyUpdates(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_MANAGE_MONTHLY_UPDATES);
    }

    public boolean canAccessCallHandling(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_HANDLE_CALLS);
    }

    public boolean canRegisterInitialIncident(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_REGISTER_INITIAL_INCIDENT);
    }

    public boolean canCompleteIncidentReport(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_COMPLETE_INCIDENT_REPORT);
    }

    public boolean canManageUsers(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_MANAGE_USERS);
    }

    public boolean canManageRolePermissions(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_MANAGE_ROLE_PERMISSIONS);
    }

    public boolean canManageSystemSettings(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_MANAGE_SYSTEM_SETTINGS);
    }

    public boolean canViewMap(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_VIEW_MAP_ROUTING);
    }

    public boolean canViewTeleSupport(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_TELE_SUPPORT);
    }

    public boolean canViewLiveVideo(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_VIEW_LIVE_VIDEO);
    }

    public boolean canPublishLiveVideo(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_PUBLISH_LIVE_VIDEO);
    }

    public boolean canAccessOperationsDashboard(User user) {
        return roleResponsibilityService.canAccessWorkspace(user == null ? null : user.getRole(), RoleResponsibilityService.WorkspacePage.OPERATIONS);
    }

    public boolean canAccessControlRoomDashboard(User user) {
        return roleResponsibilityService.canAccessWorkspace(user == null ? null : user.getRole(), RoleResponsibilityService.WorkspacePage.CONTROL_ROOM);
    }

    public boolean canAccessStationOperationsModule(User user) {
        return roleResponsibilityService.hasModule(user == null ? null : user.getRole(), RoleResponsibilityService.MODULE_STATION_OPERATIONS);
    }

    public boolean canAccessControlRoomChat(User user, EmergencyCall call) {
        if (user == null || call == null || user.getStation() == null || call.getRoutedStation() == null) {
            return false;
        }
        return canAccessCallHandling(user) && user.getStation().getId().equals(call.getRoutedStation().getId());
    }

    public boolean canSeeAiRecommendations(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_VIEW_AI_RECOMMENDATIONS);
    }

    public boolean canAccessAdminDocuments(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_VIEW_DOCUMENTATION);
    }

    public boolean canRunSystemTests(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_RUN_SYSTEM_TESTS);
    }

    public boolean canViewReports(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_VIEW_REPORTS);
    }

    public boolean canReviewOperationalApprovals(User user) {
        String role = normalizeRole(user == null ? null : user.getRole());
        return Set.of(
                OperationRole.SUPER_ADMIN,
                OperationRole.STATION_FIRE_OPERATION_OFFICER,
                OperationRole.STATION_FIRE_OFFICER,
                OperationRole.DISTRICT_OPERATION_OFFICER,
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER,
                OperationRole.COMMISSIONER_OPERATIONS
        ).contains(role);
    }

    public boolean canSubmitInvestigations(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_SUBMIT_INVESTIGATION);
    }

    public boolean canApproveInvestigations(User user) {
        return hasAction(user, RoleResponsibilityService.ACTION_REVIEW_DISTRICT_INVESTIGATION)
                || hasAction(user, RoleResponsibilityService.ACTION_REVIEW_REGIONAL_INVESTIGATION)
                || hasAction(user, RoleResponsibilityService.ACTION_REVIEW_NATIONAL_INVESTIGATION)
                || hasAction(user, RoleResponsibilityService.ACTION_FINAL_INVESTIGATION_APPROVAL);
    }

    private boolean canViewIncident(String role, User user, Incident incident) {
        if (incident == null) {
            return false;
        }
        if (isHeadquartersRole(role)) {
            return true;
        }
        if (OperationRole.FIRE_INVESTIGATION_HOD.equals(role)) {
            return "FIRE".equalsIgnoreCase(incident.getIncidentType()) || "RESCUE".equalsIgnoreCase(incident.getIncidentType());
        }
        if (isRegionalRole(role)) {
            Region userRegion = userRegion(user);
            Region incidentRegion = incidentRegion(incident);
            return userRegion != null && incidentRegion != null && userRegion.getId().equals(incidentRegion.getId());
        }
        if (isDistrictRole(role)) {
            District userDistrict = userDistrict(user);
            District incidentDistrict = incidentDistrict(incident);
            return userDistrict != null && incidentDistrict != null && userDistrict.getId().equals(incidentDistrict.getId());
        }
        if (stationScopedRoles().contains(role)) {
            return user.getStation() != null && incident.getStation() != null && user.getStation().getId().equals(incident.getStation().getId());
        }
        if (OperationRole.CONTROL_ROOM_ATTENDANT.equals(role) || OperationRole.CONTROL_ROOM_OPERATOR.equals(role)) {
            return true;
        }
        if (OperationRole.TELE_SUPPORT_PERSONNEL.equals(role)) {
            return "ACTIVE".equalsIgnoreCase(incident.getStatus())
                    || "RESPONDING".equalsIgnoreCase(incident.getStatus())
                    || "FIRE".equalsIgnoreCase(incident.getIncidentType())
                    || "RESCUE".equalsIgnoreCase(incident.getIncidentType());
        }
        return false;
    }

    private boolean isNationalOperationsRole(String role) {
        return Set.of(
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS,
                OperationRole.HEAD_RESCUE_OPERATIONS,
                OperationRole.CHIEF_FIRE_OFFICER
        ).contains(role);
    }

    private boolean isHeadquartersRole(String role) {
        return OperationRole.NATIONAL_ROLES.contains(role) || isNationalOperationsRole(role);
    }

    private boolean isRegionalRole(String role) {
        return Set.of(
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER,
                OperationRole.REGIONAL_INVESTIGATION_OFFICER
        ).contains(role);
    }

    private boolean isDistrictRole(String role) {
        return Set.of(
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.DISTRICT_OPERATION_OFFICER,
                OperationRole.DISTRICT_INVESTIGATION_OFFICER,
                OperationRole.FIRE_INVESTIGATION_OFFICER
        ).contains(role);
    }

    private Set<String> stationScopedRoles() {
        return Set.of(
                OperationRole.STATION_FIRE_OFFICER,
                OperationRole.STATION_FIRE_OPERATION_OFFICER,
                OperationRole.STATION_OPERATION_OFFICER,
                OperationRole.OPERATION_OFFICER,
                OperationRole.DEPARTMENT_OFFICER
        );
    }

    private Region userRegion(User user) {
        return user != null && user.getStation() != null && user.getStation().getDistrict() != null
                ? user.getStation().getDistrict().getRegion()
                : null;
    }

    private District userDistrict(User user) {
        return user != null && user.getStation() != null ? user.getStation().getDistrict() : null;
    }

    private Region incidentRegion(Incident incident) {
        if (incident.getRegion() != null) {
            return incident.getRegion();
        }
        if (incident.getDistrict() != null) {
            return incident.getDistrict().getRegion();
        }
        return incident.getStation() != null && incident.getStation().getDistrict() != null
                ? incident.getStation().getDistrict().getRegion()
                : null;
    }

    private District incidentDistrict(Incident incident) {
        if (incident.getDistrict() != null) {
            return incident.getDistrict();
        }
        return incident.getStation() != null ? incident.getStation().getDistrict() : null;
    }

    private String normalizeRole(String role) {
        return OperationRole.normalizeRole(role);
    }
}
