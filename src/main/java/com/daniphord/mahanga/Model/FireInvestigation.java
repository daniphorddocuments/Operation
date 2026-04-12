package com.daniphord.mahanga.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fire_investigations", indexes = {
        @Index(name = "idx_fire_investigation_status", columnList = "status"),
        @Index(name = "idx_fire_investigation_incident", columnList = "incident_id")
})
public class FireInvestigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dio_id", nullable = false)
    private User dio;

    @Column(nullable = false, unique = true)
    private String investigationNumber;

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(length = 2500)
    private String incidentDetails;

    @Column(length = 2500)
    private String causeAnalysis;

    @Column(length = 2500)
    private String witnessStatements;

    @Column(nullable = false)
    private String currentLevel = "DISTRICT_INVESTIGATION_OFFICER";

    private LocalDateTime submissionDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (investigationNumber == null || investigationNumber.isBlank()) {
            investigationNumber = "INV-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public User getDio() { return dio; }
    public void setDio(User dio) { this.dio = dio; }
    public String getInvestigationNumber() { return investigationNumber; }
    public void setInvestigationNumber(String investigationNumber) { this.investigationNumber = investigationNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIncidentDetails() { return incidentDetails; }
    public void setIncidentDetails(String incidentDetails) { this.incidentDetails = incidentDetails; }
    public String getCauseAnalysis() { return causeAnalysis; }
    public void setCauseAnalysis(String causeAnalysis) { this.causeAnalysis = causeAnalysis; }
    public String getWitnessStatements() { return witnessStatements; }
    public void setWitnessStatements(String witnessStatements) { this.witnessStatements = witnessStatements; }
    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
