package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.ReportCenterService;
import com.daniphord.mahanga.Service.RoleAccessService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportCenterController {

    private final ReportCenterService reportCenterService;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;

    public ReportCenterController(ReportCenterService reportCenterService, UserRepository userRepository, RoleAccessService roleAccessService) {
        this.reportCenterService = reportCenterService;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canViewReports(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(reportCenterService.availableReports(currentUser));
    }

    @GetMapping("/{key}/pdf")
    public ResponseEntity<?> download(
            @PathVariable String key,
            @RequestParam(name = "lang", defaultValue = "en") String lang,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            HttpSession session
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canViewReports(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            byte[] pdf = reportCenterService.generateReport(key, currentUser, lang, from, to);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key + "-" + lang + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new ByteArrayResource(pdf));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> metadata(@PathVariable String key, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canViewReports(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(reportCenterService.availableReports(currentUser).stream()
                .filter(item -> key.equals(item.get("key")))
                .findFirst()
                .orElse(Map.of("error", "Report not found")));
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }
}
