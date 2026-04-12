package com.daniphord.mahanga.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incident_status", columnList = "status"),
        @Index(name = "idx_incident_type", columnList = "incidentType"),
        @Index(name = "idx_incident_reported_at", columnList = "reportedAt")
})
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String incidentNumber;

    @NotBlank
    @Column(nullable = false)
    private String incidentType;

    @NotBlank
    @Column(nullable = false)
    private String source;

    @NotBlank
    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status = "RECEIVED";

    @Column(length = 1500)
    private String description;

    @Column(nullable = false, length = 32)
    private String reportLevel = "INITIAL";

    @Column(length = 64)
    private String operationCategory;

    @Column(length = 255)
    private String locationDetails;

    @Column(length = 500)
    private String cause;

    @Column(length = 500)
    private String actionTaken;

    @Column(length = 500)
    private String outcome;

    @Column(length = 120)
    private String sourceReference;

    @Column(length = 64)
    private String reportingMeans;

    private Integer casualtiesInjured;
    private Integer casualtiesDead;
    private String propertiesAffected;
    private Integer incidentDurationMinutes;

    private Integer responseTimeMinutes;
    private Double distanceKm;
    private Integer operationDurationMinutes;
    private Double oilUsedLitres;

    @Column(length = 1000)
    private String resourcesUsed;

    @Column(length = 1000)
    private String equipmentDispatched;

    @Column(length = 1000)
    private String personnelDispatched;

    @Column(length = 1200)
    private String personnelNames;

    @Column(length = 255)
    private String operationCommander;

    @Column(length = 255)
    private String supervisorName;

    @Column(length = 1000)
    private String otherSecurityOrgans;

    @Column(length = 1500)
    private String effectsOnPeople;

    @Column(length = 1500)
    private String effectsOnEnvironment;

    @Column(length = 1000)
    private String injuredPeopleDetails;

    @Column(length = 1000)
    private String diedPeopleDetails;

    @Column(length = 1000)
    private String casualtyDemographics;

    @Column(length = 2000)
    private String aiRecommendationSnapshot;

    @Column(length = 2000)
    private String aiImprovementSnapshot;

    @Column(nullable = false, length = 32)
    private String approvalStatus = "APPROVED";

    @Column(length = 64)
    private String approvalCurrentLevel;

    @Column(length = 1000)
    private String approvalLastComment;

    private LocalDateTime approvalSubmittedAt;
    private LocalDateTime approvalUpdatedAt;

    @Column(length = 255)
    private String village;

    @Column(length = 255)
    private String ward;

    @Column(length = 255)
    private String roadLandmark;

    @Column(length = 120)
    private String roadSymbol;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private LocalDateTime reportedAt;

    @Column(nullable = false)
    private LocalDateTime callReceivedAt;

    private LocalDateTime arrivalTime;
    private LocalDateTime dispatchedAt;
    private LocalDateTime resolvedAt;

    @Column(length = 255)
    private String reportingPerson;

    @Column(length = 255)
    private String reportingContact;

    @Column(length = 255)
    private String respondingUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_incident_id")
    private Incident parentIncident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_call_id")
    private EmergencyCall emergencyCall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Transient
    private Long parentIncidentId;

    @Transient
    private Long linkedCallId;

    @Transient
    private Long wardId;

    @Transient
    private Long villageStreetId;

    @Transient
    private Long roadLandmarkId;

    @PrePersist
    void onCreate() {
        if (reportedAt == null) {
            reportedAt = LocalDateTime.now();
        }
        if (callReceivedAt == null) {
            callReceivedAt = reportedAt;
        }
        if (incidentNumber == null || incidentNumber.isBlank()) {
            incidentNumber = "INC-" + System.currentTimeMillis();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReportLevel() { return reportLevel; }
    public void setReportLevel(String reportLevel) { this.reportLevel = reportLevel; }
    public String getOperationCategory() { return operationCategory; }
    public void setOperationCategory(String operationCategory) { this.operationCategory = operationCategory; }
    public String getLocationDetails() { return locationDetails; }
    public void setLocationDetails(String locationDetails) { this.locationDetails = locationDetails; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public String getReportingMeans() { return reportingMeans; }
    public void setReportingMeans(String reportingMeans) { this.reportingMeans = reportingMeans; }
    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Integer getCasualtiesInjured() { return casualtiesInjured; }
    public void setCasualtiesInjured(Integer casualtiesInjured) { this.casualtiesInjured = casualtiesInjured; }
    public Integer getCasualtiesDead() { return casualtiesDead; }
    public void setCasualtiesDead(Integer casualtiesDead) { this.casualtiesDead = casualtiesDead; }
    public String getPropertiesAffected() { return propertiesAffected; }
    public void setPropertiesAffected(String propertiesAffected) { this.propertiesAffected = propertiesAffected; }
    public Integer getIncidentDurationMinutes() { return incidentDurationMinutes; }
    public void setIncidentDurationMinutes(Integer incidentDurationMinutes) { this.incidentDurationMinutes = incidentDurationMinutes; }
    public Integer getResponseTimeMinutes() { return responseTimeMinutes; }
    public void setResponseTimeMinutes(Integer responseTimeMinutes) { this.responseTimeMinutes = responseTimeMinutes; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Integer getOperationDurationMinutes() { return operationDurationMinutes; }
    public void setOperationDurationMinutes(Integer operationDurationMinutes) { this.operationDurationMinutes = operationDurationMinutes; }
    public Double getOilUsedLitres() { return oilUsedLitres; }
    public void setOilUsedLitres(Double oilUsedLitres) { this.oilUsedLitres = oilUsedLitres; }
    public String getResourcesUsed() { return resourcesUsed; }
    public void setResourcesUsed(String resourcesUsed) { this.resourcesUsed = resourcesUsed; }
    public String getEquipmentDispatched() { return equipmentDispatched; }
    public void setEquipmentDispatched(String equipmentDispatched) { this.equipmentDispatched = equipmentDispatched; }
    public String getPersonnelDispatched() { return personnelDispatched; }
    public void setPersonnelDispatched(String personnelDispatched) { this.personnelDispatched = personnelDispatched; }
    public String getPersonnelNames() { return personnelNames; }
    public void setPersonnelNames(String personnelNames) { this.personnelNames = personnelNames; }
    public String getOperationCommander() { return operationCommander; }
    public void setOperationCommander(String operationCommander) { this.operationCommander = operationCommander; }
    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }
    public String getOtherSecurityOrgans() { return otherSecurityOrgans; }
    public void setOtherSecurityOrgans(String otherSecurityOrgans) { this.otherSecurityOrgans = otherSecurityOrgans; }
    public String getEffectsOnPeople() { return effectsOnPeople; }
    public void setEffectsOnPeople(String effectsOnPeople) { this.effectsOnPeople = effectsOnPeople; }
    public String getEffectsOnEnvironment() { return effectsOnEnvironment; }
    public void setEffectsOnEnvironment(String effectsOnEnvironment) { this.effectsOnEnvironment = effectsOnEnvironment; }
    public String getInjuredPeopleDetails() { return injuredPeopleDetails; }
    public void setInjuredPeopleDetails(String injuredPeopleDetails) { this.injuredPeopleDetails = injuredPeopleDetails; }
    public String getDiedPeopleDetails() { return diedPeopleDetails; }
    public void setDiedPeopleDetails(String diedPeopleDetails) { this.diedPeopleDetails = diedPeopleDetails; }
    public String getCasualtyDemographics() { return casualtyDemographics; }
    public void setCasualtyDemographics(String casualtyDemographics) { this.casualtyDemographics = casualtyDemographics; }
    public String getAiRecommendationSnapshot() { return aiRecommendationSnapshot; }
    public void setAiRecommendationSnapshot(String aiRecommendationSnapshot) { this.aiRecommendationSnapshot = aiRecommendationSnapshot; }
    public String getAiImprovementSnapshot() { return aiImprovementSnapshot; }
    public void setAiImprovementSnapshot(String aiImprovementSnapshot) { this.aiImprovementSnapshot = aiImprovementSnapshot; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public String getApprovalCurrentLevel() { return approvalCurrentLevel; }
    public void setApprovalCurrentLevel(String approvalCurrentLevel) { this.approvalCurrentLevel = approvalCurrentLevel; }
    public String getApprovalLastComment() { return approvalLastComment; }
    public void setApprovalLastComment(String approvalLastComment) { this.approvalLastComment = approvalLastComment; }
    public LocalDateTime getApprovalSubmittedAt() { return approvalSubmittedAt; }
    public void setApprovalSubmittedAt(LocalDateTime approvalSubmittedAt) { this.approvalSubmittedAt = approvalSubmittedAt; }
    public LocalDateTime getApprovalUpdatedAt() { return approvalUpdatedAt; }
    public void setApprovalUpdatedAt(LocalDateTime approvalUpdatedAt) { this.approvalUpdatedAt = approvalUpdatedAt; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getRoadLandmark() { return roadLandmark; }
    public void setRoadLandmark(String roadLandmark) { this.roadLandmark = roadLandmark; }
    public String getRoadSymbol() { return roadSymbol; }
    public void setRoadSymbol(String roadSymbol) { this.roadSymbol = roadSymbol; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public LocalDateTime getCallReceivedAt() { return callReceivedAt; }
    public void setCallReceivedAt(LocalDateTime callReceivedAt) { this.callReceivedAt = callReceivedAt; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getReportingPerson() { return reportingPerson; }
    public void setReportingPerson(String reportingPerson) { this.reportingPerson = reportingPerson; }
    public String getReportingContact() { return reportingContact; }
    public void setReportingContact(String reportingContact) { this.reportingContact = reportingContact; }
    public Incident getParentIncident() { return parentIncident; }
    public void setParentIncident(Incident parentIncident) { this.parentIncident = parentIncident; }
    public EmergencyCall getEmergencyCall() { return emergencyCall; }
    public void setEmergencyCall(EmergencyCall emergencyCall) { this.emergencyCall = emergencyCall; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }
    public Station getStation() { return station; }
    public void setStation(Station station) { this.station = station; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getRespondingUnit() { return respondingUnit; }
    public void setRespondingUnit(String respondingUnit) { this.respondingUnit = respondingUnit; }
    public Long getParentIncidentId() { return parentIncidentId; }
    public void setParentIncidentId(Long parentIncidentId) { this.parentIncidentId = parentIncidentId; }
    public Long getLinkedCallId() { return linkedCallId; }
    public void setLinkedCallId(Long linkedCallId) { this.linkedCallId = linkedCallId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Long getVillageStreetId() { return villageStreetId; }
    public void setVillageStreetId(Long villageStreetId) { this.villageStreetId = villageStreetId; }
    public Long getRoadLandmarkId() { return roadLandmarkId; }
    public void setRoadLandmarkId(Long roadLandmarkId) { this.roadLandmarkId = roadLandmarkId; }
}
