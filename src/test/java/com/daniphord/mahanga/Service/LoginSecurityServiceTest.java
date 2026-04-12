package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.AuditLogRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LoginSecurityServiceTest {

    @Autowired
    private LoginSecurityService loginSecurityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<Long> probeUserIds = new ArrayList<>();
    private final List<String> probeIps = new ArrayList<>();

    @AfterEach
    void cleanupProbeArtifacts() {
        probeIps.forEach(loginSecurityService::resetRateLimit);
        probeUserIds.forEach(userId -> {
            var auditLogs = auditLogRepository.findByUserId(userId);
            if (!auditLogs.isEmpty()) {
                auditLogRepository.deleteAll(auditLogs);
            }
            userRepository.findById(userId).ifPresent(userRepository::delete);
        });
        probeIps.clear();
        probeUserIds.clear();
    }

    @Test
    void anonymousFailuresTriggerIpRateLimit() {
        String probeIp = uniqueProbeIp("198.51.100.");
        for (int attempt = 0; attempt < loginSecurityService.maxLoginAttempts(); attempt++) {
            loginSecurityService.recordAnonymousFailure(probeIp);
        }

        assertTrue(loginSecurityService.isIpRateLimited(probeIp));
    }

    @Test
    void repeatedFailedPasswordsLockDisposableAccount() {
        String probeIp = uniqueProbeIp("203.0.113.");
        User probeUser = createProbeUser();

        for (int attempt = 0; attempt < loginSecurityService.maxLoginAttempts(); attempt++) {
            loginSecurityService.recordFailedLogin(probeUser, probeIp);
        }

        User refreshedProbeUser = userRepository.findById(probeUser.getId()).orElseThrow();
        assertTrue(loginSecurityService.isAccountLocked(refreshedProbeUser));
    }

    private User createProbeUser() {
        User probeUser = new User();
        probeUser.setUsername("lstest_" + Long.toString(System.nanoTime(), 36));
        probeUser.setPassword(passwordEncoder.encode("TempProbe1!"));
        probeUser.setRole(OperationRole.ADMIN);
        probeUser.setActive(true);
        probeUser.setFullName("Login Security Test Probe");
        probeUser.setPasswordStrength("STRONG");
        User savedUser = userRepository.save(probeUser);
        probeUserIds.add(savedUser.getId());
        return savedUser;
    }

    private String uniqueProbeIp(String prefix) {
        String probeIp = prefix + (System.nanoTime() % 200 + 10);
        probeIps.add(probeIp);
        return probeIp;
    }
}
