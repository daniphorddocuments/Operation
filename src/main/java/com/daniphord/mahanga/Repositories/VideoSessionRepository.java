package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.VideoSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoSessionRepository extends JpaRepository<VideoSession, Long> {
    List<VideoSession> findTop20ByOrderByStartTimeDesc();
    List<VideoSession> findByStatusOrderByStartTimeDesc(String status);
    Optional<VideoSession> findBySessionKey(String sessionKey);
    List<VideoSession> findByEmergencyCallIdOrderByStartTimeDesc(Long emergencyCallId);
}
