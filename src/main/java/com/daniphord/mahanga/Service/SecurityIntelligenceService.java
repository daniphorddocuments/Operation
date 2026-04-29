package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.AuditLog;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Model.UserDeviceSession;
import com.daniphord.mahanga.Repositories.AuditLogRepository;
import com.daniphord.mahanga.Repositories.UserDeviceSessionRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import com.daniphord.mahanga.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SecurityIntelligenceService {

    public static final String SESSION_SECURITY_ID = "securitySessionId";
    public static final String SESSION_REFRESH_TOKEN = "securityRefreshToken";
    public static final String SESSION_INCIDENT_MODE = "incidentMode";
    public static final String SESSION_INCIDENT_NUMBER = "incidentNumber";

    private final UserDeviceSessionRepository userDeviceSessionRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final int sessionTimeoutSeconds;
    private final int idleWarningSeconds;
    private final String emergencyNumber;

    public SecurityIntelligenceService(
            UserDeviceSessionRepository userDeviceSessionRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            NotificationService notificationService,
            AuditService auditService,
            @Value("${froms.security.session-timeout-seconds:900}") int sessionTimeoutSeconds,
            @Value("${froms.security.idle-warning-seconds:90}") int idleWarningSeconds,
            @Value("${froms.dispatch.emergency-number:114}") String emergencyNumber
    ) {
        this.userDeviceSessionRepository = userDeviceSessionRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
        this.idleWarningSeconds = idleWarningSeconds;
        this.emergencyNumber = emergencyNumber;
    }

    public LoginSessionRegistration registerLogin(
            User user,
            HttpServletRequest request,
            HttpSession session,
            String clientFingerprint,
            boolean autofillDetected,
            boolean typedPassword,
            int recentFailedAttempts
    ) {
        String userAgent = safe(RequestUtil.getUserAgent(request));
        String browser = browserLabel(userAgent);
        String operatingSystem = operatingSystemLabel(userAgent);
        String ipAddress = safe(RequestUtil.getClientIpAddress(request));
        String fingerprintHash = fingerprintHash(clientFingerprint, browser, operatingSystem, userAgent);
        boolean knownDevice = userDeviceSessionRepository.existsByUserIdAndDeviceFingerprintHash(user.getId(), fingerprintHash);
        List<UserDeviceSession> activeSessions = userDeviceSessionRepository.findByUserIdAndActiveTrueOrderByLastSeenAtDesc(user.getId());
        boolean multipleDeviceSessions = activeSessions.stream()
                .anyMatch(existing -> !safe(existing.getDeviceFingerprintHash()).equals(fingerprintHash));

        int riskScore = 0;
        if (!knownDevice) {
            riskScore += 20;
        }
        if (multipleDeviceSessions) {
            riskScore += 30;
        }
        if (recentFailedAttempts > 0) {
            riskScore += 40;
        }
        if (autofillDetected) {
            riskScore += 10;
        }
        if (activeSessions.stream().noneMatch(existing -> ipAddress.equalsIgnoreCase(safe(existing.getIpAddress())))) {
            riskScore += 20;
        }

        String riskLevel = riskLevel(riskScore);
        UserDeviceSession deviceSession = new UserDeviceSession();
        deviceSession.setUser(user);
        deviceSession.setSessionId(session.getId());
        deviceSession.setRefreshToken(UUID.randomUUID().toString());
        deviceSession.setDeviceFingerprintHash(fingerprintHash);
        deviceSession.setBrowser(browser);
        deviceSession.setOperatingSystem(operatingSystem);
        deviceSession.setUserAgent(trim(userAgent, 500));
        deviceSession.setIpAddress(trim(ipAddress, 120));
        deviceSession.setLocationSignature(trim(ipAddress, 120));
        deviceSession.setAutofillDetected(autofillDetected);
        deviceSession.setTypedPassword(typedPassword);
        deviceSession.setNewDevice(!knownDevice);
        deviceSession.setMultipleDeviceFlag(multipleDeviceSessions);
        deviceSession.setRiskScore(riskScore);
        deviceSession.setRiskLevel(riskLevel);
        deviceSession.setPresenceStatus("ACTIVE");
        deviceSession.setLastActivityAt(LocalDateTime.now());
        deviceSession.setLastSeenAt(LocalDateTime.now());
        deviceSession.setExpiresAt(LocalDateTime.now().plusSeconds(sessionTimeoutSeconds));
        userDeviceSessionRepository.save(deviceSession);

        session.setAttribute(SESSION_SECURITY_ID, deviceSession.getSessionId());
        session.setAttribute(SESSION_REFRESH_TOKEN, deviceSession.getRefreshToken());
        session.setAttribute(SESSION_INCIDENT_MODE, false);
        session.setAttribute(SESSION_INCIDENT_NUMBER, null);

        if (!knownDevice) {
            notifyAdmins("New device login", user.getUsername() + " signed in from a new " + browser + " / " + operatingSystem + " device.", "/dashboard#security-command-center");
            auditService.logAction(user, "NEW_DEVICE_LOGIN", "New device login detected", "UserDeviceSession", deviceSession.getId(), ipAddress);
        }
        if (multipleDeviceSessions) {
            notifyAdmins("Multi-device session", user.getUsername() + " has multiple active device sessions.", "/dashboard#security-command-center");
            auditService.logAction(user, "MULTI_DEVICE_SESSION", "Multiple simultaneous device sessions detected", "UserDeviceSession", deviceSession.getId(), ipAddress);
        }
        if ("MEDIUM".equals(riskLevel) || "HIGH".equals(riskLevel)) {
            notifyAdmins("Security risk alert", user.getUsername() + " signed in with " + riskLevel + " risk score " + riskScore + ".", "/dashboard#security-command-center");
        }

        boolean requiresSecondaryVerification = "HIGH".equals(riskLevel) && !OperationRole.isEmergencyOperatorRole(user.getRole());
        return new LoginSessionRegistration(deviceSession, requiresSecondaryVerification);
    }

    public void registerFailedLogin(User user, HttpServletRequest request, String clientFingerprint, boolean autofillDetected, int failedAttempts, boolean lockApplied, boolean lockExempt) {
        if (user == null) {
            return;
        }
        String ipAddress = safe(RequestUtil.getClientIpAddress(request));
        String fingerprintHash = fingerprintHash(clientFingerprint, browserLabel(RequestUtil.getUserAgent(request)), operatingSystemLabel(RequestUtil.getUserAgent(request)), RequestUtil.getUserAgent(request));
        String details = "failedAttempts=" + failedAttempts
                + ", lockApplied=" + lockApplied
                + ", lockExempt=" + lockExempt
                + ", autofillDetected=" + autofillDetected
                + ", device=" + trim(fingerprintHash, 24);
        auditService.logFailure(user, "FAILED_LOGIN_SECURITY", "Authentication failure tracked by security intelligence", ipAddress, details);
        if (failedAttempts >= 3) {
            notifyAdmins("Failed login cluster", user.getUsername() + " has " + failedAttempts + " failed login attempts from " + ipAddress + ".", "/dashboard#security-command-center");
        }
    }

    public Map<String, Object> keepAlive(HttpSession session, String refreshToken) {
        UserDeviceSession deviceSession = currentDeviceSession(session);
        if (deviceSession == null || !safe(deviceSession.getRefreshToken()).equals(safe(refreshToken))) {
            return Map.of("status", "denied");
        }
        session.setMaxInactiveInterval(sessionTimeoutSeconds);
        deviceSession.setExpiresAt(LocalDateTime.now().plusSeconds(sessionTimeoutSeconds));
        deviceSession.setLastSeenAt(LocalDateTime.now());
        userDeviceSessionRepository.save(deviceSession);
        return Map.of(
                "status", "ok",
                "expiresAt", deviceSession.getExpiresAt().toString(),
                "presenceStatus", deviceSession.getPresenceStatus(),
                "emergencyMode", Boolean.TRUE.equals(deviceSession.getEmergencyMode())
        );
    }

    public Map<String, Object> updatePresence(HttpSession session, String refreshToken, Long idleSeconds, boolean tabVisible, boolean interacting, boolean warningShown) {
        UserDeviceSession deviceSession = currentDeviceSession(session);
        if (deviceSession == null || !safe(deviceSession.getRefreshToken()).equals(safe(refreshToken))) {
            return Map.of("status", "denied");
        }
        boolean emergencyMode = Boolean.TRUE.equals(session.getAttribute(SESSION_INCIDENT_MODE));
        if (interacting) {
            deviceSession.setLastActivityAt(LocalDateTime.now());
        }
        deviceSession.setEmergencyMode(emergencyMode);
        deviceSession.setActiveIncidentNumber(session.getAttribute(SESSION_INCIDENT_NUMBER) == null ? null : String.valueOf(session.getAttribute(SESSION_INCIDENT_NUMBER)));
        deviceSession.setLastSeenAt(LocalDateTime.now());
        deviceSession.setPresenceStatus(resolvePresenceStatus(emergencyMode, tabVisible, idleSeconds));
        if (interacting || emergencyMode) {
            session.setMaxInactiveInterval(sessionTimeoutSeconds);
            deviceSession.setExpiresAt(LocalDateTime.now().plusSeconds(sessionTimeoutSeconds));
        }
        userDeviceSessionRepository.save(deviceSession);
        return Map.of(
                "status", "ok",
                "presenceStatus", deviceSession.getPresenceStatus(),
                "warningThresholdSeconds", idleWarningSeconds,
                "warningShown", warningShown,
                "emergencyMode", emergencyMode,
                "expiresAt", deviceSession.getExpiresAt().toString()
        );
    }

    public Map<String, Object> setIncidentMode(HttpSession session, String refreshToken, boolean enabled, String incidentNumber) {
        UserDeviceSession deviceSession = currentDeviceSession(session);
        if (deviceSession == null || !safe(deviceSession.getRefreshToken()).equals(safe(refreshToken))) {
            return Map.of("status", "denied");
        }
        session.setAttribute(SESSION_INCIDENT_MODE, enabled);
        session.setAttribute(SESSION_INCIDENT_NUMBER, safe(incidentNumber).isBlank() ? null : incidentNumber.trim());
        deviceSession.setEmergencyMode(enabled);
        deviceSession.setActiveIncidentNumber(safe(incidentNumber).isBlank() ? null : incidentNumber.trim());
        deviceSession.setPresenceStatus(enabled ? "ON_MISSION" : "ACTIVE");
        deviceSession.setLastActivityAt(LocalDateTime.now());
        deviceSession.setLastSeenAt(LocalDateTime.now());
        deviceSession.setExpiresAt(LocalDateTime.now().plusSeconds(sessionTimeoutSeconds));
        userDeviceSessionRepository.save(deviceSession);
        return Map.of(
                "status", "ok",
                "incidentMode", enabled,
                "incidentNumber", deviceSession.getActiveIncidentNumber(),
                "presenceStatus", deviceSession.getPresenceStatus()
        );
    }

    public void markLoggedOut(HttpSession session, String reason) {
        UserDeviceSession deviceSession = currentDeviceSession(session);
        if (deviceSession == null) {
            return;
        }
        deviceSession.setActive(false);
        deviceSession.setEndedAt(LocalDateTime.now());
        deviceSession.setLogoutReason(trim(reason, 120));
        userDeviceSessionRepository.save(deviceSession);
    }

    public Map<String, Object> sessionState(HttpSession session) {
        UserDeviceSession deviceSession = currentDeviceSession(session);
        if (deviceSession == null) {
            return Map.of();
        }
        return Map.of(
                "refreshToken", deviceSession.getRefreshToken(),
                "presenceStatus", safe(deviceSession.getPresenceStatus()),
                "riskLevel", safe(deviceSession.getRiskLevel()),
                "riskScore", deviceSession.getRiskScore() == null ? 0 : deviceSession.getRiskScore(),
                "idleWarningSeconds", idleWarningSeconds,
                "sessionTimeoutSeconds", sessionTimeoutSeconds,
                "incidentMode", Boolean.TRUE.equals(deviceSession.getEmergencyMode()),
                "incidentNumber", safe(deviceSession.getActiveIncidentNumber()),
                "emergencyNumber", emergencyNumber
        );
    }

    public Map<String, Object> adminOverview() {
        List<UserDeviceSession> activeSessions = userDeviceSessionRepository.findByActiveTrueOrderByLastSeenAtDesc();
        List<Map<String, Object>> activeUsers = activeSessions.stream()
                .map(session -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("username", session.getUser().getUsername());
                    row.put("role", session.getUser().getRole());
                    row.put("presenceStatus", session.getPresenceStatus());
                    row.put("browser", safe(session.getBrowser()));
                    row.put("operatingSystem", safe(session.getOperatingSystem()));
                    row.put("ipAddress", safe(session.getIpAddress()));
                    row.put("riskLevel", safe(session.getRiskLevel()));
                    row.put("riskScore", session.getRiskScore());
                    row.put("newDevice", Boolean.TRUE.equals(session.getNewDevice()));
                    row.put("multiDevice", Boolean.TRUE.equals(session.getMultipleDeviceFlag()));
                    row.put("autofillDetected", Boolean.TRUE.equals(session.getAutofillDetected()));
                    row.put("incidentMode", Boolean.TRUE.equals(session.getEmergencyMode()));
                    row.put("incidentNumber", safe(session.getActiveIncidentNumber()));
                    row.put("lastActivityAt", session.getLastActivityAt() == null ? "" : session.getLastActivityAt().toString());
                    row.put("loginAt", session.getLoginAt() == null ? "" : session.getLoginAt().toString());
                    return row;
                })
                .toList();
        List<Map<String, Object>> alerts = activeSessions.stream()
                .filter(session -> Boolean.TRUE.equals(session.getNewDevice())
                        || Boolean.TRUE.equals(session.getMultipleDeviceFlag())
                        || "MEDIUM".equalsIgnoreCase(session.getRiskLevel())
                        || "HIGH".equalsIgnoreCase(session.getRiskLevel()))
                .map(session -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("username", session.getUser().getUsername());
                    row.put("alert", Boolean.TRUE.equals(session.getNewDevice()) ? "New device login"
                            : Boolean.TRUE.equals(session.getMultipleDeviceFlag()) ? "Multiple device sessions"
                            : "Elevated risk session");
                    row.put("riskLevel", safe(session.getRiskLevel()));
                    row.put("riskScore", session.getRiskScore());
                    row.put("lastSeenAt", session.getLastSeenAt() == null ? "" : session.getLastSeenAt().toString());
                    return row;
                })
                .toList();
        List<Map<String, Object>> failedClusters = auditLogRepository.findByAction("FAILED_LOGIN_SECURITY").stream()
                .limit(20)
                .map(log -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("username", log.getUser() == null ? "" : log.getUser().getUsername());
                    row.put("ipAddress", safe(log.getIpAddress()));
                    row.put("details", safe(log.getDetails()));
                    row.put("timestamp", log.getTimestamp() == null ? "" : log.getTimestamp().toString());
                    return row;
                })
                .toList();
        List<Map<String, Object>> loginHistory = auditLogRepository.findByAction("LOGIN").stream()
                .sorted((left, right) -> safe(right.getTimestamp()).compareTo(safe(left.getTimestamp())))
                .limit(25)
                .map(this::loginHistoryRow)
                .toList();
        return Map.of(
                "summary", Map.of(
                        "activeUsers", activeSessions.size(),
                        "mediumOrHighRisk", activeSessions.stream().filter(session -> !"LOW".equalsIgnoreCase(session.getRiskLevel())).count(),
                        "onMission", activeSessions.stream().filter(session -> "ON_MISSION".equalsIgnoreCase(session.getPresenceStatus())).count(),
                        "failedLoginClusters", failedClusters.size()
                ),
                "activeUsers", activeUsers,
                "alerts", alerts,
                "failedLoginClusters", failedClusters,
                "loginHistory", loginHistory
        );
    }

    private Map<String, Object> loginHistoryRow(AuditLog log) {
        return Map.of(
                "username", log.getUser() == null ? "" : log.getUser().getUsername(),
                "ipAddress", safe(log.getIpAddress()),
                "status", safe(log.getStatus()),
                "description", safe(log.getDescription()),
                "timestamp", log.getTimestamp() == null ? "" : log.getTimestamp().toString()
        );
    }

    private void notifyAdmins(String title, String message, String actionUrl) {
        List<User> admins = userRepository.findByRoleIn(List.of(OperationRole.SUPER_ADMIN, OperationRole.ADMIN));
        notificationService.notifyUsers(admins, title, message, actionUrl);
    }

    private UserDeviceSession currentDeviceSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object sessionId = session.getAttribute(SESSION_SECURITY_ID);
        if (sessionId == null) {
            return null;
        }
        Optional<UserDeviceSession> deviceSession = userDeviceSessionRepository.findBySessionId(String.valueOf(sessionId));
        return deviceSession.orElse(null);
    }

    private String resolvePresenceStatus(boolean emergencyMode, boolean tabVisible, Long idleSeconds) {
        if (emergencyMode) {
            return "ON_MISSION";
        }
        long idle = idleSeconds == null ? 0L : Math.max(0L, idleSeconds);
        if (!tabVisible || idle >= idleWarningSeconds) {
            return "IDLE";
        }
        return "ACTIVE";
    }

    private String riskLevel(int score) {
        if (score >= 60) {
            return "HIGH";
        }
        if (score >= 30) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String fingerprintHash(String clientFingerprint, String browser, String operatingSystem, String userAgent) {
        return sha256Hex(safe(clientFingerprint) + "|" + safe(browser) + "|" + safe(operatingSystem) + "|" + safe(userAgent));
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String browserLabel(String userAgent) {
        String normalized = safe(userAgent).toLowerCase();
        if (normalized.contains("edg/")) {
            return "Edge";
        }
        if (normalized.contains("chrome/")) {
            return "Chrome";
        }
        if (normalized.contains("firefox/")) {
            return "Firefox";
        }
        if (normalized.contains("safari/") && !normalized.contains("chrome/")) {
            return "Safari";
        }
        return "Unknown Browser";
    }

    private String operatingSystemLabel(String userAgent) {
        String normalized = safe(userAgent).toLowerCase();
        if (normalized.contains("windows")) {
            return "Windows";
        }
        if (normalized.contains("android")) {
            return "Android";
        }
        if (normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ios")) {
            return "iOS";
        }
        if (normalized.contains("mac os")) {
            return "macOS";
        }
        if (normalized.contains("linux")) {
            return "Linux";
        }
        return "Unknown OS";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private LocalDateTime safe(LocalDateTime value) {
        return value == null ? LocalDateTime.MIN : value;
    }

    private String trim(String value, int maxLength) {
        String safeValue = safe(value);
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength);
    }

    public record LoginSessionRegistration(UserDeviceSession session, boolean secondaryVerificationRequired) {
    }
}
