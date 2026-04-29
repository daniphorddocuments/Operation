package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoleResponsibilityService {

    public static final String ACTION_MANAGE_USERS = "MANAGE_USERS";
    public static final String ACTION_MANAGE_ROLE_PERMISSIONS = "MANAGE_ROLE_PERMISSIONS";
    public static final String ACTION_MANAGE_SYSTEM_SETTINGS = "MANAGE_SYSTEM_SETTINGS";
    public static final String ACTION_VIEW_REPORTS = "VIEW_REPORTS";
    public static final String ACTION_VIEW_DOCUMENTATION = "VIEW_DOCUMENTATION";
    public static final String ACTION_RUN_SYSTEM_TESTS = "RUN_SYSTEM_TESTS";
    public static final String ACTION_HANDLE_CALLS = "HANDLE_CALLS";
    public static final String ACTION_REGISTER_INITIAL_INCIDENT = "REGISTER_INITIAL_INCIDENT";
    public static final String ACTION_COMPLETE_INCIDENT_REPORT = "COMPLETE_INCIDENT_REPORT";
    public static final String ACTION_DISPATCH_INCIDENTS = "DISPATCH_INCIDENTS";
    public static final String ACTION_VIEW_MAP_ROUTING = "VIEW_MAP_ROUTING";
    public static final String ACTION_VIEW_AI_RECOMMENDATIONS = "VIEW_AI_RECOMMENDATIONS";
    public static final String ACTION_VIEW_ASSIGNED_INCIDENTS = "VIEW_ASSIGNED_INCIDENTS";
    public static final String ACTION_UPDATE_INCIDENT_STATUS = "UPDATE_INCIDENT_STATUS";
    public static final String ACTION_MANAGE_HYDRANTS = "MANAGE_HYDRANTS";
    public static final String ACTION_MANAGE_EQUIPMENT = "MANAGE_EQUIPMENT";
    public static final String ACTION_MANAGE_MONTHLY_UPDATES = "MANAGE_MONTHLY_UPDATES";
    public static final String ACTION_VIEW_LIVE_VIDEO = "VIEW_LIVE_VIDEO";
    public static final String ACTION_PUBLISH_LIVE_VIDEO = "PUBLISH_LIVE_VIDEO";
    public static final String ACTION_TELE_SUPPORT = "TELE_SUPPORT";
    public static final String ACTION_SUBMIT_INVESTIGATION = "SUBMIT_INVESTIGATION";
    public static final String ACTION_REVIEW_DISTRICT_INVESTIGATION = "REVIEW_DISTRICT_INVESTIGATION";
    public static final String ACTION_REVIEW_REGIONAL_INVESTIGATION = "REVIEW_REGIONAL_INVESTIGATION";
    public static final String ACTION_REVIEW_NATIONAL_INVESTIGATION = "REVIEW_NATIONAL_INVESTIGATION";
    public static final String ACTION_FINAL_INVESTIGATION_APPROVAL = "FINAL_INVESTIGATION_APPROVAL";
    public static final String ACTION_VIEW_CONTROL_ROOM_DASHBOARD = "VIEW_CONTROL_ROOM_DASHBOARD";
    public static final String ACTION_VIEW_OPERATIONS_DASHBOARD = "VIEW_OPERATIONS_DASHBOARD";

    public static final String MODULE_INCIDENTS = "INCIDENTS";
    public static final String MODULE_CALLS = "CALLS";
    public static final String MODULE_DISPATCH = "DISPATCH";
    public static final String MODULE_MAP_ROUTING = "MAP_ROUTING";
    public static final String MODULE_LIVE_VIDEO = "LIVE_VIDEO";
    public static final String MODULE_TELE_SUPPORT = "TELE_SUPPORT";
    public static final String MODULE_STATION_OPERATIONS = "STATION_OPERATIONS";
    public static final String MODULE_INVESTIGATIONS = "INVESTIGATIONS";
    public static final String MODULE_INVESTIGATION_APPROVALS = "INVESTIGATION_APPROVALS";
    public static final String MODULE_USER_MANAGEMENT = "USER_MANAGEMENT";
    public static final String MODULE_SYSTEM_SETTINGS = "SYSTEM_SETTINGS";
    public static final String MODULE_REPORTS = "REPORTS";
    public static final String MODULE_DOCUMENTATION = "DOCUMENTATION";
    public static final String MODULE_SYSTEM_TESTS = "SYSTEM_TESTS";
    public static final String MODULE_AI_RECOMMENDATIONS = "AI_RECOMMENDATIONS";
    public static final String MODULE_NOTIFICATIONS = "NOTIFICATIONS";
    public static final String MODULE_EQUIPMENT = "EQUIPMENT";
    public static final String MODULE_HYDRANTS = "HYDRANTS";

    public enum WorkspacePage {
        ROLE,
        OPERATIONS,
        CONTROL_ROOM
    }

    private static final RoleWorkspaceDefinition DEFAULT_DEFINITION = new RoleWorkspaceDefinition(
            OperationRole.UNASSIGNED,
            "Restricted Workspace",
            List.of("No operational responsibilities are assigned to this role yet."),
            Set.of(),
            Set.of(MODULE_NOTIFICATIONS),
            Set.of(WorkspacePage.ROLE),
            true
    );

    private final Map<String, RoleWorkspaceDefinition> definitions;
    private final List<ActionDefinition> actionDefinitions;
    private final RolePermissionOverrideService rolePermissionOverrideService;

    public RoleResponsibilityService() {
        this(null);
    }

    @Autowired
    public RoleResponsibilityService(RolePermissionOverrideService rolePermissionOverrideService) {
        this.rolePermissionOverrideService = rolePermissionOverrideService;
        this.actionDefinitions = buildActionDefinitions();

        Map<String, RoleWorkspaceDefinition> map = new LinkedHashMap<>();
        map.put(OperationRole.SUPER_ADMIN, definition(
                OperationRole.SUPER_ADMIN,
                "System Administration",
                List.of("Manage users, permissions, settings, reports, and verification."),
                supportedActions(),
                Set.of(
                        MODULE_USER_MANAGEMENT,
                        MODULE_SYSTEM_SETTINGS,
                        MODULE_REPORTS,
                        MODULE_DOCUMENTATION,
                        MODULE_SYSTEM_TESTS,
                        MODULE_AI_RECOMMENDATIONS,
                        MODULE_EQUIPMENT,
                        MODULE_HYDRANTS,
                        MODULE_INCIDENTS,
                        MODULE_INVESTIGATIONS,
                        MODULE_INVESTIGATION_APPROVALS,
                        MODULE_CALLS,
                        MODULE_DISPATCH,
                        MODULE_MAP_ROUTING,
                        MODULE_LIVE_VIDEO,
                        MODULE_TELE_SUPPORT,
                        MODULE_STATION_OPERATIONS,
                        MODULE_NOTIFICATIONS
                ),
                Set.of(WorkspacePage.ROLE, WorkspacePage.OPERATIONS, WorkspacePage.CONTROL_ROOM),
                false
        ));
        map.put(OperationRole.ADMIN, definition(
                OperationRole.ADMIN,
                "Administration",
                List.of("Manage users, permissions, reports, and documentation."),
                Set.of(
                        ACTION_MANAGE_USERS,
                        ACTION_MANAGE_ROLE_PERMISSIONS,
                        ACTION_VIEW_REPORTS,
                        ACTION_VIEW_DOCUMENTATION
                ),
                Set.of(MODULE_USER_MANAGEMENT, MODULE_REPORTS, MODULE_DOCUMENTATION, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.ROLE),
                false
        ));
        map.put(OperationRole.CONTROL_ROOM_ATTENDANT, definition(
                OperationRole.CONTROL_ROOM_ATTENDANT,
                "Control Room Operations",
                List.of("Handle calls, register incidents, dispatch teams, and support live routing."),
                Set.of(
                        ACTION_HANDLE_CALLS,
                        ACTION_REGISTER_INITIAL_INCIDENT,
                        ACTION_COMPLETE_INCIDENT_REPORT,
                        ACTION_DISPATCH_INCIDENTS,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_PUBLISH_LIVE_VIDEO,
                        ACTION_VIEW_REPORTS,
                        ACTION_VIEW_CONTROL_ROOM_DASHBOARD
                ),
                Set.of(MODULE_CALLS, MODULE_DISPATCH, MODULE_MAP_ROUTING, MODULE_LIVE_VIDEO, MODULE_REPORTS, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.CONTROL_ROOM),
                false
        ));
        map.put(OperationRole.CONTROL_ROOM_OPERATOR, definition(
                OperationRole.CONTROL_ROOM_OPERATOR,
                "Control Room Operations",
                List.of("Handle calls, incidents, dispatch, and route guidance."),
                Set.of(
                        ACTION_HANDLE_CALLS,
                        ACTION_REGISTER_INITIAL_INCIDENT,
                        ACTION_COMPLETE_INCIDENT_REPORT,
                        ACTION_DISPATCH_INCIDENTS,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_VIEW_CONTROL_ROOM_DASHBOARD
                ),
                Set.of(MODULE_CALLS, MODULE_DISPATCH, MODULE_MAP_ROUTING, MODULE_LIVE_VIDEO, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.CONTROL_ROOM),
                false
        ));
        map.put(OperationRole.STATION_OPERATION_OFFICER, fieldRole(
                OperationRole.STATION_OPERATION_OFFICER,
                "Station Operations",
                Set.of(
                        ACTION_REGISTER_INITIAL_INCIDENT,
                        ACTION_COMPLETE_INCIDENT_REPORT,
                        ACTION_VIEW_ASSIGNED_INCIDENTS,
                        ACTION_UPDATE_INCIDENT_STATUS,
                        ACTION_MANAGE_HYDRANTS,
                        ACTION_MANAGE_EQUIPMENT,
                        ACTION_MANAGE_MONTHLY_UPDATES,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_PUBLISH_LIVE_VIDEO,
                        ACTION_TELE_SUPPORT,
                        ACTION_VIEW_REPORTS
                ),
                Set.of(
                        MODULE_INCIDENTS,
                        MODULE_STATION_OPERATIONS,
                        MODULE_EQUIPMENT,
                        MODULE_HYDRANTS,
                        MODULE_MAP_ROUTING,
                        MODULE_LIVE_VIDEO,
                        MODULE_TELE_SUPPORT,
                        MODULE_REPORTS,
                        MODULE_NOTIFICATIONS
                )
        ));
        map.put(OperationRole.STATION_FIRE_OPERATION_OFFICER, fieldRole(
                OperationRole.STATION_FIRE_OPERATION_OFFICER,
                "Station Fire Operations",
                Set.of(
                        ACTION_REGISTER_INITIAL_INCIDENT,
                        ACTION_COMPLETE_INCIDENT_REPORT,
                        ACTION_VIEW_ASSIGNED_INCIDENTS,
                        ACTION_UPDATE_INCIDENT_STATUS,
                        ACTION_MANAGE_HYDRANTS,
                        ACTION_MANAGE_EQUIPMENT,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_AI_RECOMMENDATIONS,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_TELE_SUPPORT,
                        ACTION_VIEW_REPORTS
                ),
                Set.of(
                        MODULE_INCIDENTS,
                        MODULE_STATION_OPERATIONS,
                        MODULE_EQUIPMENT,
                        MODULE_HYDRANTS,
                        MODULE_MAP_ROUTING,
                        MODULE_AI_RECOMMENDATIONS,
                        MODULE_LIVE_VIDEO,
                        MODULE_TELE_SUPPORT,
                        MODULE_REPORTS,
                        MODULE_NOTIFICATIONS
                )
        ));
        map.put(OperationRole.STATION_FIRE_OFFICER, fieldRole(
                OperationRole.STATION_FIRE_OFFICER,
                "Station Fire Oversight",
                Set.of(ACTION_VIEW_ASSIGNED_INCIDENTS, ACTION_MANAGE_EQUIPMENT, ACTION_VIEW_REPORTS),
                Set.of(MODULE_INCIDENTS, MODULE_EQUIPMENT, MODULE_HYDRANTS, MODULE_REPORTS, MODULE_NOTIFICATIONS)
        ));
        map.put(OperationRole.DISTRICT_INVESTIGATION_OFFICER, definition(
                OperationRole.DISTRICT_INVESTIGATION_OFFICER,
                "District Investigation",
                List.of("Conduct and submit investigations for assigned incidents."),
                Set.of(ACTION_VIEW_ASSIGNED_INCIDENTS, ACTION_SUBMIT_INVESTIGATION, ACTION_VIEW_REPORTS),
                Set.of(MODULE_INCIDENTS, MODULE_INVESTIGATIONS, MODULE_REPORTS, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.ROLE),
                false
        ));
        map.put(OperationRole.FIRE_INVESTIGATION_OFFICER, definition(
                OperationRole.FIRE_INVESTIGATION_OFFICER,
                "Investigation Intake",
                List.of("Prepare evidence and investigation submissions."),
                Set.of(ACTION_VIEW_ASSIGNED_INCIDENTS, ACTION_SUBMIT_INVESTIGATION),
                Set.of(MODULE_INCIDENTS, MODULE_INVESTIGATIONS, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.ROLE),
                false
        ));
        map.put(OperationRole.DISTRICT_FIRE_OFFICER, operationsRole(
                OperationRole.DISTRICT_FIRE_OFFICER,
                "District Command",
                Set.of(
                        ACTION_VIEW_ASSIGNED_INCIDENTS,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_REVIEW_DISTRICT_INVESTIGATION,
                        ACTION_VIEW_REPORTS,
                        ACTION_VIEW_OPERATIONS_DASHBOARD
                )
        ));
        map.put(OperationRole.DISTRICT_OPERATION_OFFICER, operationsRole(
                OperationRole.DISTRICT_OPERATION_OFFICER,
                "District Operations",
                Set.of(
                        ACTION_VIEW_ASSIGNED_INCIDENTS,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_TELE_SUPPORT,
                        ACTION_VIEW_AI_RECOMMENDATIONS,
                        ACTION_VIEW_REPORTS,
                        ACTION_VIEW_OPERATIONS_DASHBOARD
                )
        ));
        map.put(OperationRole.REGIONAL_INVESTIGATION_OFFICER, definition(
                OperationRole.REGIONAL_INVESTIGATION_OFFICER,
                "Regional Investigation Approval",
                List.of("Review regional investigation reports."),
                Set.of(ACTION_REVIEW_REGIONAL_INVESTIGATION, ACTION_VIEW_REPORTS),
                Set.of(MODULE_INVESTIGATION_APPROVALS, MODULE_REPORTS, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.ROLE),
                true
        ));
        map.put(OperationRole.REGIONAL_FIRE_OFFICER, operationsRole(
                OperationRole.REGIONAL_FIRE_OFFICER,
                "Regional Command",
                Set.of(
                        ACTION_VIEW_ASSIGNED_INCIDENTS,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_REPORTS,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_REVIEW_REGIONAL_INVESTIGATION,
                        ACTION_VIEW_OPERATIONS_DASHBOARD
                )
        ));
        map.put(OperationRole.REGIONAL_OPERATION_OFFICER, operationsRole(
                OperationRole.REGIONAL_OPERATION_OFFICER,
                "Regional Operations",
                Set.of(
                        ACTION_VIEW_ASSIGNED_INCIDENTS,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_TELE_SUPPORT,
                        ACTION_VIEW_AI_RECOMMENDATIONS,
                        ACTION_VIEW_REPORTS,
                        ACTION_VIEW_OPERATIONS_DASHBOARD
                )
        ));
        map.put(OperationRole.CGF, nationalRole(OperationRole.CGF, "CGF"));
        map.put(OperationRole.COMMISSIONER_OPERATIONS, nationalRole(OperationRole.COMMISSIONER_OPERATIONS, "Commissioner Operations"));
        map.put(OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS, nationalRole(OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS, "National Fire Operations"));
        map.put(OperationRole.HEAD_RESCUE_OPERATIONS, nationalRole(OperationRole.HEAD_RESCUE_OPERATIONS, "National Rescue Operations"));
        map.put(OperationRole.CHIEF_FIRE_OFFICER, nationalRole(OperationRole.CHIEF_FIRE_OFFICER, "Chief Fire Oversight"));
        map.put(OperationRole.FIRE_INVESTIGATION_HOD, definition(
                OperationRole.FIRE_INVESTIGATION_HOD,
                "National Investigation Command",
                List.of("Approve national investigation outputs."),
                Set.of(ACTION_REVIEW_NATIONAL_INVESTIGATION, ACTION_VIEW_REPORTS),
                Set.of(MODULE_INVESTIGATION_APPROVALS, MODULE_REPORTS, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.ROLE),
                true
        ));
        map.put(OperationRole.OPERATION_OFFICER, fieldRole(
                OperationRole.OPERATION_OFFICER,
                "Operational Field Support",
                Set.of(ACTION_VIEW_ASSIGNED_INCIDENTS, ACTION_UPDATE_INCIDENT_STATUS, ACTION_VIEW_MAP_ROUTING, ACTION_TELE_SUPPORT, ACTION_VIEW_REPORTS),
                Set.of(MODULE_INCIDENTS, MODULE_MAP_ROUTING, MODULE_TELE_SUPPORT, MODULE_REPORTS, MODULE_NOTIFICATIONS)
        ));
        map.put(OperationRole.DEPARTMENT_OFFICER, fieldRole(
                OperationRole.DEPARTMENT_OFFICER,
                "Department Operations",
                Set.of(ACTION_VIEW_ASSIGNED_INCIDENTS, ACTION_VIEW_MAP_ROUTING, ACTION_VIEW_REPORTS),
                Set.of(MODULE_INCIDENTS, MODULE_MAP_ROUTING, MODULE_REPORTS, MODULE_NOTIFICATIONS)
        ));
        map.put(OperationRole.TELE_SUPPORT_PERSONNEL, definition(
                OperationRole.TELE_SUPPORT_PERSONNEL,
                "Tele-Support",
                List.of("Guide field teams remotely."),
                Set.of(ACTION_TELE_SUPPORT, ACTION_VIEW_LIVE_VIDEO),
                Set.of(MODULE_TELE_SUPPORT, MODULE_LIVE_VIDEO, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.ROLE),
                true
        ));
        map.put(OperationRole.UNASSIGNED, DEFAULT_DEFINITION);

        this.definitions = Map.copyOf(map);
    }

    public RoleWorkspaceDefinition definitionFor(String role) {
        RoleWorkspaceDefinition baseDefinition = definitions.getOrDefault(normalizeRole(role), DEFAULT_DEFINITION);
        if (rolePermissionOverrideService == null || OperationRole.SUPER_ADMIN.equals(baseDefinition.roleKey())) {
            return baseDefinition;
        }
        Set<String> effectiveActions = rolePermissionOverrideService.applyOverrides(baseDefinition.roleKey(), baseDefinition.actions());
        if (effectiveActions.equals(baseDefinition.actions())) {
            return baseDefinition;
        }
        return new RoleWorkspaceDefinition(
                baseDefinition.roleKey(),
                baseDefinition.roleLabel(),
                baseDefinition.responsibilities(),
                effectiveActions,
                baseDefinition.modules(),
                baseDefinition.pages(),
                baseDefinition.readOnlyMostly()
        );
    }

    public boolean hasAction(String role, String action) {
        return definitionFor(role).actions().contains(action);
    }

    public boolean hasModule(String role, String module) {
        return definitionFor(role).modules().contains(module);
    }

    public boolean canAccessWorkspace(String role, WorkspacePage page) {
        if (page == WorkspacePage.OPERATIONS) {
            return hasAction(role, ACTION_VIEW_OPERATIONS_DASHBOARD);
        }
        if (page == WorkspacePage.CONTROL_ROOM) {
            return hasAction(role, ACTION_VIEW_CONTROL_ROOM_DASHBOARD);
        }
        return definitionFor(role).pages().contains(page);
    }

    public WorkspaceSidebar sidebarFor(String role, WorkspacePage page, boolean hasReports) {
        RoleWorkspaceDefinition definition = definitionFor(role);
        List<SidebarLink> workspace = new ArrayList<>();
        workspace.add(link("Overview", "#overview-panel"));
        if (page == WorkspacePage.CONTROL_ROOM) {
            workspace.add(link("Call History", "#control-call-history"));
        }
        if (definition.modules().contains(MODULE_MAP_ROUTING)) {
            workspace.add(link("Map & Routing", "#map-panel"));
        }
        if (definition.modules().contains(MODULE_LIVE_VIDEO)) {
            workspace.add(link("Live Video", "#control-video-panel"));
        }
        if (definition.modules().contains(MODULE_AI_RECOMMENDATIONS)) {
            workspace.add(link("AI Recommendations", "#ai-recommendations-panel"));
        }
        if (definition.modules().contains(MODULE_INVESTIGATIONS) || definition.modules().contains(MODULE_INVESTIGATION_APPROVALS)) {
            workspace.add(link("Investigations", "#investigation-module"));
        }
        if (hasReports && definition.modules().contains(MODULE_REPORTS)) {
            workspace.add(link("Reports", "#report-center-module"));
        }
        workspace.add(link("Notifications", "#notification-center"));

        List<SidebarSection> sections = List.of(
                new SidebarSection("Workspace", workspace),
                new SidebarSection("Account", List.of(link("Profile", "#profile-actions")))
        );

        String brandName = switch (page) {
            case CONTROL_ROOM -> "Control Room Command Center";
            case OPERATIONS -> "Operations Command Center";
            case ROLE -> "Fire and Rescue Operations Management System";
        };

        return new WorkspaceSidebar(
                "FROMS Workspace",
                brandName,
                definition.roleLabel(),
                "Role scope",
                "Only modules tied to the current role are visible.",
                sections
        );
    }

    public Map<String, Object> rolePermissionDefinition(String role) {
        RoleWorkspaceDefinition definition = definitionFor(role);
        return Map.of(
                "roleKey", definition.roleKey(),
                "roleLabel", definition.roleLabel(),
                "actions", actionDefinitions().stream()
                        .map(action -> Map.of(
                                "key", action.key(),
                                "label", action.label(),
                                "description", action.description(),
                                "enabled", definition.actions().contains(action.key())
                        ))
                        .toList(),
                "modules", definition.modules(),
                "pages", definition.pages(),
                "responsibilities", definition.responsibilities(),
                "readOnly", definition.readOnlyMostly()
        );
    }

    public List<Map<String, Object>> rolePermissionDefinitions() {
        return definitions.values().stream()
                .map(definition -> rolePermissionDefinition(definition.roleKey()))
                .toList();
    }

    public List<ActionDefinition> actionDefinitions() {
        return actionDefinitions;
    }

    public Set<String> supportedActions() {
        return actionDefinitions.stream().map(ActionDefinition::key).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isSupportedAction(String action) {
        return supportedActions().contains(action);
    }

    private RoleWorkspaceDefinition fieldRole(String roleKey, String roleLabel, Set<String> actions, Set<String> modules) {
        return definition(
                roleKey,
                roleLabel,
                List.of("Perform field-level operational duties assigned to this role."),
                actions,
                modules,
                Set.of(WorkspacePage.ROLE),
                false
        );
    }

    private RoleWorkspaceDefinition operationsRole(String roleKey, String roleLabel, Set<String> actions) {
        return definition(
                roleKey,
                roleLabel,
                List.of("Monitor command-level operations, routing visibility, and approvals."),
                actions,
                Set.of(MODULE_INCIDENTS, MODULE_MAP_ROUTING, MODULE_LIVE_VIDEO, MODULE_INVESTIGATION_APPROVALS, MODULE_REPORTS, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.OPERATIONS, WorkspacePage.ROLE),
                true
        );
    }

    private RoleWorkspaceDefinition nationalRole(String roleKey, String roleLabel) {
        return definition(
                roleKey,
                roleLabel,
                List.of("Monitor national incident posture and command reports."),
                Set.of(
                        ACTION_VIEW_ASSIGNED_INCIDENTS,
                        ACTION_VIEW_MAP_ROUTING,
                        ACTION_VIEW_LIVE_VIDEO,
                        ACTION_VIEW_AI_RECOMMENDATIONS,
                        ACTION_VIEW_REPORTS,
                        ACTION_VIEW_OPERATIONS_DASHBOARD
                ),
                Set.of(MODULE_INCIDENTS, MODULE_MAP_ROUTING, MODULE_LIVE_VIDEO, MODULE_AI_RECOMMENDATIONS, MODULE_REPORTS, MODULE_NOTIFICATIONS),
                Set.of(WorkspacePage.OPERATIONS),
                true
        );
    }

    private RoleWorkspaceDefinition definition(
            String roleKey,
            String roleLabel,
            List<String> responsibilities,
            Set<String> actions,
            Set<String> modules,
            Set<WorkspacePage> pages,
            boolean readOnlyMostly
    ) {
        return new RoleWorkspaceDefinition(roleKey, roleLabel, responsibilities, actions, modules, pages, readOnlyMostly);
    }

    private List<ActionDefinition> buildActionDefinitions() {
        return List.of(
                action(ACTION_MANAGE_USERS, "Manage users", "Create, update, and deactivate users."),
                action(ACTION_MANAGE_ROLE_PERMISSIONS, "Manage role permissions", "Change enabled actions for a role."),
                action(ACTION_MANAGE_SYSTEM_SETTINGS, "Manage system settings", "Configure geography and system settings."),
                action(ACTION_VIEW_REPORTS, "View reports", "Access operational and administrative reports."),
                action(ACTION_VIEW_DOCUMENTATION, "View documentation", "Open system documentation and manuals."),
                action(ACTION_RUN_SYSTEM_TESTS, "Run system tests", "Run startup and system verification tests."),
                action(ACTION_HANDLE_CALLS, "Handle calls", "Receive and manage emergency calls."),
                action(ACTION_REGISTER_INITIAL_INCIDENT, "Register incident", "Create an initial incident record."),
                action(ACTION_COMPLETE_INCIDENT_REPORT, "Complete incident report", "Finalize detailed incident reporting."),
                action(ACTION_DISPATCH_INCIDENTS, "Dispatch incidents", "Dispatch teams and route responses."),
                action(ACTION_VIEW_MAP_ROUTING, "View map routing", "Use routing and map guidance."),
                action(ACTION_VIEW_AI_RECOMMENDATIONS, "View AI recommendations", "Read AI-generated response guidance."),
                action(ACTION_VIEW_ASSIGNED_INCIDENTS, "View assigned incidents", "See incidents within the role scope."),
                action(ACTION_UPDATE_INCIDENT_STATUS, "Update incident status", "Update field progress and status."),
                action(ACTION_MANAGE_HYDRANTS, "Manage hydrants", "Create and maintain hydrant records."),
                action(ACTION_MANAGE_EQUIPMENT, "Manage equipment", "Create and maintain equipment records."),
                action(ACTION_MANAGE_MONTHLY_UPDATES, "Manage monthly updates", "Submit monthly station readiness updates."),
                action(ACTION_VIEW_LIVE_VIDEO, "View live video", "Watch live field video streams."),
                action(ACTION_PUBLISH_LIVE_VIDEO, "Publish live video", "Start live field video streams."),
                action(ACTION_TELE_SUPPORT, "Tele-support", "Provide or request remote support."),
                action(ACTION_SUBMIT_INVESTIGATION, "Submit investigation", "Submit investigation findings."),
                action(ACTION_REVIEW_DISTRICT_INVESTIGATION, "Review district investigation", "Approve or reject district investigations."),
                action(ACTION_REVIEW_REGIONAL_INVESTIGATION, "Review regional investigation", "Approve or reject regional investigations."),
                action(ACTION_REVIEW_NATIONAL_INVESTIGATION, "Review national investigation", "Approve or reject national investigations."),
                action(ACTION_FINAL_INVESTIGATION_APPROVAL, "Final investigation approval", "Issue final approval for investigations."),
                action(ACTION_VIEW_CONTROL_ROOM_DASHBOARD, "View control room dashboard", "Access control room workspace."),
                action(ACTION_VIEW_OPERATIONS_DASHBOARD, "View operations dashboard", "Access operations workspace.")
        );
    }

    private ActionDefinition action(String key, String label, String description) {
        return new ActionDefinition(key, label, description);
    }

    private SidebarLink link(String label, String href) {
        return new SidebarLink(label, href);
    }

    private String normalizeRole(String role) {
        return OperationRole.normalizeRole(role);
    }

    public record RoleWorkspaceDefinition(
            String roleKey,
            String roleLabel,
            List<String> responsibilities,
            Set<String> actions,
            Set<String> modules,
            Set<WorkspacePage> pages,
            boolean readOnlyMostly
    ) {
    }

    public record SidebarLink(String label, String href) {
    }

    public record SidebarSection(String title, List<SidebarLink> items) {
    }

    public record WorkspaceSidebar(
            String brandKicker,
            String brandName,
            String brandMeta,
            String accessTitle,
            String accessNote,
            List<SidebarSection> sections
    ) {
    }

    public record ActionDefinition(String key, String label, String description) {
    }
}
