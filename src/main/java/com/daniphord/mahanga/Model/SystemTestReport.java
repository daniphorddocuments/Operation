package com.daniphord.mahanga.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_test_reports", indexes = {
        @Index(name = "idx_system_test_report_started", columnList = "startedAt"),
        @Index(name = "idx_system_test_report_status", columnList = "status")
})
public class SystemTestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reportNumber;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String triggerMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id")
    private User triggeredByUser;

    @Column(nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Integer passedChecks = 0;

    @Column(nullable = false)
    private Integer warningChecks = 0;

    @Column(nullable = false)
    private Integer failedChecks = 0;

    @Column(nullable = false)
    private Integer totalChecks = 0;

    @Column(length = 1200)
    private String summary;

    @Lob
    @Column(nullable = false)
    private String detailsJson = "[]";

    @PrePersist
    void onCreate() {
        if (reportNumber == null || reportNumber.isBlank()) {
            reportNumber = "TEST-" + System.currentTimeMillis();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportNumber() { return reportNumber; }
    public void setReportNumber(String reportNumber) { this.reportNumber = reportNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTriggerMode() { return triggerMode; }
    public void setTriggerMode(String triggerMode) { this.triggerMode = triggerMode; }
    public User getTriggeredByUser() { return triggeredByUser; }
    public void setTriggeredByUser(User triggeredByUser) { this.triggeredByUser = triggeredByUser; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getPassedChecks() { return passedChecks; }
    public void setPassedChecks(Integer passedChecks) { this.passedChecks = passedChecks; }
    public Integer getWarningChecks() { return warningChecks; }
    public void setWarningChecks(Integer warningChecks) { this.warningChecks = warningChecks; }
    public Integer getFailedChecks() { return failedChecks; }
    public void setFailedChecks(Integer failedChecks) { this.failedChecks = failedChecks; }
    public Integer getTotalChecks() { return totalChecks; }
    public void setTotalChecks(Integer totalChecks) { this.totalChecks = totalChecks; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
}
