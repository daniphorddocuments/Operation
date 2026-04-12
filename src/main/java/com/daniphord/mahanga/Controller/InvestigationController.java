package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.FireInvestigation;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.InvestigationWorkflowService;
import com.daniphord.mahanga.Service.RoleAccessService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {

    private final InvestigationWorkflowService investigationWorkflowService;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;

    public InvestigationController(
            InvestigationWorkflowService investigationWorkflowService,
            UserRepository userRepository,
            RoleAccessService roleAccessService
    ) {
        this.investigationWorkflowService = investigationWorkflowService;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping
    public ResponseEntity<?> listInvestigations(HttpSession session) {
        User currentUser = currentUser(session);
        if (!investigationWorkflowService.hasInvestigationAccess(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(investigationWorkflowService.visibleInvestigations(currentUser).stream()
                .map(this::toView)
                .toList());
    }

    @GetMapping("/eligible-incidents")
    public ResponseEntity<?> eligibleIncidents(HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canSubmitInvestigations(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(investigationWorkflowService.eligibleIncidents(currentUser).stream()
                .map(incident -> Map.of(
                        "id", incident.getId(),
                        "incidentNumber", incident.getIncidentNumber(),
                        "incidentType", incident.getIncidentType(),
                        "status", incident.getStatus(),
                        "location", incident.getDistrict() != null ? incident.getDistrict().getName() : (incident.getVillage() == null ? "" : incident.getVillage())
                ))
                .toList());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> startInvestigation(
            @RequestParam Long incidentId,
            @RequestParam String incidentDetails,
            @RequestParam String causeAnalysis,
            @RequestParam String witnessStatements,
            @RequestParam(name = "attachments", required = false) List<MultipartFile> attachments,
            HttpSession session
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canSubmitInvestigations(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            FireInvestigation report = investigationWorkflowService.startInvestigation(
                    incidentId,
                    incidentDetails,
                    causeAnalysis,
                    witnessStatements,
                    attachments,
                    currentUser
            );
            return ResponseEntity.ok(toView(report));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/{reportId}/decision")
    public ResponseEntity<?> reviewInvestigation(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> payload,
            HttpSession session
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canApproveInvestigations(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            boolean approve = Boolean.TRUE.equals(payload.get("approve"));
            String comment = payload.get("comment") == null ? null : payload.get("comment").toString();
            FireInvestigation report = investigationWorkflowService.review(reportId, approve, comment, currentUser);
            return ResponseEntity.ok(toView(report));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/{reportId}/logs")
    public ResponseEntity<?> logs(@PathVariable Long reportId, HttpSession session) {
        if (!canViewReport(reportId, currentUser(session))) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(investigationWorkflowService.logs(reportId).stream()
                .map(log -> Map.of(
                        "id", log.getId(),
                        "level", log.getLevel(),
                        "status", log.getStatus(),
                        "comment", log.getComment() == null ? "" : log.getComment(),
                        "approver", log.getApprover().getFullName() != null ? log.getApprover().getFullName() : log.getApprover().getUsername(),
                        "timestamp", log.getTimestamp() == null ? "" : log.getTimestamp().toString()
                ))
                .toList());
    }

    @GetMapping("/{reportId}/attachments")
    public ResponseEntity<?> attachments(@PathVariable Long reportId, HttpSession session) {
        if (!canViewReport(reportId, currentUser(session))) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(investigationWorkflowService.attachments(reportId).stream()
                .map(attachment -> Map.of(
                        "id", attachment.getId(),
                        "fileName", attachment.getFileName(),
                        "fileType", attachment.getFileType(),
                        "uploadedAt", attachment.getUploadedAt() == null ? "" : attachment.getUploadedAt().toString()
                ))
                .toList());
    }

    @GetMapping("/{reportId}/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable Long reportId, @RequestParam(name = "lang", defaultValue = "en") String lang, HttpSession session) {
        User currentUser = currentUser(session);
        if (!canViewReport(reportId, currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            byte[] pdf = investigationWorkflowService.generatePdf(reportId, currentUser, lang);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=investigation-" + reportId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new ByteArrayResource(pdf));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }

    private boolean canViewReport(Long reportId, User currentUser) {
        return investigationWorkflowService.visibleInvestigations(currentUser).stream()
                .anyMatch(report -> report.getId().equals(reportId));
    }

    private Map<String, Object> toView(FireInvestigation report) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", report.getId());
        view.put("investigationNumber", report.getInvestigationNumber());
        view.put("status", report.getStatus());
        view.put("currentLevel", report.getCurrentLevel());
        view.put("incidentId", report.getIncident().getId());
        view.put("incidentNumber", report.getIncident().getIncidentNumber());
        view.put("incidentType", report.getIncident().getIncidentType());
        view.put("location", report.getIncident().getDistrict() != null ? report.getIncident().getDistrict().getName() : (report.getIncident().getVillage() == null ? "" : report.getIncident().getVillage()));
        view.put("incidentDetails", report.getIncidentDetails() == null ? "" : report.getIncidentDetails());
        view.put("causeAnalysis", report.getCauseAnalysis() == null ? "" : report.getCauseAnalysis());
        view.put("witnessStatements", report.getWitnessStatements() == null ? "" : report.getWitnessStatements());
        view.put("dioName", report.getDio() == null ? "" : (report.getDio().getFullName() != null ? report.getDio().getFullName() : report.getDio().getUsername()));
        view.put("updatedAt", report.getUpdatedAt() == null ? "" : report.getUpdatedAt().toString());
        return view;
    }
}
