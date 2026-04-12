package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Model.UserNotification;
import com.daniphord.mahanga.Repositories.UserNotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final SignalHandler signalHandler;

    public NotificationService(UserNotificationRepository userNotificationRepository, SignalHandler signalHandler) {
        this.userNotificationRepository = userNotificationRepository;
        this.signalHandler = signalHandler;
    }

    public void notifyUsers(List<User> users, String title, String message, String actionUrl) {
        for (User user : users) {
            if (user == null) {
                continue;
            }
            UserNotification notification = new UserNotification();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setActionUrl(actionUrl);
            userNotificationRepository.save(notification);
            signalHandler.broadcast("INVESTIGATION_NOTIFICATION", Map.of(
                    "userId", user.getId(),
                    "title", title,
                    "message", message,
                    "actionUrl", actionUrl
            ));
        }
    }

    public List<UserNotification> latestNotifications(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userNotificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    public long unreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return userNotificationRepository.countByUserIdAndReadFalse(userId);
    }
}
