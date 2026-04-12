package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.EmergencyCallMessage;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.EmergencyCallRepository;
import com.daniphord.mahanga.Repositories.EmergencyCallMessageRepository;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.RegionRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ControlRoomService {

    private final EmergencyCallRepository emergencyCallRepository;
    private final EmergencyCallMessageRepository emergencyCallMessageRepository;
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final StationRepository stationRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AiRouteService aiRouteService;
    private final SignalHandler signalHandler;
    private final RoleAccessService roleAccessService;
    private final GeographyService geographyService;

    public ControlRoomService(
            EmergencyCallRepository emergencyCallRepository,
            EmergencyCallMessageRepository emergencyCallMessageRepository,
            RegionRepository regionRepository,
            DistrictRepository districtRepository,
            StationRepository stationRepository,
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AiRouteService aiRouteService,
            SignalHandler signalHandler,
            RoleAccessService roleAccessService,
            GeographyService geographyService
    ) {
        this.emergencyCallRepository = emergencyCallRepository;
        this.emergencyCallMessageRepository = emergencyCallMessageRepository;
        this.regionRepository = regionRepository;
        this.districtRepository = districtRepository;
        this.stationRepository = stationRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.aiRouteService = aiRouteService;
        this.signalHandler = signalHandler;
        this.roleAccessService = roleAccessService;
        this.geographyService = geographyService;
    }

    public EmergencyCall logPublicReport(EmergencyCall emergencyCall, Long regionId, Long districtId, Long stationId) {
        resolveGeography(emergencyCall, regionId, districtId, stationId);
        geographyService.applyEmergencyCallLocation(emergencyCall);
        if (emergencyCall.getSourceChannel() == null || emergencyCall.getSourceChannel().isBlank()) {
            emergencyCall.setSourceChannel("PUBLIC_PORTAL");
        }
        emergencyCall.setStatus("REPORTED");
        EmergencyCall saved = emergencyCallRepository.save(emergencyCall);
        notifyStationOfPublicReport(saved);
        signalHandler.broadcast("PUBLIC_CALL_CREATED", callEventPayload(saved, "FIRE STATION FIRE STATION PUBLIC CALLING EMERGENCY PLEASE"));
        return saved;
    }

    public EmergencyCall publicReport(String reportNumber, String token) {
        return emergencyCallRepository.findByReportNumberAndPublicAccessToken(reportNumber, token)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    public EmergencyCall findById(Long callId) {
        return emergencyCallRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Call not found"));
    }

    public List<EmergencyCall> latestCalls() {
        return emergencyCallRepository.findTop20ByOrderByCallTimeDesc();
    }

    public Map<String, Object> dashboardMetrics() {
        return dashboardMetrics(emergencyCallRepository.findAll());
    }

    public Map<String, Object> dashboardMetrics(List<EmergencyCall> calls) {
        return Map.of(
                "incomingCalls", calls.stream().filter(call -> "REPORTED".equalsIgnoreCase(call.getStatus())).count(),
                "activeCalls", calls.stream().filter(call -> "ROUTED".equalsIgnoreCase(call.getStatus()) || "ACTIVE".equalsIgnoreCase(call.getStatus())).count(),
                "recordingsAvailable", calls.stream().filter(call -> call.getRecordingUrl() != null && !call.getRecordingUrl().isBlank()).count()
        );
    }

    public EmergencyCall routeCall(Long callId, Long stationId, String recordingUrl, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_DISPATCH_INCIDENTS);
        return acceptCall(callId, stationId, recordingUrl, currentUser);
    }

    public EmergencyCall acceptCall(Long callId, Long stationId, String recordingUrl, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_HANDLE_CALLS);
        EmergencyCall emergencyCall = emergencyCallRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Call not found"));
        Station station = resolveTargetStation(emergencyCall, stationId);
        emergencyCall.setRoutedStation(station);
        emergencyCall.setRecordingUrl(recordingUrl);
        emergencyCall.setStatus("ROUTED");
        EmergencyCall savedCall = emergencyCallRepository.save(emergencyCall);
        refreshLinkedInitialIncident(savedCall, station);
        notifyDispatchedStation(savedCall, station);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callId", savedCall.getId());
        payload.put("reportNumber", savedCall.getReportNumber());
        payload.put("callerNumber", savedCall.getCallerNumber());
        payload.put("station", station.getName());
        payload.put("stationId", station.getId());
        payload.put("districtId", station.getDistrict() != null ? station.getDistrict().getId() : null);
        payload.put("regionId", station.getDistrict() != null && station.getDistrict().getRegion() != null ? station.getDistrict().getRegion().getId() : null);
        payload.put("incidentType", savedCall.getIncidentType());
        signalHandler.broadcast("CALL_ACCEPTED", payload);
        return savedCall;
    }

    public EmergencyCall rejectCall(Long callId, String reason, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_HANDLE_CALLS);
        EmergencyCall emergencyCall = emergencyCallRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Call not found"));
        emergencyCall.setStatus("REJECTED");
        if (reason != null && !reason.isBlank()) {
            String details = emergencyCall.getDetails() == null ? "" : emergencyCall.getDetails().trim() + System.lineSeparator();
            emergencyCall.setDetails((details + "Rejected reason: " + reason.trim()).trim());
        }
        EmergencyCall savedCall = emergencyCallRepository.save(emergencyCall);
        signalHandler.broadcast("CALL_REJECTED", Map.of(
                "callId", savedCall.getId(),
                "reportNumber", savedCall.getReportNumber(),
                "reason", reason == null ? "" : reason
        ));
        return savedCall;
    }

    public List<EmergencyCallMessage> messages(Long callId) {
        return emergencyCallMessageRepository.findByEmergencyCallIdOrderByCreatedAtAsc(callId);
    }

    public List<EmergencyCall> callsAvailableForIncidentRegistration(User currentUser) {
        if (currentUser == null || currentUser.getStation() == null) {
            return List.of();
        }
        Long stationId = currentUser.getStation().getId();
        return emergencyCallRepository.findAll().stream()
                .filter(call -> call.getRoutedStation() != null && stationId.equals(call.getRoutedStation().getId()))
                .filter(call -> !"REJECTED".equalsIgnoreCase(call.getStatus()))
                .filter(call -> incidentRepository.findFirstByEmergencyCallIdAndReportLevelIgnoreCase(call.getId(), "INITIAL").isEmpty())
                .sorted(java.util.Comparator.comparing(EmergencyCall::getCallTime, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed())
                .limit(25)
                .toList();
    }

    public EmergencyCallMessage addPublicMessage(String reportNumber, String token, String message) {
        EmergencyCall call = publicReport(reportNumber, token);
        EmergencyCallMessage saved = persistMessage(call, null, "PUBLIC", message);
        broadcastMessage(saved);
        return saved;
    }

    public EmergencyCallMessage addControlRoomMessage(Long callId, User sender, String message) {
        roleAccessService.enforceAction(sender, RoleResponsibilityService.ACTION_HANDLE_CALLS);
        EmergencyCall call = findById(callId);
        if (sender == null || call.getRoutedStation() == null || sender.getStation() == null || !call.getRoutedStation().getId().equals(sender.getStation().getId())) {
            throw new IllegalArgumentException("You are not allowed to send a message for this report");
        }
        EmergencyCallMessage saved = persistMessage(call, sender, "CONTROL_ROOM", message);
        broadcastMessage(saved);
        return saved;
    }

    private EmergencyCallMessage persistMessage(EmergencyCall call, User sender, String senderType, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank");
        }
        EmergencyCallMessage entity = new EmergencyCallMessage();
        entity.setEmergencyCall(call);
        entity.setSenderUser(sender);
        entity.setSenderType(senderType);
        entity.setMessage(message.trim());
        return emergencyCallMessageRepository.save(entity);
    }

    private void broadcastMessage(EmergencyCallMessage saved) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reportNumber", saved.getEmergencyCall().getReportNumber());
        payload.put("callId", saved.getEmergencyCall().getId());
        payload.put("senderType", saved.getSenderType());
        payload.put("message", saved.getMessage());
        payload.put("createdAt", saved.getCreatedAt().toString());
        payload.put("stationId", saved.getEmergencyCall().getRoutedStation() != null ? saved.getEmergencyCall().getRoutedStation().getId() : null);
        payload.put("districtId", saved.getEmergencyCall().getDistrict() != null ? saved.getEmergencyCall().getDistrict().getId() : null);
        payload.put("regionId", saved.getEmergencyCall().getRegion() != null ? saved.getEmergencyCall().getRegion().getId() : null);
        payload.put("alertMessage", "PUBLIC".equalsIgnoreCase(saved.getSenderType())
                ? "FIRE STATION FIRE STATION PUBLIC CALLING EMERGENCY PLEASE"
                : "");
        signalHandler.broadcast("PUBLIC_REPORT_MESSAGE", payload);
    }

    private void resolveGeography(EmergencyCall emergencyCall, Long regionId, Long districtId, Long stationId) {
        if (regionId == null || districtId == null || stationId == null) {
            throw new IllegalArgumentException("Region, district, and nearby fire station are required");
        }
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("Region not found"));
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new IllegalArgumentException("District not found"));
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));
        if (district.getRegion() == null || !district.getRegion().getId().equals(region.getId())) {
            throw new IllegalArgumentException("Selected district does not belong to the chosen region");
        }
        if (station.getDistrict() == null || !station.getDistrict().getId().equals(district.getId())) {
            throw new IllegalArgumentException("Selected station does not belong to the chosen district");
        }
        emergencyCall.setRegion(region);
        emergencyCall.setDistrict(district);
        emergencyCall.setRoutedStation(station);
    }

    private Station resolveTargetStation(EmergencyCall emergencyCall, Long stationId) {
        if (stationId != null) {
            return stationRepository.findById(stationId)
                    .orElseThrow(() -> new IllegalArgumentException("Station not found"));
        }
        return aiRouteService.previewForCall(emergencyCall)
                .flatMap(recommendation -> stationRepository.findById(recommendation.stationId()))
                .or(() -> emergencyCall.getRoutedStation() == null ? java.util.Optional.empty() : java.util.Optional.of(emergencyCall.getRoutedStation()))
                .orElseThrow(() -> new IllegalArgumentException("Unable to determine nearest station for this call"));
    }

    private Incident refreshLinkedInitialIncident(EmergencyCall call, Station station) {
        java.util.Optional<Incident> existing = incidentRepository.findFirstByEmergencyCallIdAndReportLevelIgnoreCase(call.getId(), "INITIAL");
        if (existing.isEmpty()) {
            return null;
        }
        Incident incident = existing.get();
        incident.setStation(station);
        incident.setDistrict(call.getDistrict());
        incident.setRegion(call.getRegion());
        incident.setLatitude(call.getLatitude());
        incident.setLongitude(call.getLongitude());
        incident.setStatus("ACTIVE");
        if (incident.getSourceReference() == null || incident.getSourceReference().isBlank()) {
            incident.setSourceReference(call.getReportNumber());
        }
        if (incident.getReportingPerson() == null || incident.getReportingPerson().isBlank()) {
            incident.setReportingPerson(call.getCallerName());
        }
        if (incident.getReportingContact() == null || incident.getReportingContact().isBlank()) {
            incident.setReportingContact(call.getCallerNumber());
        }
        if (incident.getCallReceivedAt() == null) {
            incident.setCallReceivedAt(call.getCallTime() == null ? LocalDateTime.now() : call.getCallTime());
        }
        if (incident.getDistanceKm() == null) {
            aiRouteService.previewForCall(call)
                    .ifPresent(route -> incident.setDistanceKm(route.roadDistanceKm()));
        }
        return incidentRepository.save(incident);
    }

    private void notifyDispatchedStation(EmergencyCall call, Station station) {
        List<User> recipients = userRepository.findAll().stream()
                .filter(user -> user.getStation() != null && station.getId().equals(user.getStation().getId()))
                .filter(user -> List.of(
                        OperationRole.STATION_OPERATION_OFFICER,
                        OperationRole.STATION_FIRE_OPERATION_OFFICER,
                        OperationRole.STATION_FIRE_OFFICER
                ).contains(OperationRole.normalizeRole(user.getRole())))
                .toList();
        if (recipients.isEmpty()) {
            return;
        }
        notificationService.notifyUsers(
                recipients,
                "New 114 dispatch assignment",
                "Call " + call.getReportNumber() + " was accepted and routed to " + station.getName() + ". Review the AI route and incident details immediately.",
                "/control-room/dashboard"
        );
    }

    private void notifyStationOfPublicReport(EmergencyCall call) {
        Station station = call.getRoutedStation();
        if (station == null) {
            return;
        }
        List<User> recipients = userRepository.findAll().stream()
                .filter(user -> user.getStation() != null && station.getId().equals(user.getStation().getId()))
                .filter(user -> List.of(
                        OperationRole.STATION_OPERATION_OFFICER,
                        OperationRole.STATION_FIRE_OPERATION_OFFICER,
                        OperationRole.STATION_FIRE_OFFICER
                ).contains(OperationRole.normalizeRole(user.getRole())))
                .toList();
        if (recipients.isEmpty()) {
            return;
        }
        notificationService.notifyUsers(
                recipients,
                "New public emergency report",
                "Public report " + call.getReportNumber() + " was sent to " + station.getName() + ". Review the caller details, messages, and live video if available.",
                "/dashboard"
        );
    }

    private Map<String, Object> callEventPayload(EmergencyCall call, String alertMessage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callId", call.getId());
        payload.put("reportNumber", call.getReportNumber());
        payload.put("callerName", call.getCallerName());
        payload.put("callerNumber", call.getCallerNumber());
        payload.put("incidentType", call.getIncidentType());
        payload.put("status", call.getStatus());
        payload.put("stationId", call.getRoutedStation() != null ? call.getRoutedStation().getId() : null);
        payload.put("stationName", call.getRoutedStation() != null ? call.getRoutedStation().getName() : "");
        payload.put("districtId", call.getDistrict() != null ? call.getDistrict().getId() : null);
        payload.put("regionId", call.getRegion() != null ? call.getRegion().getId() : null);
        payload.put("location", call.getLocationText());
        payload.put("alertMessage", alertMessage);
        return payload;
    }
}
