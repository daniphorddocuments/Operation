package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashboardDefinitionService {

    private static final DashboardDefinition DEFAULT_DEFINITION = new DashboardDefinition(
            "Operations Dashboard",
            "Operational access",
            "Field and support coordination",
            List.of("Support active fire and rescue operations.", "Maintain visibility on assigned incidents."),
            List.of("View assigned incidents", "Update reports", "Join tele-support calls"),
            List.of("Incident visibility", "Tele-support participation", "Field reporting")
    );

    private final Map<String, DashboardDefinition> definitions = Map.ofEntries(
            Map.entry("SUPER_ADMIN", definition(
                    "System Administration Dashboard",
                    "System administration",
                    "Full system control, user governance, geography setup, monitoring, reports, and documentation management.",
                    List.of("Manage all users and role assignments.", "Configure regions, districts, stations, and system setup.", "Monitor logs, errors, documents, and verification reports."),
                    List.of("User management", "System configuration", "Investigator registration", "System-wide reports", "Monitoring and documentation")
            )),
            Map.entry(OperationRole.CGF, definition(
                    "CGF Strategic Dashboard",
                    "National command",
                    "Top-level decision support across operations, critical incidents, investigations, and strategic analytics.",
                    List.of("Provide strategic leadership across the Fire and Rescue Force.", "Review national command posture and final investigation approvals.", "Download national reports and direct high-level decisions."),
                    List.of("National summary analytics", "Critical incident visibility", "Final investigation approvals", "Download all reports", "Strategic monitoring")
            )),
            Map.entry(OperationRole.COMMISSIONER_OPERATIONS, definition(
                    "Commissioner Operations Dashboard",
                    "National operations",
                    "National operations oversight with live monitoring, investigation approvals, and tele-support coordination.",
                    List.of("Supervise operations nationwide.", "Review investigation escalations before CGF approval.", "Coordinate tele-support and incident command posture."),
                    List.of("National incident dashboard", "Live monitoring", "Investigation approvals", "Tele-support oversight", "Response performance")
            )),
            Map.entry(OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS, definition(
                    "Fire Operations Dashboard",
                    "Fire command",
                    "Fire-fighting operational analytics, response oversight, and tele-fire guidance.",
                    List.of("Direct fire suppression readiness and national posture.", "Review fire-specific trends and response pressure."),
                    List.of("Fire incident analytics", "Live fire monitoring", "Equipment posture", "Tele-fire consultation")
            )),
            Map.entry(OperationRole.HEAD_RESCUE_OPERATIONS, definition(
                    "Rescue Operations Dashboard",
                    "Rescue command",
                    "Rescue-focused command dashboard for live incidents, readiness, and remote support.",
                    List.of("Manage rescue operations.", "Track rescue readiness and support coverage."),
                    List.of("Rescue incident analytics", "Monitor rescue teams", "Tele-rescue consultation", "Performance overview")
            )),
            Map.entry(OperationRole.REGIONAL_FIRE_OFFICER, definition(
                    "Regional Fire Officer Dashboard",
                    "Regional command",
                    "Regional supervision with performance analytics, investigation approvals, and operations monitoring.",
                    List.of("Manage regional operations.", "Review regional performance and approve investigation stages."),
                    List.of("Regional performance dashboard", "Investigation approvals", "Regional operations monitoring", "Resource visibility")
            )),
            Map.entry(OperationRole.REGIONAL_OPERATION_OFFICER, definition(
                    "Regional Operations Officer Dashboard",
                    "Regional operations",
                    "Regional incident coordination, live video monitoring, and tele-support control.",
                    List.of("Coordinate regional response.", "Track incidents and live field activity across the region."),
                    List.of("Regional incident overview", "Live video monitoring", "Tele-support coordination", "Resource allocation")
            )),
            Map.entry(OperationRole.DISTRICT_FIRE_OFFICER, definition(
                    "District Fire Officer Dashboard",
                    "District command",
                    "District approval dashboard for investigations and fire-service oversight.",
                    List.of("Approve or deny district investigation reports.", "Review comments, history, and attached evidence."),
                    List.of("Pending investigation reports", "Approve or deny with comments", "Download investigation reports", "District oversight")
            )),
            Map.entry(OperationRole.DISTRICT_OPERATION_OFFICER, definition(
                    "District Operations Officer Dashboard",
                    "District operations",
                    "District operational dashboard for incident analytics, live monitoring, and tele-support coordination.",
                    List.of("Coordinate district operations.", "Monitor live field activity and allocate district resources."),
                    List.of("District incidents overview", "Live video monitoring", "Tele-support coordination", "Resource allocation")
            )),
            Map.entry(OperationRole.STATION_FIRE_OFFICER, definition(
                    "Station Fire Officer Dashboard",
                    "Station command",
                    "Station-level supervision of readiness, assigned incidents, and local reporting.",
                    List.of("Supervise station readiness and assigned incidents.", "Review station activity and response posture."),
                    List.of("Assigned incidents", "Station readiness", "Local reports", "Response tracking")
            )),
            Map.entry(OperationRole.STATION_FIRE_OPERATION_OFFICER, definition(
                    "Station Fire Operation Officer Dashboard",
                    "Station fire operations",
                    "Combined station command for fire readiness, field coordination, live monitoring, and assigned incident control.",
                    List.of("Coordinate station incident response and fire-service readiness.", "Manage assigned station incidents with operational and fire oversight."),
                    List.of("Assigned incidents", "Station readiness", "Map and directions", "Tele-support coordination", "Incident status reporting")
            )),
            Map.entry(OperationRole.STATION_OPERATION_OFFICER, definition(
                    "Station Operation Officer Dashboard",
                    "Station operations",
                    "Station incident response dashboard with live video, tele-support, and field updates.",
                    List.of("Respond to assigned incidents.", "Start live field video and request support when needed."),
                    List.of("Assigned incidents", "Start live video", "Map and directions", "Tele-rescue or tele-fire support", "Incident status reporting")
            )),
            Map.entry(OperationRole.OPERATION_OFFICER, definition(
                    "Operation Officer Dashboard",
                    "Field operations",
                    "Field-response dashboard for assigned operational actions and reporting.",
                    List.of("Execute field tasks.", "Report progress and coordinate with support teams."),
                    List.of("Assigned incidents", "Status updates", "Tele-support participation", "Location guidance")
            )),
            Map.entry(OperationRole.TELE_SUPPORT_PERSONNEL, definition(
                    "Tele-Support Dashboard",
                    "Remote specialist support",
                    "Remote expert guidance for fire and rescue teams",
                    List.of("Provide remote expert assistance.", "Support officers in real time during incidents."),
                    List.of("Receive support requests", "Accept or decline requests", "Join video call", "Guide field officers in real time", "View incident details during call")
            )),
            Map.entry(OperationRole.FIRE_INVESTIGATION_HOD, definition(
                    "Fire Investigation HOD Dashboard",
                    "National investigation command",
                    "National investigation control dashboard with review, approvals, and investigation analytics.",
                    List.of("Review regional fire investigation reports.", "Maintain investigation quality, standards, and escalation control."),
                    List.of("Review all regional reports", "Approve or deny", "Investigation analytics", "Download final PDFs")
            )),
            Map.entry(OperationRole.REGIONAL_INVESTIGATION_OFFICER, definition(
                    "Regional Investigation Dashboard",
                    "Regional investigation",
                    "Regional approval and technical review of district investigations",
                    List.of("Review district investigation findings.", "Return reports with comments when corrections are required."),
                    List.of("Approve district submissions", "Deny with comments", "Review evidence and witness statements", "Download investigation PDF")
            )),
            Map.entry(OperationRole.DISTRICT_INVESTIGATION_OFFICER, definition(
                    "District Investigation Dashboard",
                    "District investigation",
                    "Field investigation intake, evidence recording, and report submission",
                    List.of("Start new investigations for fire and rescue incidents.", "Capture cause analysis and witness statements."),
                    List.of("Start investigation", "Upload evidence files", "Submit to District Fire Officer", "Track approval history")
            )),
            Map.entry(OperationRole.DEPARTMENT_OFFICER, definition(
                    "Department Dashboard",
                    "Department operations",
                    "Department-level operational monitoring",
                    List.of("Support departmental operations.", "Track assigned incident portfolio."),
                    List.of("View assigned incidents", "Monitor team activity", "Support tele-coordination")
            ))
    );

    @Cacheable("dashboardDefinitions")
    public DashboardDefinition definitionFor(String role) {
        String normalized = normalizeRole(role);
        return definitions.getOrDefault(normalized, DEFAULT_DEFINITION);
    }

    @Cacheable("dashboardRoutes")
    public String routeFor(String role) {
        String normalized = normalizeRole(role);
        if (List.of(
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS,
                OperationRole.HEAD_RESCUE_OPERATIONS,
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER,
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.DISTRICT_OPERATION_OFFICER
        ).contains(normalized)) {
            return "/operations/dashboard";
        }
        if (OperationRole.CONTROL_ROOM_ATTENDANT.equals(normalized)) {
            return "/control-room/dashboard";
        }
        return "/dashboard";
    }

    private static DashboardDefinition definition(
            String dashboardName,
            String scopeLabel,
            String overview,
            List<String> responsibilities,
            List<String> features
    ) {
        return new DashboardDefinition(
                dashboardName,
                scopeLabel,
                overview,
                responsibilities,
                features,
                List.of("Active tele-support sessions", "Pending support requests", "Online available experts")
        );
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    public record DashboardDefinition(
            String dashboardName,
            String scopeLabel,
            String overview,
            List<String> responsibilities,
            List<String> features,
            List<String> teleSupportHighlights
    ) {
    }
}
