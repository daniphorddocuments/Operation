package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByStationId(Long stationId);
    List<User> findByAccountLockedUntilAfter(LocalDateTime timestamp);
    List<User> findByRoleIn(List<String> roles);
}
