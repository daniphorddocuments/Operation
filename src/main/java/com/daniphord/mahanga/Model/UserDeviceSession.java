package com.daniphord.mahanga.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_sessions", indexes = {
        @Index(name = "idx_user_device_session_user", columnList = "user_id"),
        @Index(name = "idx_user_device_session_active", columnList = "active"),
        @Index(name = "idx_user_device_session_last_seen", columnList = "lastSeenAt"),
        @Index(name = "idx_user_device_session_risk", columnList = "riskLevel")
})
public class UserDeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 120)
    private String sessionId;

    @Column(nullable = false, unique = true, length = 120)
    private String refreshToken;

    @Column(nullable = false, length = 128)
    private String deviceFingerprintHash;

    @Column(length = 120)
    private String browser;

    @Column(length = 120)
    private String operatingSystem;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 120)
    private String ipAddress;

    @Column(length = 120)
    private String locationSignature;

    @Column(length = 32, nullable = false)
    private String presenceStatus = "ACTIVE";

    @Column(length = 32, nullable = false)
    private String riskLevel = "LOW";

    @Column(nullable = false)
    private Integer riskScore = 0;

    @Column(nullable = false)
    private Boolean newDevice = false;

    @Column(nullable = false)
    private Boolean multipleDeviceFlag = false;

    @Column(nullable = false)
    private Boolean autofillDetected = false;

    @Column(nullable = false)
    private Boolean typedPassword = true;

    @Column(nullable = false)
    private Boolean emergencyMode = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 64)
    private String activeIncidentNumber;

    @Column(nullable = false)
    private LocalDateTime loginAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastSeenAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now();

    private LocalDateTime endedAt;

    @Column(length = 120)
    private String logoutReason;

    @PreUpdate
    void onUpdate() {
        lastSeenAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getDeviceFingerprintHash() { return deviceFingerprintHash; }
    public void setDeviceFingerprintHash(String deviceFingerprintHash) { this.deviceFingerprintHash = deviceFingerprintHash; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getLocationSignature() { return locationSignature; }
    public void setLocationSignature(String locationSignature) { this.locationSignature = locationSignature; }
    public String getPresenceStatus() { return presenceStatus; }
    public void setPresenceStatus(String presenceStatus) { this.presenceStatus = presenceStatus; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public Boolean getNewDevice() { return newDevice; }
    public void setNewDevice(Boolean newDevice) { this.newDevice = newDevice; }
    public Boolean getMultipleDeviceFlag() { return multipleDeviceFlag; }
    public void setMultipleDeviceFlag(Boolean multipleDeviceFlag) { this.multipleDeviceFlag = multipleDeviceFlag; }
    public Boolean getAutofillDetected() { return autofillDetected; }
    public void setAutofillDetected(Boolean autofillDetected) { this.autofillDetected = autofillDetected; }
    public Boolean getTypedPassword() { return typedPassword; }
    public void setTypedPassword(Boolean typedPassword) { this.typedPassword = typedPassword; }
    public Boolean getEmergencyMode() { return emergencyMode; }
    public void setEmergencyMode(Boolean emergencyMode) { this.emergencyMode = emergencyMode; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getActiveIncidentNumber() { return activeIncidentNumber; }
    public void setActiveIncidentNumber(String activeIncidentNumber) { this.activeIncidentNumber = activeIncidentNumber; }
    public LocalDateTime getLoginAt() { return loginAt; }
    public void setLoginAt(LocalDateTime loginAt) { this.loginAt = loginAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public String getLogoutReason() { return logoutReason; }
    public void setLogoutReason(String logoutReason) { this.logoutReason = logoutReason; }
}
