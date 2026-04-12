package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.EmergencyCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyCallRepository extends JpaRepository<EmergencyCall, Long> {
    List<EmergencyCall> findTop20ByOrderByCallTimeDesc();
    List<EmergencyCall> findByStatusIgnoreCase(String status);
    Optional<EmergencyCall> findByReportNumber(String reportNumber);
    Optional<EmergencyCall> findByReportNumberAndPublicAccessToken(String reportNumber, String publicAccessToken);
}
