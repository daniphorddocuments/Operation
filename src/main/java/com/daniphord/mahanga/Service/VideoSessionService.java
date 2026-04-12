package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Model.VideoSession;
import com.daniphord.mahanga.Repositories.EmergencyCallRepository;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.VideoSessionRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VideoSessionService {

    private final VideoSessionRepository videoSessionRepository;
    private final EmergencyCallRepository emergencyCallRepository;
    private final IncidentRepository incidentRepository;
    private final SignalHandler signalHandler;
    private final Path storagePath;

    public VideoSessionService(
            VideoSessionRepository videoSessionRepository,
            EmergencyCallRepository emergencyCallRepository,
            IncidentRepository incidentRepository,
            SignalHandler signalHandler,
            @Value("${froms.video.storage-path:videos}") String storagePath
    ) {
        this.videoSessionRepository = videoSessionRepository;
        this.emergencyCallRepository = emergencyCallRepository;
        this.incidentRepository = incidentRepository;
        this.signalHandler = signalHandler;
        this.storagePath = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    public VideoSession startSession(User officer, Long incidentId, String locationLabel, BigDecimal latitude, BigDecimal longitude, boolean audioEnabled) {
        Incident incident = incidentId == null ? null : incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        String participantType = participantTypeFor(officer);
        Long stationId = incident != null && incident.getStation() != null
                ? incident.getStation().getId()
                : officer != null && officer.getStation() != null ? officer.getStation().getId() : null;
        Long districtId = incident != null && incident.getDistrict() != null
                ? incident.getDistrict().getId()
                : officer != null && officer.getStation() != null && officer.getStation().getDistrict() != null ? officer.getStation().getDistrict().getId() : null;
        Long regionId = incident != null && incident.getRegion() != null
                ? incident.getRegion().getId()
                : incident != null && incident.getDistrict() != null && incident.getDistrict().getRegion() != null ? incident.getDistrict().getRegion().getId()
                : officer != null && officer.getStation() != null && officer.getStation().getDistrict() != null && officer.getStation().getDistrict().getRegion() != null
                ? officer.getStation().getDistrict().getRegion().getId()
                : null;

        VideoSession session = new VideoSession();
        session.setOfficer(officer);
        session.setIncident(incident);
        session.setLocationLabel(locationLabel);
        session.setLatitude(latitude);
        session.setLongitude(longitude);
        session.setSessionKey(UUID.randomUUID().toString());
        session.setStatus("LIVE");
        session.setParticipantType(participantType);
        session.setAudioEnabled(audioEnabled);

        VideoSession saved = videoSessionRepository.save(session);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("videoSessionId", saved.getId());
        payload.put("incidentId", incident != null ? incident.getId() : null);
        payload.put("callId", null);
        payload.put("incidentNumber", incident != null ? incident.getIncidentNumber() : "Unassigned");
        payload.put("officer", displayNameFor(officer));
        payload.put("location", saved.getLocationLabel() == null ? "Live location" : saved.getLocationLabel());
        payload.put("startTime", saved.getStartTime().toString());
        payload.put("participantType", participantType);
        payload.put("audioEnabled", Boolean.TRUE.equals(saved.getAudioEnabled()));
        payload.put("stationId", stationId);
        payload.put("districtId", districtId);
        payload.put("regionId", regionId);
        signalHandler.broadcast("VIDEO_STARTED", payload);
        return saved;
    }

    public VideoSession startPublicSession(EmergencyCall emergencyCall, String callerName, String locationLabel, BigDecimal latitude, BigDecimal longitude, boolean audioEnabled) {
        VideoSession session = new VideoSession();
        session.setEmergencyCall(emergencyCall);
        session.setLocationLabel(locationLabel);
        session.setLatitude(latitude);
        session.setLongitude(longitude);
        session.setSessionKey(UUID.randomUUID().toString());
        session.setStatus("LIVE");
        session.setParticipantType("PUBLIC");
        session.setAudioEnabled(audioEnabled);

        VideoSession saved = videoSessionRepository.save(session);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("videoSessionId", saved.getId());
        payload.put("incidentId", null);
        payload.put("callId", emergencyCall.getId());
        payload.put("incidentNumber", emergencyCall.getReportNumber());
        payload.put("officer", callerName == null || callerName.isBlank() ? "Public Reporter" : callerName);
        payload.put("location", saved.getLocationLabel() == null ? "Public scene" : saved.getLocationLabel());
        payload.put("startTime", saved.getStartTime().toString());
        payload.put("participantType", "PUBLIC");
        payload.put("audioEnabled", Boolean.TRUE.equals(saved.getAudioEnabled()));
        payload.put("stationId", emergencyCall.getRoutedStation() != null ? emergencyCall.getRoutedStation().getId() : null);
        payload.put("districtId", emergencyCall.getDistrict() != null ? emergencyCall.getDistrict().getId() : null);
        payload.put("regionId", emergencyCall.getRegion() != null ? emergencyCall.getRegion().getId() : null);
        signalHandler.broadcast("VIDEO_STARTED", payload);
        return saved;
    }

    public VideoSession endSession(Long videoSessionId) {
        VideoSession session = videoSessionRepository.findById(videoSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Video session not found"));
        session.setEndTime(LocalDateTime.now());
        session.setStatus("ENDED");
        VideoSession saved = videoSessionRepository.save(session);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("videoSessionId", saved.getId());
        payload.put("incidentId", saved.getIncident() != null ? saved.getIncident().getId() : null);
        signalHandler.broadcast("VIDEO_ENDED", payload);
        return saved;
    }

    public VideoSession endPublicSession(Long videoSessionId, EmergencyCall emergencyCall) {
        VideoSession session = videoSessionRepository.findById(videoSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Video session not found"));
        if (session.getEmergencyCall() == null || emergencyCall == null || !session.getEmergencyCall().getId().equals(emergencyCall.getId())) {
            throw new IllegalArgumentException("Public video session does not belong to this report");
        }
        return endSession(videoSessionId);
    }

    public VideoSession attachRecording(Long videoSessionId, MultipartFile file) {
        VideoSession session = videoSessionRepository.findById(videoSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Video session not found"));
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Recording file is required");
        }
        try {
            Files.createDirectories(storagePath);
            String filename = "incident-" + (session.getIncident() != null ? session.getIncident().getId() : "general")
                    + (session.getEmergencyCall() != null ? "-call-" + session.getEmergencyCall().getId() : "")
                    + "-session-" + session.getId() + ".webm";
            Path target = storagePath.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            session.setFilePath(target.toString());
            session.setStatus("RECORDED");
            if (session.getEndTime() == null) {
                session.setEndTime(LocalDateTime.now());
            }
            return videoSessionRepository.save(session);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save video recording", exception);
        }
    }

    public List<VideoSession> latestSessions() {
        return videoSessionRepository.findTop20ByOrderByStartTimeDesc();
    }

    public List<VideoSession> sessionsForCall(Long callId) {
        return videoSessionRepository.findByEmergencyCallIdOrderByStartTimeDesc(callId);
    }

    public Resource recordingResource(Long videoSessionId) {
        VideoSession session = videoSessionRepository.findById(videoSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Video session not found"));
        if (session.getFilePath() == null || session.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Recording file not available");
        }
        return new FileSystemResource(session.getFilePath());
    }

    public Map<String, Object> metrics() {
        List<VideoSession> sessions = videoSessionRepository.findAll();
        return Map.of(
                "liveStreams", sessions.stream().filter(session -> "LIVE".equalsIgnoreCase(session.getStatus())).count(),
                "recordedStreams", sessions.stream().filter(session -> "RECORDED".equalsIgnoreCase(session.getStatus())).count(),
                "totalSessions", sessions.size()
        );
    }

    private String participantTypeFor(User officer) {
        String role = OperationRole.normalizeRole(officer == null ? null : officer.getRole());
        if (OperationRole.CONTROL_ROOM_ATTENDANT.equals(role)) {
            return "CONTROL_ROOM";
        }
        return "OFFICER";
    }

    private String displayNameFor(User officer) {
        if (officer != null && officer.getFullName() != null && !officer.getFullName().isBlank()) {
            return officer.getFullName();
        }
        String role = OperationRole.normalizeRole(officer == null ? null : officer.getRole());
        if (OperationRole.CONTROL_ROOM_ATTENDANT.equals(role)) {
            return "Control Room Attendant";
        }
        if (OperationRole.STATION_OPERATION_OFFICER.equals(role)) {
            return "Station Operation Officer";
        }
        return "Live Operator";
    }
}
