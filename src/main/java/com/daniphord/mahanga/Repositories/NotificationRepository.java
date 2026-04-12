package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Notification;
import com.daniphord.mahanga.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserAndReadFalse(User user);
    long countByUserAndReadFalse(User user);
}
