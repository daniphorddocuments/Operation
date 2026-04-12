package com.daniphord.mahanga.Config;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.RegionRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.UserService;
import com.daniphord.mahanga.Util.OperationRole;
import com.daniphord.mahanga.Util.PasswordStrengthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;

@Configuration
public class DataBootstrap {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);
    private static final List<String> LEGACY_BOOTSTRAP_USERNAMES = List.of(
            "cgf.command",
            "ops.command",
            "tele.support"
    );

    @Bean
    @Order(3)
    CommandLineRunner bootstrapUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserService userService
    ) {
        return args -> {
            seedCommandAccount(userRepository, passwordEncoder, "Mahanga", "Nsunga@2018", "SUPER_ADMIN", "Mahanga");
            int deletedUsers = userService.deleteLegacyBootstrapUsers(LEGACY_BOOTSTRAP_USERNAMES);
            if (deletedUsers > 0) {
                log.warn("Removed {} legacy bootstrap user account(s) during startup cleanup", deletedUsers);
            }
        };
    }

    @Bean
    @Order(1)
    CommandLineRunner bootstrapTanzaniaGeography(
            RegionRepository regionRepository,
            DistrictRepository districtRepository,
            MainlandGeographyCatalog mainlandGeographyCatalog
    ) {
        return args -> mainlandGeographyCatalog.geography().forEach((regionName, districts) -> {
            Region region = ensureRegion(regionRepository, regionName);
            List<District> existingDistricts = districtRepository.findByRegionIdOrderByNameAsc(region.getId());
            for (String districtName : districts) {
                ensureDistrict(districtRepository, existingDistricts, region, districtName);
            }
        });
    }

    private Region ensureRegion(RegionRepository regionRepository, String regionName) {
        Region region = regionRepository.findByNameIgnoreCase(regionName).orElseGet(Region::new);
        region.setName(regionName);
        region.setCode(defaultRegionCode(regionName));
        return regionRepository.save(region);
    }

    private void ensureDistrict(
            DistrictRepository districtRepository,
            List<District> existingDistricts,
            Region region,
            String districtName
    ) {
        boolean exists = existingDistricts.stream()
                .anyMatch(district -> districtName.equalsIgnoreCase(district.getName()));
        if (exists) {
            return;
        }
        District district = new District();
        district.setName(districtName);
        district.setRegion(region);
        districtRepository.save(district);
    }

    private String defaultRegionCode(String regionName) {
        return regionName.substring(0, Math.min(3, regionName.length())).toUpperCase(Locale.ROOT);
    }

    private void seedCommandAccount(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String rawPassword,
            String role,
            String fullName
    ) {
        java.util.Optional<User> existingAccount = userRepository.findByUsername(username);
        if (existingAccount.isPresent()) {
            User account = existingAccount.get();
            boolean updated = false;
            if (!passwordEncoder.matches(rawPassword, account.getPassword())) {
                account.setPassword(passwordEncoder.encode(rawPassword));
                account.setPasswordStrength(PasswordStrengthUtil.evaluate(rawPassword, username));
                updated = true;
            }
            if (!Boolean.TRUE.equals(account.getActive())) {
                account.setActive(true);
                updated = true;
            }
            if (account.getFailedLoginAttempts() == null || account.getFailedLoginAttempts() != 0) {
                account.setFailedLoginAttempts(0);
                updated = true;
            }
            if (account.getAccountLockedUntil() != null) {
                account.setAccountLockedUntil(null);
                updated = true;
            }
            if (!role.equalsIgnoreCase(account.getRole())) {
                account.setRole(role);
                updated = true;
            }
            if (account.getFullName() == null || account.getFullName().isBlank()) {
                account.setFullName(fullName);
                updated = true;
            }
            if (account.getDesignation() == null || account.getDesignation().isBlank()) {
                account.setDesignation(role);
                updated = true;
            }
            if (updated) {
                userRepository.save(account);
                log.warn("Normalized default FROMS command account for presentation access: username={}, role={}", username, role);
            } else {
                log.info("Default FROMS command account already exists: username={}, role={}", username, role);
            }
            return;
        }

        User account = new User();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setPasswordStrength(PasswordStrengthUtil.evaluate(rawPassword, username));
        account.setRole(role);
        account.setFullName(fullName);
        account.setDesignation(role);
        account.setStation(null);
        userRepository.save(account);
        log.warn("Seeded default FROMS command account: username={}, role={}. Change the password immediately.", username, role);
    }
}
