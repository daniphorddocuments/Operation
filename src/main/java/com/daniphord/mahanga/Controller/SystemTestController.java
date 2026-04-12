package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.SystemTestReport;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.SystemTestService;
import com.daniphord.mahanga.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system-tests")
public class SystemTestController {

    private final SystemTestService systemTestService;
    private final UserRepository userRepository;

    public SystemTestController(SystemTestService systemTestService, UserRepository userRepository) {
        this.systemTestService = systemTestService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> reports(HttpSession session) {
        User currentUser = currentUser(session);
        if (!systemTestService.canRunTests(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can view system test reports"));
        }
        return ResponseEntity.ok(systemTestService.latestReports().stream().map(this::toView).toList());
    }

    @PostMapping("/run")
    public ResponseEntity<?> run(HttpSession session, HttpServletRequest request) {
        User currentUser = currentUser(session);
        if (!systemTestService.canRunTests(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can run system verification"));
        }
        return ResponseEntity.ok(toView(systemTestService.runManual(currentUser, RequestUtil.getClientIpAddress(request))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> report(@PathVariable Long id, HttpSession session) {
        User currentUser = currentUser(session);
        if (!systemTestService.canRunTests(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can view system test reports"));
        }
        SystemTestReport report = systemTestService.report(id);
        return ResponseEntity.ok(Map.of(
                "report", toView(report),
                "details", systemTestService.reportDetails(id)
        ));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> reportPdf(@PathVariable Long id, @RequestParam(name = "lang", defaultValue = "en") String lang, HttpSession session) {
        User currentUser = currentUser(session);
        if (!systemTestService.canRunTests(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can download system test reports"));
        }
        byte[] pdf = systemTestService.generatePdf(id, lang);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=system-test-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }

    private Map<String, Object> toView(SystemTestReport report) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", report.getId());
        view.put("reportNumber", report.getReportNumber());
        view.put("status", report.getStatus());
        view.put("triggerMode", report.getTriggerMode());
        view.put("startedAt", report.getStartedAt() == null ? "" : report.getStartedAt().toString());
        view.put("completedAt", report.getCompletedAt() == null ? "" : report.getCompletedAt().toString());
        view.put("summary", report.getSummary() == null ? "" : report.getSummary());
        view.put("passedChecks", report.getPassedChecks());
        view.put("warningChecks", report.getWarningChecks());
        view.put("failedChecks", report.getFailedChecks());
        view.put("totalChecks", report.getTotalChecks());
        return view;
    }
}
