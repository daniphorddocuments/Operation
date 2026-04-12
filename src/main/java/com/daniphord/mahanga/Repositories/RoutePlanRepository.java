package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.RoutePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {
    List<RoutePlan> findTop10ByEmergencyCallIdOrderByCreatedAtDesc(Long emergencyCallId);
    List<RoutePlan> findTop10ByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
