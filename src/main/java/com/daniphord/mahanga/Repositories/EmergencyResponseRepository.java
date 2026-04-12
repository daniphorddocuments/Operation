package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.EmergencyResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyResponseRepository extends JpaRepository<EmergencyResponse, Long> {
    List<EmergencyResponse> findByIncidentId(Long incidentId);
    List<EmergencyResponse> findByStatusIgnoreCase(String status);
}
