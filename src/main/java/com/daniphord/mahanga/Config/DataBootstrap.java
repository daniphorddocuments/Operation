package com.daniphord.mahanga.Config;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataBootstrap {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);

    @Bean
    CommandLineRunner bootstrapUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            seedSuperAdmin(userRepository, passwordEncoder, "superadmin", "admin123");
            seedSuperAdmin(userRepository, passwordEncoder, "Mahanga", "123");
        };
    }

    private void seedSuperAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String rawPassword
    ) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("Default super admin account already exists: username={}", username);
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole("SUPER_ADMIN");
        userRepository.save(admin);
        log.info("Seeded default super admin account: username={}", username);
    }
}
