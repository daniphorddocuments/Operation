package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.FireInvestigation;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.InvestigationAttachment;
import com.daniphord.mahanga.Model.InvestigationLog;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.FireInvestigationRepository;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.InvestigationAttachmentRepository;
import com.daniphord.mahanga.Repositories.InvestigationLogRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class InvestigationWorkflowService {

    private static final List<String> APPROVAL_CHAIN = List.of(
            OperationRole.DISTRICT_INVESTIGATION_OFFICER,
            OperationRole.DISTRICT_FIRE_OFFICER,
            OperationRole.REGIONAL_INVESTIGATION_OFFICER,
            OperationRole.REGIONAL_FIRE_OFFICER,
            OperationRole.FIRE_INVESTIGATION_HOD,
            OperationRole.COMMISSIONER_OPERATIONS,
            OperationRole.CGF
    );

    private final FireInvestigationRepository fireInvestigationRepository;
    private final IncidentRepository incidentRepository;
    private final InvestigationLogRepository investigationLogRepository;
    private final InvestigationAttachmentRepository investigationAttachmentRepository;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;
    private final NotificationService notificationService;
    private final Path storageRoot;
    private final PdfBrandingService pdfBrandingService;

    public InvestigationWorkflowService(
            FireInvestigationRepository fireInvestigationRepository,
            IncidentRepository incidentRepository,
            InvestigationLogRepository investigationLogRepository,
            InvestigationAttachmentRepository investigationAttachmentRepository,
            UserRepository userRepository,
            RoleAccessService roleAccessService,
            NotificationService notificationService,
            PdfBrandingService pdfBrandingService,
            @Value("${froms.investigation.storage-path:investigations}") String storagePath
    ) {
        this.fireInvestigationRepository = fireInvestigationRepository;
        this.incidentRepository = incidentRepository;
        this.investigationLogRepository = investigationLogRepository;
        this.investigationAttachmentRepository = investigationAttachmentRepository;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
        this.notificationService = notificationService;
        this.pdfBrandingService = pdfBrandingService;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    public boolean hasInvestigationAccess(User user) {
        String role = normalizeRole(user == null ? null : user.getRole());
        return Set.of(
                "SUPER_ADMIN",
                OperationRole.CGF,
                OperationRole.COMMISSIONER_OPERATIONS,
                OperationRole.FIRE_INVESTIGATION_HOD,
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_INVESTIGATION_OFFICER,
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.DISTRICT_INVESTIGATION_OFFICER
        ).contains(role);
    }

    public List<Incident> eligibleIncidents(User user) {
        return roleAccessService.visibleIncidents(user, incidentRepository.findAll()).stream()
                .filter(incident -> "FIRE".equalsIgnoreCase(incident.getIncidentType()) || "RESCUE".equalsIgnoreCase(incident.getIncidentType()))
                .sorted(Comparator.comparing(Incident::getReportedAt).reversed())
                .toList();
    }

    public List<FireInvestigation> visibleInvestigations(User user) {
        if (!hasInvestigationAccess(user)) {
            return List.of();
        }
        String role = normalizeRole(user.getRole());
        return fireInvestigationRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(report -> canView(role, user, report))
                .toList();
    }

    public Map<String, Object> dashboardMetrics(User user) {
        List<FireInvestigation> reports = visibleInvestigations(user);
        String role = normalizeRole(user == null ? null : user.getRole());
        return Map.of(
                "pendingReports", reports.stream().filter(report -> role.equals(report.getCurrentLevel())).count(),
                "submittedReports", reports.stream().filter(report -> !"FINALIZED".equalsIgnoreCase(report.getStatus())).count(),
                "approvedReports", reports.stream().filter(report -> "FINALIZED".equalsIgnoreCase(report.getStatus())).count(),
                "deniedReports", reports.stream().filter(report -> report.getStatus().startsWith("DENIED")).count()
        );
    }

    public FireInvestigation startInvestigation(
            Long incidentId,
            String incidentDetails,
            String causeAnalysis,
            String witnessStatements,
            List<MultipartFile> attachments,
            User currentUser
    ) throws Exception {
        ensureRole(currentUser, OperationRole.DISTRICT_INVESTIGATION_OFFICER);
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        Optional<FireInvestigation> existing = fireInvestigationRepository.findByIncidentId(incidentId);
        FireInvestigation report = existing.orElseGet(FireInvestigation::new);
        if (report.getId() != null && !OperationRole.DISTRICT_INVESTIGATION_OFFICER.equals(normalizeRole(report.getCurrentLevel()))) {
            throw new IllegalStateException("This investigation is already in approval flow and cannot be edited by DIO");
        }

        report.setIncident(incident);
        report.setDio(currentUser);
        report.setIncidentDetails(incidentDetails);
        report.setCauseAnalysis(causeAnalysis);
        report.setWitnessStatements(witnessStatements);
        report.setStatus("PENDING_DFO");
        report.setCurrentLevel(OperationRole.DISTRICT_FIRE_OFFICER);
        report.setSubmissionDate(LocalDateTime.now());
        FireInvestigation saved = fireInvestigationRepository.save(report);
        saveAttachments(saved, attachments);
        log(saved, currentUser, OperationRole.DISTRICT_INVESTIGATION_OFFICER, "SUBMITTED", "Investigation submitted to District Fire Officer");
        notifyNextApprovers(saved, OperationRole.DISTRICT_FIRE_OFFICER, "New district investigation submitted", "Investigation " + saved.getInvestigationNumber() + " is awaiting your review.");
        return saved;
    }

    public FireInvestigation review(Long reportId, boolean approve, String comment, User currentUser) throws Exception {
        FireInvestigation report = fireInvestigationRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Investigation not found"));
        String role = normalizeRole(currentUser == null ? null : currentUser.getRole());
        if (!role.equals(report.getCurrentLevel()) && !"SUPER_ADMIN".equals(role)) {
            throw new IllegalAccessException("You are not allowed to review this investigation at the current stage");
        }
        if (!approve && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("Comment is required when denying an investigation");
        }

        int currentIndex = APPROVAL_CHAIN.indexOf(report.getCurrentLevel());
        if (currentIndex < 0) {
            throw new IllegalStateException("Invalid workflow state");
        }

        if (approve) {
            if (currentIndex == APPROVAL_CHAIN.size() - 1) {
                report.setStatus("FINALIZED");
                report.setCurrentLevel(OperationRole.CGF);
                fireInvestigationRepository.save(report);
                log(report, currentUser, role, "APPROVED", safeComment(comment, "Final approval granted"));
                notificationService.notifyUsers(
                        List.of(report.getDio()),
                        "Investigation finalized",
                        "Investigation " + report.getInvestigationNumber() + " has received final approval.",
                        "/dashboard"
                );
                return report;
            }
            String nextRole = APPROVAL_CHAIN.get(currentIndex + 1);
            report.setCurrentLevel(nextRole);
            report.setStatus("PENDING_" + nextRole);
            fireInvestigationRepository.save(report);
            log(report, currentUser, role, "APPROVED", safeComment(comment, "Approved and forwarded"));
            notifyNextApprovers(report, nextRole, "Investigation awaiting approval", "Investigation " + report.getInvestigationNumber() + " has been approved and forwarded to your level.");
            return report;
        }

        String previousRole = APPROVAL_CHAIN.get(Math.max(0, currentIndex - 1));
        report.setCurrentLevel(previousRole);
        report.setStatus("DENIED_" + role);
        fireInvestigationRepository.save(report);
        log(report, currentUser, role, "DENIED", comment);
        notifyNextApprovers(report, previousRole, "Investigation returned for revision", "Investigation " + report.getInvestigationNumber() + " was denied at " + role + " level. Review the comments and revise.");
        return report;
    }

    public List<InvestigationLog> logs(Long reportId) {
        return investigationLogRepository.findByReportIdOrderByTimestampAsc(reportId);
    }

    public List<InvestigationAttachment> attachments(Long reportId) {
        return investigationAttachmentRepository.findByReportIdOrderByUploadedAtAsc(reportId);
    }

    public byte[] generatePdf(Long reportId, User currentUser, String lang) throws Exception {
        FireInvestigation report = fireInvestigationRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Investigation not found"));
        if (!canView(normalizeRole(currentUser == null ? null : currentUser.getRole()), currentUser, report)) {
            throw new IllegalAccessException("You cannot download this investigation");
        }

        String bodyHtml = buildPdfHtml(report, logs(reportId), attachments(reportId), lang);
        boolean sw = "sw".equalsIgnoreCase(lang);
        return pdfBrandingService.generatePdf(sw ? "RIPOTI YA UCHUNGUZI WA MOTO WA FROMS" : "FROMS FIRE INVESTIGATION REPORT", bodyHtml, lang);
    }

    private void saveAttachments(FireInvestigation report, List<MultipartFile> attachments) throws IOException {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        Files.createDirectories(storageRoot.resolve(String.valueOf(report.getId())));
        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }
            String originalName = attachment.getOriginalFilename() == null ? "evidence" : attachment.getOriginalFilename().replaceAll("[^A-Za-z0-9._-]", "_");
            Path target = storageRoot.resolve(String.valueOf(report.getId())).resolve(System.currentTimeMillis() + "-" + originalName);
            Files.copy(attachment.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            InvestigationAttachment savedAttachment = new InvestigationAttachment();
            savedAttachment.setReport(report);
            savedAttachment.setFileName(originalName);
            savedAttachment.setFilePath(target.toString());
            savedAttachment.setFileType(attachment.getContentType() == null ? "application/octet-stream" : attachment.getContentType());
            investigationAttachmentRepository.save(savedAttachment);
        }
    }

    private void notifyNextApprovers(FireInvestigation report, String role, String title, String message) {
        notificationService.notifyUsers(recipientsForRole(report, role), title, message, "/dashboard");
    }

    private List<User> recipientsForRole(FireInvestigation report, String role) {
        Region region = report.getIncident().getDistrict() != null ? report.getIncident().getDistrict().getRegion() : report.getIncident().getRegion();
        return userRepository.findAll().stream()
                .filter(user -> role.equals(normalizeRole(user.getRole())))
                .filter(user -> sameScope(user, report, region))
                .toList();
    }

    private boolean sameScope(User user, FireInvestigation report, Region region) {
        String role = normalizeRole(user.getRole());
        if (Set.of(OperationRole.CGF, OperationRole.COMMISSIONER_OPERATIONS, OperationRole.FIRE_INVESTIGATION_HOD, "SUPER_ADMIN").contains(role)) {
            return true;
        }
        if (Set.of(OperationRole.REGIONAL_FIRE_OFFICER, OperationRole.REGIONAL_INVESTIGATION_OFFICER).contains(role)) {
            return user.getStation() != null
                    && user.getStation().getDistrict() != null
                    && user.getStation().getDistrict().getRegion() != null
                    && region != null
                    && user.getStation().getDistrict().getRegion().getId().equals(region.getId());
        }
        if (Set.of(OperationRole.DISTRICT_FIRE_OFFICER, OperationRole.DISTRICT_INVESTIGATION_OFFICER).contains(role)) {
            return user.getStation() != null
                    && user.getStation().getDistrict() != null
                    && report.getIncident().getDistrict() != null
                    && user.getStation().getDistrict().getId().equals(report.getIncident().getDistrict().getId());
        }
        return false;
    }

    private boolean canView(String role, User user, FireInvestigation report) {
        if (report == null || user == null) {
            return false;
        }
        if (Set.of("SUPER_ADMIN", OperationRole.CGF, OperationRole.COMMISSIONER_OPERATIONS, OperationRole.FIRE_INVESTIGATION_HOD).contains(role)) {
            return true;
        }
        if (OperationRole.DISTRICT_INVESTIGATION_OFFICER.equals(role)) {
            return report.getDio() != null && report.getDio().getId().equals(user.getId());
        }
        if (Set.of(OperationRole.DISTRICT_FIRE_OFFICER, OperationRole.REGIONAL_INVESTIGATION_OFFICER, OperationRole.REGIONAL_FIRE_OFFICER).contains(role)) {
            return sameScope(user, report, report.getIncident().getDistrict() != null ? report.getIncident().getDistrict().getRegion() : report.getIncident().getRegion());
        }
        return false;
    }

    private void ensureRole(User user, String role) throws IllegalAccessException {
        if (user == null || !role.equals(normalizeRole(user.getRole()))) {
            throw new IllegalAccessException("Only " + role + " can perform this action");
        }
    }

    private void log(FireInvestigation report, User approver, String level, String status, String comment) {
        InvestigationLog log = new InvestigationLog();
        log.setReport(report);
        log.setApprover(approver);
        log.setLevel(level);
        log.setStatus(status);
        log.setComment(comment);
        investigationLogRepository.save(log);
    }

    private String buildPdfHtml(FireInvestigation report, List<InvestigationLog> logs, List<InvestigationAttachment> attachments, String lang) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        boolean sw = "sw".equalsIgnoreCase(lang);
        StringBuilder historyRows = new StringBuilder();
        for (InvestigationLog log : logs) {
            historyRows.append("<tr>")
                    .append("<td>").append(escape(log.getLevel())).append("</td>")
                    .append("<td>").append(escape(log.getApprover().getFullName() != null ? log.getApprover().getFullName() : log.getApprover().getUsername())).append("</td>")
                    .append("<td>").append(escape(log.getStatus())).append("</td>")
                    .append("<td>").append(escape(log.getComment())).append("</td>")
                    .append("<td>").append(log.getTimestamp() == null ? "" : formatter.format(log.getTimestamp())).append("</td>")
                    .append("</tr>");
        }
        StringBuilder attachmentItems = new StringBuilder();
        for (InvestigationAttachment attachment : attachments) {
            attachmentItems.append("<li>")
                    .append(escape(attachment.getFileName()))
                    .append(" (").append(escape(attachment.getFileType())).append(")")
                    .append("</li>");
        }

        return """
                <table class="meta-table">
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                    <tr><td><strong>%s</strong></td><td>%s</td><td><strong>%s</strong></td><td>%s</td></tr>
                </table>
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                <div class="section-card">
                    <h2>%s</h2>
                    <p>%s</p>
                </div>
                <div class="section-card">
                    <h2>%s</h2>
                    <table class="data-table">
                        <thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead>
                        <tbody>%s</tbody>
                    </table>
                </div>
                <div class="section-card">
                    <h2>%s</h2>
                    <ul>%s</ul>
                </div>
                """.formatted(
                sw ? "Namba ya Uchunguzi" : "Investigation Number",
                escape(report.getInvestigationNumber()),
                sw ? "Tukio" : "Incident",
                escape(report.getIncident().getIncidentNumber()),
                sw ? "Hali" : "Status",
                escape(report.getStatus()),
                sw ? "Ngazi ya Sasa" : "Current Level",
                escape(report.getCurrentLevel()),
                sw ? "Maelezo ya Tukio" : "Incident Details",
                escape(report.getIncidentDetails()),
                sw ? "Uchambuzi wa Chanzo" : "Cause Analysis",
                escape(report.getCauseAnalysis()),
                sw ? "Kauli za Mashahidi" : "Witness Statements",
                escape(report.getWitnessStatements()),
                sw ? "Historia ya Idhini" : "Approval History",
                sw ? "Ngazi" : "Level",
                sw ? "Mthibitishaji" : "Approver",
                sw ? "Hali" : "Status",
                sw ? "Maoni" : "Comment",
                sw ? "Muda" : "Timestamp",
                historyRows,
                sw ? "Viambatanisho" : "Attachments",
                attachmentItems
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String safeComment(String comment, String fallback) {
        return comment == null || comment.isBlank() ? fallback : comment;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}
