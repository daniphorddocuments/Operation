package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.FireInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FireInvestigationRepository extends JpaRepository<FireInvestigation, Long> {
    List<FireInvestigation> findByStatusOrderByUpdatedAtDesc(String status);
    List<FireInvestigation> findAllByOrderByUpdatedAtDesc();
    Optional<FireInvestigation> findByIncidentId(Long incidentId);
}
