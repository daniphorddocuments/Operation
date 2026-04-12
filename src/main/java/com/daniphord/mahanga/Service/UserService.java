package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import com.daniphord.mahanga.Util.PasswordStrengthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // GET all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // GET user by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user, Long stationId, Long districtId, Long regionId) throws Exception {
        user.setEmail(clean(user.getEmail()));
        user.setCheckNumber(clean(user.getCheckNumber()));
        user.setFullName(clean(user.getFullName()));
        user.setDesignation(clean(user.getDesignation()));
        user.setPhoneNumber(clean(user.getPhoneNumber()));
        String normalizedRole = normalizeRole(user.getRole());
        if (normalizedRole.isBlank()) {
            normalizedRole = OperationRole.UNASSIGNED;
        }
        user.setRole(normalizedRole);
        if (user.getCheckNumber() != null && !user.getCheckNumber().isBlank()) {
            user.setUsername(user.getCheckNumber().trim());
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new Exception("Username/check number is required");
        }
        user.setUsername(user.getUsername().trim());
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new Exception("A user with this username/check number already exists");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new Exception("Password is required");
        }

        user.setStation(resolveScopeStation(normalizedRole, stationId, districtId, regionId));
        user.setPasswordStrength(PasswordStrengthUtil.evaluate(user.getPassword(), user.getUsername()));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(Long id, User updatedUser, Long stationId, Long districtId, Long regionId) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found"));

        if (updatedUser.getCheckNumber() != null && !updatedUser.getCheckNumber().isBlank()) {
            user.setCheckNumber(updatedUser.getCheckNumber().trim());
            user.setUsername(updatedUser.getCheckNumber().trim());
        } else {
            user.setCheckNumber(null);
            user.setUsername(clean(updatedUser.getUsername()));
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new Exception("Username/check number is required");
        }
        userRepository.findByUsername(user.getUsername())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalStateException("A user with this username/check number already exists");
                });

        user.setFullName(clean(updatedUser.getFullName()));
        user.setDesignation(clean(updatedUser.getDesignation()));
        user.setPhoneNumber(clean(updatedUser.getPhoneNumber()));
        user.setEmail(clean(updatedUser.getEmail()));
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            user.setPasswordStrength(PasswordStrengthUtil.evaluate(updatedUser.getPassword(), user.getUsername()));
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        String normalizedRole = normalizeRole(updatedUser.getRole());
        if (normalizedRole.isBlank()) {
            normalizedRole = OperationRole.UNASSIGNED;
        }
        user.setRole(normalizedRole);
        user.setStation(resolveScopeStation(normalizedRole, stationId, districtId, regionId));

        return userRepository.save(user);
    }

    public User lockUser(Long id) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found"));
        user.setAccountLockedUntil(LocalDateTime.now().plusYears(100));
        return userRepository.save(user);
    }

    public User unlockUser(Long id) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found"));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        return userRepository.save(user);
    }

    public User changePassword(Long id, String currentPassword, String newPassword) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new Exception("Current password is incorrect");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8) {
            throw new Exception("New password must be at least 8 characters");
        }
        user.setPasswordStrength(PasswordStrengthUtil.evaluate(newPassword, user.getUsername()));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        return userRepository.save(user);
    }

    public List<User> getLockedUsers() {
        return userRepository.findByAccountLockedUntilAfter(LocalDateTime.now());
    }

    public Map<String, Object> userSecuritySummary() {
        List<User> lockedUsers = getLockedUsers();
        long inactiveUsers = userRepository.findAll().stream()
                .filter(user -> !Boolean.TRUE.equals(user.getActive()))
                .count();
        long weakPasswords = userRepository.findAll().stream()
                .filter(user -> "WEAK".equalsIgnoreCase(user.getPasswordStrength()))
                .count();
        return Map.of(
                "totalUsers", userRepository.count(),
                "lockedUsers", lockedUsers.size(),
                "inactiveUsers", inactiveUsers,
                "weakPasswords", weakPasswords
        );
    }

    // DELETE user
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) return false;
        userRepository.deleteById(id);
        return true;
    }

    public List<User> getUsersByStationId(Long stationId) {
        if (stationId == null) {
            return getAllUsers();
        }
        return userRepository.findByStationId(stationId);
    }

    public int deleteLegacyBootstrapUsers(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return 0;
        }
        List<String> normalizedUsernames = usernames.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .toList();
        if (normalizedUsernames.isEmpty()) {
            return 0;
        }
        List<User> purgeCandidates = userRepository.findAll().stream()
                .filter(user -> user.getUsername() != null)
                .filter(user -> normalizedUsernames.contains(user.getUsername().trim().toLowerCase()))
                .toList();
        if (purgeCandidates.isEmpty()) {
            return 0;
        }
        userRepository.deleteAll(purgeCandidates);
        return purgeCandidates.size();
    }

    private String normalizeRole(String role) {
        return OperationRole.normalizeRole(role);
    }

    private boolean requiresStation(String role) {
        return List.of(
                OperationRole.STATION_FIRE_OFFICER,
                OperationRole.STATION_FIRE_OPERATION_OFFICER,
                OperationRole.STATION_OPERATION_OFFICER,
                OperationRole.OPERATION_OFFICER,
                OperationRole.CONTROL_ROOM_OPERATOR,
                OperationRole.CONTROL_ROOM_ATTENDANT,
                OperationRole.TELE_SUPPORT_PERSONNEL
        ).contains(role);
    }

    private boolean requiresRegionalScope(String role) {
        return List.of(
                OperationRole.REGIONAL_FIRE_OFFICER,
                OperationRole.REGIONAL_OPERATION_OFFICER,
                OperationRole.REGIONAL_INVESTIGATION_OFFICER
        ).contains(role);
    }

    private boolean requiresDistrictScope(String role) {
        return List.of(
                OperationRole.DISTRICT_FIRE_OFFICER,
                OperationRole.DISTRICT_OPERATION_OFFICER,
                OperationRole.DISTRICT_INVESTIGATION_OFFICER
        ).contains(role);
    }

    private Station resolveScopeStation(String role, Long stationId, Long districtId, Long regionId) throws Exception {
        if (requiresStation(role)) {
            if (stationId == null) {
                throw new Exception("Station is required for station-level roles");
            }
            return stationRepository.findById(stationId)
                    .orElseThrow(() -> new Exception("Station not found"));
        }
        if (requiresDistrictScope(role)) {
            if (districtId == null) {
                throw new Exception("District is required for district-level roles");
            }
            return firstActiveStationInDistrict(districtId)
                    .orElseThrow(() -> new Exception("No active station is registered in the selected district"));
        }
        if (requiresRegionalScope(role)) {
            if (regionId == null) {
                throw new Exception("Region is required for regional-level roles");
            }
            return districtRepository.findByRegionIdOrderByNameAsc(regionId).stream()
                    .map(district -> firstActiveStationInDistrict(district.getId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new Exception("No active station is registered in the selected region"));
        }
        return null;
    }

    private Optional<Station> firstActiveStationInDistrict(Long districtId) {
        return stationRepository.findByDistrictIdOrderByNameAsc(districtId).stream()
                .filter(station -> Boolean.TRUE.equals(station.getActive()))
                .findFirst();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
