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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipment_report_items")
public class EquipmentReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_report_id", nullable = false)
    private EquipmentReport equipmentReport;

    @Column(nullable = false)
    private String equipmentName;

    @Column(nullable = false)
    private Integer available = 0;

    @Column(nullable = false)
    private Integer working = 0;

    @Column(nullable = false)
    private Integer damaged = 0;

    @Column(nullable = false)
    private Integer required = 0;

    @Column(nullable = false)
    private Integer shortage = 0;

    @PrePersist
    @PreUpdate
    public void computeShortage() {
        int availableWorking = working == null ? 0 : working;
        int requiredCount = required == null ? 0 : required;
        shortage = Math.max(requiredCount - availableWorking, 0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EquipmentReport getEquipmentReport() {
        return equipmentReport;
    }

    public void setEquipmentReport(EquipmentReport equipmentReport) {
        this.equipmentReport = equipmentReport;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public Integer getAvailable() {
        return available;
    }

    public void setAvailable(Integer available) {
        this.available = available;
    }

    public Integer getWorking() {
        return working;
    }

    public void setWorking(Integer working) {
        this.working = working;
    }

    public Integer getDamaged() {
        return damaged;
    }

    public void setDamaged(Integer damaged) {
        this.damaged = damaged;
    }

    public Integer getRequired() {
        return required;
    }

    public void setRequired(Integer required) {
        this.required = required;
    }

    public Integer getShortage() {
        return shortage;
    }

    public void setShortage(Integer shortage) {
        this.shortage = shortage;
    }
}
