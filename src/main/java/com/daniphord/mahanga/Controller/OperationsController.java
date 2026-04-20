package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.EmergencyResponse;
import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.HydrantReport;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Recommendation;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Service.AiRouteService;
import com.daniphord.mahanga.Service.ControlRoomService;
import com.daniphord.mahanga.Service.DashboardDefinitionService;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.EquipmentManagementService;
import com.daniphord.mahanga.Service.GeographyService;
import com.daniphord.mahanga.Service.InvestigationWorkflowService;
import com.daniphord.mahanga.Service.NotificationService;
import com.daniphord.mahanga.Service.OperationsService;
import com.daniphord.mahanga.Service.RoleAccessService;
import com.daniphord.mahanga.Service.RoleResponsibilityService;
import com.daniphord.mahanga.Service.ReportCenterService;
import com.daniphord.mahanga.Service.SystemTestService;
import com.daniphord.mahanga.Service.SystemDocumentationService;
import com.daniphord.mahanga.Service.UserService;
import com.daniphord.mahanga.Service.UserManualService;
import com.daniphord.mahanga.Service.VideoSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping
public class OperationsController {

    private final OperationsService operationsService;
    private final ControlRoomService controlRoomService;
    private final EquipmentManagementService equipmentManagementService;
    private final DashboardDefinitionService dashboardDefinitionService;
    private final VideoSessionService videoSessionService;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final RoleAccessService roleAccessService;
    private final UserService userService;
    private final InvestigationWorkflowService investigationWorkflowService;
    private final NotificationService notificationService;
    private final GeographyService geographyService;
    private final SystemTestService systemTestService;
    private final SystemDocumentationService systemDocumentationService;
    private final ReportCenterService reportCenterService;
    private final UserManualService userManualService;
    private final AiRouteService aiRouteService;

    public OperationsController(
            OperationsService operationsService,
            ControlRoomService controlRoomService,
            EquipmentManagementService equipmentManagementService,
            DashboardDefinitionService dashboardDefinitionService,
            VideoSessionService videoSessionService,
            UserRepository userRepository,
            StationRepository stationRepository,
            RoleAccessService roleAccessService,
            UserService userService,
            InvestigationWorkflowService investigationWorkflowService,
            NotificationService notificationService,
            GeographyService geographyService,
            SystemTestService systemTestService,
            SystemDocumentationService systemDocumentationService,
            ReportCenterService reportCenterService,
            UserManualService userManualService,
            AiRouteService aiRouteService
    ) {
        this.operationsService = operationsService;
        this.controlRoomService = controlRoomService;
        this.equipmentManagementService = equipmentManagementService;
        this.dashboardDefinitionService = dashboardDefinitionService;
        this.videoSessionService = videoSessionService;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.roleAccessService = roleAccessService;
        this.userService = userService;
        this.investigationWorkflowService = investigationWorkflowService;
        this.notificationService = notificationService;
        this.geographyService = geographyService;
        this.systemTestService = systemTestService;
        this.systemDocumentationService = systemDocumentationService;
        this.reportCenterService = reportCenterService;
        this.userManualService = userManualService;
        this.aiRouteService = aiRouteService;
    }

    @GetMapping("/dashboard")
    public String roleDashboard(HttpSession session, Model model) {
        return populateDashboard(session, model, "role-dashboard");
    }

    @GetMapping("/operations/dashboard")
    public String operationsDashboard(HttpSession session, Model model) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canAccessOperationsDashboard(currentUser)) {
            throw new IllegalStateException("Action not allowed for your role");
        }
        return populateDashboard(session, model, "operations-dashboard");
    }

    private String populateDashboard(HttpSession session, Model model, String viewName) {
        User currentUser = currentUser(session);
        var visibleIncidents = roleAccessService.visibleIncidents(currentUser, operationsService.getAllIncidents());
        var visibleCalls = roleAccessService.visibleCalls(currentUser, controlRoomService.latestCalls());
        var visibleEquipment = equipmentManagementService.visibleEquipment(currentUser);
        var visibleHydrantReports = equipmentManagementService.getHydrantReports(currentUser);
        var visibleRecommendations = roleAccessService.visibleRecommendations(currentUser, operationsService.latestRecommendations());
        var dashboardDefinition = dashboardDefinitionService.definitionFor((String) session.getAttribute("role"));
        boolean canManageUsers = roleAccessService.canManageUsers(currentUser);
        boolean canManageSystemSettings = roleAccessService.canManageSystemSettings(currentUser);
        boolean canRunSystemTests = roleAccessService.canRunSystemTests(currentUser);
        boolean canAccessAdminDocuments = roleAccessService.canAccessAdminDocuments(currentUser);
        boolean canManageEquipment = roleAccessService.canManageEquipment(currentUser);
        boolean canManageHydrants = roleAccessService.canManageHydrants(currentUser);
        boolean canManageMonthlyUpdates = roleAccessService.canManageMonthlyUpdates(currentUser);
        boolean canReviewOperationalApprovals = roleAccessService.canReviewOperationalApprovals(currentUser);
        boolean canRegisterOperationalIncident = roleAccessService.canRegisterInitialIncident(currentUser) || roleAccessService.canCompleteIncidentReport(currentUser);
        var availableReports = reportCenterService.availableReports(currentUser);
        boolean isAdminWorkbench = canManageUsers || canManageSystemSettings;
        Map<Long, String> recommendationByIncidentId = latestRecommendationSummaryByIncident(visibleRecommendations);
        var incidentRecordViews = incidentRecords(visibleIncidents, recommendationByIncidentId);
        var equipmentRecordViews = equipmentRecords(visibleEquipment);
        var hydrantRecordViews = hydrantRecords(visibleHydrantReports);
        var equipmentCategoryViews = equipmentCategoryViews(visibleEquipment);
        var equipmentTypeSummaryViews = equipmentTypeSummaries(visibleEquipment);
        AiRouteService.RouteRecommendation priorityRoute = visibleIncidents.stream()
                .filter(incident -> incident.getLatitude() != null && incident.getLongitude() != null)
                .findFirst()
                .flatMap(aiRouteService::previewForIncident)
                .or(() -> visibleCalls.stream()
                        .filter(call -> call.getLatitude() != null && call.getLongitude() != null)
                        .findFirst()
                        .flatMap(aiRouteService::previewForCall))
                .orElse(null);
        model.addAttribute("metrics", operationsService.dashboardMetrics(visibleIncidents));
        model.addAttribute("equipmentMetrics", equipmentManagementService.dashboardMetrics(currentUser));
        model.addAttribute("incidents", visibleIncidents);
        model.addAttribute("incidentRecords", incidentRecordViews);
        model.addAttribute("equipmentRecords", equipmentRecordViews);
        model.addAttribute("hydrantRecords", hydrantRecordViews);
        model.addAttribute("equipmentCategoryViews", equipmentCategoryViews);
        model.addAttribute("equipmentTypeSummaries", equipmentTypeSummaryViews);
        model.addAttribute("incidentInitialCount", incidentRecordViews.stream().filter(item -> "INITIAL".equals(String.valueOf(item.get("reportLevel")))).count());
        model.addAttribute("incidentFullCount", incidentRecordViews.stream().filter(item -> "FULL".equals(String.valueOf(item.get("reportLevel")))).count());
        model.addAttribute("pendingIncidentApprovalCount", incidentRecordViews.stream().filter(item -> "PENDING".equals(String.valueOf(item.get("approvalStatus")))).count());
        model.addAttribute("pendingOperationalApprovalCount",
                equipmentRecordViews.stream().filter(item -> "PENDING".equals(String.valueOf(item.get("approvalStatus")))).count()
                        + hydrantRecordViews.stream().filter(item -> "PENDING".equals(String.valueOf(item.get("approvalStatus")))).count());
        model.addAttribute("recommendations", visibleRecommendations);
        model.addAttribute("stations", roleAccessService.visibleStations(currentUser, stationRepository.findAll()));
        model.addAttribute("callMetrics", controlRoomService.dashboardMetrics(visibleCalls));
        model.addAttribute("calls", visibleCalls);
        model.addAttribute("priorityRoute", priorityRoute);
        model.addAttribute("scopeMap", geographyService.scopeMap(currentUser));
        model.addAttribute("teleSupportMetrics", teleSupportMetrics(currentUser, visibleIncidents, visibleCalls));
        model.addAttribute("videoSessions", videoSessionService.latestSessions());
        model.addAttribute("videoMetrics", videoSessionService.metrics());
        model.addAttribute("dashboardDefinition", dashboardDefinition);
        model.addAttribute("roleWorkspace", roleAccessService.workspaceDefinition(currentUser));
        model.addAttribute("workspaceSidebar", roleAccessService.sidebar(
                currentUser,
                "operations-dashboard".equals(viewName) ? RoleResponsibilityService.WorkspacePage.OPERATIONS : RoleResponsibilityService.WorkspacePage.ROLE,
                !availableReports.isEmpty()
        ));
        model.addAttribute("loggedInUsername", session.getAttribute("username"));
        model.addAttribute("loggedInRole", session.getAttribute("role"));
        model.addAttribute("currentUserId", currentUser != null ? currentUser.getId() : null);
        model.addAttribute("currentStationId", currentUser != null && currentUser.getStation() != null ? currentUser.getStation().getId() : null);
        model.addAttribute("currentDistrictId", currentUser != null && currentUser.getStation() != null && currentUser.getStation().getDistrict() != null
                ? currentUser.getStation().getDistrict().getId()
                : null);
        model.addAttribute("currentRegionId", currentUser != null && currentUser.getStation() != null && currentUser.getStation().getDistrict() != null
                && currentUser.getStation().getDistrict().getRegion() != null
                ? currentUser.getStation().getDistrict().getRegion().getId()
                : null);
        model.addAttribute("internalUserManual", userManualService.internalDashboardManual((String) session.getAttribute("role")));
        model.addAttribute("operationsUserManual", userManualService.operationsDashboardManual((String) session.getAttribute("role")));
        model.addAttribute("isAdminWorkbench", isAdminWorkbench);
        model.addAttribute("canStartLiveVideo", roleAccessService.canPublishLiveVideo(currentUser));
        model.addAttribute("canDispatchTeams", roleAccessService.canDispatchTeams(currentUser));
        model.addAttribute("canRegisterOperationalIncident", canRegisterOperationalIncident);
        model.addAttribute("canManageEquipment", canManageEquipment);
        model.addAttribute("canManageHydrants", canManageHydrants);
        model.addAttribute("canReviewOperationalApprovals", canReviewOperationalApprovals);
        model.addAttribute("canManageMonthlyUpdates", canManageMonthlyUpdates);
        model.addAttribute("canAccessStationOperationsModule", roleAccessService.canAccessStationOperationsModule(currentUser));
        model.addAttribute("canManageUsers", canManageUsers);
        model.addAttribute("canViewMap", roleAccessService.canViewMap(currentUser));
        model.addAttribute("canViewTeleSupport", roleAccessService.canViewTeleSupport(currentUser));
        model.addAttribute("canViewLiveVideo", roleAccessService.canViewLiveVideo(currentUser));
        model.addAttribute("canSeeAiRecommendations", roleAccessService.canSeeAiRecommendations(currentUser));
        model.addAttribute("canAccessOperationsDashboard", roleAccessService.canAccessOperationsDashboard(currentUser));
        model.addAttribute("canAccessControlRoomDashboard", roleAccessService.canAccessControlRoomDashboard(currentUser));
        model.addAttribute("userSecuritySummary", userService.userSecuritySummary());
        model.addAttribute("lockedAccounts", userService.getLockedUsers());
        model.addAttribute("allUsers", roleAccessService.canManageUsers(currentUser) ? userService.getAllUsers() : java.util.List.of());
        model.addAttribute("stationOptions", stationRepository.findAll().stream()
                .map(station -> Map.of("id", station.getId(), "name", station.getName()))
                .toList());
        model.addAttribute("canManageGeography", canManageSystemSettings);
        model.addAttribute("regionOptions", geographyService.regionViews());
        model.addAttribute("canRunSystemTests", canRunSystemTests);
        model.addAttribute("canViewReports", roleAccessService.canViewReports(currentUser));
        model.addAttribute("availableReports", availableReports);
        model.addAttribute("incidentSourceCalls", controlRoomService.callsAvailableForIncidentRegistration(currentUser).stream()
                .map(call -> Map.of(
                        "id", call.getId(),
                        "reportNumber", call.getReportNumber(),
                        "incidentType", call.getIncidentType() == null ? "" : call.getIncidentType(),
                        "callerName", call.getCallerName() == null ? "" : call.getCallerName(),
                        "callerNumber", call.getCallerNumber() == null ? "" : call.getCallerNumber(),
                        "reportingMeans", call.getSourceChannel() == null ? "" : call.getSourceChannel(),
                        "location", call.getLocationText() == null ? "" : call.getLocationText(),
                        "callTime", call.getCallTime() == null ? "" : call.getCallTime().toString()
                ))
                .toList());
        model.addAttribute("availableInitialReports", operationsService.latestInitialIncidentsWithoutFull(currentUser).stream()
                .map(incident -> Map.of(
                        "id", incident.getId(),
                        "incidentNumber", incident.getIncidentNumber(),
                        "incidentType", incident.getIncidentType(),
                        "location", incident.getLocationDetails() == null ? "" : incident.getLocationDetails(),
                        "reportedAt", incident.getReportedAt() == null ? "" : incident.getReportedAt().toString()
                ))
                .toList());
        model.addAttribute("canAccessAdminDocuments", canAccessAdminDocuments);
        model.addAttribute("adminDocuments", canAccessAdminDocuments ? systemDocumentationService.adminDocuments() : java.util.List.of());
        model.addAttribute("systemTestReports", canRunSystemTests ? systemTestService.latestReports().stream()
                .map(report -> Map.of(
                        "id", report.getId(),
                        "reportNumber", report.getReportNumber(),
                        "status", report.getStatus(),
                        "triggerMode", report.getTriggerMode(),
                        "startedAt", report.getStartedAt() == null ? "" : report.getStartedAt().toString(),
                        "summary", report.getSummary() == null ? "" : report.getSummary(),
                        "passedChecks", report.getPassedChecks(),
                        "warningChecks", report.getWarningChecks(),
                        "failedChecks", report.getFailedChecks()
                ))
                .toList() : java.util.List.of());
        model.addAttribute("canAccessInvestigations", investigationWorkflowService.hasInvestigationAccess(currentUser));
        model.addAttribute("teleSupportRequestOptions", visibleIncidents.stream()
                .filter(incident -> !"RESOLVED".equalsIgnoreCase(incident.getStatus()))
                .map(incident -> Map.of(
                        "id", incident.getId(),
                        "incidentNumber", incident.getIncidentNumber(),
                        "incidentType", incident.getIncidentType(),
                        "location", incident.getLocationDetails() != null && !incident.getLocationDetails().isBlank()
                                ? incident.getLocationDetails()
                                : incident.getDistrict() != null ? incident.getDistrict().getName() : (incident.getVillage() == null ? "" : incident.getVillage()),
                        "status", incident.getStatus()
                ))
                .limit(20)
                .toList());
        model.addAttribute("canSubmitInvestigations", roleAccessService.canSubmitInvestigations(currentUser));
        model.addAttribute("canApproveInvestigations", roleAccessService.canApproveInvestigations(currentUser));
        model.addAttribute("investigationMetrics", investigationWorkflowService.dashboardMetrics(currentUser));
        model.addAttribute("investigations", investigationWorkflowService.visibleInvestigations(currentUser).stream()
                .map(report -> Map.of(
                        "id", report.getId(),
                        "investigationNumber", report.getInvestigationNumber(),
                        "status", report.getStatus(),
                        "currentLevel", report.getCurrentLevel(),
                        "incidentNumber", report.getIncident().getIncidentNumber(),
                        "incidentType", report.getIncident().getIncidentType(),
                        "location", report.getIncident().getDistrict() != null ? report.getIncident().getDistrict().getName() : (report.getIncident().getVillage() == null ? "" : report.getIncident().getVillage()),
                        "updatedAt", report.getUpdatedAt() == null ? "" : report.getUpdatedAt().toString()
                ))
                .toList());
        model.addAttribute("investigationEligibleIncidents", investigationWorkflowService.eligibleIncidents(currentUser).stream()
                .map(incident -> Map.of(
                        "id", incident.getId(),
                        "incidentNumber", incident.getIncidentNumber(),
                        "incidentType", incident.getIncidentType(),
                        "location", incident.getDistrict() != null ? incident.getDistrict().getName() : (incident.getVillage() == null ? "" : incident.getVillage())
                ))
                .toList());
        Long userId = currentUser != null ? currentUser.getId() : null;
        model.addAttribute("notifications", notificationService.latestNotifications(userId).stream()
                .map(notification -> Map.of(
                        "title", notification.getTitle(),
                        "message", notification.getMessage(),
                        "createdAt", notification.getCreatedAt() == null ? "" : notification.getCreatedAt().toString()
                ))
                .toList());
        model.addAttribute("notificationUnreadCount", notificationService.unreadCount(userId));
        return viewName;
    }

    @GetMapping("/api/incidents")
    @ResponseBody
    public ResponseEntity<?> listIncidents(HttpSession session) {
        User currentUser = currentUser(session);
        var visibleRecommendations = roleAccessService.visibleRecommendations(currentUser, operationsService.latestRecommendations());
        return ResponseEntity.ok(incidentRecords(
                roleAccessService.visibleIncidents(currentUser, operationsService.getAllIncidents()),
                latestRecommendationSummaryByIncident(visibleRecommendations)
        ));
    }

    @PostMapping("/api/incidents")
    @ResponseBody
    public ResponseEntity<?> createIncident(@RequestBody Incident incident, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canRegisterInitialIncident(currentUser) && !roleAccessService.canCompleteIncidentReport(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            return ResponseEntity.ok(toIncidentRecord(operationsService.createIncident(incident, currentUser), Map.of()));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/api/incidents/{incidentId}/resubmit")
    @ResponseBody
    public ResponseEntity<?> resubmitIncident(@PathVariable Long incidentId, @RequestBody Incident incident, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canRegisterInitialIncident(currentUser) && !roleAccessService.canCompleteIncidentReport(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            return ResponseEntity.ok(toIncidentRecord(operationsService.resubmitIncident(incidentId, incident, currentUser), Map.of()));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/api/incidents/{incidentId}/decision")
    @ResponseBody
    public ResponseEntity<?> reviewIncident(
            @PathVariable Long incidentId,
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
            Incident incident = operationsService.reviewIncident(incidentId, approve, comment, currentUser);
            return ResponseEntity.ok(toIncidentRecord(incident, Map.of()));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/api/incidents/{incidentId}/dispatch")
    @ResponseBody
    public ResponseEntity<?> dispatchResponse(@PathVariable Long incidentId, @RequestBody EmergencyResponse response, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canDispatchTeams(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(operationsService.dispatchResponse(incidentId, response, currentUser));
    }

    @PostMapping("/api/incidents/{incidentId}/complete")
    @ResponseBody
    public ResponseEntity<?> completeIncident(
            @PathVariable Long incidentId,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpSession session
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canCompleteIncidentReport(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            return ResponseEntity.ok(toIncidentRecord(operationsService.completeIncident(incidentId, payload, currentUser), Map.of()));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/api/incidents/{incidentId}/tele-support-request")
    @ResponseBody
    public ResponseEntity<?> requestTeleSupport(
            @PathVariable Long incidentId,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpSession session
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canViewTeleSupport(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            String message = payload == null || payload.get("message") == null ? "" : payload.get("message").toString();
            return ResponseEntity.ok(operationsService.requestTeleSupport(incidentId, message, currentUser));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/api/incidents/dashboard-metrics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> dashboardMetrics(HttpSession session) {
        User currentUser = currentUser(session);
        return ResponseEntity.ok(operationsService.dashboardMetrics(
                roleAccessService.visibleIncidents(currentUser, operationsService.getAllIncidents())
        ));
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }

    private Map<String, Object> teleSupportMetrics(User currentUser, java.util.List<Incident> incidents, java.util.List<com.daniphord.mahanga.Model.EmergencyCall> calls) {
        long pendingRequests = incidents.stream()
                .filter(incident -> "ACTIVE".equalsIgnoreCase(incident.getStatus()))
                .filter(incident -> "HIGH".equalsIgnoreCase(incident.getSeverity()) || "CRITICAL".equalsIgnoreCase(incident.getSeverity()))
                .count();
        long activeSessions = incidents.stream()
                .filter(incident -> "RESPONDING".equalsIgnoreCase(incident.getStatus()))
                .count();
        long onlineExperts = userRepository.findAll().stream()
                .map(User::getRole)
                .map(this::normalizeRole)
                .filter(role -> "TELE_SUPPORT_PERSONNEL".equals(role)
                        || "HEAD_FIRE_FIGHTING_OPERATIONS".equals(role)
                        || "HEAD_RESCUE_OPERATIONS".equals(role))
                .count();
        String supportMode = currentUser == null ? "Operational view" : switch (normalizeRole(currentUser.getRole())) {
            case "STATION_OPERATION_OFFICER", "OPERATION_OFFICER" -> "Request and join remote support";
            case "TELE_SUPPORT_PERSONNEL" -> "Accept remote assistance requests";
            case "CONTROL_ROOM_ATTENDANT" -> "Coordinate support activation";
            default -> "Monitor support readiness and session load";
        };

        return Map.of(
                "pendingRequests", pendingRequests,
                "activeSessions", activeSessions,
                "onlineExperts", onlineExperts,
                "linkedCalls", calls.stream().filter(call -> "ROUTED".equalsIgnoreCase(call.getStatus()) || "ACTIVE".equalsIgnoreCase(call.getStatus())).count(),
                "supportMode", supportMode
        );
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private Map<Long, String> latestRecommendationSummaryByIncident(List<Recommendation> recommendations) {
        Map<Long, String> summaryByIncident = new LinkedHashMap<>();
        for (Recommendation recommendation : recommendations) {
            if (recommendation.getIncident() == null || recommendation.getIncident().getId() == null) {
                continue;
            }
            summaryByIncident.putIfAbsent(recommendation.getIncident().getId(), recommendation.getSummary());
        }
        return summaryByIncident;
    }

    private List<Map<String, Object>> incidentRecords(List<Incident> incidents, Map<Long, String> recommendationByIncidentId) {
        java.util.Set<Long> initialsWithFullReport = incidents.stream()
                .filter(item -> "FULL".equalsIgnoreCase(item.getReportLevel()))
                .map(Incident::getParentIncident)
                .filter(java.util.Objects::nonNull)
                .map(Incident::getId)
                .collect(java.util.stream.Collectors.toSet());
        return incidents.stream()
                .map(incident -> toIncidentRecord(incident, recommendationByIncidentId, initialsWithFullReport.contains(incident.getId())))
                .toList();
    }

    private Map<String, Object> toIncidentRecord(Incident incident, Map<Long, String> recommendationByIncidentId) {
        return toIncidentRecord(incident, recommendationByIncidentId, false);
    }

    private Map<String, Object> toIncidentRecord(Incident incident, Map<Long, String> recommendationByIncidentId, boolean hasFullReport) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", incident.getId());
        record.put("incidentNumber", incident.getIncidentNumber());
        record.put("reportLevel", incident.getReportLevel());
        record.put("operationCategory", incident.getOperationCategory());
        record.put("incidentType", incident.getIncidentType());
        record.put("severity", incident.getSeverity());
        record.put("status", incident.getStatus());
        record.put("location", incident.getLocationDetails() != null && !incident.getLocationDetails().isBlank()
                ? incident.getLocationDetails()
                : incident.getDistrict() != null ? incident.getDistrict().getName() : incident.getVillage());
        record.put("ward", safe(incident.getWard()));
        record.put("village", safe(incident.getVillage()));
        record.put("roadLandmark", safe(incident.getRoadLandmark()));
        record.put("roadSymbol", safe(incident.getRoadSymbol()));
        record.put("reportingMeans", safe(incident.getReportingMeans()));
        record.put("reportingPerson", safe(incident.getReportingPerson()));
        record.put("reportingContact", safe(incident.getReportingContact()));
        record.put("parentIncidentId", incident.getParentIncident() == null ? null : incident.getParentIncident().getId());
        record.put("parentIncidentNumber", incident.getParentIncident() == null ? "" : safe(incident.getParentIncident().getIncidentNumber()));
        record.put("linkedCallId", incident.getEmergencyCall() == null ? null : incident.getEmergencyCall().getId());
        record.put("linkedCallReportNumber", incident.getEmergencyCall() == null ? "" : safe(incident.getEmergencyCall().getReportNumber()));
        record.put("hasFullReport", hasFullReport);
        record.put("reportedAt", incident.getReportedAt() == null ? "" : incident.getReportedAt().toString());
        record.put("incidentDurationMinutes", valueOrZero(incident.getIncidentDurationMinutes()));
        record.put("responseTimeMinutes", valueOrZero(incident.getResponseTimeMinutes()));
        record.put("operationDurationMinutes", valueOrZero(incident.getOperationDurationMinutes()));
        record.put("otherSecurityOrgans", safe(incident.getOtherSecurityOrgans()));
        record.put("oilUsedLitres", incident.getOilUsedLitres() == null ? "" : incident.getOilUsedLitres());
        record.put("effectsOnPeople", safe(incident.getEffectsOnPeople()));
        record.put("effectsOnEnvironment", safe(incident.getEffectsOnEnvironment()));
        record.put("casualtiesInjured", valueOrZero(incident.getCasualtiesInjured()));
        record.put("casualtiesDead", valueOrZero(incident.getCasualtiesDead()));
        record.put("injuredPeopleDetails", safe(incident.getInjuredPeopleDetails()));
        record.put("diedPeopleDetails", safe(incident.getDiedPeopleDetails()));
        record.put("casualtyDemographics", safe(incident.getCasualtyDemographics()));
        record.put("resourcesUsed", safe(incident.getResourcesUsed()));
        record.put("equipmentDispatched", safe(incident.getEquipmentDispatched()));
        record.put("personnelDispatched", safe(incident.getPersonnelDispatched()));
        record.put("personnelNames", safe(incident.getPersonnelNames()));
        record.put("supervisorName", safe(incident.getSupervisorName()));
        record.put("operationCommander", safe(incident.getOperationCommander()));
        record.put("description", safe(incident.getDescription()));
        record.put("actionTaken", safe(incident.getActionTaken()));
        record.put("outcome", safe(incident.getOutcome()));
        record.put("aiRecommendation", safe(incident.getAiRecommendationSnapshot()).isBlank()
                ? recommendationByIncidentId.getOrDefault(incident.getId(), "")
                : incident.getAiRecommendationSnapshot());
        record.put("aiImprovement", safe(incident.getAiImprovementSnapshot()));
        record.put("approvalStatus", safe(incident.getApprovalStatus()));
        record.put("approvalCurrentLevel", safe(incident.getApprovalCurrentLevel()));
        record.put("approvalLastComment", safe(incident.getApprovalLastComment()));
        record.put("createdBy", incident.getCreatedBy() != null
                ? (incident.getCreatedBy().getFullName() != null ? incident.getCreatedBy().getFullName() : incident.getCreatedBy().getUsername())
                : "");
        record.put("createdById", incident.getCreatedBy() == null ? null : incident.getCreatedBy().getId());
        record.put("routeRecommendation", routeRecommendation(incident));
        return record;
    }

    private Map<String, Object> routeRecommendation(Incident incident) {
        return aiRouteService.previewForIncident(incident)
                .map(route -> Map.of(
                        "stationId", route.stationId(),
                        "stationName", route.stationName(),
                        "stationDistrict", route.stationDistrict(),
                        "straightDistanceKm", route.straightDistanceKm(),
                        "roadDistanceKm", route.roadDistanceKm(),
                        "etaMinutes", route.etaMinutes(),
                        "learningFactor", route.learningFactor(),
                        "directions", route.directions(),
                        "embedUrl", route.embedUrl(),
                        "directionsUrl", route.directionsUrl()
                ))
                .orElse(Map.of());
    }

    private List<Map<String, Object>> equipmentRecords(List<Equipment> equipment) {
        return equipment.stream()
                .map(this::toEquipmentRecord)
                .toList();
    }

    private Map<String, Object> toEquipmentRecord(Equipment equipment) {
        String normalizedType = normalizedEquipmentType(equipment);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", equipment.getId());
        record.put("name", equipment.getName());
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
        record.put("station", equipment.getStation() == null ? "" : equipment.getStation().getName());
        record.put("createdBy", equipment.getCreatedBy() != null
                ? (equipment.getCreatedBy().getFullName() != null ? equipment.getCreatedBy().getFullName() : equipment.getCreatedBy().getUsername())
                : "");
        record.put("createdById", equipment.getCreatedBy() == null ? null : equipment.getCreatedBy().getId());
        record.put("approvalStatus", safe(equipment.getApprovalStatus()));
        record.put("approvalCurrentLevel", safe(equipment.getApprovalCurrentLevel()));
        record.put("approvalLastComment", safe(equipment.getApprovalLastComment()));
        return record;
    }

    private List<Map<String, Object>> hydrantRecords(List<HydrantReport> reports) {
        return reports.stream()
                .map(report -> {
                    Map<String, Object> record = new LinkedHashMap<>();
                    record.put("id", report.getId());
                    record.put("region", report.getRegion() == null ? "" : report.getRegion().getName());
                    record.put("district", report.getDistrict() == null ? "" : report.getDistrict().getName());
                    record.put("station", report.getStation() == null ? "" : report.getStation().getName());
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
                })
                .toList();
    }

    private List<EquipmentCategoryView> equipmentCategoryViews(List<Equipment> equipment) {
        List<EquipmentCategoryView> categories = new ArrayList<>();
        categories.add(categoryView("equipment-fire-tender", "Fire Tender", "FIRE_TENDER", "Station and district vehicle readiness.", equipment));
        categories.add(categoryView("equipment-command-car", "Command Car", "COMMAND_CAR", "Command and incident control vehicle readiness.", equipment));
        categories.add(categoryView("equipment-management-car", "Management Car", "MANAGEMENT_CAR", "Management transport and supervision vehicle availability.", equipment));
        categories.add(categoryView("equipment-hazmat-car", "Hazmat Car", "HAZMAT_CAR", "Hazmat response vehicle readiness and coverage.", equipment));
        categories.add(categoryView("equipment-ambulance", "Ambulance", "AMBULANCE", "Casualty transport and medical support availability.", equipment));
        categories.add(categoryView("equipment-rescue-equipment", "Rescue Equipment", "RESCUE_EQUIPMENT", "Rescue-specific tools and deployment stock.", equipment));
        categories.add(categoryView("equipment-fire-fighting-equipment", "Fire Fighting Equipment", "FIRE_FIGHTING_EQUIPMENT", "Suppression tools and operational readiness.", equipment));
        categories.add(categoryView("equipment-ba", "BA", "BA", "Breathing apparatus availability and condition.", equipment));
        categories.add(categoryView("equipment-fire-fighting-chemicals", "Fire Fighting Chemicals", "FIRE_FIGHTING_CHEMICALS", "Foam and chemical inventory.", equipment));
        return categories;
    }

    private EquipmentCategoryView categoryView(String anchorId, String label, String type, String description, List<Equipment> equipment) {
        List<Map<String, Object>> items = equipment.stream()
                .filter(item -> type.equals(normalizedEquipmentType(item)))
                .map(this::toEquipmentRecord)
                .toList();
        return new EquipmentCategoryView(anchorId, label, type, description, items);
    }

    private String normalizedEquipmentType(Equipment equipment) {
        if (equipment == null) {
            return "FIRE_FIGHTING_EQUIPMENT";
        }
        String subtypeType = subtypeAsEquipmentType(equipment.getSubtype());
        return subtypeType.isBlank() ? normalizeEquipmentType(equipment.getType()) : subtypeType;
    }

    private List<EquipmentTypeSummaryView> equipmentTypeSummaries(List<Equipment> equipment) {
        List<EquipmentTypeSummaryView> summaries = new ArrayList<>();
        for (EquipmentCategoryView category : equipmentCategoryViews(equipment)) {
            long total = category.items().stream().mapToLong(item -> ((Number) item.get("quantityInStore")).longValue()).sum();
            long inUse = category.items().stream()
                    .filter(item -> "IN_USE".equalsIgnoreCase(String.valueOf(item.get("operationalStatus"))))
                    .mapToLong(item -> ((Number) item.get("quantityInStore")).longValue())
                    .sum();
            long notInUse = category.items().stream()
                    .filter(item -> !"IN_USE".equalsIgnoreCase(String.valueOf(item.get("operationalStatus"))))
                    .mapToLong(item -> ((Number) item.get("quantityInStore")).longValue())
                    .sum();
            summaries.add(new EquipmentTypeSummaryView(category.label(), total, inUse, notInUse));
        }
        return summaries;
    }

    private String normalizeEquipmentType(String type) {
        if (type == null || type.isBlank()) {
            return "FIRE_FIGHTING_EQUIPMENT";
        }
        String normalized = type.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "FIRETENDER", "TENDER", "FIRE_TENDER" -> "FIRE_TENDER";
            case "COMMAND_CAR", "COMMAND_VEHICLE" -> "COMMAND_CAR";
            case "MANAGEMENT_CAR", "MANAGEMENT_VEHICLE", "SUPERVISION_CAR" -> "MANAGEMENT_CAR";
            case "HAZMAT_CAR", "HAZMAT_TRUCK", "HAZMAT_TRACK", "HAZMAT_VEHICLE" -> "HAZMAT_CAR";
            case "AMBULANCE" -> "AMBULANCE";
            case "RESCUE", "RESCUE_EQUIPMENT" -> "RESCUE_EQUIPMENT";
            case "FIRE_FIGHTING", "FIRE_EQUIPMENT", "FIRE_FIGHTING_EQUIPMENT" -> "FIRE_FIGHTING_EQUIPMENT";
            case "BA", "BREATHING_APPARATUS", "BREATHING_APARATUS" -> "BA";
            case "CHEMICAL", "CHEMICALS", "FIRE_FIGHTING_CHEMICALS" -> "FIRE_FIGHTING_CHEMICALS";
            default -> normalized;
        };
    }

    private String equipmentTypeLabel(String type) {
        return switch (type) {
            case "FIRE_TENDER" -> "Fire Tender";
            case "COMMAND_CAR" -> "Command Car";
            case "MANAGEMENT_CAR" -> "Management Car";
            case "HAZMAT_CAR" -> "Hazmat Car";
            case "AMBULANCE" -> "Ambulance";
            case "RESCUE_EQUIPMENT" -> "Rescue Equipment";
            case "FIRE_FIGHTING_EQUIPMENT" -> "Fire Fighting Equipment";
            case "BA" -> "BA";
            case "FIRE_FIGHTING_CHEMICALS" -> "Fire Fighting Chemicals";
            default -> type.replace('_', ' ');
        };
    }

    private String subtypeAsEquipmentType(String subtype) {
        if (subtype == null || subtype.isBlank()) {
            return "";
        }
        String normalized = subtype.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "AMBULANCE" -> "AMBULANCE";
            case "COMMAND_CAR" -> "COMMAND_CAR";
            case "MANAGEMENT_CAR" -> "MANAGEMENT_CAR";
            case "HAZMAT_TRUCK", "HAZMAT_TRACK", "HAZMAT_CAR" -> "HAZMAT_CAR";
            default -> "";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record EquipmentCategoryView(
            String anchorId,
            String label,
            String type,
            String description,
            List<Map<String, Object>> items
    ) {
    }

    private record EquipmentTypeSummaryView(String label, long total, long inUse, long notInUse) {
    }
}
