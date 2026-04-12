package com.daniphord.mahanga.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emergency_calls")
public class EmergencyCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reportNumber;

    @Column(nullable = false, unique = true, length = 64)
    private String publicAccessToken;

    private String callerName;
    private String callerNumber;
    private String incidentType;
    private String sourceChannel;
    private String status = "REPORTED";
    private String village;
    private String ward;
    private String roadSymbol;
    private String locationText;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(length = 1200)
    private String details;

    @Column(nullable = false)
    private LocalDateTime callTime;

    private String recordingUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station routedStation;

    @jakarta.persistence.Transient
    private Long wardId;

    @jakarta.persistence.Transient
    private Long villageStreetId;

    @jakarta.persistence.Transient
    private Long roadLandmarkId;

    @PrePersist
    void onCreate() {
        if (callTime == null) {
            callTime = LocalDateTime.now();
        }
        if (reportNumber == null || reportNumber.isBlank()) {
            reportNumber = "REP-" + System.currentTimeMillis();
        }
        if (publicAccessToken == null || publicAccessToken.isBlank()) {
            publicAccessToken = UUID.randomUUID().toString().replace("-", "");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportNumber() { return reportNumber; }
    public void setReportNumber(String reportNumber) { this.reportNumber = reportNumber; }
    public String getPublicAccessToken() { return publicAccessToken; }
    public void setPublicAccessToken(String publicAccessToken) { this.publicAccessToken = publicAccessToken; }
    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }
    public String getCallerNumber() { return callerNumber; }
    public void setCallerNumber(String callerNumber) { this.callerNumber = callerNumber; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }
    public String getSourceChannel() { return sourceChannel; }
    public void setSourceChannel(String sourceChannel) { this.sourceChannel = sourceChannel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getRoadSymbol() { return roadSymbol; }
    public void setRoadSymbol(String roadSymbol) { this.roadSymbol = roadSymbol; }
    public String getLocationText() { return locationText; }
    public void setLocationText(String locationText) { this.locationText = locationText; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCallTime() { return callTime; }
    public void setCallTime(LocalDateTime callTime) { this.callTime = callTime; }
    public String getRecordingUrl() { return recordingUrl; }
    public void setRecordingUrl(String recordingUrl) { this.recordingUrl = recordingUrl; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }
    public Station getRoutedStation() { return routedStation; }
    public void setRoutedStation(Station routedStation) { this.routedStation = routedStation; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Long getVillageStreetId() { return villageStreetId; }
    public void setVillageStreetId(Long villageStreetId) { this.villageStreetId = villageStreetId; }
    public Long getRoadLandmarkId() { return roadLandmarkId; }
    public void setRoadLandmarkId(Long roadLandmarkId) { this.roadLandmarkId = roadLandmarkId; }
}
