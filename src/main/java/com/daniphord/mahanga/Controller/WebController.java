package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.AuditService;
import com.daniphord.mahanga.Service.DashboardDefinitionService;
import com.daniphord.mahanga.Service.LoginSecurityService;
import com.daniphord.mahanga.Service.UserManualService;
import com.daniphord.mahanga.Service.UserService;
import com.daniphord.mahanga.Util.InputValidator;
import com.daniphord.mahanga.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Controller
public class WebController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final DashboardDefinitionService dashboardDefinitionService;
    private final UserManualService userManualService;
    private final UserService userService;
    private final LoginSecurityService loginSecurityService;
    private final int sessionTimeoutSeconds;

    public WebController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            DashboardDefinitionService dashboardDefinitionService,
            UserManualService userManualService,
            UserService userService,
            LoginSecurityService loginSecurityService,
            @Value("${froms.security.session-timeout-seconds:900}") int sessionTimeoutSeconds
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.dashboardDefinitionService = dashboardDefinitionService;
        this.userManualService = userManualService;
        this.userService = userService;
        this.loginSecurityService = loginSecurityService;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    @GetMapping("/")
    public String landingPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) {
            return "redirect:" + dashboardForRole((String) session.getAttribute("role"));
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "session", required = false) String sessionState,
            HttpSession session,
            Model model
    ) {
        if (session.getAttribute("userId") != null) {
            return "redirect:" + dashboardForRole((String) session.getAttribute("role"));
        }
        if ("expired".equalsIgnoreCase(sessionState)) {
            model.addAttribute("error", "Your session expired because of inactivity. Sign in again.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String ipAddress = RequestUtil.getClientIpAddress(request);
        if (loginSecurityService.isIpRateLimited(ipAddress)) {
            redirectAttributes.addFlashAttribute("error", loginSecurityService.rateLimitMessage(ipAddress));
            return "redirect:/login";
        }

        if (!InputValidator.isValidUsername(username)) {
            loginSecurityService.recordAnonymousFailure(ipAddress);
            redirectAttributes.addFlashAttribute("error", "Incorrect username or password.");
            return "redirect:/login";
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            loginSecurityService.recordAnonymousFailure(ipAddress);
            redirectAttributes.addFlashAttribute("error", "Incorrect username or password.");
            return "redirect:/login";
        }

        User user = userOpt.get();
        if (!Boolean.TRUE.equals(user.getActive())) {
            redirectAttributes.addFlashAttribute("error", "Your account is inactive. Contact command administration.");
            auditService.logFailure(user, "LOGIN_ATTEMPT", "Inactive account login attempt", ipAddress, null);
            return "redirect:/login";
        }

        if (loginSecurityService.isAccountLocked(user)) {
            redirectAttributes.addFlashAttribute("error", loginSecurityService.lockedMessage(user));
            auditService.logFailure(user, "LOGIN_ATTEMPT", "Login blocked because account is locked", ipAddress, null);
            return "redirect:/login";
        }

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", loginSecurityService.recordFailedLogin(user, ipAddress));
            return "redirect:/login";
        }

        user = loginSecurityService.registerSuccessfulLogin(user, ipAddress);

        session.setMaxInactiveInterval(sessionTimeoutSeconds);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", normalizeRole(user.getRole()));
        session.setAttribute("stationId", user.getStation() != null ? user.getStation().getId() : null);

        auditService.logAction(user, "LOGIN", "User signed in to FROMS", "User", user.getId(), ipAddress);
        return "redirect:" + dashboardForRole(user.getRole());
    }

    @GetMapping("/logout")
    public String logout(@RequestParam(value = "reason", required = false) String reason, HttpSession session) {
        session.invalidate();
        if ("expired".equalsIgnoreCase(reason)) {
            return "redirect:/login?session=expired";
        }
        return "redirect:/login";
    }

    @PostMapping("/change-language")
    public String changeLanguage(@RequestParam String lang, HttpSession session, HttpServletRequest request) {
        if ("sw".equalsIgnoreCase(lang) || "en".equalsIgnoreCase(lang)) {
            session.setAttribute("currentLang", lang.toLowerCase());
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer == null || referer.isBlank() ? "/" : referer);
    }

    @PostMapping("/api/me/language")
    public org.springframework.http.ResponseEntity<?> apiChangeLanguage(@org.springframework.web.bind.annotation.RequestBody Map<String, String> body, HttpSession session) {
        String lang = body.get("lang");
        if ("en".equalsIgnoreCase(lang) || "sw".equalsIgnoreCase(lang)) {
            session.setAttribute("currentLang", lang.toLowerCase());
            return org.springframework.http.ResponseEntity.ok(Map.of("status", "ok", "lang", lang.toLowerCase()));
        }
        return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", "Invalid language"));
    }

    @PostMapping("/api/session/keepalive")
    public ResponseEntity<?> keepSessionAlive(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No active session"));
        }
        session.setMaxInactiveInterval(sessionTimeoutSeconds);
        session.setAttribute("lastKeepAliveAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/api/me/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload, HttpSession session, HttpServletRequest request) {
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof Long id)) {
            return ResponseEntity.status(401).body(Map.of("error", "No active session"));
        }
        try {
            userService.changePassword(id, payload.get("currentPassword"), payload.get("newPassword"));
            auditService.logAction(
                    userRepository.findById(id).orElse(null),
                    "PASSWORD_CHANGED",
                    "User changed account password",
                    "User",
                    id,
                    RequestUtil.getClientIpAddress(request)
            );
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Password updated successfully"));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    private String dashboardForRole(String role) {
        return dashboardDefinitionService.routeFor(role);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}
