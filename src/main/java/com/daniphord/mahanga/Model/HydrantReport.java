package com.daniphord.mahanga.Model;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hydrant_reports")
public class HydrantReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Column(nullable = false)
    private Integer totalHydrants = 0;

    @Column(nullable = false)
    private Integer working = 0;

    @Column(nullable = false)
    private Integer notWorking = 0;

    @Column(nullable = false)
    private Integer lowPressure = 0;

    @Column(length = 1000)
    private String remarks;

    @OneToMany(mappedBy = "hydrantReport", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HydrantLocation> locations = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, length = 32)
    private String approvalStatus = "APPROVED";

    @Column(length = 64)
    private String approvalCurrentLevel;

    @Column(length = 1000)
    private String approvalLastComment;

    private LocalDateTime approvalSubmittedAt;
    private LocalDateTime approvalUpdatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void recomputeTotals() {
        int computedTotal = safe(working) + safe(notWorking) + safe(lowPressure);
        this.totalHydrants = computedTotal;
        this.updatedAt = LocalDateTime.now();
    }

    private int safe(Integer val) {
        return val == null ? 0 : val;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Integer getTotalHydrants() {
        return totalHydrants;
    }

    public void setTotalHydrants(Integer totalHydrants) {
        this.totalHydrants = totalHydrants;
    }

    public Integer getWorking() {
        return working;
    }

    public void setWorking(Integer working) {
        this.working = working;
    }

    public Integer getNotWorking() {
        return notWorking;
    }

    public void setNotWorking(Integer notWorking) {
        this.notWorking = notWorking;
    }

    public Integer getLowPressure() {
        return lowPressure;
    }

    public void setLowPressure(Integer lowPressure) {
        this.lowPressure = lowPressure;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<HydrantLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<HydrantLocation> locations) {
        this.locations = locations;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovalCurrentLevel() {
        return approvalCurrentLevel;
    }

    public void setApprovalCurrentLevel(String approvalCurrentLevel) {
        this.approvalCurrentLevel = approvalCurrentLevel;
    }

    public String getApprovalLastComment() {
        return approvalLastComment;
    }

    public void setApprovalLastComment(String approvalLastComment) {
        this.approvalLastComment = approvalLastComment;
    }

    public LocalDateTime getApprovalSubmittedAt() {
        return approvalSubmittedAt;
    }

    public void setApprovalSubmittedAt(LocalDateTime approvalSubmittedAt) {
        this.approvalSubmittedAt = approvalSubmittedAt;
    }

    public LocalDateTime getApprovalUpdatedAt() {
        return approvalUpdatedAt;
    }

    public void setApprovalUpdatedAt(LocalDateTime approvalUpdatedAt) {
        this.approvalUpdatedAt = approvalUpdatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
