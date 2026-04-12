package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.GeographyService;
import com.daniphord.mahanga.Service.RoleAccessService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/geography")
public class GeographyController {

    private final GeographyService geographyService;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;

    public GeographyController(GeographyService geographyService, UserRepository userRepository, RoleAccessService roleAccessService) {
        this.geographyService = geographyService;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping("/regions")
    public ResponseEntity<?> regions() {
        return ResponseEntity.ok(geographyService.regionViews());
    }

    @GetMapping("/regions/{regionId}/districts")
    public ResponseEntity<?> districts(@PathVariable Long regionId) {
        return ResponseEntity.ok(geographyService.districtViews(regionId));
    }

    @GetMapping("/districts/{districtId}/stations")
    public ResponseEntity<?> stations(@PathVariable Long districtId) {
        return ResponseEntity.ok(geographyService.stationViews(districtId));
    }

    @GetMapping("/districts/{districtId}/wards")
    public ResponseEntity<?> wards(@PathVariable Long districtId) {
        return ResponseEntity.ok(geographyService.wardViews(districtId));
    }

    @GetMapping("/wards/{wardId}/villages-streets")
    public ResponseEntity<?> villagesAndStreets(@PathVariable Long wardId) {
        return ResponseEntity.ok(geographyService.villageStreetViews(wardId));
    }

    @GetMapping("/villages-streets/{villageStreetId}/road-landmarks")
    public ResponseEntity<?> roadLandmarks(@PathVariable Long villageStreetId) {
        return ResponseEntity.ok(geographyService.roadLandmarkViews(villageStreetId));
    }

    @PostMapping("/regions")
    public ResponseEntity<?> createRegion(@RequestBody Map<String, String> payload, HttpSession session) {
        if (!isSuperAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can register regions"));
        }
        try {
            return ResponseEntity.ok(geographyService.createRegion(payload.get("name"), payload.get("code")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PutMapping("/regions/{id}")
    public ResponseEntity<?> updateRegion(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpSession session) {
        if (!isSuperAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can update regions"));
        }
        try {
            return ResponseEntity.ok(geographyService.updateRegion(id, payload.get("name"), payload.get("code")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/districts")
    public ResponseEntity<?> createDistrict(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (!isSuperAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can register districts"));
        }
        Object regionId = payload.get("regionId");
        if (!(regionId instanceof Number number)) {
            return ResponseEntity.badRequest().body(Map.of("error", "regionId is required"));
        }
        try {
            return ResponseEntity.ok(geographyService.createDistrict(number.longValue(), payload.get("name").toString()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PutMapping("/districts/{id}")
    public ResponseEntity<?> updateDistrict(@PathVariable Long id, @RequestBody Map<String, Object> payload, HttpSession session) {
        if (!isSuperAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can update districts"));
        }
        Object regionId = payload.get("regionId");
        if (!(regionId instanceof Number number)) {
            return ResponseEntity.badRequest().body(Map.of("error", "regionId is required"));
        }
        try {
            return ResponseEntity.ok(geographyService.updateDistrict(id, number.longValue(), payload.get("name").toString()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/stations")
    public ResponseEntity<?> createStation(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (!isSuperAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can register stations"));
        }
        Object districtId = payload.get("districtId");
        if (!(districtId instanceof Number number)) {
            return ResponseEntity.badRequest().body(Map.of("error", "districtId is required"));
        }
        try {
            return ResponseEntity.ok(geographyService.createStation(
                    number.longValue(),
                    stringValue(payload.get("name")),
                    stringValue(payload.get("village")),
                    stringValue(payload.get("controlRoomNumber")),
                    stringValue(payload.get("phoneNumber"))
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PutMapping("/stations/{id}")
    public ResponseEntity<?> updateStation(@PathVariable Long id, @RequestBody Map<String, Object> payload, HttpSession session) {
        if (!isSuperAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only system admin can update stations"));
        }
        Object districtId = payload.get("districtId");
        if (!(districtId instanceof Number number)) {
            return ResponseEntity.badRequest().body(Map.of("error", "districtId is required"));
        }
        try {
            return ResponseEntity.ok(geographyService.updateStation(
                    id,
                    number.longValue(),
                    stringValue(payload.get("name")),
                    stringValue(payload.get("village")),
                    stringValue(payload.get("controlRoomNumber")),
                    stringValue(payload.get("phoneNumber")),
                    payload.get("active") instanceof Boolean active ? active : null
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    private boolean isSuperAdmin(HttpSession session) {
        return roleAccessService.canManageSystemSettings(currentUser(session));
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
