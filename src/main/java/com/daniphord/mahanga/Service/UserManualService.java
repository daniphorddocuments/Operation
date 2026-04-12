package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserManualService {

    public UserManual publicLandingManual() {
        return new UserManual(
                "Public User Manual",
                "Citizens, field witnesses, village leaders, and community responders",
                "Use the public access flow to submit a verified fire, rescue, or accident report without creating an account.",
                List.of(
                        "Open Public Emergency Access from the landing page.",
                        "Provide caller contacts, incident type, and the correct region, district, and station.",
                        "Describe the scene clearly, complete the CAPTCHA, and submit the report.",
                        "Keep the tracking page open so you can chat with the control room or start live video if requested."
                ),
                List.of(
                        "Send a new emergency report.",
                        "Follow the assigned station and current case status.",
                        "Share additional scene updates through the case-room chat.",
                        "Start a live video session when the control room needs visual confirmation."
                ),
                List.of(
                        "Use this channel only for active emergency events that need Fire and Rescue response.",
                        "False, duplicate, or incomplete reports delay dispatch and remain auditable.",
                        "Keep your phone reachable after submission so the control room can verify details quickly."
                ),
                "If life is in immediate danger, call 114 while also submitting the report."
        );
    }

    public List<FaqItem> publicLandingFaqs() {
        UserManual manual = publicLandingManual();
        return List.of(
                new FaqItem(
                        "How do I report an emergency quickly?",
                        "Open Public Emergency Access, enter the caller details, choose the correct region, district, and station, then describe the scene and submit the verified report."
                ),
                new FaqItem(
                        "When should I call 114 instead of only using the portal?",
                        manual.helpNote()
                ),
                new FaqItem(
                        "Can I track what happens after I submit a report?",
                        "Yes. The system creates a case room where you can follow the report status, chat with the assigned control room, and start live video if the team requests scene visibility."
                ),
                new FaqItem(
                        "What information helps the control room most?",
                        String.join(" ", manual.quickStart())
                ),
                new FaqItem(
                        "Why does the form ask for region, district, and nearby fire station?",
                        "Those fields help the control room and station teams route the incident to the right operational area faster and reduce dispatch confusion."
                )
        );
    }

    public UserManual internalDashboardManual(String role) {
        String normalized = OperationRole.normalizeRole(role);
        if (List.of(OperationRole.SUPER_ADMIN, OperationRole.ADMIN).contains(normalized)) {
            return new UserManual(
                    "Internal Administration Manual",
                    "System administrators and platform governors",
                    "Use the internal dashboard to manage users, geography, documents, and system assurance work without affecting live operations unnecessarily.",
                    List.of(
                            "Review notifications, locked accounts, and security status before making changes.",
                            "Open only the user, geography, documentation, or system-test module needed for the task.",
                            "Confirm role or station updates before saving so command visibility stays accurate.",
                            "Return to the dashboard overview and verify the result after each privileged action."
                    ),
                    List.of(
                            "Create, activate, deactivate, and assign users.",
                            "Maintain regions, districts, and station mappings.",
                            "Download or publish controlled documentation.",
                            "Run verification and review system test reports."
                    ),
                    List.of(
                            "Only assign roles that match the formal command structure.",
                            "Use system tests before or immediately after critical configuration changes.",
                            "Treat account locking, password resets, and document updates as auditable actions."
                    ),
                    "Coordinate major configuration changes with command leadership before applying them."
            );
        }
        if (List.of(
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS,
                OperationRole.HEAD_RESCUE_OPERATIONS
        ).contains(normalized)) {
            return new UserManual(
                    "Internal Command Manual",
                    "National command and directorate leadership",
                    "Use the internal dashboard for a role-filtered command view of incidents, investigations, support posture, and downloadable oversight reports.",
                    List.of(
                            "Start from the overview and KPI cards to understand the current operational load.",
                            "Review visible incidents, support posture, and any investigation items requiring your level.",
                            "Use reports and notifications to confirm trends before issuing directives.",
                            "Escalate to operations or control-room views when you need live field or call context."
                    ),
                    List.of(
                            "Monitor command-level incident pressure.",
                            "Review tele-support readiness and role-scoped investigations.",
                            "Use maps, reports, and live context available to your role.",
                            "Track notifications affecting national posture."
                    ),
                    List.of(
                            "Only incidents and modules allowed for your role are shown here.",
                            "Treat the dashboard as a decision surface, not a substitute for formal dispatch authority outside your scope.",
                            "Download reports after confirming the visible scope matches the decision you are documenting."
                    ),
                    "Use the operations dashboard when you need the most direct incident and response picture."
            );
        }
        if (List.of(
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER
        ).contains(normalized)) {
            return new UserManual(
                    "Regional Internal Manual",
                    "Regional fire and operations leadership",
                    "Use the internal dashboard to supervise only the incidents, investigations, and stations inside your region.",
                    List.of(
                            "Check the overview first to confirm the active incident count and support posture for your region.",
                            "Review visible incidents and regional approvals requiring action.",
                            "Use the map, reports, and tele-support areas when field teams need coordination.",
                            "Confirm regional notifications before escalating to headquarters."
                    ),
                    List.of(
                            "Track regional incidents and resource visibility.",
                            "Review regional investigation flow where applicable.",
                            "Coordinate tele-support and regional reporting.",
                            "Monitor region-specific notifications."
                    ),
                    List.of(
                            "This dashboard should be used only for incidents inside your assigned region.",
                            "Escalate cross-region issues through national command instead of acting outside your boundary.",
                            "Keep district and station assignments accurate when making follow-up decisions."
                    ),
                    "Switch to the operations dashboard when you need broader incident and report panels."
            );
        }
        if (List.of(
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.DISTRICT_OPERATION_OFFICER
        ).contains(normalized)) {
            return new UserManual(
                    "District Internal Manual",
                    "District command and district operations officers",
                    "Use the internal dashboard to manage district-level incident visibility, approvals, and support coordination for the locations assigned to you.",
                    List.of(
                            "Review district KPIs and open incidents at the start of the shift.",
                            "Open pending investigation or coordination items that need district action.",
                            "Use tele-support, reports, and notifications to track follow-up work.",
                            "Confirm station-level escalation paths before closing or approving items."
                    ),
                    List.of(
                            "Monitor district incident status and severity.",
                            "Handle district approvals and follow-up review.",
                            "Coordinate support requests affecting stations in the district.",
                            "Export role-scoped reports when briefing leadership."
                    ),
                    List.of(
                            "Only district-visible records are authoritative for action from this screen.",
                            "Do not approve or deny outside your assigned approval level.",
                            "Use station and regional channels when the issue moves beyond district control."
                    ),
                    "Keep comments clear when approving, denying, or escalating district work."
            );
        }
        if (List.of(
                OperationRole.STATION_FIRE_OFFICER,
                OperationRole.STATION_OPERATION_OFFICER,
                OperationRole.STATION_FIRE_OPERATION_OFFICER,
                OperationRole.OPERATION_OFFICER,
                OperationRole.DEPARTMENT_OFFICER
        ).contains(normalized)) {
            return new UserManual(
                    "Field Internal Manual",
                    "Station command, field operators, and department officers",
                    "Use the internal dashboard for station-scoped visibility, assigned incidents, reporting duties, and support requests.",
                    List.of(
                            "Review the overview and assigned incident count at sign-in.",
                            "Open incident, map, or tele-support modules needed for your current assignment.",
                            "Send timely updates so district and regional command can see your progress.",
                            "Return to the dashboard after field actions to verify status changes were saved."
                    ),
                    List.of(
                            "Track assigned incidents and current response posture.",
                            "Use tele-support and map access when available to your role.",
                            "Update field reporting and local coordination tasks.",
                            "Monitor notifications related to your station or assignment."
                    ),
                    List.of(
                            "Work only within incidents and stations already assigned to your role.",
                            "Request escalation when district or regional action is required.",
                            "Do not leave incident records stale after a field status change."
                    ),
                    "Use the control-room dashboard only when your workflow requires live call or stream coordination."
            );
        }
        if (OperationRole.TELE_SUPPORT_PERSONNEL.equals(normalized)) {
            return new UserManual(
                    "Tele-Support Manual",
                    "Remote specialists and expert support personnel",
                    "Use the internal dashboard to watch assigned support demand, accept remote guidance work, and stay focused on live or high-risk scenes.",
                    List.of(
                            "Check pending support demand and available incident context from the overview.",
                            "Open only the incidents and support prompts visible to your role.",
                            "Join support activity quickly and keep recommendations concise and operational.",
                            "Review notifications before ending the session or changing focus."
                    ),
                    List.of(
                            "Monitor scenes likely to require remote expert guidance.",
                            "Support live incidents with focused recommendations.",
                            "Track support workload and command notifications.",
                            "Review only the operational context needed to guide field teams."
                    ),
                    List.of(
                            "Tele-support does not replace the local incident commander.",
                            "Give recommendations only within the evidence visible to you.",
                            "Escalate unresolved risks to command instead of improvising outside your role."
                    ),
                    "Keep remote guidance practical, short, and linked to the incident status visible in the system."
            );
        }
        if (List.of(
                OperationRole.FIRE_INVESTIGATION_HOD,
                OperationRole.FIRE_INVESTIGATION_OFFICER,
                OperationRole.REGIONAL_INVESTIGATION_OFFICER,
                OperationRole.DISTRICT_INVESTIGATION_OFFICER
        ).contains(normalized)) {
            return new UserManual(
                    "Investigation Manual",
                    "Investigation officers and investigation leadership",
                    "Use the internal dashboard to create, review, approve, and track investigation records according to your approval level.",
                    List.of(
                            "Start with the investigation overview to see pending, submitted, approved, and denied work.",
                            "Open the report history and evidence trail before taking action.",
                            "Approve, deny, or escalate only when your review level matches the current level shown.",
                            "Export the PDF record after final review or when supporting a formal briefing."
                    ),
                    List.of(
                            "Start investigations for eligible incidents.",
                            "Review status history and evidence attachments.",
                            "Approve or deny reports with comments at the correct level.",
                            "Download bilingual PDFs for official documentation."
                    ),
                    List.of(
                            "Do not act on reports outside your assigned approval level.",
                            "Use comments when returning work so the next officer sees exactly what must change.",
                            "Treat downloaded PDFs and evidence as controlled records."
                    ),
                    "Check the current level on every report before approving or denying it."
            );
        }
        return new UserManual(
                "Internal User Manual",
                "General internal users",
                "Use the internal dashboard as a role-filtered entry point for the incidents, reports, and modules assigned to you.",
                List.of(
                        "Review the overview card and notifications at sign-in.",
                        "Open only the modules your role exposes in the sidebar.",
                        "Update records promptly after every operational action.",
                        "Log out when you finish using a shared or supervised workstation."
                ),
                List.of(
                        "View assigned incidents and role-scoped records.",
                        "Use notifications and reports available to your role.",
                        "Maintain current operational updates.",
                        "Escalate issues through the command chain when needed."
                ),
                List.of(
                        "The system shows only the records your role is allowed to see.",
                        "Do not attempt to work around missing access; escalate instead.",
                        "Keep updates factual because the dashboard is part of the audit trail."
                ),
                "If a needed module is missing, request the correct role or station assignment."
        );
    }

    public UserManual operationsDashboardManual(String role) {
        String normalized = OperationRole.normalizeRole(role);
        if (List.of(
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS,
                OperationRole.HEAD_RESCUE_OPERATIONS
        ).contains(normalized)) {
            return new UserManual(
                    "Operations Command Manual",
                    "National command and directorate operations leadership",
                    "Use this dashboard to monitor the live operational picture, compare response pressure, and review recommendation output before giving direction.",
                    List.of(
                            "Review incident, response-time, and maintenance KPIs first.",
                            "Check the incident feed for active or severe events that need attention.",
                            "Read AI recommendations as decision support, then confirm with live context and reports.",
                            "Move to the control-room workspace if call handling or live communication evidence is required."
                    ),
                    List.of(
                            "Monitor national or command-scope incidents.",
                            "Track maintenance pressure and field readiness indicators.",
                            "Review recommendation output and role-scoped reports.",
                            "Use notifications to keep command actions current."
                    ),
                    List.of(
                            "Recommendations support command judgment; they do not replace formal verification.",
                            "Review severity and status before escalating an incident for intervention.",
                            "Export reports only after confirming the visible data matches your command scope."
                    ),
                    "Use the role dashboard when you need investigation, user, or role-specific internal modules."
            );
        }
        if (List.of(
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER
        ).contains(normalized)) {
            return new UserManual(
                    "Regional Operations Manual",
                    "Regional fire and operations leadership",
                    "Use this dashboard to supervise the regional operational picture, validate response load, and brief headquarters with current regional facts.",
                    List.of(
                            "Check the KPI cards at handover to understand active emergencies and response pace.",
                            "Review the incident feed and recommendation area for regional priorities.",
                            "Open reports when you need a formal regional summary.",
                            "Escalate to headquarters when the incident pattern exceeds regional capacity."
                    ),
                    List.of(
                            "Track the regional incident feed.",
                            "Review regional operational recommendations.",
                            "Monitor maintenance and readiness signals affecting the region.",
                            "Download region-scoped operational reports."
                    ),
                    List.of(
                            "Only incidents visible inside your region should drive action from this workspace.",
                            "Cross-region coordination must move through national command.",
                            "Keep field status updates current before briefing leadership."
                    ),
                    "Use the control-room workspace when call routing or recordings are part of the decision."
            );
        }
        return new UserManual(
                "District Operations Manual",
                "District and field operations users entering the operations workspace",
                "Use this dashboard for the clearest view of active incidents, response timing, and operational recommendations inside your permitted scope.",
                List.of(
                        "Start with the KPI strip to see urgent pressure points.",
                        "Read the incident feed before acting on recommendations or report output.",
                        "Use notifications and reports to keep district or station leadership aligned.",
                        "Return here after field updates to confirm the picture changed as expected."
                ),
                List.of(
                        "Track visible incidents and response status.",
                        "Review recommendations and report output for your scope.",
                        "Watch readiness pressure from maintenance alerts.",
                        "Support briefings with current operational data."
                ),
                List.of(
                        "Only act within the district or station authority attached to your role.",
                        "Use command escalation when the incident exceeds your boundary.",
                        "Treat stale incident status as an operational risk and correct it quickly."
                ),
                "Use the role dashboard for investigation workflow or other specialized role modules."
        );
    }

    public UserManual controlRoomDashboardManual(String role) {
        String normalized = OperationRole.normalizeRole(role);
        if (List.of(OperationRole.CGF, OperationRole.COMMISSIONER_OPERATIONS).contains(normalized)) {
            return new UserManual(
                    "Control Room Oversight Manual",
                    "Senior command users reviewing control-room activity",
                    "Use this dashboard to observe public intake, routed-call history, and live field-stream evidence without leaving the command workspace.",
                    List.of(
                            "Review incoming and active call counts before opening detailed records.",
                            "Use call history to verify routing performance and communication evidence.",
                            "Observe live video when command needs direct scene confirmation.",
                            "Switch back to operations after the control-room question is resolved."
                    ),
                    List.of(
                            "Observe routed-call flow.",
                            "Review call recordings and chat history.",
                            "Monitor live field streams and session evidence.",
                            "Download role-scoped reports connected to control-room activity."
                    ),
                    List.of(
                            "This workspace is primarily observational for senior command.",
                            "Use formal control-room staff for operational call-handling actions.",
                            "Keep sensitive caller information within authorized command channels."
                    ),
                    "Use this workspace when you need communications evidence, not just incident summaries."
            );
        }
        if (OperationRole.STATION_OPERATION_OFFICER.equals(normalized)) {
            return new UserManual(
                    "Field Stream Manual",
                    "Station operation officers interacting with control-room streaming",
                    "Use this dashboard when you need to publish live scene video, verify call context, or coordinate directly with the control room.",
                    List.of(
                            "Confirm the correct incident or routed call before starting a stream.",
                            "Use Start Live Video only when the control room needs a scene view.",
                            "End the stream promptly and upload the recording if required.",
                            "Review any control-room feedback before returning to field operations."
                    ),
                    List.of(
                            "Publish live scene video.",
                            "Check routed call context and active communication status.",
                            "Coordinate recordings and follow-up evidence.",
                            "Support control-room situational awareness from the field."
                    ),
                    List.of(
                            "Start streams only for authorized operational scenes.",
                            "Avoid broadcasting unrelated people or locations when not required.",
                            "Return to the operations dashboard for broader incident management."
                    ),
                    "Treat live video as operational evidence and stop broadcasting when the control room confirms it is no longer needed."
            );
        }
        if (OperationRole.CONTROL_ROOM_ATTENDANT.equals(normalized)) {
            return new UserManual(
                    "Control Room Streaming Manual",
                    "Control-room attendants handling calls and publishing live video",
                    "Use this dashboard to receive public reports, coordinate routing, and publish a control-room live stream when command visibility is required.",
                    List.of(
                            "Review the incoming queue before opening the live video panel.",
                            "Start live video only for the active control-room situation you are handling.",
                            "Allow location capture so the stream metadata is attached correctly.",
                            "End the stream as soon as the command task is complete and upload the recording if needed."
                    ),
                    List.of(
                            "Publish control-room live video.",
                            "Track incoming, active, and recorded calls.",
                            "Coordinate chat, routing, and communication evidence.",
                            "Download reports available to your role."
                    ),
                    List.of(
                            "Caller information and recordings remain controlled operational records.",
                            "Broadcast only official control-room activity related to the incident workflow.",
                            "Keep report numbers, routing decisions, and stream evidence synchronized."
                    ),
                    "Treat control-room video as auditable evidence and stop the stream immediately after the required briefing or verification is complete."
            );
        }
        return new UserManual(
                "Control Room User Manual",
                "Control-room attendants and call-handling personnel",
                "Use this dashboard to receive public reports, review routing history, monitor live streams, and keep communications traceable.",
                List.of(
                        "Start with the incoming and active call counters so you know the current queue.",
                        "Open call history to review the latest routed or pending reports.",
                        "Use the live video panel when a field officer or public reporter shares scene footage.",
                        "Review notifications and reports before ending the shift or handing over."
                ),
                List.of(
                        "Track incoming, active, and recorded calls.",
                        "Review or upload evidence linked to live sessions.",
                        "Support call chat and control-room communication continuity.",
                        "Download reports available to your role."
                ),
                List.of(
                        "Caller information and recordings are controlled operational records.",
                        "Route or discuss calls only through the authorized control-room process.",
                        "Use the dashboard timeline and call history to preserve accountability during handover."
                ),
                "Keep report numbers, caller updates, and routing actions synchronized so field teams receive the latest information."
        );
    }

    public record UserManual(
            String title,
            String audience,
            String summary,
            List<String> quickStart,
            List<String> keyTasks,
            List<String> accessBoundaries,
            String helpNote
    ) {
    }

    public record FaqItem(String question, String answer) {
    }
}
