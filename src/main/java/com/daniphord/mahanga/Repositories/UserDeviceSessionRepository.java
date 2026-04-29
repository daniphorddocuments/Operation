package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.UserDeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceSessionRepository extends JpaRepository<UserDeviceSession, Long> {
    Optional<UserDeviceSession> findBySessionId(String sessionId);
    List<UserDeviceSession> findByUserIdAndActiveTrueOrderByLastSeenAtDesc(Long userId);
    List<UserDeviceSession> findByActiveTrueOrderByLastSeenAtDesc();
    boolean existsByUserIdAndDeviceFingerprintHash(Long userId, String deviceFingerprintHash);
}
