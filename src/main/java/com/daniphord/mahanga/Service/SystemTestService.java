package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.SystemTestReport;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.AuditLogRepository;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.EmergencyCallRepository;
import com.daniphord.mahanga.Repositories.FireInvestigationRepository;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.RegionRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.SystemTestReportRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.InputValidator;
import com.daniphord.mahanga.Util.OperationRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SystemTestService {

    private final SystemTestReportRepository systemTestReportRepository;
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final EmergencyCallRepository emergencyCallRepository;
    private final FireInvestigationRepository fireInvestigationRepository;
    private final IncidentRepository incidentRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final PdfBrandingService pdfBrandingService;
    private final PythonAiService pythonAiService;
    private final LoginSecurityService loginSecurityService;
    private final PasswordEncoder passwordEncoder;
    private final int serverPort;

    public SystemTestService(
            SystemTestReportRepository systemTestReportRepository,
            RegionRepository regionRepository,
            DistrictRepository districtRepository,
            StationRepository stationRepository,
            UserRepository userRepository,
            EmergencyCallRepository emergencyCallRepository,
            FireInvestigationRepository fireInvestigationRepository,
            IncidentRepository incidentRepository,
            AuditLogRepository auditLogRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            PdfBrandingService pdfBrandingService,
            PythonAiService pythonAiService,
            LoginSecurityService loginSecurityService,
            PasswordEncoder passwordEncoder,
            @Value("${server.port:1111}") int serverPort
    ) {
        this.systemTestReportRepository = systemTestReportRepository;
        this.regionRepository = regionRepository;
        this.districtRepository = districtRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.emergencyCallRepository = emergencyCallRepository;
        this.fireInvestigationRepository = fireInvestigationRepository;
        this.incidentRepository = incidentRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.pdfBrandingService = pdfBrandingService;
        this.pythonAiService = pythonAiService;
        this.loginSecurityService = loginSecurityService;
        this.passwordEncoder = passwordEncoder;
        this.serverPort = serverPort;
    }

    public boolean canRunTests(User user) {
        return user != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRole());
    }

    public List<SystemTestReport> latestReports() {
        return systemTestReportRepository.findTop20ByOrderByStartedAtDesc();
    }

    public SystemTestReport report(Long id) {
        return systemTestReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("System test report not found"));
    }

    public List<Map<String, Object>> reportDetails(Long id) {
        try {
            return objectMapper.readValue(report(id).getDetailsJson(), new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse system test details", exception);
        }
    }

    public SystemTestReport runManual(User triggeredBy, String ipAddress) {
        SystemTestReport report = run("MANUAL", triggeredBy);
        auditService.logAction(triggeredBy, "SYSTEM_TEST_RUN", "Administrator executed system verification", "SystemTestReport", report.getId(), ipAddress);
        return report;
    }

    @Scheduled(cron = "0 0 2 1 1,7 *")
    public void scheduledSixMonthVerification() {
        run("SCHEDULED", null);
    }

    public byte[] generatePdf(Long reportId, String lang) {
        SystemTestReport report = report(reportId);
        List<Map<String, Object>> details = reportDetails(reportId);
        Map<String, Object> aiAnalysis = details.stream()
                .filter(item -> "AI Executive Analysis".equals(item.get("name")))
                .findFirst()
                .orElse(Map.of());
        StringBuilder rows = new StringBuilder();
        for (Map<String, Object> item : details) {
            rows.append("<tr>")
                    .append("<td>").append(escape(localizeCheckName(stringValue(item.get("name")), lang))).append("</td>")
                    .append("<td>").append(escape(localizeStatus(stringValue(item.get("status")), lang))).append("</td>")
                    .append("<td>").append(escape(stringValue(item.get("message")))).append("</td>")
                    .append("</tr>");
        }
        boolean sw = "sw".equalsIgnoreCase(lang);
        String bodyHtml = """
                <table class="meta-table">
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                </table>
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                %s
                <div class="section-card">
                    <h2>%s</h2>
                    <table class="data-table">
                        <thead><tr><th>%s</th><th>%s</th><th>%s</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                </div>
                """.formatted(
                sw ? "Namba ya Ripoti" : "Report Number",
                escape(report.getReportNumber()),
                sw ? "Hali" : "Status",
                escape(report.getStatus()),
                sw ? "Aina ya Uendeshaji" : "Trigger Mode",
                escape(report.getTriggerMode()),
                sw ? "Imeanza" : "Started",
                report.getStartedAt() == null ? "" : report.getStartedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                sw ? "Imekamilika" : "Completed",
                report.getCompletedAt() == null ? "" : report.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                sw ? "Jumla ya Ukaguzi" : "Total Checks",
                report.getTotalChecks(),
                sw ? "Muhtasari wa Utendaji" : "Executive Summary",
                escape(localizedSummary(report, lang)),
                aiSection(aiAnalysis, lang),
                sw ? "Matokeo ya Ukaguzi" : "Check Results",
                sw ? "Ukaguzi" : "Check",
                sw ? "Hali" : "Status",
                sw ? "Maelezo" : "Message",
                rows
        );
        return pdfBrandingService.generatePdf(sw ? "RIPOTI YA UPIMAJI WA MFUMO WA FROMS" : "FROMS SYSTEM TEST REPORT", bodyHtml, lang);
    }

    private SystemTestReport run(String triggerMode, User triggeredBy) {
        SystemTestReport previousCompletedReport = systemTestReportRepository.findFirstByCompletedAtIsNotNullOrderByCompletedAtDesc();

        SystemTestReport report = new SystemTestReport();
        report.setTriggerMode(triggerMode);
        report.setTriggeredByUser(triggeredBy);
        report.setStatus("RUNNING");
        systemTestReportRepository.save(report);

        Map<String, Object> snapshot = buildSnapshot(previousCompletedReport);
        Map<String, Object> runtimeStatus = buildRuntimeStatus();
        List<Map<String, Object>> details = new ArrayList<>();

        evaluate(details, "System inventory", checkSystemInventory(snapshot));
        evaluate(details, "Architecture standard alignment", checkArchitectureStandardAlignment());
        evaluate(details, "Module coverage", checkModuleCoverage());
        evaluate(details, "Dashboard UX readiness", checkDashboardUxReadiness());
        evaluate(details, "Authentication policy", checkAuthenticationPolicy());
        evaluate(details, "API authorization model", checkApiAuthorizationModel());
        evaluate(details, "XSS and CSRF posture", checkXssAndCsrfPosture());
        evaluate(details, "Brute-force resilience", checkBruteForceResilience());
        evaluate(details, "SQL injection resilience", checkSqlInjectionResilience());
        evaluate(details, "Scalability readiness", checkScalabilityReadiness());
        evaluate(details, "User activity", checkUserActivity(snapshot));
        evaluate(details, "Password strength", checkPasswordStrength(snapshot));
        evaluate(details, "Failed login monitoring", checkFailedLoginMonitoring(snapshot));
        evaluate(details, "Geography registry", checkGeographyIntegrity(snapshot));
        evaluate(details, "Incident load", checkIncidentWorkload(snapshot));
        evaluate(details, "Incident command workflow", checkIncidentCommandWorkflow());
        evaluate(details, "Public routing readiness", checkPublicRouting());
        evaluate(details, "Investigation workflow", checkInvestigationRoles());
        evaluate(details, "Admin operations readiness", checkAdminOperationsReadiness());
        evaluate(details, "Audit logging", checkAuditLogging(snapshot));
        evaluate(details, "System performance", checkPerformanceAndResponseRate(snapshot));
        evaluate(details, "Reliability and fail-safe", checkReliabilityAndFailSafe());
        evaluate(details, "Monitoring and observability", checkMonitoringAndObservability());
        evaluate(details, "Data integrity and transactions", checkDataIntegrityAndTransactions());
        evaluate(details, "Deployment and backup readiness", checkDeploymentAndBackupReadiness());
        evaluate(details, "Test suite coverage", checkTestSuiteCoverage());
        evaluate(details, "Application runtime", checkApplicationRuntime(runtimeStatus));
        evaluate(details, "Public access runtime", checkPublicAccessRuntime(runtimeStatus));
        evaluate(details, "Documentation files", checkDocumentationPresence());
        evaluate(details, "Python AI sidecar", checkPythonSidecar(runtimeStatus));
        evaluate(details, "AI recommendation engine", checkAiRecommendationEngine());
        evaluate(details, "Vulnerability snapshot", checkVulnerabilitySnapshot(snapshot));
        evaluate(details, "Video subsystem", checkVideoStorage());
        evaluate(details, "AI Executive Analysis", checkAiExecutiveAnalysis(snapshot, details, runtimeStatus));

        long passed = details.stream().filter(item -> "PASS".equals(item.get("status"))).count();
        long warnings = details.stream().filter(item -> "WARN".equals(item.get("status"))).count();
        long failed = details.stream().filter(item -> "FAIL".equals(item.get("status"))).count();

        report.setPassedChecks((int) passed);
        report.setWarningChecks((int) warnings);
        report.setFailedChecks((int) failed);
        report.setTotalChecks(details.size());
        report.setStatus(failed > 0 ? "FAIL" : warnings > 0 ? "WARN" : "PASS");
        report.setSummary(buildSummary(snapshot, passed, warnings, failed, details));
        report.setCompletedAt(LocalDateTime.now());
        try {
            report.setDetailsJson(objectMapper.writeValueAsString(details));
        } catch (Exception exception) {
            report.setDetailsJson("[]");
        }
        return systemTestReportRepository.save(report);
    }

    private Map<String, Object> buildSnapshot(SystemTestReport previousCompletedReport) {
        List<User> users = userRepository.findAll();
        List<Region> regions = regionRepository.findAll();
        List<District> districts = districtRepository.findAll();
        List<Station> stations = stationRepository.findAll();
        List<Incident> incidents = incidentRepository.findAll();

        long activeUsers = users.stream().filter(user -> Boolean.TRUE.equals(user.getActive())).count();
        long inactiveUsers = users.size() - activeUsers;
        long lockedUsers = users.stream().filter(User::isLocked).count();
        long weakPasswords = users.stream().filter(user -> "WEAK".equalsIgnoreCase(user.getPasswordStrength())).count();
        long mediumPasswords = users.stream().filter(user -> "MEDIUM".equalsIgnoreCase(user.getPasswordStrength())).count();
        long strongPasswords = users.stream().filter(user -> "STRONG".equalsIgnoreCase(user.getPasswordStrength())).count();
        long activeIncidents = incidents.stream().filter(incident -> "ACTIVE".equalsIgnoreCase(incident.getStatus())).count();
        long failedLoginsSinceLastTest = auditLogRepository.findByStatus("FAILURE").stream()
                .filter(item -> item.getTimestamp() != null)
                .filter(item -> previousCompletedReport == null || previousCompletedReport.getCompletedAt() == null || item.getTimestamp().isAfter(previousCompletedReport.getCompletedAt()))
                .filter(item -> containsSecurityKeyword(item.getAction()) || containsSecurityKeyword(item.getDescription()))
                .count();

        List<String> weakUsers = users.stream()
                .filter(user -> "WEAK".equalsIgnoreCase(user.getPasswordStrength()))
                .map(user -> user.getUsername() + " (" + user.getRole() + ")")
                .sorted()
                .limit(8)
                .toList();

        double averageResponseTimeMinutes = incidents.stream()
                .map(Incident::getResponseTimeMinutes)
                .filter(value -> value != null && value > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        Map<String, Double> queryMetrics = new LinkedHashMap<>();
        queryMetrics.put("userCountMs", measureMillis(() -> userRepository.count()));
        queryMetrics.put("incidentCountMs", measureMillis(() -> incidentRepository.count()));
        queryMetrics.put("regionLoadMs", measureMillis(() -> regionRepository.findAll().size()));
        queryMetrics.put("stationLoadMs", measureMillis(() -> stationRepository.findAll().size()));
        queryMetrics.put("callLoadMs", measureMillis(() -> emergencyCallRepository.findTop20ByOrderByCallTimeDesc().size()));

        double averageQueryLatencyMs = queryMetrics.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double slowestQueryMs = queryMetrics.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        List<String> vulnerabilities = new ArrayList<>();
        if (weakPasswords > 0) {
            vulnerabilities.add("Weak passwords detected for " + weakPasswords + " user account(s)");
        }
        if (lockedUsers > 0) {
            vulnerabilities.add("Locked accounts pending administrator review: " + lockedUsers);
        }
        if (failedLoginsSinceLastTest > 0) {
            vulnerabilities.add("Recent failed logins since last test: " + failedLoginsSinceLastTest);
        }
        if (averageQueryLatencyMs > 350.0) {
            vulnerabilities.add("Average repository latency is elevated at " + formatDouble(averageQueryLatencyMs) + " ms");
        }
        if (!Files.exists(Path.of("videos"))) {
            vulnerabilities.add("Video evidence storage directory is missing");
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("previousCompletedAt", previousCompletedReport == null || previousCompletedReport.getCompletedAt() == null ? "" : previousCompletedReport.getCompletedAt().toString());
        snapshot.put("totalUsers", users.size());
        snapshot.put("activeUsers", activeUsers);
        snapshot.put("inactiveUsers", inactiveUsers);
        snapshot.put("lockedUsers", lockedUsers);
        snapshot.put("weakPasswords", weakPasswords);
        snapshot.put("mediumPasswords", mediumPasswords);
        snapshot.put("strongPasswords", strongPasswords);
        snapshot.put("weakUsers", weakUsers);
        snapshot.put("regions", regions.size());
        snapshot.put("districts", districts.size());
        snapshot.put("stations", stations.size());
        snapshot.put("incidents", incidents.size());
        snapshot.put("activeIncidents", activeIncidents);
        snapshot.put("publicReports", emergencyCallRepository.count());
        snapshot.put("investigations", fireInvestigationRepository.count());
        snapshot.put("auditEntries", auditLogRepository.count());
        snapshot.put("failedLoginsSinceLastTest", failedLoginsSinceLastTest);
        snapshot.put("averageResponseTimeMinutes", averageResponseTimeMinutes);
        snapshot.put("queryMetrics", queryMetrics);
        snapshot.put("averageQueryLatencyMs", averageQueryLatencyMs);
        snapshot.put("slowestQueryMs", slowestQueryMs);
        snapshot.put("vulnerabilities", vulnerabilities);
        return snapshot;
    }

    private void evaluate(List<Map<String, Object>> details, String name, Map<String, Object> result) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.putAll(result);
        details.add(item);
    }

    private Map<String, Object> pass(String message) {
        return result("PASS", message);
    }

    private Map<String, Object> warn(String message) {
        return result("WARN", message);
    }

    private Map<String, Object> fail(String message) {
        return result("FAIL", message);
    }

    private Map<String, Object> result(String status, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("status", status);
        item.put("message", message);
        return item;
    }

    private Map<String, Object> checkSystemInventory(Map<String, Object> snapshot) {
        return pass("Users: " + snapshot.get("totalUsers")
                + " total, " + snapshot.get("activeUsers") + " active, " + snapshot.get("inactiveUsers") + " inactive. "
                + "Regions: " + snapshot.get("regions") + ", districts: " + snapshot.get("districts")
                + ", stations: " + snapshot.get("stations") + ", incidents: " + snapshot.get("incidents")
                + ", public reports: " + snapshot.get("publicReports") + ", investigations: " + snapshot.get("investigations") + ".");
    }

    private Map<String, Object> checkArchitectureStandardAlignment() {
        String pom = readWorkspaceText("pom.xml");
        String properties = readWorkspaceText("src/main/resources/application.properties");
        String uiFragment = readWorkspaceText("src/main/resources/templates/fragments/ui.html");
        boolean springBootWeb = pom.contains("spring-boot-starter-web");
        boolean restEndpoints = !scanWorkspaceForMarkers(Path.of("src/main/java/com/daniphord/mahanga/Controller"), ".java", List.of("@RestController", "@ResponseBody"), 3).isEmpty();
        boolean postgresDriver = pom.contains("<artifactId>postgresql</artifactId>");
        boolean postgresProfileDetected = workspaceFileExists("src/main/resources/application-postgres.properties");
        boolean reactDetected = workspaceFileExists("package.json")
                && readWorkspaceText("package.json").toLowerCase(java.util.Locale.ROOT).contains("\"react\"");
        boolean tailwindDetected = workspaceFileExists("tailwind.config.js")
                || workspaceFileExists("tailwind.config.cjs")
                || (workspaceFileExists("package.json")
                && readWorkspaceText("package.json").toLowerCase(java.util.Locale.ROOT).contains("tailwindcss"));
        boolean thymeleafDetected = pom.contains("spring-boot-starter-thymeleaf");
        boolean bootstrapDetected = uiFragment.contains("bootstrap@");
        boolean h2Default = properties.contains("jdbc:h2:file:");
        boolean relationalRuntimeReady = postgresDriver && (postgresProfileDetected || !h2Default);

        if (springBootWeb && restEndpoints && relationalRuntimeReady && reactDetected && tailwindDetected && !thymeleafDetected && !bootstrapDetected && !h2Default) {
            return pass("Spring Boot REST backend, React/Tailwind frontend, and PostgreSQL-aligned runtime settings were detected.");
        }

        if (!springBootWeb || !restEndpoints || !postgresDriver) {
            List<String> coreGaps = new ArrayList<>();
            if (!springBootWeb || !restEndpoints) {
                coreGaps.add("Spring Boot REST API evidence is incomplete");
            }
            if (!postgresDriver) {
                coreGaps.add("PostgreSQL driver support was not detected");
            }
            return fail(String.join("; ", coreGaps) + ".");
        }

        List<String> gaps = new ArrayList<>();
        if (!reactDetected) {
            gaps.add("React frontend was not detected");
        }
        if (!tailwindDetected) {
            gaps.add("Tailwind CSS was not detected");
        }
        if (thymeleafDetected || bootstrapDetected) {
            gaps.add("current presentation layer is Thymeleaf/Bootstrap/static JavaScript");
        }
        if (!relationalRuntimeReady) {
            gaps.add("PostgreSQL production profile was not detected");
        } else if (h2Default) {
            gaps.add("developer H2 fallback remains enabled alongside the PostgreSQL deployment profile");
        }

        if (gaps.isEmpty()) {
            return pass("Spring Boot REST backend and PostgreSQL-ready runtime settings were detected.");
        }
        return warn("Spring Boot backend and relational database support are present, but " + String.join("; ", gaps) + ".");
    }

    private Map<String, Object> checkModuleCoverage() {
        Map<String, List<String>> modules = new LinkedHashMap<>();
        modules.put("Authentication", List.of(
                "src/main/java/com/daniphord/mahanga/Config/SecurityConfig.java",
                "src/main/java/com/daniphord/mahanga/Controller/WebController.java",
                "src/main/java/com/daniphord/mahanga/Service/LoginSecurityService.java"
        ));
        modules.put("User Management", List.of(
                "src/main/java/com/daniphord/mahanga/Controller/UserController.java",
                "src/main/java/com/daniphord/mahanga/Service/UserService.java"
        ));
        modules.put("Incident Management", List.of(
                "src/main/java/com/daniphord/mahanga/Controller/OperationsController.java",
                "src/main/java/com/daniphord/mahanga/Service/OperationsService.java",
                "src/main/java/com/daniphord/mahanga/Model/Incident.java"
        ));
        modules.put("Fire Investigation", List.of(
                "src/main/java/com/daniphord/mahanga/Controller/InvestigationController.java",
                "src/main/java/com/daniphord/mahanga/Service/InvestigationWorkflowService.java"
        ));
        modules.put("Reporting", List.of(
                "src/main/java/com/daniphord/mahanga/Service/ReportCenterService.java",
                "src/main/java/com/daniphord/mahanga/Service/PdfBrandingService.java"
        ));
        modules.put("Notification", List.of(
                "src/main/java/com/daniphord/mahanga/Service/NotificationService.java",
                "src/main/java/com/daniphord/mahanga/Model/UserNotification.java"
        ));
        modules.put("Analytics", List.of(
                "src/main/java/com/daniphord/mahanga/Service/DashboardDefinitionService.java",
                "src/main/java/com/daniphord/mahanga/Service/PythonAiService.java",
                "src/main/java/com/daniphord/mahanga/Model/Recommendation.java"
        ));
        modules.put("System Admin", List.of(
                "src/main/java/com/daniphord/mahanga/Controller/SystemTestController.java",
                "src/main/java/com/daniphord/mahanga/Service/SystemDocumentationService.java",
                "src/main/java/com/daniphord/mahanga/Controller/BrandingController.java"
        ));

        List<String> missing = modules.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(path -> !workspaceFileExists(path)))
                .map(Map.Entry::getKey)
                .toList();

        if (missing.isEmpty()) {
            return pass("Operational source modules were detected for authentication, users, incidents, investigation, reporting, notifications, analytics, and system administration.");
        }
        return fail("Required modules missing from the codebase: " + String.join(", ", missing) + ".");
    }

    private Map<String, Object> checkDashboardUxReadiness() {
        String templates = readWorkspaceText("src/main/resources/templates/role-dashboard.html")
                + readWorkspaceText("src/main/resources/templates/operations-dashboard.html")
                + readWorkspaceText("src/main/resources/templates/control-room-dashboard.html")
                + readWorkspaceText("src/main/resources/templates/public-emergency-report.html")
                + readWorkspaceText("src/main/resources/templates/landing.html");
        String css = readWorkspaceText("src/main/resources/static/ui/app-ui.css");
        String js = readWorkspaceText("src/main/resources/static/ui/app-ui.js");
        String uiFragment = readWorkspaceText("src/main/resources/templates/fragments/ui.html");

        boolean stickyTopbar = templates.contains("ui-site-topbar") && css.contains("backdrop-filter") && css.contains("position: sticky");
        boolean sidebarNavigation = templates.contains("dashboard-sidebar");
        boolean darkMode = js.contains("data-dark-mode-toggle") && css.contains("body.dark-mode");
        boolean notifications = templates.contains("notification-center");
        boolean languageSelector = templates.contains("ui-language-select");
        boolean avatarPanel = templates.contains("ui-profile-avatar");
        boolean responsiveViewport = uiFragment.contains("viewport") && css.contains("@media");
        boolean mapPanel = templates.contains("openstreetmap") || templates.contains("ui-map-frame");
        boolean quickActions = templates.contains("ui-command-card") || css.contains(".ui-command-card");
        boolean chartsRemoved = templates.contains("Charts removed per admin request") || js.contains("charts removed per admin request");
        boolean sidebarCollapseDetected = templates.contains("sidebar-toggle") || js.contains("sidebar-toggle") || js.contains("dashboard-sidebar-collapsed");
        boolean loadingSkeletonDetected = templates.toLowerCase(java.util.Locale.ROOT).contains("skeleton")
                || css.toLowerCase(java.util.Locale.ROOT).contains("skeleton")
                || js.toLowerCase(java.util.Locale.ROOT).contains("skeleton");

        boolean coreReady = stickyTopbar && sidebarNavigation && darkMode && notifications && languageSelector
                && avatarPanel && responsiveViewport && mapPanel && quickActions;
        if (coreReady && !chartsRemoved && sidebarCollapseDetected && loadingSkeletonDetected) {
            return pass("Dashboard UI includes sticky top navigation, sidebar navigation, dark mode, notifications, quick actions, charts, map support, and responsive behaviors.");
        }

        List<String> gaps = new ArrayList<>();
        if (!coreReady) {
            gaps.add("one or more topbar/sidebar/notification/responsive dashboard elements were not detected");
        }
        if (chartsRemoved) {
            gaps.add("analytics charts are partially disabled in current dashboard templates");
        }
        if (!sidebarCollapseDetected) {
            gaps.add("no collapsible sidebar control was detected");
        }
        if (!loadingSkeletonDetected) {
            gaps.add("loading skeleton components were not detected");
        }

        return warn("Government-style dashboard presentation is in place with sticky glass topbars, role sidebars, language selection, avatar/profile panels, dark mode, quick actions, and map support, but " + String.join("; ", gaps) + ".");
    }

    private Map<String, Object> checkAuthenticationPolicy() {
        String pom = readWorkspaceText("pom.xml");
        String properties = readWorkspaceText("src/main/resources/application.properties");
        String securityConfig = readWorkspaceText("src/main/java/com/daniphord/mahanga/Config/SecurityConfig.java");
        String loginSecurity = readWorkspaceText("src/main/java/com/daniphord/mahanga/Service/LoginSecurityService.java");
        List<String> jwtMarkers = scanWorkspaceForMarkers(Path.of("src/main/java"), ".java", List.of("Jwt", "JWT"), 4);
        List<String> otpMarkers = scanWorkspaceForMarkers(Path.of("src/main/java"), ".java", List.of("OTP", "otp", "one-time password", "multi-factor"), 4);

        boolean springSecurity = pom.contains("spring-boot-starter-security");
        boolean bcrypt = securityConfig.contains("BCryptPasswordEncoder");
        boolean sessionTimeout = properties.contains("server.servlet.session.timeout=");
        boolean accountLockout = loginSecurity.contains("recordFailedLogin") && loginSecurity.contains("lockedMessage");
        boolean sessionBasedAuth = securityConfig.contains("SessionCreationPolicy.IF_REQUIRED") || securityConfig.contains("request.getSession");

        if (springSecurity && bcrypt && sessionTimeout && accountLockout && !sessionBasedAuth && !jwtMarkers.isEmpty() && !otpMarkers.isEmpty()) {
            return pass("Spring Security, BCrypt, JWT authentication, session timeout, account lockout, and OTP controls were detected.");
        }

        List<String> findings = new ArrayList<>();
        if (!springSecurity) {
            findings.add("Spring Security starter was not detected");
        }
        if (!bcrypt) {
            findings.add("BCrypt password hashing configuration was not detected");
        }
        if (!sessionTimeout) {
            findings.add("session timeout is not configured");
        }
        if (!accountLockout) {
            findings.add("account lockout flow is not fully wired");
        }
        if (sessionBasedAuth && jwtMarkers.isEmpty()) {
            findings.add("authentication remains session-based and JWT tokens were not detected");
        }
        if (otpMarkers.isEmpty()) {
            findings.add("OTP or MFA controls for critical actions were not detected");
        }

        if (!springSecurity || !bcrypt || !sessionTimeout) {
            return fail(String.join("; ", findings) + ".");
        }
        if (findings.isEmpty()) {
            return pass("BCrypt hashing, session timeout, login throttling, and account lockout are configured.");
        }
        return warn("BCrypt hashing, session timeout, login throttling, and account lockout are configured, but " + String.join("; ", findings) + ".");
    }

    private Map<String, Object> checkApiAuthorizationModel() {
        String securityConfig = readWorkspaceText("src/main/java/com/daniphord/mahanga/Config/SecurityConfig.java");
        boolean requestMatchers = securityConfig.contains(".requestMatchers(") && securityConfig.contains(".hasAnyRole(");
        boolean apiAuthenticatedFallback = securityConfig.contains(".anyRequest().authenticated()");
        boolean roleAccessServicePresent = workspaceFileExists("src/main/java/com/daniphord/mahanga/Service/RoleAccessService.java");
        boolean methodLevelAuthorization = !scanWorkspaceForMarkers(Path.of("src/main/java"), ".java", List.of("@PreAuthorize"), 3).isEmpty();

        if (requestMatchers && apiAuthenticatedFallback && roleAccessServicePresent && methodLevelAuthorization) {
            return pass("Endpoint and method-level authorization controls were detected through SecurityConfig and @PreAuthorize guards.");
        }
        if (requestMatchers && apiAuthenticatedFallback && roleAccessServicePresent) {
            return warn("Endpoint access is enforced centrally through SecurityConfig and role-scoped services, but no @PreAuthorize method-level guards were detected.");
        }
        return fail("Sensitive endpoint authorization rules are incomplete or not consistently enforced in the current source tree.");
    }

    private Map<String, Object> checkXssAndCsrfPosture() {
        String securityConfig = readWorkspaceText("src/main/java/com/daniphord/mahanga/Config/SecurityConfig.java");
        String validator = readWorkspaceText("src/main/java/com/daniphord/mahanga/Util/InputValidator.java");
        boolean csrf = securityConfig.contains(".csrf(") && securityConfig.contains("CookieCsrfTokenRepository");
        boolean csp = securityConfig.contains(".contentSecurityPolicy(");
        boolean xssHeaders = securityConfig.contains(".xssProtection(") && securityConfig.contains(".frameOptions(");
        boolean sanitization = validator.contains("sanitizeInput");
        List<String> riskyUiBindings = scanWorkspaceForMarkers(Path.of("src/main/resources/static/ui"), ".js", List.of("innerHTML"), 4);

        if (csrf && csp && xssHeaders && sanitization && riskyUiBindings.isEmpty()) {
            return pass("CSRF tokens, CSP headers, XSS headers, and input sanitization were detected without risky direct DOM HTML rendering patterns.");
        }
        if (csrf && csp && xssHeaders) {
            return warn("CSRF tokens, CSP, and browser hardening headers are configured, but direct innerHTML DOM rendering remains in " + String.join(", ", riskyUiBindings) + ".");
        }

        List<String> gaps = new ArrayList<>();
        if (!csrf) {
            gaps.add("CSRF token enforcement was not detected");
        }
        if (!csp) {
            gaps.add("Content-Security-Policy header was not detected");
        }
        if (!xssHeaders) {
            gaps.add("XSS/frame protection headers were not fully detected");
        }
        if (!sanitization) {
            gaps.add("input sanitization helpers were not detected");
        }
        return fail(String.join("; ", gaps) + ".");
    }

    private Map<String, Object> checkBruteForceResilience() {
        String anonymousProbeIp = "198.51.100.10";
        String accountProbeIp = "198.51.100.11";
        User probeUser = null;

        try {
            loginSecurityService.resetRateLimit(anonymousProbeIp);
            loginSecurityService.resetRateLimit(accountProbeIp);

            for (int attempt = 0; attempt < loginSecurityService.maxLoginAttempts(); attempt++) {
                loginSecurityService.recordAnonymousFailure(anonymousProbeIp);
            }
            boolean ipRateLimited = loginSecurityService.isIpRateLimited(anonymousProbeIp);

            probeUser = createDisposableProbeUser();
            for (int attempt = 0; attempt < loginSecurityService.maxLoginAttempts(); attempt++) {
                loginSecurityService.recordFailedLogin(probeUser, accountProbeIp);
            }
            User refreshedProbeUser = userRepository.findById(probeUser.getId()).orElse(probeUser);
            boolean accountLocked = loginSecurityService.isAccountLocked(refreshedProbeUser);

            if (ipRateLimited && accountLocked) {
                return pass("Login security throttled a disposable probe IP after "
                        + loginSecurityService.maxLoginAttempts()
                        + " invalid attempts and locked a disposable probe account after "
                        + loginSecurityService.maxLoginAttempts()
                        + " incorrect password attempts.");
            }
            if (accountLocked) {
                return warn("Disposable probe account lockout activated after repeated incorrect passwords, but IP-based login throttling did not activate for anonymous probe attempts.");
            }
            if (ipRateLimited) {
                return warn("IP-based login throttling activated, but the disposable probe account was not locked after repeated incorrect passwords.");
            }
            return fail("Login security did not throttle the disposable probe IP or lock the disposable probe account after repeated incorrect attempts.");
        } catch (Exception exception) {
            return fail("Brute-force probe could not complete: " + summarize(exception.getMessage()));
        } finally {
            loginSecurityService.resetRateLimit(anonymousProbeIp);
            loginSecurityService.resetRateLimit(accountProbeIp);
            cleanupDisposableProbeUser(probeUser);
        }
    }

    private Map<String, Object> checkSqlInjectionResilience() {
        List<String> payloads = List.of(
                "' OR '1'='1",
                "admin' --",
                "\" OR \"1\"=\"1"
        );

        boolean usernameValidationRejected = payloads.stream().noneMatch(InputValidator::isValidUsername);
        boolean userLookupSafe = payloads.stream().allMatch(payload -> userRepository.findByUsername(payload).isEmpty());
        boolean publicLookupSafe = payloads.stream().allMatch(payload -> emergencyCallRepository.findByReportNumberAndPublicAccessToken(payload, payload).isEmpty());
        List<String> riskyMarkers = scanSqlRiskMarkers();

        if (usernameValidationRejected && userLookupSafe && publicLookupSafe && riskyMarkers.isEmpty()) {
            return pass("SQL-style payload probes were rejected by login username validation, returned no user or public-report matches, and source scanning found no risky dynamic query-construction markers.");
        }

        List<String> findings = new ArrayList<>();
        if (!usernameValidationRejected) {
            findings.add("Login username validation accepted at least one SQL-style payload");
        }
        if (!userLookupSafe) {
            findings.add("User lookup returned a record for a crafted SQL-style payload");
        }
        if (!publicLookupSafe) {
            findings.add("Public report lookup returned a record for a crafted SQL-style payload");
        }
        if (!riskyMarkers.isEmpty()) {
            findings.add("Source scan found risky SQL markers: " + String.join(", ", riskyMarkers));
        }
        return fail(String.join(". ", findings) + ".");
    }

    private Map<String, Object> checkScalabilityReadiness() {
        String pom = readWorkspaceText("pom.xml");
        String properties = readWorkspaceText("src/main/resources/application.properties");
        String securityConfig = readWorkspaceText("src/main/java/com/daniphord/mahanga/Config/SecurityConfig.java");
        boolean hikariPooling = properties.contains("spring.datasource.hikari.maximum-pool-size")
                && properties.contains("spring.datasource.hikari.minimum-idle");
        boolean postgresDriver = pom.contains("<artifactId>postgresql</artifactId>");
        boolean mysqlDriver = pom.contains("mysql-connector") || pom.contains("<artifactId>mysql</artifactId>");
        boolean postgresProfileDetected = workspaceFileExists("src/main/resources/application-postgres.properties");
        boolean cacheLayer = pom.contains("data-redis")
                || !scanWorkspaceForMarkers(Path.of("src/main/java"), ".java", List.of("@Cacheable", "@EnableCaching", "RedisTemplate"), 3).isEmpty();
        boolean defaultH2 = properties.contains("jdbc:h2:file:");
        boolean sessionAffinityRequired = securityConfig.contains("SessionCreationPolicy.IF_REQUIRED") || securityConfig.contains("request.getSession");
        boolean localArtifactStorage = properties.contains("froms.video.storage-path=videos")
                && properties.contains("froms.investigation.storage-path=investigations");
        boolean productionDatabaseReady = (postgresDriver || mysqlDriver) && (postgresProfileDetected || !defaultH2);

        if (hikariPooling && productionDatabaseReady && cacheLayer && !sessionAffinityRequired && !localArtifactStorage) {
            return pass("Database pooling, distributed caching, shared storage, and scale-ready authentication patterns were detected.");
        }

        List<String> gaps = new ArrayList<>();
        if (!hikariPooling) {
            gaps.add("connection pooling configuration is incomplete");
        }
        if (sessionAffinityRequired) {
            gaps.add("authentication state depends on local server sessions");
        }
        if (localArtifactStorage) {
            gaps.add("video and investigation artifacts are stored on the local filesystem");
        }
        if (!cacheLayer) {
            gaps.add("no Redis or Spring caching layer was detected");
        }
        if (!productionDatabaseReady) {
            gaps.add("no PostgreSQL/MySQL production runtime profile was detected");
        }

        if (!hikariPooling || (!postgresDriver && !mysqlDriver)) {
            return fail("Core scale prerequisites are missing; " + String.join("; ", gaps) + ".");
        }
        return warn("Connection pooling and production database support are present, but " + String.join("; ", gaps) + ".");
    }

    private Map<String, Object> checkUserActivity(Map<String, Object> snapshot) {
        long inactiveUsers = longValue(snapshot.get("inactiveUsers"));
        long lockedUsers = longValue(snapshot.get("lockedUsers"));
        if (inactiveUsers > 0 || lockedUsers > 0) {
            return warn("Inactive users: " + inactiveUsers + ". Locked users: " + lockedUsers + ". Review access hygiene before the next audit cycle.");
        }
        return pass("All registered accounts are active and currently unlocked.");
    }

    private Map<String, Object> checkPasswordStrength(Map<String, Object> snapshot) {
        long weakPasswords = longValue(snapshot.get("weakPasswords"));
        long mediumPasswords = longValue(snapshot.get("mediumPasswords"));
        long strongPasswords = longValue(snapshot.get("strongPasswords"));
        @SuppressWarnings("unchecked")
        List<String> weakUsers = (List<String>) snapshot.get("weakUsers");
        if (weakPasswords > 0) {
            String weakUserSummary = weakUsers.isEmpty() ? "No usernames available." : String.join(", ", weakUsers);
            return fail("Password strength review found " + weakPasswords + " weak, " + mediumPasswords + " medium, " + strongPasswords + " strong account(s). Weak users: " + weakUserSummary + ".");
        }
        if (mediumPasswords > 0) {
            return warn("No weak passwords found, but " + mediumPasswords + " account(s) are still medium strength. Strong accounts: " + strongPasswords + ".");
        }
        return pass("All stored password ratings are strong.");
    }

    private Map<String, Object> checkFailedLoginMonitoring(Map<String, Object> snapshot) {
        long failedLogins = longValue(snapshot.get("failedLoginsSinceLastTest"));
        String previousCompletedAt = stringValue(snapshot.get("previousCompletedAt"));
        if (failedLogins > 0) {
            return warn("Failed login and lockout related events since the last completed test"
                    + (previousCompletedAt.isBlank() ? "" : " at " + previousCompletedAt)
                    + ": " + failedLogins + ".");
        }
        return pass("No failed login events were recorded since the last completed system test.");
    }

    private Map<String, Object> checkGeographyIntegrity(Map<String, Object> snapshot) {
        List<Region> regions = regionRepository.findAll();
        List<District> districts = districtRepository.findAll();
        List<Station> stations = stationRepository.findAll();
        boolean ok = !regions.isEmpty()
                && districts.stream().allMatch(district -> district.getRegion() != null)
                && stations.stream().allMatch(station -> station.getDistrict() != null);
        if (!ok) {
            return fail("Region/district/station master data has missing relationships.");
        }
        return pass("Geography registry is linked correctly across " + snapshot.get("regions") + " regions, " + snapshot.get("districts") + " districts, and " + snapshot.get("stations") + " stations.");
    }

    private Map<String, Object> checkIncidentWorkload(Map<String, Object> snapshot) {
        long incidents = longValue(snapshot.get("incidents"));
        long activeIncidents = longValue(snapshot.get("activeIncidents"));
        double averageResponseTime = doubleValue(snapshot.get("averageResponseTimeMinutes"));
        return pass("Incident records: " + incidents + " total, " + activeIncidents + " active. Average operational response time: " + formatDouble(averageResponseTime) + " minutes.");
    }

    private Map<String, Object> checkIncidentCommandWorkflow() {
        String incidentModel = readWorkspaceText("src/main/java/com/daniphord/mahanga/Model/Incident.java");
        String operationsController = readWorkspaceText("src/main/java/com/daniphord/mahanga/Controller/OperationsController.java");
        String controlRoomController = readWorkspaceText("src/main/java/com/daniphord/mahanga/Controller/ControlRoomController.java");
        String notificationService = readWorkspaceText("src/main/java/com/daniphord/mahanga/Service/NotificationService.java");
        String roleDashboard = readWorkspaceText("src/main/resources/templates/role-dashboard.html");

        boolean incidentBasics = incidentModel.contains("incidentType")
                && incidentModel.contains("severity")
                && incidentModel.contains("reportedAt")
                && incidentModel.contains("callReceivedAt");
        boolean locationCapture = incidentModel.contains("latitude")
                && incidentModel.contains("longitude")
                && incidentModel.contains("region")
                && incidentModel.contains("district");
        boolean responseTracking = incidentModel.contains("dispatchedAt")
                && incidentModel.contains("arrivalTime")
                && incidentModel.contains("resolvedAt");
        boolean incidentApi = operationsController.contains("@PostMapping(\"/api/incidents\")")
                && operationsController.contains("@PostMapping(\"/api/incidents/{incidentId}/dispatch\")");
        boolean publicIntake = controlRoomController.contains("/public/emergency/report");
        boolean liveMap = roleDashboard.contains("openstreetmap") || roleDashboard.contains("ui-map-frame");
        boolean realtimeAlerts = notificationService.contains("signalHandler.broadcast");

        if (incidentBasics && locationCapture && responseTracking && incidentApi && publicIntake && liveMap && realtimeAlerts) {
            return pass("Incident intake captures type, severity, geography, and timing data; dispatch workflow, public reporting, live map context, and real-time notification broadcasting are present.");
        }

        List<String> gaps = new ArrayList<>();
        if (!incidentBasics) {
            gaps.add("incident type/severity/time capture is incomplete");
        }
        if (!locationCapture) {
            gaps.add("geographic capture fields were not fully detected");
        }
        if (!responseTracking) {
            gaps.add("dispatch/arrival/completion timestamps were not fully detected");
        }
        if (!incidentApi) {
            gaps.add("incident create/dispatch endpoints were not fully detected");
        }
        if (!publicIntake) {
            gaps.add("public incident intake route was not detected");
        }
        if (!liveMap) {
            gaps.add("map integration was not detected");
        }
        if (!realtimeAlerts) {
            gaps.add("real-time notification broadcasting was not detected");
        }
        return fail(String.join("; ", gaps) + ".");
    }

    private Map<String, Object> checkPublicRouting() {
        boolean callsRouted = emergencyCallRepository.findAll().stream()
                .filter(call -> call.getRoutedStation() != null)
                .allMatch(call -> call.getDistrict() != null && call.getRegion() != null);
        return callsRouted ? pass("Public reports with routing data remain scoped to selected station geography.") : warn("Some public reports exist without fully linked routing geography.");
    }

    private Map<String, Object> checkInvestigationRoles() {
        List<String> requiredRoles = List.of(
                OperationRole.DISTRICT_INVESTIGATION_OFFICER,
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.REGIONAL_INVESTIGATION_OFFICER,
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.FIRE_INVESTIGATION_HOD,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.CGF
        );
        long reports = fireInvestigationRepository.count();
        boolean roleCatalogReady = OperationRole.ALL_FROMS_ROLES.containsAll(requiredRoles);
        boolean rolesPresent = userRepository.findAll().stream()
                .map(User::getRole)
                .map(role -> role == null ? "" : role.toUpperCase())
                .collect(Collectors.toSet())
                .containsAll(requiredRoles);
        if (rolesPresent) {
            return pass("Investigation workflow chain is configured. Current investigation reports: " + reports + ".");
        }
        if (reports == 0 && roleCatalogReady) {
            return pass("Investigation workflow chain and approval-role catalogue are configured. No live investigation reports currently require role-account coverage.");
        }
        if (!rolesPresent) {
            return warn("Investigation workflow exists, but not every approval role has an account in current data.");
        }
        return pass("Investigation workflow chain is configured. Current investigation reports: " + reports + ".");
    }

    private Map<String, Object> checkAdminOperationsReadiness() {
        boolean userManagement = workspaceFileExists("src/main/java/com/daniphord/mahanga/Controller/UserController.java")
                && workspaceFileExists("src/main/java/com/daniphord/mahanga/Service/UserService.java");
        boolean systemTestAdmin = workspaceFileExists("src/main/java/com/daniphord/mahanga/Controller/SystemTestController.java");
        boolean brandingAdmin = workspaceFileExists("src/main/java/com/daniphord/mahanga/Controller/BrandingController.java");
        boolean documentationAdmin = workspaceFileExists("src/main/java/com/daniphord/mahanga/Controller/DocumentationController.java")
                && workspaceFileExists("src/main/java/com/daniphord/mahanga/Service/SystemDocumentationService.java");
        boolean auditServicePresent = workspaceFileExists("src/main/java/com/daniphord/mahanga/Service/AuditService.java");
        boolean auditViewerDetected = !scanWorkspaceForMarkers(Path.of("src/main/resources/templates"), ".html", List.of("audit"), 2).isEmpty()
                || !scanWorkspaceForMarkers(Path.of("src/main/java/com/daniphord/mahanga/Controller"), ".java", List.of("AuditLogRepository", "/api/audit", "audit log"), 2).isEmpty();

        if (userManagement && systemTestAdmin && brandingAdmin && documentationAdmin && auditServicePresent && auditViewerDetected) {
            return pass("Administrative controls for users, branding, documentation, verification, and audit-log review were detected.");
        }

        List<String> findings = new ArrayList<>();
        if (!userManagement) {
            findings.add("user management module is incomplete");
        }
        if (!systemTestAdmin) {
            findings.add("system verification controller is missing");
        }
        if (!brandingAdmin || !documentationAdmin) {
            findings.add("branding/documentation administration is incomplete");
        }
        if (!auditServicePresent) {
            findings.add("audit service is missing");
        }
        if (!auditViewerDetected) {
            findings.add("a dedicated admin audit-log viewer was not detected");
        }

        if (userManagement && systemTestAdmin && brandingAdmin && documentationAdmin && auditServicePresent) {
            return warn("Administrative modules for users, documents, branding, and verification are present, but " + String.join("; ", findings) + ".");
        }
        return fail(String.join("; ", findings) + ".");
    }

    private Map<String, Object> checkAuditLogging(Map<String, Object> snapshot) {
        long entries = longValue(snapshot.get("auditEntries"));
        long failedLogins = longValue(snapshot.get("failedLoginsSinceLastTest"));
        return entries > 0
                ? pass("Audit log storage is active. Current entries: " + entries + ". Failed-login related events since the last test: " + failedLogins + ".")
                : warn("No audit log records found yet.");
    }

    private Map<String, Object> checkPerformanceAndResponseRate(Map<String, Object> snapshot) {
        double averageQueryLatencyMs = doubleValue(snapshot.get("averageQueryLatencyMs"));
        double slowestQueryMs = doubleValue(snapshot.get("slowestQueryMs"));
        double averageResponseTime = doubleValue(snapshot.get("averageResponseTimeMinutes"));
        if (averageQueryLatencyMs > 350.0 || slowestQueryMs > 700.0) {
            return warn("Repository latency is elevated. Average query latency: " + formatDouble(averageQueryLatencyMs)
                    + " ms, slowest measured query: " + formatDouble(slowestQueryMs)
                    + " ms. Average operational response time remains " + formatDouble(averageResponseTime) + " minutes.");
        }
        return pass("Average query latency: " + formatDouble(averageQueryLatencyMs)
                + " ms, slowest measured query: " + formatDouble(slowestQueryMs)
                + " ms. Average operational response time: " + formatDouble(averageResponseTime) + " minutes.");
    }

    private Map<String, Object> checkReliabilityAndFailSafe() {
        String properties = readWorkspaceText("src/main/resources/application.properties");
        String deploymentGuide = readWorkspaceText("DEPLOYMENT.md");
        boolean webExceptionHandler = workspaceFileExists("src/main/java/com/daniphord/mahanga/Config/WebExceptionHandler.java");
        boolean fileLogging = properties.contains("logging.pattern.file");
        boolean userFriendlyErrors = properties.contains("server.error.include-message")
                && workspaceFileExists("src/main/resources/templates/error.html");
        boolean restartGuidance = deploymentGuide.contains("Restart=on-failure");
        boolean scheduledVerification = readWorkspaceText("src/main/java/com/daniphord/mahanga/Service/SystemTestService.java").contains("@Scheduled");

        if (webExceptionHandler && fileLogging && userFriendlyErrors && restartGuidance && scheduledVerification) {
            return pass("Global exception handling, file logging, user-facing error view, restart guidance, and scheduled verification controls were detected.");
        }

        List<String> gaps = new ArrayList<>();
        if (!webExceptionHandler) {
            gaps.add("global web exception handling is missing");
        }
        if (!fileLogging) {
            gaps.add("file logging pattern is not configured");
        }
        if (!userFriendlyErrors) {
            gaps.add("user-facing error handling is incomplete");
        }
        if (!restartGuidance) {
            gaps.add("deployment restart/fail-safe guidance was not detected");
        }
        if (!scheduledVerification) {
            gaps.add("scheduled verification run was not detected");
        }
        return warn(String.join("; ", gaps) + ".");
    }

    private Map<String, Object> checkMonitoringAndObservability() {
        String pom = readWorkspaceText("pom.xml");
        String properties = readWorkspaceText("src/main/resources/application.properties");
        boolean actuator = pom.contains("spring-boot-starter-actuator");
        boolean metricsStack = !scanWorkspaceForMarkers(Path.of("src/main/java"), ".java", List.of("MeterRegistry", "Observation", "Prometheus"), 3).isEmpty();
        boolean logging = properties.contains("logging.level.root") && properties.contains("logging.pattern.console");
        boolean latencyAudit = readWorkspaceText("src/main/java/com/daniphord/mahanga/Service/SystemTestService.java").contains("averageQueryLatencyMs");

        if (actuator && metricsStack && logging) {
            return pass("Application metrics, observability instrumentation, and log output were detected.");
        }
        if (logging && latencyAudit) {
            return warn("Log output and audit-time latency measurements exist, but no Spring Actuator/Prometheus-style runtime observability stack was detected.");
        }
        return fail("Monitoring coverage is limited; runtime metrics and observability instrumentation were not detected.");
    }

    private Map<String, Object> checkDataIntegrityAndTransactions() {
        long constrainedModels = countFilesWithMarkers(
                Path.of("src/main/java/com/daniphord/mahanga/Model"),
                ".java",
                List.of("@Index", "unique = true", "nullable = false", "@NotBlank", "@NotNull")
        );
        List<String> transactionMarkers = scanWorkspaceForMarkers(Path.of("src/main/java/com/daniphord/mahanga"), ".java", List.of("@Transactional"), 4);
        boolean inputValidation = workspaceFileExists("src/main/java/com/daniphord/mahanga/Util/InputValidator.java");

        if (constrainedModels >= 8 && !transactionMarkers.isEmpty() && inputValidation) {
            return pass("Entity constraints, input validation, and transactional service boundaries were detected.");
        }
        if (constrainedModels >= 8 && inputValidation) {
            return warn("Entity constraints and validation are widely used across " + constrainedModels + " model file(s), but no explicit @Transactional service boundary was detected.");
        }
        return fail("Data integrity controls are incomplete; constrained models detected: " + constrainedModels + ".");
    }

    private Map<String, Object> checkDeploymentAndBackupReadiness() {
        String deploymentGuide = readWorkspaceText("DEPLOYMENT.md");
        boolean docker = workspaceFileExists("Dockerfile");
        boolean railway = workspaceFileExists("railway.json");
        boolean deploymentDocumented = workspaceFileExists("DEPLOYMENT.md");
        boolean backupRunbook = deploymentGuide.contains("Backup") && deploymentGuide.contains("cron");
        boolean cloudProviderSpecific = deploymentGuide.toLowerCase(java.util.Locale.ROOT).contains("aws")
                || deploymentGuide.toLowerCase(java.util.Locale.ROOT).contains("azure")
                || deploymentGuide.toLowerCase(java.util.Locale.ROOT).contains("gcp");

        if (docker && railway && deploymentDocumented && backupRunbook && cloudProviderSpecific) {
            return pass("Container deployment, cloud deployment guidance, and backup runbooks were detected.");
        }
        if (docker && deploymentDocumented && backupRunbook) {
            return warn("Docker packaging, deployment guidance, and backup runbooks are present, but no AWS/Azure/GCP-specific deployment automation was detected.");
        }
        return fail("Deployment or backup readiness is incomplete; Docker/deployment/backup evidence is missing.");
    }

    private Map<String, Object> checkTestSuiteCoverage() {
        List<String> testFiles = listRelativeFiles(Path.of("src/test/java"), ".java");
        long unitTests = testFiles.stream().filter(name -> name.contains("/Service/")).count();
        long apiTests = testFiles.stream().filter(name -> name.contains("/Controller/")).count();
        long securityTests = testFiles.stream()
                .filter(name -> name.contains("Security") || name.contains("Login") || name.contains("SignalAccess") || name.contains("SystemTest"))
                .count();
        long performanceTests = testFiles.stream()
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT).contains("load")
                        || name.toLowerCase(java.util.Locale.ROOT).contains("stress")
                        || name.toLowerCase(java.util.Locale.ROOT).contains("performance"))
                .count();

        if (unitTests > 0 && apiTests > 0 && securityTests > 0 && performanceTests > 0) {
            return pass("Unit, API, security, and performance/load tests were detected in the workspace.");
        }
        if (unitTests > 0 && apiTests > 0 && securityTests > 0) {
            return warn("Detected " + testFiles.size() + " Java test file(s) spanning unit, API, and security coverage, but no dedicated load/performance test suite was detected.");
        }
        return fail("Test coverage is incomplete; detected " + testFiles.size() + " test file(s) with limited category coverage.");
    }

    private Map<String, Object> checkDocumentationPresence() {
        List<String> requiredFiles = List.of(
                "README.md",
                "DOCUMENTATION_INDEX.md",
                "HELP.md",
                "DEPLOYMENT.md",
                "SECURITY_AUDIT.md"
        );
        boolean present = requiredFiles.stream().allMatch(file -> Files.exists(Path.of(file)));
        return present ? pass("Core documentation files are present on disk.") : warn("One or more documentation files are missing from the workspace.");
    }

    private Map<String, Object> buildRuntimeStatus() {
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("loginPage", endpointStatus("http://127.0.0.1:" + serverPort + "/login"));
        runtime.put("publicReportPage", endpointStatus("http://127.0.0.1:" + serverPort + "/public/emergency/report"));
        runtime.put("aiSidecar", pythonAiService.health().orElse(Map.of("status", "down")));
        runtime.put("videoStoragePresent", Files.exists(Path.of("videos")));
        return runtime;
    }

    private Map<String, Object> checkApplicationRuntime(Map<String, Object> runtimeStatus) {
        @SuppressWarnings("unchecked")
        Map<String, Object> loginPage = (Map<String, Object>) runtimeStatus.getOrDefault("loginPage", Map.of());
        String status = stringValue(loginPage.get("status"));
        if ("UP".equalsIgnoreCase(status)) {
            return pass("Application login route responded with HTTP " + loginPage.get("statusCode")
                    + " in " + loginPage.get("latencyMs") + " ms.");
        }
        return fail("Application login route is not responding on port " + serverPort + ".");
    }

    private Map<String, Object> checkPublicAccessRuntime(Map<String, Object> runtimeStatus) {
        @SuppressWarnings("unchecked")
        Map<String, Object> publicReportPage = (Map<String, Object>) runtimeStatus.getOrDefault("publicReportPage", Map.of());
        String status = stringValue(publicReportPage.get("status"));
        if ("UP".equalsIgnoreCase(status)) {
            return pass("Public emergency access responded with HTTP " + publicReportPage.get("statusCode")
                    + " in " + publicReportPage.get("latencyMs") + " ms.");
        }
        return warn("Public emergency access page is not responding locally.");
    }

    private Map<String, Object> checkPythonSidecar(Map<String, Object> runtimeStatus) {
        @SuppressWarnings("unchecked")
        Map<String, Object> aiSidecar = (Map<String, Object>) runtimeStatus.getOrDefault("aiSidecar", Map.of());
        String status = stringValue(aiSidecar.get("status"));
        String service = stringValue(aiSidecar.get("service"));
        if ("ok".equalsIgnoreCase(status)) {
            return pass("Python AI sidecar responded successfully" + (service.isBlank() ? "." : " as " + service + "."));
        }
        return warn("Python AI sidecar is not running.");
    }

    private Map<String, Object> checkAiRecommendationEngine() {
        var payload = pythonAiService.recommendIncident(Map.of(
                "incidentType", "FIRE",
                "severity", "CRITICAL",
                "status", "ACTIVE",
                "details", "Smoke, trapped civilians, hospital wing exposure, fuel smell",
                "resourcesUsed", "rescue team, ambulance"
        ));
        if (payload.isEmpty()) {
            return warn("Python recommendation endpoint is unavailable.");
        }
        List<String> actions = stringList(payload.get().get("recommendedActions"));
        String priority = stringValue(payload.get().get("priority"));
        String model = stringValue(payload.get().get("model"));
        if (actions.isEmpty()) {
            return warn("Python recommendation endpoint responded without recommended actions.");
        }
        return pass("Python recommendation engine returned priority " + priority + " with " + actions.size()
                + " action(s) using model " + model + ".");
    }

    private Map<String, Object> checkAiExecutiveAnalysis(
            Map<String, Object> snapshot,
            List<Map<String, Object>> details,
            Map<String, Object> runtimeStatus
    ) {
        var payload = pythonAiService.analyzeSystemAudit(snapshot, details, runtimeStatus);
        if (payload.isEmpty()) {
            return warn("Python executive system analysis is unavailable.");
        }
        String riskLevel = stringValue(payload.get().get("riskLevel"));
        String summary = stringValue(payload.get().get("executiveSummary"));
        String findings = joinItems(payload.get().get("findings"));
        String actions = joinItems(payload.get().get("recommendedActions"));
        String model = stringValue(payload.get().get("model"));
        String message = joinSentences(
                summary,
                findings.isBlank() ? "" : "Findings: " + findings,
                actions.isBlank() ? "" : "Recommended actions: " + actions,
                model.isBlank() ? "" : "Model: " + model
        );
        return switch (riskLevel.toUpperCase()) {
            case "HIGH" -> fail(message);
            case "MEDIUM" -> warn(message);
            default -> pass(message);
        };
    }

    private Map<String, Object> endpointStatus(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            long started = System.nanoTime();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            double latencyMs = (System.nanoTime() - started) / 1_000_000.0;
            return Map.of(
                    "status", response.statusCode() < 500 ? "UP" : "DOWN",
                    "statusCode", response.statusCode(),
                    "latencyMs", formatDouble(latencyMs)
            );
        } catch (Exception exception) {
            return Map.of(
                    "status", "DOWN",
                    "statusCode", 0,
                    "latencyMs", "0.0"
            );
        }
    }

    private Map<String, Object> checkVulnerabilitySnapshot(Map<String, Object> snapshot) {
        @SuppressWarnings("unchecked")
        List<String> vulnerabilities = (List<String>) snapshot.get("vulnerabilities");
        if (vulnerabilities == null || vulnerabilities.isEmpty()) {
            return pass("No immediate vulnerability flags were raised by the current internal audit heuristics.");
        }
        return warn(String.join(". ", vulnerabilities) + ".");
    }

    private Map<String, Object> checkVideoStorage() {
        return Files.exists(Path.of("videos")) ? pass("Video storage path exists.") : warn("Video storage path has not been created yet.");
    }

    private User createDisposableProbeUser() {
        User probeUser = new User();
        probeUser.setUsername("stprobe_" + Long.toString(System.currentTimeMillis(), 36));
        probeUser.setPassword(passwordEncoder.encode("TempProbe1!"));
        probeUser.setRole(OperationRole.ADMIN);
        probeUser.setActive(true);
        probeUser.setFullName("System Test Probe");
        probeUser.setPasswordStrength("STRONG");
        return userRepository.save(probeUser);
    }

    private void cleanupDisposableProbeUser(User probeUser) {
        if (probeUser == null || probeUser.getId() == null) {
            return;
        }
        var auditLogs = auditLogRepository.findByUserId(probeUser.getId());
        if (!auditLogs.isEmpty()) {
            auditLogRepository.deleteAll(auditLogs);
        }
        userRepository.findById(probeUser.getId()).ifPresent(userRepository::delete);
    }

    private List<String> scanSqlRiskMarkers() {
        Path sourceRoot = Path.of("src/main/java");
        if (!Files.exists(sourceRoot)) {
            return List.of("source tree unavailable");
        }

        List<String> markers = List.of(
                "createNativeQuery(\" +",
                "createNativeQuery(query +",
                "createNativeQuery(sql +",
                "jdbcTemplate.query(\" +",
                "jdbcTemplate.queryForObject(\" +",
                "jdbcTemplate.update(\" +",
                "@Query(value = \"SELECT \" +"
        );
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("SystemTestService.java"))
                    .flatMap(path -> sqlMarkerHits(path, markers).stream())
                    .sorted(Comparator.naturalOrder())
                    .distinct()
                    .limit(6)
                    .toList();
        } catch (Exception exception) {
            return List.of("source scan failed: " + exception.getClass().getSimpleName());
        }
    }

    private List<String> sqlMarkerHits(Path path, List<String> markers) {
        try {
            String content = Files.readString(path);
            return markers.stream()
                    .filter(content::contains)
                    .map(marker -> relativeWorkspacePath(path) + " [" + marker + "]")
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String readWorkspaceText(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (Exception exception) {
            return "";
        }
    }

    private boolean workspaceFileExists(String relativePath) {
        return Files.exists(Path.of(relativePath));
    }

    private List<String> scanWorkspaceForMarkers(Path root, String fileSuffix, List<String> markers, int limit) {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(fileSuffix))
                    .flatMap(path -> markerHits(path, markers).stream())
                    .distinct()
                    .sorted()
                    .limit(limit)
                    .toList();
        } catch (Exception exception) {
            return List.of("scan failed: " + exception.getClass().getSimpleName());
        }
    }

    private long countFilesWithMarkers(Path root, String fileSuffix, List<String> markers) {
        if (!Files.exists(root)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(fileSuffix))
                    .filter(path -> fileContainsAnyMarker(path, markers))
                    .count();
        } catch (Exception exception) {
            return 0L;
        }
    }

    private List<String> listRelativeFiles(Path root, String fileSuffix) {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(fileSuffix))
                    .map(this::relativeWorkspacePath)
                    .sorted()
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<String> markerHits(Path path, List<String> markers) {
        try {
            String content = Files.readString(path);
            return markers.stream()
                    .filter(content::contains)
                    .map(marker -> relativeWorkspacePath(path) + " [" + marker + "]")
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private boolean fileContainsAnyMarker(Path path, List<String> markers) {
        try {
            String content = Files.readString(path);
            return markers.stream().anyMatch(content::contains);
        } catch (Exception exception) {
            return false;
        }
    }

    private String relativeWorkspacePath(Path path) {
        Path workspace = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(workspace)) {
            return workspace.relativize(normalized).toString().replace('\\', '/');
        }
        return normalized.toString().replace('\\', '/');
    }

    private String buildSummary(Map<String, Object> snapshot, long passed, long warnings, long failed, List<Map<String, Object>> details) {
        String base = "Verification completed with " + passed + " pass, " + warnings + " warning, " + failed + " fail. "
                + "Users: " + snapshot.get("totalUsers") + " total (" + snapshot.get("activeUsers") + " active / " + snapshot.get("inactiveUsers") + " inactive), "
                + "regions: " + snapshot.get("regions") + ", incidents: " + snapshot.get("incidents")
                + ", failed logins since last test: " + snapshot.get("failedLoginsSinceLastTest") + ".";
        String aiSummary = details.stream()
                .filter(item -> "AI Executive Analysis".equals(item.get("name")))
                .map(item -> stringValue(item.get("message")))
                .findFirst()
                .orElse("");
        if (aiSummary.isBlank()) {
            return base;
        }
        return base + " AI executive analysis: " + summarize(aiSummary);
    }

    private String aiSection(Map<String, Object> aiAnalysis, String lang) {
        if (aiAnalysis.isEmpty()) {
            return "";
        }
        boolean sw = "sw".equalsIgnoreCase(lang);
        return """
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                """.formatted(
                sw ? "Uchanganuzi wa AI" : "AI Executive Analysis",
                escape(stringValue(aiAnalysis.get("message")))
        );
    }

    private String localizedSummary(SystemTestReport report, String lang) {
        if (!"sw".equalsIgnoreCase(lang)) {
            return report.getSummary();
        }
        return "Uthibitishaji umekamilika kwa ukaguzi " + report.getPassedChecks() + " uliopita, tahadhari "
                + report.getWarningChecks() + ", na ukaguzi " + report.getFailedChecks() + " ulioshindikana.";
    }

    private String localizeCheckName(String name, String lang) {
        if (!"sw".equalsIgnoreCase(lang)) {
            return name;
        }
        return switch (name) {
            case "System inventory" -> "Muhtasari wa Mfumo";
            case "Architecture standard alignment" -> "Ulinganifu wa Usanifu wa Kimataifa";
            case "Module coverage" -> "Ufunikaji wa Moduli";
            case "Dashboard UX readiness" -> "Utayari wa Dashibodi na UX";
            case "Authentication policy" -> "Sera ya Uthibitishaji";
            case "API authorization model" -> "Muundo wa Uidhinishaji wa API";
            case "XSS and CSRF posture" -> "Ulinzi wa XSS na CSRF";
            case "Brute-force resilience" -> "Ustahimilivu wa Mashambulizi ya Brute-force";
            case "SQL injection resilience" -> "Ustahimilivu wa SQL Injection";
            case "Scalability readiness" -> "Utayari wa Upanuzi";
            case "User activity" -> "Shughuli za Watumiaji";
            case "Password strength" -> "Nguvu ya Nywila";
            case "Failed login monitoring" -> "Ufuatiliaji wa Kushindwa Kuingia";
            case "Geography registry" -> "Usajili wa Jiografia";
            case "Incident load" -> "Mzigo wa Matukio";
            case "Incident command workflow" -> "Mtiririko wa Uendeshaji wa Matukio";
            case "Public routing readiness" -> "Utayari wa Uelekezaji wa Umma";
            case "Investigation workflow" -> "Mtiririko wa Uchunguzi";
            case "Admin operations readiness" -> "Utayari wa Usimamizi wa Admin";
            case "Audit logging" -> "Uhifadhi wa Audit";
            case "System performance" -> "Utendaji wa Mfumo";
            case "Reliability and fail-safe" -> "Utegemewaji na Kinga ya Hitilafu";
            case "Monitoring and observability" -> "Ufuatiliaji na Uonekano wa Mfumo";
            case "Data integrity and transactions" -> "Uadilifu wa Data na Miamala";
            case "Deployment and backup readiness" -> "Utayari wa Uwekaji na Nakala Rudufu";
            case "Test suite coverage" -> "Ufunikaji wa Majaribio";
            case "Application runtime" -> "Uendeshaji wa Mfumo";
            case "Public access runtime" -> "Uendeshaji wa Ufikiaji wa Umma";
            case "Documentation files" -> "Faili za Nyaraka";
            case "Python AI sidecar" -> "Huduma ya Pembeni ya Python AI";
            case "AI recommendation engine" -> "Injini ya Mapendekezo ya AI";
            case "AI Executive Analysis" -> "Uchanganuzi wa AI";
            case "Vulnerability snapshot" -> "Muhtasari wa Udhaifu";
            case "Video subsystem" -> "Mfumo Mdogo wa Video";
            default -> name;
        };
    }

    private String localizeStatus(String status, String lang) {
        if (!"sw".equalsIgnoreCase(lang)) {
            return status;
        }
        return switch (status) {
            case "PASS" -> "IMEPITA";
            case "WARN" -> "TAHADHARI";
            case "FAIL" -> "IMESHINDWA";
            default -> status;
        };
    }

    private boolean containsSecurityKeyword(String value) {
        String normalized = value == null ? "" : value.toUpperCase();
        return normalized.contains("LOGIN") || normalized.contains("LOCK") || normalized.contains("AUTH");
    }

    private double measureMillis(ThrowingSupplier supplier) {
        long started = System.nanoTime();
        try {
            supplier.run();
        } catch (Exception ignored) {
            return 999.0;
        }
        return (System.nanoTime() - started) / 1_000_000.0;
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private String formatDouble(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> items) {
            return items.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private String joinItems(Object value) {
        return String.join("; ", stringList(value));
    }

    private String joinSentences(String... values) {
        return java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String summarize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= 240 ? value : value.substring(0, 237) + "...";
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        void run() throws Exception;
    }
}
