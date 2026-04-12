package com.daniphord.mahanga.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "investigation_logs", indexes = {
        @Index(name = "idx_investigation_log_report", columnList = "report_id"),
        @Index(name = "idx_investigation_log_timestamp", columnList = "timestamp")
})
public class InvestigationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private FireInvestigation report;

    @Column(nullable = false)
    private String level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Column(nullable = false)
    private String status;

    @Column(length = 2500)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FireInvestigation getReport() { return report; }
    public void setReport(FireInvestigation report) { this.report = report; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public User getApprover() { return approver; }
    public void setApprover(User approver) { this.approver = approver; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
