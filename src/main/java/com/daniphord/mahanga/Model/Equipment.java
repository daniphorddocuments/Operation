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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment", indexes = {
        @Index(name = "idx_equipment_status", columnList = "operationalStatus"),
        @Index(name = "idx_equipment_station", columnList = "station_id")
})
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String type;

    @Column(length = 64)
    private String subtype;

    @Column(unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private String conditionStatus = "GOOD";

    @Column(nullable = false)
    private String operationalStatus = "AVAILABLE";

    private LocalDate purchaseDate;
    private LocalDate maintenanceDueDate;
    private LocalDateTime lastServicedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private Boolean maintenanceRequired = false;
    private Integer quantityInStore = 1;

    @Column(nullable = false, length = 32)
    private String approvalStatus = "APPROVED";

    @Column(length = 64)
    private String approvalCurrentLevel;

    @Column(length = 1000)
    private String approvalLastComment;

    private LocalDateTime approvalSubmittedAt;
    private LocalDateTime approvalUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSubtype() { return subtype; }
    public void setSubtype(String subtype) { this.subtype = subtype; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getConditionStatus() { return conditionStatus; }
    public void setConditionStatus(String conditionStatus) { this.conditionStatus = conditionStatus; }
    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public LocalDate getMaintenanceDueDate() { return maintenanceDueDate; }
    public void setMaintenanceDueDate(LocalDate maintenanceDueDate) { this.maintenanceDueDate = maintenanceDueDate; }
    public LocalDateTime getLastServicedAt() { return lastServicedAt; }
    public void setLastServicedAt(LocalDateTime lastServicedAt) { this.lastServicedAt = lastServicedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getMaintenanceRequired() { return maintenanceRequired; }
    public void setMaintenanceRequired(Boolean maintenanceRequired) { this.maintenanceRequired = maintenanceRequired; }
    public Integer getQuantityInStore() { return quantityInStore; }
    public void setQuantityInStore(Integer quantityInStore) { this.quantityInStore = quantityInStore; }
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
    public Station getStation() { return station; }
    public void setStation(Station station) { this.station = station; }
    public User getAssignedOfficer() { return assignedOfficer; }
    public void setAssignedOfficer(User assignedOfficer) { this.assignedOfficer = assignedOfficer; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
