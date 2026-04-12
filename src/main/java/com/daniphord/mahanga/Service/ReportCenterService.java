package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.FireInvestigation;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.EquipmentRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportCenterService {

    private final OperationsService operationsService;
    private final RoleAccessService roleAccessService;
    private final InvestigationWorkflowService investigationWorkflowService;
    private final DashboardDefinitionService dashboardDefinitionService;
    private final PdfBrandingService pdfBrandingService;
    private final StationRepository stationRepository;
    private final UserManualService userManualService;
    private final EquipmentRepository equipmentRepository;

    public ReportCenterService(
            OperationsService operationsService,
            RoleAccessService roleAccessService,
            InvestigationWorkflowService investigationWorkflowService,
            DashboardDefinitionService dashboardDefinitionService,
            PdfBrandingService pdfBrandingService,
            StationRepository stationRepository,
            UserManualService userManualService,
            EquipmentRepository equipmentRepository
    ) {
        this.operationsService = operationsService;
        this.roleAccessService = roleAccessService;
        this.investigationWorkflowService = investigationWorkflowService;
        this.dashboardDefinitionService = dashboardDefinitionService;
        this.pdfBrandingService = pdfBrandingService;
        this.stationRepository = stationRepository;
        this.userManualService = userManualService;
        this.equipmentRepository = equipmentRepository;
    }

    public List<Map<String, Object>> availableReports(User user) {
        if (user == null) {
            return List.of();
        }
        String role = normalizeRole(user.getRole());
        List<Map<String, Object>> reports = new ArrayList<>();
        if (canSeeFireReports(role)) {
            reports.add(report("fire-fighting", "Fire Fighting Report", "Operational fire incident summary for the user scope.", true));
        }
        if (canSeeRescueReports(role)) {
            reports.add(report("rescue", "Rescue Report", "Operational rescue incident summary for the user scope.", true));
        }
        if (canSeeEquipmentReports(role)) {
            reports.add(report("equipment-summary", "Equipment Report", "Registered equipment and readiness summary for the user scope.", true));
        }
        if (investigationWorkflowService.hasInvestigationAccess(user)) {
            reports.add(report("investigation-summary", "Investigation Report", "Workflow status and fire investigation summary for the user scope.", true));
        }
        reports.add(report("user-manual", "User Manual", "Role-specific user manual for the current dashboard user.", false));
        return reports;
    }

    public byte[] generateReport(String key, User user, String lang, String fromDate, String toDate) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }
        DateRange range = parseDateRange(fromDate, toDate);
        return switch (key) {
            case "fire-fighting" -> generateIncidentTypeReport(user, "FIRE", lang, range);
            case "rescue" -> generateIncidentTypeReport(user, "RESCUE", lang, range);
            case "equipment-summary" -> generateEquipmentSummary(user, lang, range);
            case "investigation-summary" -> generateInvestigationSummary(user, lang, range);
            case "user-manual" -> generateUserManual(user, lang);
            default -> throw new IllegalArgumentException("Unknown report key");
        };
    }

    private byte[] generateIncidentTypeReport(User user, String incidentType, String lang, DateRange range) {
        String role = normalizeRole(user.getRole());
        if (("FIRE".equalsIgnoreCase(incidentType) && !canSeeFireReports(role))
                || ("RESCUE".equalsIgnoreCase(incidentType) && !canSeeRescueReports(role))) {
            throw new IllegalArgumentException("You cannot access this report");
        }
        boolean sw = "sw".equalsIgnoreCase(lang);
        List<Incident> incidents = roleAccessService.visibleIncidents(user, operationsService.getAllIncidents()).stream()
                .filter(incident -> incidentType.equalsIgnoreCase(incident.getIncidentType()))
                .filter(incident -> range.includes(effectiveIncidentDate(incident)))
                .toList();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        StringBuilder rows = new StringBuilder();
        for (Incident incident : incidents) {
            rows.append("<tr>")
                    .append("<td>").append(escape(incident.getIncidentNumber())).append("</td>")
                    .append("<td>").append(escape(incident.getSeverity())).append("</td>")
                    .append("<td>").append(escape(incident.getStatus())).append("</td>")
                    .append("<td>").append(escape(locationLabel(incident))).append("</td>")
                    .append("<td>").append(incident.getReportedAt() == null ? "" : formatter.format(incident.getReportedAt())).append("</td>")
                    .append("</tr>");
        }
        String body = """
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                <table class="meta-table">
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                    <tr><td><strong>%s</strong></td><td colspan="3">%s</td></tr>
                </table>
                <div class="section-card">
                    <h2>%s</h2>
                    <table class="data-table">
                        <thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                </div>
                """.formatted(
                sw ? "Muhtasari wa Ripoti" : "Report Summary",
                sw ? ("Ripoti hii inaonyesha matukio ya aina ya " + ("FIRE".equalsIgnoreCase(incidentType) ? "moto" : "uokoaji") + " yanayoonekana kwa ngazi ya mtumiaji huyu.")
                        : ("This report presents " + incidentType.toLowerCase(Locale.ENGLISH) + " incidents visible within the current user's scope."),
                sw ? "Wajibu" : "Role",
                escape(role),
                sw ? "Aina ya Ripoti" : "Report Type",
                escape(incidentType),
                sw ? "Jumla ya Matukio" : "Total Incidents",
                incidents.size(),
                sw ? "Vituo Vinavyoonekana" : "Visible Stations",
                roleAccessService.visibleStations(user, stationRepository.findAll()).size(),
                sw ? "Kipindi cha Tarehe" : "Date Range",
                escape(range.label()),
                sw ? "Matukio ya " + ("FIRE".equalsIgnoreCase(incidentType) ? "Moto" : "Uokoaji") : ("FIRE".equalsIgnoreCase(incidentType) ? "Fire Incidents" : "Rescue Incidents"),
                sw ? "Namba ya Tukio" : "Incident Number",
                sw ? "Uzito" : "Severity",
                sw ? "Hali" : "Status",
                sw ? "Mahali" : "Location",
                sw ? "Muda wa Kuripoti" : "Reported At",
                rows.length() == 0 ? "<tr><td colspan=\"5\">" + (sw ? "Hakuna rekodi zinazopatikana." : "No records available.") + "</td></tr>" : rows
        );
        String title = sw
                ? ("FIRE".equalsIgnoreCase(incidentType) ? "RIPOTI YA OPERESHENI ZA MOTO" : "RIPOTI YA OPERESHENI ZA UOKOAJI")
                : ("FIRE".equalsIgnoreCase(incidentType) ? "FIRE FIGHTING REPORT" : "RESCUE REPORT");
        return pdfBrandingService.generatePdf(title, body, lang);
    }

    private byte[] generateEquipmentSummary(User user, String lang, DateRange range) {
        boolean sw = "sw".equalsIgnoreCase(lang);
        List<Equipment> equipment = roleAccessService.visibleEquipment(user, equipmentRepository.findAll()).stream()
                .filter(item -> range.includes(effectiveEquipmentDate(item)))
                .toList();
        StringBuilder rows = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        for (Equipment item : equipment) {
            rows.append("<tr>")
                    .append("<td>").append(escape(item.getName())).append("</td>")
                    .append("<td>").append(escape(equipmentTypeLabel(item))).append("</td>")
                    .append("<td>").append(escape(item.getOperationalStatus())).append("</td>")
                    .append("<td>").append(escape(item.getStation() != null ? item.getStation().getName() : "")).append("</td>")
                    .append("<td>").append(item.getCreatedAt() == null ? "" : formatter.format(item.getCreatedAt())).append("</td>")
                    .append("</tr>");
        }
        String body = """
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                <table class="meta-table">
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                </table>
                <div class="section-card">
                    <h2>%s</h2>
                    <table class="data-table">
                        <thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                </div>
                """.formatted(
                sw ? "Muhtasari wa Vifaa" : "Equipment Summary",
                sw ? "Ripoti hii inaonyesha vifaa vilivyosajiliwa katika kipindi kilichochaguliwa ndani ya eneo la mtumiaji."
                        : "This report presents equipment registered within the selected date range and the current user's scope.",
                sw ? "Wajibu" : "Role",
                escape(normalizeRole(user.getRole())),
                sw ? "Vituo Vinavyoonekana" : "Visible Stations",
                roleAccessService.visibleStations(user, stationRepository.findAll()).size(),
                sw ? "Jumla ya Rekodi" : "Total Records",
                equipment.size(),
                sw ? "Kipindi cha Tarehe" : "Date Range",
                escape(range.label()),
                sw ? "Vifaa Vilivyosajiliwa" : "Registered Equipment",
                sw ? "Jina" : "Name",
                sw ? "Aina" : "Type",
                sw ? "Hali ya Uendeshaji" : "Operational Status",
                sw ? "Kituo" : "Station",
                sw ? "Tarehe ya Usajili" : "Registered At",
                rows.length() == 0 ? "<tr><td colspan=\"5\">" + (sw ? "Hakuna rekodi zinazopatikana." : "No records available.") + "</td></tr>" : rows
        );
        return pdfBrandingService.generatePdf(sw ? "RIPOTI YA VIFAA" : "EQUIPMENT REPORT", body, lang);
    }

    private byte[] generateInvestigationSummary(User user, String lang, DateRange range) {
        if (!investigationWorkflowService.hasInvestigationAccess(user)) {
            throw new IllegalArgumentException("You cannot access this report");
        }
        boolean sw = "sw".equalsIgnoreCase(lang);
        List<FireInvestigation> reports = investigationWorkflowService.visibleInvestigations(user).stream()
                .filter(report -> range.includes(effectiveInvestigationDate(report)))
                .toList();
        StringBuilder rows = new StringBuilder();
        for (FireInvestigation report : reports) {
            rows.append("<tr>")
                    .append("<td>").append(escape(report.getInvestigationNumber())).append("</td>")
                    .append("<td>").append(escape(report.getIncident() == null ? "" : report.getIncident().getIncidentNumber())).append("</td>")
                    .append("<td>").append(escape(report.getStatus())).append("</td>")
                    .append("<td>").append(escape(report.getCurrentLevel())).append("</td>")
                    .append("</tr>");
        }
        String body = """
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                <table class="meta-table">
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                    <tr><td><strong>%s</strong></td><td colspan="3">%s</td></tr>
                </table>
                <div class="section-card">
                    <h2>%s</h2>
                    <table class="data-table">
                        <thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                </div>
                """.formatted(
                sw ? "Muhtasari wa Ufuatiliaji wa Uchunguzi" : "Investigation Monitoring Summary",
                sw ? "Ripoti hii inaonyesha uchunguzi unaoonekana kwa ngazi ya mtumiaji huyu pamoja na hali yake ya workflow."
                        : "This report presents the investigations visible to the current user's level together with their workflow status.",
                sw ? "Wajibu" : "Role",
                escape(normalizeRole(user.getRole())),
                sw ? "Jumla ya Uchunguzi" : "Total Investigations",
                reports.size(),
                sw ? "Kipindi cha Tarehe" : "Date Range",
                escape(range.label()),
                sw ? "Uchunguzi Unaonekana" : "Visible Investigations",
                sw ? "Namba ya Uchunguzi" : "Investigation Number",
                sw ? "Namba ya Tukio" : "Incident Number",
                sw ? "Hali" : "Status",
                sw ? "Ngazi ya Sasa" : "Current Level",
                rows.length() == 0 ? "<tr><td colspan=\"4\">" + (sw ? "Hakuna rekodi zinazopatikana." : "No records available.") + "</td></tr>" : rows
        );
        return pdfBrandingService.generatePdf(sw ? "RIPOTI YA UCHUNGUZI" : "INVESTIGATION REPORT", body, lang);
    }

    private byte[] generateUserManual(User user, String lang) {
        boolean sw = "sw".equalsIgnoreCase(lang);
        DashboardDefinitionService.DashboardDefinition definition = dashboardDefinitionService.definitionFor(user.getRole());
        UserManualService.UserManual manual = userManualService.internalDashboardManual(user.getRole());
        String quickStart = manual.quickStart().stream()
                .map(item -> "<li>" + escape(item) + "</li>")
                .reduce("", String::concat);
        String tasks = manual.keyTasks().stream()
                .map(item -> "<li>" + escape(item) + "</li>")
                .reduce("", String::concat);
        String boundaries = manual.accessBoundaries().stream()
                .map(item -> "<li>" + escape(item) + "</li>")
                .reduce("", String::concat);
        String body = """
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                <table class="meta-table">
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                </table>
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                <div class="section-card">
                    <h2>%s</h2>
                    <ol>%s</ol>
                </div>
                <div class="section-card">
                    <h2>%s</h2>
                    <ul>%s</ul>
                </div>
                <div class="section-card">
                    <h2>%s</h2>
                    <ul>%s</ul>
                </div>
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                """.formatted(
                sw ? "Mwongozo wa Mtumiaji kwa Wajibu" : "Role User Manual",
                sw ? "Hati hii inaeleza matumizi ya dashibodi na wajibu wa mtumiaji kulingana na ngazi yake ya kazi ndani ya FROMS."
                        : "This document explains dashboard use and operational responsibilities for the current role within FROMS.",
                sw ? "Wajibu" : "Role",
                escape(normalizeRole(user.getRole())),
                sw ? "Dashibodi" : "Dashboard",
                escape(definition.dashboardName()),
                sw ? "Muhtasari wa Wajibu" : "Role Overview",
                escape(manual.summary()),
                sw ? "Hatua za Kuanza" : "Quick Start",
                quickStart,
                sw ? "Majukumu Makuu" : "Core Tasks",
                tasks,
                sw ? "Mipaka ya Upatikanaji" : "Access Boundaries",
                boundaries,
                sw ? "Ujumbe wa Mwisho" : "Operator Note",
                escape(manual.helpNote())
        );
        return pdfBrandingService.generatePdf(sw ? "MWONGOZO WA MTUMIAJI WA FROMS" : "FROMS USER MANUAL", body, lang);
    }

    private DateRange parseDateRange(String fromDate, String toDate) {
        LocalDate from = parseDate(fromDate);
        LocalDate to = parseDate(toDate);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The start date must be before or equal to the end date");
        }
        return new DateRange(from, to);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid date range value");
        }
    }

    private LocalDateTime effectiveInvestigationDate(FireInvestigation report) {
        if (report.getSubmissionDate() != null) {
            return report.getSubmissionDate();
        }
        if (report.getUpdatedAt() != null) {
            return report.getUpdatedAt();
        }
        return report.getCreatedAt();
    }

    private LocalDateTime effectiveIncidentDate(Incident incident) {
        if (incident == null) {
            return null;
        }
        if (incident.getReportedAt() != null) {
            return incident.getReportedAt();
        }
        if (incident.getCallReceivedAt() != null) {
            return incident.getCallReceivedAt();
        }
        if (incident.getApprovalUpdatedAt() != null) {
            return incident.getApprovalUpdatedAt();
        }
        return incident.getApprovalSubmittedAt();
    }

    private LocalDateTime effectiveEquipmentDate(Equipment item) {
        if (item == null) {
            return null;
        }
        if (item.getCreatedAt() != null) {
            return item.getCreatedAt();
        }
        if (item.getUpdatedAt() != null) {
            return item.getUpdatedAt();
        }
        if (item.getLastServicedAt() != null) {
            return item.getLastServicedAt();
        }
        return item.getPurchaseDate() == null ? null : item.getPurchaseDate().atStartOfDay();
    }

    private boolean canSeeFireReports(String role) {
        return List.of(
                "SUPER_ADMIN",
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS,
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER,
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.DISTRICT_OPERATION_OFFICER,
                OperationRole.STATION_FIRE_OFFICER,
                OperationRole.STATION_OPERATION_OFFICER,
                OperationRole.OPERATION_OFFICER,
                OperationRole.CONTROL_ROOM_ATTENDANT
        ).contains(role);
    }

    private boolean canSeeRescueReports(String role) {
        return List.of(
                "SUPER_ADMIN",
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.HEAD_RESCUE_OPERATIONS,
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER,
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.DISTRICT_OPERATION_OFFICER,
                OperationRole.STATION_FIRE_OFFICER,
                OperationRole.STATION_OPERATION_OFFICER,
                OperationRole.OPERATION_OFFICER,
                OperationRole.CONTROL_ROOM_ATTENDANT
        ).contains(role);
    }

    private boolean canSeeEquipmentReports(String role) {
        return List.of(
                "SUPER_ADMIN",
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS,
                OperationRole.HEAD_RESCUE_OPERATIONS,
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER,
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.DISTRICT_OPERATION_OFFICER,
                OperationRole.STATION_FIRE_OFFICER,
                OperationRole.STATION_FIRE_OPERATION_OFFICER,
                OperationRole.STATION_OPERATION_OFFICER,
                OperationRole.OPERATION_OFFICER
        ).contains(role);
    }

    private Map<String, Object> report(String key, String title, String description, boolean supportsDateRange) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("title", title);
        item.put("description", description);
        item.put("supportsDateRange", supportsDateRange);
        return item;
    }

    private String locationLabel(Incident incident) {
        if (incident.getDistrict() != null && incident.getDistrict().getName() != null) {
            return incident.getDistrict().getName();
        }
        if (incident.getVillage() != null && !incident.getVillage().isBlank()) {
            return incident.getVillage();
        }
        return incident.getStation() != null ? incident.getStation().getName() : "";
    }

    private String equipmentTypeLabel(Equipment item) {
        if (item == null) {
            return "";
        }
        String rawType = item.getSubtype() != null && !item.getSubtype().isBlank() ? item.getSubtype() : item.getType();
        if (rawType == null || rawType.isBlank()) {
            return "";
        }
        String normalized = rawType.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "FIRETENDER", "TENDER", "FIRE_TENDER" -> "Fire Tender";
            case "COMMAND_CAR", "COMMAND_VEHICLE" -> "Command Car";
            case "MANAGEMENT_CAR", "MANAGEMENT_VEHICLE", "SUPERVISION_CAR" -> "Management Car";
            case "HAZMAT_CAR", "HAZMAT_TRUCK", "HAZMAT_TRACK", "HAZMAT_VEHICLE" -> "Hazmat Car";
            case "AMBULANCE" -> "Ambulance";
            case "RESCUE", "RESCUE_EQUIPMENT" -> "Rescue Equipment";
            case "FIRE_FIGHTING", "FIRE_EQUIPMENT", "FIRE_FIGHTING_EQUIPMENT" -> "Fire Fighting Equipment";
            case "BA", "BREATHING_APPARATUS", "BREATHING_APARATUS" -> "BA";
            case "CHEMICAL", "CHEMICALS", "FIRE_FIGHTING_CHEMICALS" -> "Fire Fighting Chemicals";
            default -> rawType.replace('_', ' ');
        };
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private record DateRange(LocalDate from, LocalDate to) {
        boolean includes(LocalDateTime value) {
            if (value == null) {
                return from == null && to == null;
            }
            if (from != null && value.isBefore(from.atStartOfDay())) {
                return false;
            }
            if (to != null && value.isAfter(LocalDateTime.of(to, LocalTime.MAX))) {
                return false;
            }
            return true;
        }

        String label() {
            if (from == null && to == null) {
                return "All dates";
            }
            if (from != null && to != null) {
                return from + " to " + to;
            }
            if (from != null) {
                return "From " + from;
            }
            return "Up to " + to;
        }
    }
}
