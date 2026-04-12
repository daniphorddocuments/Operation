package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.EmergencyCallMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyCallMessageRepository extends JpaRepository<EmergencyCallMessage, Long> {
    List<EmergencyCallMessage> findByEmergencyCallIdOrderByCreatedAtAsc(Long emergencyCallId);
}
