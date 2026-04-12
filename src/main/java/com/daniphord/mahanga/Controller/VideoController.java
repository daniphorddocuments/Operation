package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.VideoSession;
import com.daniphord.mahanga.Service.ControlRoomService;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.RoleAccessService;
import com.daniphord.mahanga.Service.VideoSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/video")
public class VideoController {

    private final VideoSessionService videoSessionService;
    private final UserRepository userRepository;
    private final ControlRoomService controlRoomService;
    private final RoleAccessService roleAccessService;

    public VideoController(
            VideoSessionService videoSessionService,
            UserRepository userRepository,
            ControlRoomService controlRoomService,
            RoleAccessService roleAccessService
    ) {
        this.videoSessionService = videoSessionService;
        this.userRepository = userRepository;
        this.controlRoomService = controlRoomService;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping("/sessions")
    @ResponseBody
    public ResponseEntity<?> latestSessions(HttpSession httpSession) {
        User currentUser = currentUser(httpSession);
        if (currentUser == null || !roleAccessService.canViewLiveVideo(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(Map.of(
                "items", videoSessionService.latestSessions().stream().map(session -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", session.getId());
                    item.put("status", session.getStatus());
                    item.put("startTime", session.getStartTime());
                    item.put("endTime", session.getEndTime());
                    item.put("locationLabel", session.getLocationLabel() == null ? "" : session.getLocationLabel());
                    item.put("officer", session.getOfficer() != null ? session.getOfficer().getFullName() : "Live Operator");
                    item.put("incidentId", session.getIncident() != null ? session.getIncident().getId() : null);
                    item.put("filePath", session.getFilePath() == null ? "" : session.getFilePath());
                    item.put("participantType", session.getParticipantType());
                    item.put("audioEnabled", Boolean.TRUE.equals(session.getAudioEnabled()));
                    return item;
                }).toList(),
                "metrics", videoSessionService.metrics()
        ));
    }

    @PostMapping("/sessions/start")
    @ResponseBody
    public ResponseEntity<?> startSession(
            @RequestParam(required = false) Long incidentId,
            @RequestParam(required = false) String locationLabel,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(name = "audioEnabled", defaultValue = "true") boolean audioEnabled,
            HttpSession httpSession
    ) {
        User currentUser = currentUser(httpSession);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        if (!roleAccessService.canPublishLiveVideo(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only Control Room Attendants and Station Operation Officers can start live streams"));
        }
        return ResponseEntity.ok(sessionPayload(videoSessionService.startSession(currentUser, incidentId, locationLabel, latitude, longitude, audioEnabled)));
    }

    @PostMapping("/sessions/{id}/end")
    @ResponseBody
    public ResponseEntity<?> endSession(@PathVariable Long id, HttpSession httpSession) {
        User currentUser = currentUser(httpSession);
        if (currentUser == null || !roleAccessService.canPublishLiveVideo(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(sessionPayload(videoSessionService.endSession(id)));
    }

    @PostMapping("/public/reports/{reportNumber}/sessions/start")
    @ResponseBody
    public ResponseEntity<?> startPublicSession(
            @PathVariable String reportNumber,
            @RequestParam String token,
            @RequestParam(required = false) String locationLabel,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(name = "audioEnabled", defaultValue = "true") boolean audioEnabled
    ) {
        try {
            EmergencyCall call = controlRoomService.publicReport(reportNumber, token);
            return ResponseEntity.ok(sessionPayload(videoSessionService.startPublicSession(call, call.getCallerName(), locationLabel, latitude, longitude, audioEnabled)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/public/reports/{reportNumber}/sessions/{id}/end")
    @ResponseBody
    public ResponseEntity<?> endPublicSession(
            @PathVariable String reportNumber,
            @PathVariable Long id,
            @RequestParam String token
    ) {
        try {
            EmergencyCall call = controlRoomService.publicReport(reportNumber, token);
            return ResponseEntity.ok(sessionPayload(videoSessionService.endPublicSession(id, call)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> uploadRecording(
            @RequestParam Long videoSessionId,
            @RequestParam("file") MultipartFile file,
            HttpSession httpSession
    ) {
        User currentUser = currentUser(httpSession);
        if (currentUser == null || !roleAccessService.canPublishLiveVideo(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(videoSessionService.attachRecording(videoSessionId, file));
    }

    @GetMapping("/sessions/{id}/file")
    public ResponseEntity<?> recordingFile(@PathVariable Long id, HttpSession httpSession) {
        User currentUser = currentUser(httpSession);
        if (currentUser == null || !roleAccessService.canViewLiveVideo(currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        Resource resource = videoSessionService.recordingResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "video/webm")
                .body(resource);
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }

    private Map<String, Object> sessionPayload(VideoSession session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", session.getId());
        payload.put("status", session.getStatus());
        payload.put("participantType", session.getParticipantType());
        payload.put("startTime", session.getStartTime());
        payload.put("endTime", session.getEndTime());
        payload.put("locationLabel", session.getLocationLabel() == null ? "" : session.getLocationLabel());
        payload.put("incidentId", session.getIncident() != null ? session.getIncident().getId() : null);
        payload.put("callId", session.getEmergencyCall() != null ? session.getEmergencyCall().getId() : null);
        payload.put("sessionKey", session.getSessionKey());
        payload.put("audioEnabled", Boolean.TRUE.equals(session.getAudioEnabled()));
        return payload;
    }
}
