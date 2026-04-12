package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoleResponsibilityService {

    public static final String ACTION_MANAGE_USERS = "MANAGE_USERS";
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

    private final Map<String, RoleWorkspaceDefinition> definitions = Map.ofEntries(
            Map.entry(OperationRole.SUPER_ADMIN, definition(
                    OperationRole.SUPER_ADMIN,
                    "System Administration",
                    List.of(
                            "Manage user accounts and role assignments.",
                            "Configure regions, districts, stations, and system settings.",
                            "Review reports, documentation, and system verification outputs."
                    ),
                    actions(
                            ACTION_MANAGE_USERS,
                            ACTION_MANAGE_SYSTEM_SETTINGS,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_DOCUMENTATION,
                            ACTION_RUN_SYSTEM_TESTS
                    ),
                    modules(
                            MODULE_USER_MANAGEMENT,
                            MODULE_SYSTEM_SETTINGS,
                            MODULE_REPORTS,
                            MODULE_DOCUMENTATION,
                            MODULE_SYSTEM_TESTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.ADMIN, definition(
                    OperationRole.ADMIN,
                    "System Administration",
                    List.of(
                            "Manage user accounts and role assignments.",
                            "Review administrative reports and official system documentation."
                    ),
                    actions(
                            ACTION_MANAGE_USERS,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_DOCUMENTATION
                    ),
                    modules(
                            MODULE_USER_MANAGEMENT,
                            MODULE_REPORTS,
                            MODULE_DOCUMENTATION,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.CONTROL_ROOM_ATTENDANT, definition(
                    OperationRole.CONTROL_ROOM_ATTENDANT,
                    "Control Room Operations",
                    List.of(
                            "Receive 114 calls and validate emergency details.",
                            "Register initial incidents and complete incident reporting.",
                            "Dispatch the nearest station with route guidance and live control-room support."
                    ),
                    actions(
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
                    modules(
                            MODULE_CALLS,
                            MODULE_DISPATCH,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.CONTROL_ROOM),
                    false
            )),
            Map.entry(OperationRole.CONTROL_ROOM_OPERATOR, definition(
                    OperationRole.CONTROL_ROOM_OPERATOR,
                    "Control Room Operations",
                    List.of(
                            "Receive 114 calls and validate emergency details.",
                            "Register initial incidents and complete incident reporting.",
                            "Dispatch the nearest station with route guidance."
                    ),
                    actions(
                            ACTION_HANDLE_CALLS,
                            ACTION_REGISTER_INITIAL_INCIDENT,
                            ACTION_COMPLETE_INCIDENT_REPORT,
                            ACTION_DISPATCH_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_LIVE_VIDEO,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_CONTROL_ROOM_DASHBOARD
                    ),
                    modules(
                            MODULE_CALLS,
                            MODULE_DISPATCH,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.CONTROL_ROOM),
                    false
            )),
            Map.entry(OperationRole.STATION_OPERATION_OFFICER, definition(
                    OperationRole.STATION_OPERATION_OFFICER,
                    "Station Operations",
                    List.of(
                            "Execute assigned operations and update incident status.",
                            "Register hydrants, equipment, and monthly station readiness updates.",
                            "Publish live field video and request tele-support when needed."
                    ),
                    actions(
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
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_STATION_OPERATIONS,
                            MODULE_EQUIPMENT,
                            MODULE_HYDRANTS,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_TELE_SUPPORT,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.STATION_FIRE_OPERATION_OFFICER, definition(
                    OperationRole.STATION_FIRE_OPERATION_OFFICER,
                    "Station Fire Operations",
                    List.of(
                            "Coordinate assigned station incidents and register station reports.",
                            "Maintain equipment, hydrant readiness, and local operational reporting.",
                            "Use AI guidance, live monitoring, and tele-support as required."
                    ),
                    actions(
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
                    modules(
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
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.STATION_FIRE_OFFICER, definition(
                    OperationRole.STATION_FIRE_OFFICER,
                    "Station Fire Oversight",
                    List.of(
                            "Review assigned station incidents and readiness.",
                            "Monitor local equipment status and station reporting."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_MANAGE_EQUIPMENT,
                            ACTION_VIEW_REPORTS
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_STATION_OPERATIONS,
                            MODULE_EQUIPMENT,
                            MODULE_HYDRANTS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.DISTRICT_INVESTIGATION_OFFICER, definition(
                    OperationRole.DISTRICT_INVESTIGATION_OFFICER,
                    "District Investigation",
                    List.of(
                            "Conduct investigations for assigned incidents.",
                            "Compile findings, evidence, and submit reports into the approval workflow."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_SUBMIT_INVESTIGATION,
                            ACTION_VIEW_REPORTS
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_INVESTIGATIONS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.FIRE_INVESTIGATION_OFFICER, definition(
                    OperationRole.FIRE_INVESTIGATION_OFFICER,
                    "Investigation Intake",
                    List.of(
                            "Prepare investigation inputs and field evidence for the district workflow."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_SUBMIT_INVESTIGATION
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_INVESTIGATIONS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.DISTRICT_FIRE_OFFICER, definition(
                    OperationRole.DISTRICT_FIRE_OFFICER,
                    "District Command",
                    List.of(
                            "Review district incident activity, routing visibility, and live operational status.",
                            "Approve or deny district investigation reports."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_LIVE_VIDEO,
                            ACTION_REVIEW_DISTRICT_INVESTIGATION,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_INVESTIGATION_APPROVALS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS, WorkspacePage.ROLE),
                    true
            )),
            Map.entry(OperationRole.REGIONAL_INVESTIGATION_OFFICER, definition(
                    OperationRole.REGIONAL_INVESTIGATION_OFFICER,
                    "Regional Investigation Approval",
                    List.of(
                            "Review and approve or deny regional investigation reports."
                    ),
                    actions(
                            ACTION_REVIEW_REGIONAL_INVESTIGATION,
                            ACTION_VIEW_REPORTS
                    ),
                    modules(
                            MODULE_INVESTIGATION_APPROVALS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    true
            )),
            Map.entry(OperationRole.REGIONAL_FIRE_OFFICER, definition(
                    OperationRole.REGIONAL_FIRE_OFFICER,
                    "Regional Command",
                    List.of(
                            "Provide regional operational oversight.",
                            "Review investigation reports at the regional approval stage."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_LIVE_VIDEO,
                            ACTION_REVIEW_REGIONAL_INVESTIGATION,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_INVESTIGATION_APPROVALS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS, WorkspacePage.ROLE),
                    true
            )),
            Map.entry(OperationRole.REGIONAL_OPERATION_OFFICER, definition(
                    OperationRole.REGIONAL_OPERATION_OFFICER,
                    "Regional Operations",
                    List.of(
                            "Coordinate regional response visibility and remote support readiness."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_LIVE_VIDEO,
                            ACTION_TELE_SUPPORT,
                            ACTION_VIEW_AI_RECOMMENDATIONS,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_EQUIPMENT,
                            MODULE_HYDRANTS,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_TELE_SUPPORT,
                            MODULE_AI_RECOMMENDATIONS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS),
                    false
            )),
            Map.entry(OperationRole.DISTRICT_OPERATION_OFFICER, definition(
                    OperationRole.DISTRICT_OPERATION_OFFICER,
                    "District Operations",
                    List.of(
                            "Monitor district incident posture and coordinate remote support."
                    ),
                    actions(
                            ACTION_REGISTER_INITIAL_INCIDENT,
                            ACTION_COMPLETE_INCIDENT_REPORT,
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_TELE_SUPPORT,
                            ACTION_VIEW_AI_RECOMMENDATIONS,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_EQUIPMENT,
                            MODULE_HYDRANTS,
                            MODULE_MAP_ROUTING,
                            MODULE_TELE_SUPPORT,
                            MODULE_AI_RECOMMENDATIONS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS, WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.COMMISSIONER_OPERATIONS, definition(
                    OperationRole.COMMISSIONER_OPERATIONS,
                    "National Operations Command",
                    List.of(
                            "Provide national operations oversight.",
                            "Review investigation approvals before final CGF authorization.",
                            "Review national reports and strategic route intelligence."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_LIVE_VIDEO,
                            ACTION_VIEW_AI_RECOMMENDATIONS,
                            ACTION_VIEW_REPORTS,
                            ACTION_REVIEW_NATIONAL_INVESTIGATION,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_EQUIPMENT,
                            MODULE_HYDRANTS,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_AI_RECOMMENDATIONS,
                            MODULE_INVESTIGATION_APPROVALS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS),
                    true
            )),
            Map.entry(OperationRole.CGF, definition(
                    OperationRole.CGF,
                    "Strategic Final Approval",
                    List.of(
                            "Provide strategic decisions using national reports and summary dashboards.",
                            "Issue final investigation approval at the last workflow stage."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_LIVE_VIDEO,
                            ACTION_VIEW_REPORTS,
                            ACTION_FINAL_INVESTIGATION_APPROVAL,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_INVESTIGATION_APPROVALS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS),
                    true
            )),
            Map.entry(OperationRole.FIRE_INVESTIGATION_HOD, definition(
                    OperationRole.FIRE_INVESTIGATION_HOD,
                    "National Investigation Command",
                    List.of(
                            "Review investigation quality and approve reports at the HOD stage."
                    ),
                    actions(
                            ACTION_REVIEW_NATIONAL_INVESTIGATION,
                            ACTION_VIEW_REPORTS
                    ),
                    modules(
                            MODULE_INVESTIGATION_APPROVALS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    true
            )),
            Map.entry(OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS, definition(
                    OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS,
                    "National Fire Operations",
                    List.of(
                            "Review national fire operations posture and readiness."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_LIVE_VIDEO,
                            ACTION_VIEW_AI_RECOMMENDATIONS,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_AI_RECOMMENDATIONS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS),
                    true
            )),
            Map.entry(OperationRole.HEAD_RESCUE_OPERATIONS, definition(
                    OperationRole.HEAD_RESCUE_OPERATIONS,
                    "National Rescue Operations",
                    List.of(
                            "Review national rescue operations posture and readiness."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_LIVE_VIDEO,
                            ACTION_VIEW_AI_RECOMMENDATIONS,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_MAP_ROUTING,
                            MODULE_LIVE_VIDEO,
                            MODULE_AI_RECOMMENDATIONS,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS),
                    true
            )),
            Map.entry(OperationRole.CHIEF_FIRE_OFFICER, definition(
                    OperationRole.CHIEF_FIRE_OFFICER,
                    "Chief Fire Oversight",
                    List.of(
                            "Review national fire posture and command reports."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_REPORTS,
                            ACTION_VIEW_OPERATIONS_DASHBOARD
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_MAP_ROUTING,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.OPERATIONS),
                    true
            )),
            Map.entry(OperationRole.OPERATION_OFFICER, definition(
                    OperationRole.OPERATION_OFFICER,
                    "Operational Field Support",
                    List.of(
                            "Handle assigned incidents and submit field progress updates.",
                            "Use map guidance and request tele-support when required."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_UPDATE_INCIDENT_STATUS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_TELE_SUPPORT,
                            ACTION_VIEW_REPORTS
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_MAP_ROUTING,
                            MODULE_TELE_SUPPORT,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.DEPARTMENT_OFFICER, definition(
                    OperationRole.DEPARTMENT_OFFICER,
                    "Department Operations",
                    List.of(
                            "Track assigned incidents and departmental operational posture."
                    ),
                    actions(
                            ACTION_VIEW_ASSIGNED_INCIDENTS,
                            ACTION_VIEW_MAP_ROUTING,
                            ACTION_VIEW_REPORTS
                    ),
                    modules(
                            MODULE_INCIDENTS,
                            MODULE_MAP_ROUTING,
                            MODULE_REPORTS,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    false
            )),
            Map.entry(OperationRole.TELE_SUPPORT_PERSONNEL, definition(
                    OperationRole.TELE_SUPPORT_PERSONNEL,
                    "Tele-Support",
                    List.of(
                            "Respond to support requests and guide field teams remotely."
                    ),
                    actions(
                            ACTION_TELE_SUPPORT,
                            ACTION_VIEW_LIVE_VIDEO
                    ),
                    modules(
                            MODULE_TELE_SUPPORT,
                            MODULE_LIVE_VIDEO,
                            MODULE_NOTIFICATIONS
                    ),
                    pages(WorkspacePage.ROLE),
                    true
            )),
            Map.entry(OperationRole.UNASSIGNED, DEFAULT_DEFINITION)
    );

    public RoleWorkspaceDefinition definitionFor(String role) {
        return definitions.getOrDefault(normalizeRole(role), DEFAULT_DEFINITION);
    }

    public boolean hasAction(String role, String action) {
        return definitionFor(role).actions().contains(action);
    }

    public boolean hasModule(String role, String module) {
        return definitionFor(role).modules().contains(module);
    }

    public boolean canAccessWorkspace(String role, WorkspacePage page) {
        return definitionFor(role).pages().contains(page);
    }

    public WorkspaceSidebar sidebarFor(String role, WorkspacePage page, boolean hasReports) {
        RoleWorkspaceDefinition definition = definitionFor(role);
        List<SidebarSection> sections = switch (page) {
            case CONTROL_ROOM -> controlRoomSidebar(definition, hasReports);
            case OPERATIONS -> operationsSidebar(definition, hasReports);
            case ROLE -> roleSidebar(definition, hasReports);
        };
        return switch (page) {
            case CONTROL_ROOM -> new WorkspaceSidebar(
                    "FROMS Workspace",
                    "Control Room Command Center",
                    "Call intake, incident registration, dispatch, routing, and live communication support",
                    "Control room scope",
                    "Only control-room responsibilities defined for your role are visible here.",
                    sections
            );
            case OPERATIONS -> new WorkspaceSidebar(
                    "FROMS Workspace",
                    "Operations Command Center",
                    operationsBrandMeta(definition),
                    "Operations scope",
                    operationsAccessNote(definition),
                    sections
            );
            case ROLE -> new WorkspaceSidebar(
                    "FROMS Workspace",
                    "Fire and Rescue Operations Management System",
                    definition.roleLabel(),
                    "Role scope",
                    "No responsibility means no access and no visibility in this dashboard.",
                    sections
            );
        };
    }

    private List<SidebarSection> roleSidebar(RoleWorkspaceDefinition definition, boolean hasReports) {
        List<SidebarLink> workspace = new ArrayList<>();
        workspace.add(link("Overview", "#overview-panel"));
        workspace.add(link("Analytics", "#analytics-panel"));
        if (definition.modules().contains(MODULE_TELE_SUPPORT)) {
            workspace.add(link("Tele-Support", "#tele-support-module"));
        }
        if (definition.modules().contains(MODULE_MAP_ROUTING)) {
            workspace.add(link("Map & Routing", "#map-panel"));
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
        workspace.add(link("User Manual", "#user-manual-panel"));
        workspace.add(link("Notifications", "#notification-center"));

        List<SidebarLink> administration = new ArrayList<>();
        if (definition.modules().contains(MODULE_USER_MANAGEMENT)) {
            administration.add(link("Users", "#user-management-module"));
        }
        if (definition.modules().contains(MODULE_SYSTEM_SETTINGS)) {
            administration.add(link("System Settings", "#geography-module"));
        }
        if (definition.modules().contains(MODULE_DOCUMENTATION)) {
            administration.add(link("Documentation", "#documentation-module"));
        }
        if (definition.modules().contains(MODULE_SYSTEM_TESTS)) {
            administration.add(link("System Test", "#system-test-module"));
        }

        List<SidebarLink> incidentDesk = new ArrayList<>();
        if (definition.modules().contains(MODULE_INCIDENTS)) {
            incidentDesk.add(link("View Registered Incidents", "#incident-feed-panel"));
            if (definition.actions().contains(ACTION_REGISTER_INITIAL_INCIDENT) || definition.actions().contains(ACTION_COMPLETE_INCIDENT_REPORT)) {
                incidentDesk.add(link("Register Incident", "#incident-registration-module"));
            }
        }

        List<SidebarLink> equipmentDesk = new ArrayList<>();
        if (definition.modules().contains(MODULE_EQUIPMENT) || definition.modules().contains(MODULE_HYDRANTS)) {
            equipmentDesk.add(link(
                    OperationRole.STATION_FIRE_OPERATION_OFFICER.equals(definition.roleKey())
                            ? "Equipment Management"
                            : "Equipments",
                    "#equipment-summary-module"
            ));
            if (definition.modules().contains(MODULE_HYDRANTS)) {
                equipmentDesk.add(link("Hydrants", "#hydrants-module"));
            }
            if (definition.modules().contains(MODULE_EQUIPMENT)) {
                equipmentDesk.add(link("Fire Tender", "#equipment-fire-tender"));
                equipmentDesk.add(link("Command Car", "#equipment-command-car"));
                equipmentDesk.add(link("Management Car", "#equipment-management-car"));
                equipmentDesk.add(link("Hazmat Car", "#equipment-hazmat-car"));
                equipmentDesk.add(link("Ambulance", "#equipment-ambulance"));
                equipmentDesk.add(link("Rescue Equipment", "#equipment-rescue-equipment"));
                equipmentDesk.add(link("Fire Fighting Equipment", "#equipment-fire-fighting-equipment"));
                equipmentDesk.add(link("BA", "#equipment-ba"));
                equipmentDesk.add(link("Fire Fighting Chemicals", "#equipment-fire-fighting-chemicals"));
            }
        }

        List<SidebarSection> sections = new ArrayList<>();
        sections.add(new SidebarSection("Workspace", workspace));
        if (!incidentDesk.isEmpty()) {
            sections.add(new SidebarSection("Incident Desk", incidentDesk));
        }
        if (!equipmentDesk.isEmpty()) {
            sections.add(new SidebarSection(
                    OperationRole.STATION_FIRE_OPERATION_OFFICER.equals(definition.roleKey())
                            ? "Equipment Management"
                            : "Equipments",
                    equipmentDesk
            ));
        }
        if (!administration.isEmpty()) {
            sections.add(new SidebarSection("Administration", administration));
        }
        sections.add(new SidebarSection("Account", List.of(link("Profile", "#profile-actions"))));
        return sections;
    }

    private List<SidebarSection> operationsSidebar(RoleWorkspaceDefinition definition, boolean hasReports) {
        List<SidebarLink> workspace = new ArrayList<>();
        workspace.add(link("Overview", "#operations-overview"));
        if (definition.modules().contains(MODULE_MAP_ROUTING)) {
            workspace.add(link("AI Route", "#operations-route-panel"));
        }
        if (definition.modules().contains(MODULE_INCIDENTS)) {
            workspace.add(link("Incident Feed", "#operations-incident-feed"));
            workspace.add(link("Detailed Incident Desk", "/dashboard#incident-feed-panel"));
        }
        if (definition.modules().contains(MODULE_EQUIPMENT) || definition.modules().contains(MODULE_HYDRANTS)) {
            workspace.add(link("Equipment Desk", "/dashboard#equipment-summary-module"));
        }
        if (definition.modules().contains(MODULE_AI_RECOMMENDATIONS)) {
            workspace.add(link("AI Recommendations", "#operations-ai-panel"));
        }
        if (hasReports && definition.modules().contains(MODULE_REPORTS)) {
            workspace.add(link("Reports", "#operations-report-panel"));
        }
        workspace.add(link("User Manual", "#operations-manual-panel"));
        workspace.add(link("Notifications", "#notification-center"));

        return List.of(
                new SidebarSection("Workspace", workspace),
                new SidebarSection("Account", List.of(link("Profile", "#profile-actions")))
        );
    }

    private List<SidebarSection> controlRoomSidebar(RoleWorkspaceDefinition definition, boolean hasReports) {
        List<SidebarLink> workspace = new ArrayList<>();
        workspace.add(link("Overview", "#control-overview"));
        if (definition.modules().contains(MODULE_MAP_ROUTING)) {
            workspace.add(link("Dispatch Route", "#control-route-panel"));
        }
        if (definition.modules().contains(MODULE_LIVE_VIDEO)) {
            workspace.add(link("Live Video", "#control-video-panel"));
        }
        if (definition.modules().contains(MODULE_CALLS)) {
            workspace.add(link("Call History", "#control-call-history"));
        }
        workspace.add(link("User Manual", "#control-manual-panel"));
        workspace.add(link("Notifications", "#notification-center"));
        if (hasReports && definition.modules().contains(MODULE_REPORTS)) {
            workspace.add(link("Reports", "#control-reports-panel"));
        }

        return List.of(
                new SidebarSection("Workspace", workspace),
                new SidebarSection("Account", List.of(link("Profile", "#profile-actions")))
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

    private Set<String> actions(String... values) {
        return Set.of(values);
    }

    private Set<String> modules(String... values) {
        return Set.of(values);
    }

    private Set<WorkspacePage> pages(WorkspacePage... values) {
        return Set.of(values);
    }

    private SidebarLink link(String label, String href) {
        return new SidebarLink(label, href);
    }

    private String normalizeRole(String role) {
        return OperationRole.normalizeRole(role);
    }

    private String operationsBrandMeta(RoleWorkspaceDefinition definition) {
        if (isNationalOperationsRole(definition.roleKey())) {
            return "National oversight from Fire and Rescue Force Headquarters, Dodoma";
        }
        if (OperationRole.DISTRICT_FIRE_OFFICER.equals(definition.roleKey())
                || OperationRole.DISTRICT_OPERATION_OFFICER.equals(definition.roleKey())) {
            return "District oversight for incidents, routes, approvals, and reports";
        }
        return "Regional and national oversight for incidents, routes, approvals, and reports";
    }

    private String operationsAccessNote(RoleWorkspaceDefinition definition) {
        if (isNationalOperationsRole(definition.roleKey())) {
            return "Headquarters roles monitor the full country from Fire and Rescue Force Headquarters, Dodoma.";
        }
        if (OperationRole.DISTRICT_FIRE_OFFICER.equals(definition.roleKey())
                || OperationRole.DISTRICT_OPERATION_OFFICER.equals(definition.roleKey())) {
            return "This workspace is filtered to your district only, even when the command menu matches regional oversight roles.";
        }
        return "This workspace exposes only the command modules tied to your responsibilities.";
    }

    private boolean isNationalOperationsRole(String role) {
        return Set.of(
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS,
                OperationRole.HEAD_RESCUE_OPERATIONS,
                OperationRole.CHIEF_FIRE_OFFICER
        ).contains(normalizeRole(role));
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
}
