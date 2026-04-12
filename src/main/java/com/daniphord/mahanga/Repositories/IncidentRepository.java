package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatusIgnoreCase(String status);
    List<Incident> findByIncidentTypeIgnoreCase(String incidentType);
    List<Incident> findByReportedAtAfter(LocalDateTime reportedAt);
    Optional<Incident> findFirstBySourceIgnoreCaseAndSourceReference(String source, String sourceReference);
    Optional<Incident> findFirstByEmergencyCallIdAndReportLevelIgnoreCase(Long emergencyCallId, String reportLevel);
    boolean existsByParentIncident_IdAndReportLevelIgnoreCase(Long parentIncidentId, String reportLevel);
}
