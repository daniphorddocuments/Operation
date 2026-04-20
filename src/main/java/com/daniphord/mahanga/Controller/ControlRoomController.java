package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.AiRouteService;
import com.daniphord.mahanga.Service.CaptchaService;
import com.daniphord.mahanga.Service.ControlRoomService;
import com.daniphord.mahanga.Service.GeographyService;
import com.daniphord.mahanga.Service.NotificationService;
import com.daniphord.mahanga.Service.ReportCenterService;
import com.daniphord.mahanga.Service.RoleAccessService;
import com.daniphord.mahanga.Service.RoleResponsibilityService;
import com.daniphord.mahanga.Service.UserManualService;
import com.daniphord.mahanga.Service.VideoSessionService;
import com.daniphord.mahanga.Util.RateLimiter;
import com.daniphord.mahanga.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping
public class ControlRoomController {

    private static final Logger log = LoggerFactory.getLogger(ControlRoomController.class);
    private static final RateLimiter PUBLIC_RATE_LIMITER = new RateLimiter(5, 300, 900);

    private final ControlRoomService controlRoomService;
    private final CaptchaService captchaService;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;
    private final VideoSessionService videoSessionService;
    private final GeographyService geographyService;
    private final ReportCenterService reportCenterService;
    private final NotificationService notificationService;
    private final UserManualService userManualService;
    private final AiRouteService aiRouteService;

    public ControlRoomController(
            ControlRoomService controlRoomService,
            CaptchaService captchaService,
            UserRepository userRepository,
            RoleAccessService roleAccessService,
            VideoSessionService videoSessionService,
            GeographyService geographyService,
            ReportCenterService reportCenterService,
            NotificationService notificationService,
            UserManualService userManualService,
            AiRouteService aiRouteService
    ) {
        this.controlRoomService = controlRoomService;
        this.captchaService = captchaService;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
        this.videoSessionService = videoSessionService;
        this.geographyService = geographyService;
        this.reportCenterService = reportCenterService;
        this.notificationService = notificationService;
        this.userManualService = userManualService;
        this.aiRouteService = aiRouteService;
    }

    @GetMapping("/public/emergency/report")
    public String publicEmergencyPage(HttpSession session, Model model) {
        model.addAttribute("captchaQuestion", captchaService.currentQuestion(session));
        model.addAttribute("regions", geographyService.regionViews());
        return "public-emergency-report";
    }

    @GetMapping("/public/emergency/captcha")
    @ResponseBody
    public ResponseEntity<Map<String, String>> publicCaptcha(HttpSession session) {
        return ResponseEntity.ok(captchaService.issueChallenge(session));
    }

    @PostMapping("/public/emergency/report")
    public String submitPublicEmergencyReport(
            @Valid @ModelAttribute("call") EmergencyCall call,
            @RequestParam Long regionId,
            @RequestParam Long districtId,
            @RequestParam Long stationId,
            @RequestParam String captchaAnswer,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String ipAddress = RequestUtil.getClientIpAddress(request);
        if (PUBLIC_RATE_LIMITER.isRateLimited(ipAddress)) {
            log.warn("Public emergency report rate limited. ip={}", ipAddress);
            redirectAttributes.addFlashAttribute("error", "Too many emergency reports from this source. Please wait and try again.");
            return "redirect:/public/emergency/report";
        }
        if (!captchaService.verify(session, captchaAnswer)) {
            PUBLIC_RATE_LIMITER.recordAttempt(ipAddress);
            log.warn("Public emergency report failed captcha. ip={}", ipAddress);
            redirectAttributes.addFlashAttribute("error", "CAPTCHA verification failed. Please try again.");
            return "redirect:/public/emergency/report";
        }
        try {
            EmergencyCall saved = controlRoomService.logPublicReport(call, regionId, districtId, stationId);
            log.info("Public emergency report recorded. callId={}, ip={}, incidentType={}, stationId={}", saved.getId(), ipAddress, saved.getIncidentType(), stationId);
            return "redirect:/public/emergency/report/" + saved.getReportNumber() + "?token=" + saved.getPublicAccessToken();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/public/emergency/report";
        }
    }

    @PostMapping("/public/api/incidents")
    @ResponseBody
    public ResponseEntity<?> submitPublicEmergencyReportApi(
            @Valid @RequestBody EmergencyCall call,
            @RequestParam Long regionId,
            @RequestParam Long districtId,
            @RequestParam Long stationId,
            @RequestParam String captchaAnswer,
            HttpServletRequest request,
            HttpSession session
    ) {
        String ipAddress = RequestUtil.getClientIpAddress(request);
        if (PUBLIC_RATE_LIMITER.isRateLimited(ipAddress)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many emergency reports from this source. Please wait and try again."));
        }
        if (!captchaService.verify(session, captchaAnswer)) {
            PUBLIC_RATE_LIMITER.recordAttempt(ipAddress);
            return ResponseEntity.badRequest().body(Map.of("error", "CAPTCHA verification failed. Please try again."));
        }
        try {
            EmergencyCall saved = controlRoomService.logPublicReport(call, regionId, districtId, stationId);
            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "reportNumber", saved.getReportNumber(),
                    "token", saved.getPublicAccessToken(),
                    "status", saved.getStatus()
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/public/emergency/report/{reportNumber}")
    public String publicEmergencyCasePage(@PathVariable String reportNumber, @RequestParam String token, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        try {
            EmergencyCall call = controlRoomService.publicReport(reportNumber, token);
            model.addAttribute("captchaQuestion", captchaService.currentQuestion(session));
            model.addAttribute("regions", geographyService.regionViews());
            model.addAttribute("publicReport", call);
            model.addAttribute("publicRouteRecommendation", aiRouteService.previewForCall(call).orElse(null));
            model.addAttribute("publicMessages", controlRoomService.messages(call.getId()));
            model.addAttribute("publicVideoSessions", videoSessionService.sessionsForCall(call.getId()));
            return "public-emergency-report";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", "Report not found or access token is invalid.");
            return "redirect:/public/emergency/report";
        }
    }

    @GetMapping("/api/public/reports/{reportNumber}")
    @ResponseBody
    public ResponseEntity<?> publicReport(@PathVariable String reportNumber, @RequestParam String token) {
        try {
            EmergencyCall call = controlRoomService.publicReport(reportNumber, token);
            var routePreview = aiRouteService.previewForCall(call).orElse(null);
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("id", call.getId());
            payload.put("reportNumber", call.getReportNumber());
            payload.put("status", call.getStatus());
            payload.put("callerName", call.getCallerName() == null ? "" : call.getCallerName());
            payload.put("incidentType", call.getIncidentType() == null ? "" : call.getIncidentType());
            payload.put("station", call.getRoutedStation() != null ? call.getRoutedStation().getName() : "");
            payload.put("ward", call.getWard() == null ? "" : call.getWard());
            payload.put("village", call.getVillage() == null ? "" : call.getVillage());
            payload.put("roadSymbol", call.getRoadSymbol() == null ? "" : call.getRoadSymbol());
            payload.put("landmark", call.getLocationText() == null ? "" : call.getLocationText());
            payload.put("details", call.getDetails() == null ? "" : call.getDetails());
            payload.put("routeRecommendation", routePreview == null ? Map.of() : Map.of(
                    "stationId", routePreview.stationId(),
                    "stationName", routePreview.stationName(),
                    "roadDistanceKm", routePreview.roadDistanceKm(),
                    "etaMinutes", routePreview.etaMinutes(),
                    "embedUrl", routePreview.embedUrl(),
                    "directionsUrl", routePreview.directionsUrl(),
                    "directions", routePreview.directions()
            ));
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/public/api/reports/{reportNumber}")
    @ResponseBody
    public ResponseEntity<?> publicReportAlias(@PathVariable String reportNumber, @RequestParam String token) {
        return publicReport(reportNumber, token);
    }

    @GetMapping("/api/public/reports/{reportNumber}/messages")
    @ResponseBody
    public ResponseEntity<?> publicMessages(@PathVariable String reportNumber, @RequestParam String token) {
        try {
            EmergencyCall call = controlRoomService.publicReport(reportNumber, token);
            return ResponseEntity.ok(controlRoomService.messages(call.getId()).stream().map(message -> Map.of(
                    "id", message.getId(),
                    "senderType", message.getSenderType(),
                    "message", message.getMessage(),
                    "createdAt", message.getCreatedAt().toString()
            )).toList());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/public/api/reports/{reportNumber}/messages")
    @ResponseBody
    public ResponseEntity<?> publicMessagesAlias(@PathVariable String reportNumber, @RequestParam String token) {
        return publicMessages(reportNumber, token);
    }

    @PostMapping("/api/public/reports/{reportNumber}/messages")
    @ResponseBody
    public ResponseEntity<?> sendPublicMessage(@PathVariable String reportNumber, @RequestParam String token, @RequestBody Map<String, String> payload) {
        try {
            var message = controlRoomService.addPublicMessage(reportNumber, token, payload.get("message"));
            return ResponseEntity.ok(Map.of(
                    "id", message.getId(),
                    "senderType", message.getSenderType(),
                    "message", message.getMessage(),
                    "createdAt", message.getCreatedAt().toString()
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/public/api/reports/{reportNumber}/messages")
    @ResponseBody
    public ResponseEntity<?> sendPublicMessageAlias(@PathVariable String reportNumber, @RequestParam String token, @RequestBody Map<String, String> payload) {
        return sendPublicMessage(reportNumber, token, payload);
    }

    @PostMapping("/api/control-room/calls/{callId}/messages")
    @ResponseBody
    public ResponseEntity<?> sendControlRoomMessage(@PathVariable Long callId, @RequestBody Map<String, String> payload, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canAccessCallHandling(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            var message = controlRoomService.addControlRoomMessage(callId, currentUser, payload.get("message"));
            return ResponseEntity.ok(Map.of(
                    "id", message.getId(),
                    "senderType", message.getSenderType(),
                    "message", message.getMessage(),
                    "createdAt", message.getCreatedAt().toString()
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/api/control-room/calls/{callId}/messages")
    @ResponseBody
    public ResponseEntity<?> controlRoomMessages(@PathVariable Long callId, HttpSession session) {
        User currentUser = currentUser(session);
        try {
            EmergencyCall call = controlRoomService.findById(callId);
            if (currentUser == null || currentUser.getStation() == null || call.getRoutedStation() == null || !currentUser.getStation().getId().equals(call.getRoutedStation().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You cannot access this chat"));
            }
            return ResponseEntity.ok(controlRoomService.messages(callId).stream().map(message -> Map.of(
                    "id", message.getId(),
                    "senderType", message.getSenderType(),
                    "message", message.getMessage(),
                    "createdAt", message.getCreatedAt().toString()
            )).toList());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/control-room/dashboard")
    public String controlRoomDashboard(Model model, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canAccessControlRoomDashboard(currentUser)) {
            throw new IllegalStateException("Action not allowed for your role");
        }
        var visibleCalls = roleAccessService.visibleCalls(currentUser, controlRoomService.latestCalls());
        var availableReports = reportCenterService.availableReports(currentUser);
        EmergencyCall incomingCall = visibleCalls.stream()
                .filter(call -> "REPORTED".equalsIgnoreCase(call.getStatus()))
                .findFirst()
                .orElse(null);
        AiRouteService.RouteRecommendation priorityRoute = visibleCalls.stream()
                .filter(call -> call.getLatitude() != null && call.getLongitude() != null)
                .findFirst()
                .flatMap(aiRouteService::previewForCall)
                .orElse(null);
        model.addAttribute("callMetrics", controlRoomService.dashboardMetrics(visibleCalls));
        model.addAttribute("calls", visibleCalls);
        model.addAttribute("incomingCall", incomingCall);
        model.addAttribute("priorityRoute", priorityRoute);
        model.addAttribute("scopeMap", geographyService.scopeMap(currentUser));
        model.addAttribute("roleWorkspace", roleAccessService.workspaceDefinition(currentUser));
        model.addAttribute("workspaceSidebar", roleAccessService.sidebar(
                currentUser,
                RoleResponsibilityService.WorkspacePage.CONTROL_ROOM,
                !availableReports.isEmpty()
        ));
        model.addAttribute("loggedInUsername", session.getAttribute("username"));
        model.addAttribute("loggedInRole", session.getAttribute("role"));
        model.addAttribute("currentUserId", currentUser != null ? currentUser.getId() : null);
        model.addAttribute("canAccessCallHandling", roleAccessService.canAccessCallHandling(currentUser));
        model.addAttribute("canStartLiveVideo", roleAccessService.canPublishLiveVideo(currentUser));
        model.addAttribute("canAccessOperationsDashboard", roleAccessService.canAccessOperationsDashboard(currentUser));
        model.addAttribute("canAccessControlRoomDashboard", roleAccessService.canAccessControlRoomDashboard(currentUser));
        model.addAttribute("currentUserStationId", currentUser != null && currentUser.getStation() != null ? currentUser.getStation().getId() : null);
        model.addAttribute("currentDistrictId", currentUser != null && currentUser.getStation() != null && currentUser.getStation().getDistrict() != null
                ? currentUser.getStation().getDistrict().getId()
                : null);
        model.addAttribute("currentRegionId", currentUser != null && currentUser.getStation() != null && currentUser.getStation().getDistrict() != null
                && currentUser.getStation().getDistrict().getRegion() != null
                ? currentUser.getStation().getDistrict().getRegion().getId()
                : null);
        model.addAttribute("controlRoomUserManual", userManualService.controlRoomDashboardManual((String) session.getAttribute("role")));
        model.addAttribute("videoSessions", videoSessionService.latestSessions());
        model.addAttribute("videoMetrics", videoSessionService.metrics());
        model.addAttribute("canViewReports", roleAccessService.canViewReports(currentUser));
        model.addAttribute("availableReports", availableReports);
        Long userId = currentUser != null ? currentUser.getId() : null;
        model.addAttribute("notifications", notificationService.latestNotifications(userId).stream()
                .map(notification -> Map.of(
                        "title", notification.getTitle(),
                        "message", notification.getMessage(),
                        "createdAt", notification.getCreatedAt() == null ? "" : notification.getCreatedAt().toString()
                ))
                .toList());
        model.addAttribute("notificationUnreadCount", notificationService.unreadCount(userId));
        return "control-room-dashboard";
    }

    @GetMapping("/api/control-room/calls")
    @ResponseBody
    public ResponseEntity<?> latestCalls(HttpSession session) {
        User currentUser = currentUser(session);
        return ResponseEntity.ok(roleAccessService.visibleCalls(currentUser, controlRoomService.latestCalls()));
    }

    @PostMapping("/api/control-room/calls/{callId}/accept")
    @ResponseBody
    public ResponseEntity<?> acceptCall(@PathVariable Long callId, @RequestBody(required = false) Map<String, Object> payload, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canAccessCallHandling(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Action not allowed for your role"));
        }
        Long stationId = null;
        String recordingUrl = null;
        if (payload != null) {
            Object stationIdValue = payload.get("stationId");
            if (stationIdValue instanceof Number number) {
                stationId = number.longValue();
            }
            recordingUrl = payload.get("recordingUrl") == null ? null : payload.get("recordingUrl").toString();
        }
        try {
            return ResponseEntity.ok(controlRoomService.acceptCall(callId, stationId, recordingUrl, currentUser));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/api/control-room/calls/{callId}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectCall(@PathVariable Long callId, @RequestBody(required = false) Map<String, String> payload, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canAccessCallHandling(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            String reason = payload == null ? null : payload.get("reason");
            return ResponseEntity.ok(controlRoomService.rejectCall(callId, reason, currentUser));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/api/control-room/calls/{callId}/route")
    @ResponseBody
    public ResponseEntity<?> routeCall(
            @PathVariable Long callId,
            @RequestBody Map<String, Object> payload,
            HttpSession session
    ) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canDispatchTeams(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Action not allowed for your role"));
        }
        Object stationId = payload.get("stationId");
        if (!(stationId instanceof Number number)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "stationId is required"));
        }
        String recordingUrl = payload.get("recordingUrl") == null ? null : payload.get("recordingUrl").toString();
        return ResponseEntity.ok(controlRoomService.routeCall(callId, number.longValue(), recordingUrl, currentUser));
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}
