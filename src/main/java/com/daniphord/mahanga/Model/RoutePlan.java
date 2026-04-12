package com.daniphord.mahanga.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "routes", indexes = {
        @Index(name = "idx_routes_created_at", columnList = "createdAt"),
        @Index(name = "idx_routes_call_id", columnList = "emergency_call_id"),
        @Index(name = "idx_routes_incident_id", columnList = "incident_id")
})
public class RoutePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_call_id")
    private EmergencyCall emergencyCall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal straightDistanceKm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal roadDistanceKm;

    @Column(nullable = false)
    private Integer etaMinutes;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal learningFactor;

    @Column(length = 2000)
    private String directionsText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public EmergencyCall getEmergencyCall() {
        return emergencyCall;
    }

    public void setEmergencyCall(EmergencyCall emergencyCall) {
        this.emergencyCall = emergencyCall;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public BigDecimal getStraightDistanceKm() {
        return straightDistanceKm;
    }

    public void setStraightDistanceKm(BigDecimal straightDistanceKm) {
        this.straightDistanceKm = straightDistanceKm;
    }

    public BigDecimal getRoadDistanceKm() {
        return roadDistanceKm;
    }

    public void setRoadDistanceKm(BigDecimal roadDistanceKm) {
        this.roadDistanceKm = roadDistanceKm;
    }

    public Integer getEtaMinutes() {
        return etaMinutes;
    }

    public void setEtaMinutes(Integer etaMinutes) {
        this.etaMinutes = etaMinutes;
    }

    public BigDecimal getLearningFactor() {
        return learningFactor;
    }

    public void setLearningFactor(BigDecimal learningFactor) {
        this.learningFactor = learningFactor;
    }

    public String getDirectionsText() {
        return directionsText;
    }

    public void setDirectionsText(String directionsText) {
        this.directionsText = directionsText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
