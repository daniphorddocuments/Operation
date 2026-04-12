package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.RateLimiter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LoginSecurityService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RateLimiter loginRateLimiter;

    public LoginSecurityService(
            UserRepository userRepository,
            AuditService auditService,
            RateLimiter loginRateLimiter
    ) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.loginRateLimiter = loginRateLimiter;
    }

    public int maxLoginAttempts() {
        return MAX_LOGIN_ATTEMPTS;
    }

    public boolean isIpRateLimited(String ipAddress) {
        return loginRateLimiter.isRateLimited(identifier(ipAddress));
    }

    public void recordAnonymousFailure(String ipAddress) {
        loginRateLimiter.recordAttempt(identifier(ipAddress));
    }

    public String rateLimitMessage(String ipAddress) {
        long remainingSeconds = loginRateLimiter.getLockoutRemainingSeconds(identifier(ipAddress));
        long remainingMinutes = Math.max(1, (remainingSeconds + 59) / 60);
        return "Too many login attempts from this source. Try again after " + remainingMinutes + " minute(s).";
    }

    public boolean isAccountLocked(User user) {
        return user != null
                && user.getAccountLockedUntil() != null
                && user.getAccountLockedUntil().isAfter(LocalDateTime.now());
    }

    public String lockedMessage(User user) {
        long minutesRemaining = Math.max(1, Duration.between(LocalDateTime.now(), user.getAccountLockedUntil()).toMinutes());
        return "Your account is locked. Try again after " + minutesRemaining + " minute(s).";
    }

    public String recordFailedLogin(User user, String ipAddress) {
        loginRateLimiter.recordAttempt(identifier(ipAddress));

        int failedAttempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(failedAttempts);
        if (failedAttempts >= MAX_LOGIN_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES));
            userRepository.save(user);
            auditService.logFailure(
                    user,
                    "ACCOUNT_LOCKED",
                    "Account locked after too many incorrect password attempts",
                    ipAddress,
                    "Admin attention required"
            );
            auditService.logFailure(user, "LOGIN_ATTEMPT", "Failed login attempt", ipAddress, null);
            return "Incorrect username or password. Your account has been locked. An administrator has been notified.";
        }

        userRepository.save(user);
        auditService.logFailure(user, "LOGIN_ATTEMPT", "Failed login attempt", ipAddress, null);
        int remainingAttempts = MAX_LOGIN_ATTEMPTS - failedAttempts;
        return "Incorrect username or password. You have " + remainingAttempts + " attempt(s) remaining before your account is locked.";
    }

    public User registerSuccessfulLogin(User user, String ipAddress) {
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        loginRateLimiter.reset(identifier(ipAddress));
        return userRepository.save(user);
    }

    public void resetRateLimit(String ipAddress) {
        loginRateLimiter.reset(identifier(ipAddress));
    }

    private String identifier(String ipAddress) {
        return ipAddress == null || ipAddress.isBlank() ? "UNKNOWN" : ipAddress.trim();
    }
}
