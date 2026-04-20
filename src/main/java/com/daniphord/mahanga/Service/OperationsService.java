package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.EmergencyResponse;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Recommendation;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.EmergencyCallRepository;
import com.daniphord.mahanga.Repositories.EmergencyResponseRepository;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.RecommendationRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class OperationsService {

    private final IncidentRepository incidentRepository;
    private final EmergencyCallRepository emergencyCallRepository;
    private final EmergencyResponseRepository emergencyResponseRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final SignalHandler signalHandler;
    private final PythonAiService pythonAiService;
    private final RoleAccessService roleAccessService;
    private final OperationalApprovalService operationalApprovalService;
    private final NotificationService notificationService;
    private final AiRouteService aiRouteService;
    private final GeographyService geographyService;

    public OperationsService(
            IncidentRepository incidentRepository,
            EmergencyCallRepository emergencyCallRepository,
            EmergencyResponseRepository emergencyResponseRepository,
            RecommendationRepository recommendationRepository,
            UserRepository userRepository,
            SignalHandler signalHandler,
            PythonAiService pythonAiService,
            RoleAccessService roleAccessService,
            OperationalApprovalService operationalApprovalService,
            NotificationService notificationService,
            AiRouteService aiRouteService,
            GeographyService geographyService
    ) {
        this.incidentRepository = incidentRepository;
        this.emergencyCallRepository = emergencyCallRepository;
        this.emergencyResponseRepository = emergencyResponseRepository;
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
        this.signalHandler = signalHandler;
        this.pythonAiService = pythonAiService;
        this.roleAccessService = roleAccessService;
        this.operationalApprovalService = operationalApprovalService;
        this.notificationService = notificationService;
        this.aiRouteService = aiRouteService;
        this.geographyService = geographyService;
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public Optional<Incident> getIncident(Long id) {
        return incidentRepository.findById(id);
    }

    public Incident createIncident(Incident incident, User currentUser) {
        normalizeIncident(incident);
        geographyService.applyIncidentLocation(incident);
        String reportLevel = normalizeReportLevel(incident.getReportLevel(), currentUser);
        incident.setReportLevel(reportLevel);
        enforceIncidentRegistrationPermission(currentUser, reportLevel);
        incident.setCreatedBy(currentUser);

        Incident parentIncident = resolveParentIncident(incident);
        EmergencyCall linkedCall = resolveLinkedCall(incident);
        validateIncidentWorkflow(incident, parentIncident, linkedCall, currentUser);

        mergeIncidentWorkflowContext(incident, parentIncident, linkedCall, currentUser);

        if (incident.getLocationDetails() == null || incident.getLocationDetails().isBlank()) {
            incident.setLocationDetails(incident.getVillage() != null && !incident.getVillage().isBlank()
                    ? incident.getVillage()
                    : incident.getDistrict() != null ? incident.getDistrict().getName() : "");
        }
        if (incident.getSource() == null || incident.getSource().isBlank()) {
            incident.setSource(resolveIncidentSource(null, incident.getReportingMeans()));
        }
        if (incident.getReportingMeans() == null || incident.getReportingMeans().isBlank()) {
            incident.setReportingMeans(resolveReportingMeans(null, null));
        }

        Incident saved = incidentRepository.save(incident);
        saved = enrichRouteMetadata(saved);
        Recommendation recommendation = generateRecommendations(saved);
        if (recommendation != null) {
            saved.setAiRecommendationSnapshot(recommendation.getSummary());
            saved.setAiImprovementSnapshot(recommendation.getRationale());
            saved = incidentRepository.save(saved);
        }
        saved = operationalApprovalService.initializeIncidentApproval(saved, currentUser);
        signalHandler.broadcast("INCIDENT_CREATED", incidentEventPayload(saved));
        return saved;
    }

    public Map<String, Object> requestTeleSupport(Long incidentId, String requestMessage, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_TELE_SUPPORT);
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        if (roleAccessService.visibleIncidents(currentUser, List.of(incident)).isEmpty()) {
            throw new IllegalArgumentException("You cannot request tele-support for an incident outside your scope");
        }

        String requester = currentUser == null
                ? "Unknown requester"
                : !isBlank(currentUser.getFullName()) ? currentUser.getFullName() : currentUser.getUsername();
        String comment = isBlank(requestMessage)
                ? "Remote specialist guidance requested for the active scene."
                : requestMessage.trim();

        List<User> recipients = userRepository.findAll().stream()
                .filter(user -> {
                    String role = OperationRole.normalizeRole(user.getRole());
                    return OperationRole.TELE_SUPPORT_PERSONNEL.equals(role)
                            || OperationRole.HEAD_FIRE_FIGHTING_OPERATIONS.equals(role)
                            || OperationRole.HEAD_RESCUE_OPERATIONS.equals(role);
                })
                .toList();

        notificationService.notifyUsers(
                recipients,
                "Tele-support request",
                "Incident " + incident.getIncidentNumber() + " requires tele-support. " + comment,
                "/dashboard#tele-support-module"
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", incident.getId());
        payload.put("incidentNumber", incident.getIncidentNumber());
        payload.put("incidentType", value(incident.getIncidentType()));
        payload.put("status", value(incident.getStatus()));
        payload.put("severity", value(incident.getSeverity()));
        payload.put("location", firstNonBlank(
                incident.getLocationDetails(),
                incident.getDistrict() != null ? incident.getDistrict().getName() : null,
                incident.getVillage()
        ));
        payload.put("message", comment);
        payload.put("requester", requester);
        payload.put("requesterRole", currentUser != null ? normalizeRole(currentUser.getRole()) : "");
        payload.put("stationId", incident.getStation() != null ? incident.getStation().getId() : currentUser != null && currentUser.getStation() != null ? currentUser.getStation().getId() : null);
        payload.put("districtId", incident.getDistrict() != null ? incident.getDistrict().getId() : incident.getStation() != null && incident.getStation().getDistrict() != null ? incident.getStation().getDistrict().getId() : null);
        payload.put("regionId", incident.getRegion() != null ? incident.getRegion().getId()
                : incident.getDistrict() != null && incident.getDistrict().getRegion() != null ? incident.getDistrict().getRegion().getId()
                : incident.getStation() != null && incident.getStation().getDistrict() != null && incident.getStation().getDistrict().getRegion() != null
                ? incident.getStation().getDistrict().getRegion().getId()
                : null);
        payload.put("alertMessage", "HELLO SPECIALIST AGENT NEED TELESUPPORT");
        signalHandler.broadcast("TELE_SUPPORT_REQUESTED", payload);
        return payload;
    }

    public List<Incident> latestInitialIncidentsWithoutFull(User currentUser) {
        return roleAccessService.visibleIncidents(currentUser, incidentRepository.findAll()).stream()
                .filter(incident -> "INITIAL".equalsIgnoreCase(incident.getReportLevel()))
                .filter(incident -> !incidentRepository.existsByParentIncident_IdAndReportLevelIgnoreCase(incident.getId(), "FULL"))
                .sorted(Comparator.comparing(Incident::getReportedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(25)
                .toList();
    }

    public Incident reviewIncident(Long incidentId, boolean approve, String comment, User currentUser) {
        return operationalApprovalService.reviewIncident(incidentId, approve, comment, currentUser);
    }

    public Incident resubmitIncident(Long incidentId, Incident updates, User currentUser) {
        Incident existing = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        if (existing.getCreatedBy() == null || currentUser == null || !existing.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the original initiator can edit and resubmit this incident");
        }
        if (!"DENIED".equalsIgnoreCase(existing.getApprovalStatus())) {
            throw new IllegalStateException("Only denied incidents can be edited and resubmitted");
        }

        normalizeIncident(updates);
        geographyService.applyIncidentLocation(updates);
        String reportLevel = normalizeReportLevel(updates.getReportLevel(), currentUser);
        existing.setReportLevel(reportLevel);
        enforceIncidentRegistrationPermission(currentUser, reportLevel);

        existing.setIncidentType(updates.getIncidentType());
        existing.setSeverity(updates.getSeverity());
        existing.setStatus(updates.getStatus());
        existing.setDescription(updates.getDescription());
        existing.setOperationCategory(updates.getOperationCategory());
        existing.setLocationDetails(updates.getLocationDetails());
        existing.setCause(updates.getCause());
        existing.setActionTaken(updates.getActionTaken());
        existing.setOutcome(updates.getOutcome());
        existing.setSourceReference(updates.getSourceReference());
        existing.setReportingMeans(updates.getReportingMeans());
        existing.setCasualtiesInjured(updates.getCasualtiesInjured());
        existing.setCasualtiesDead(updates.getCasualtiesDead());
        existing.setPropertiesAffected(updates.getPropertiesAffected());
        existing.setIncidentDurationMinutes(updates.getIncidentDurationMinutes());
        existing.setResponseTimeMinutes(updates.getResponseTimeMinutes());
        existing.setDistanceKm(updates.getDistanceKm());
        existing.setOperationDurationMinutes(updates.getOperationDurationMinutes());
        existing.setOilUsedLitres(updates.getOilUsedLitres());
        existing.setResourcesUsed(updates.getResourcesUsed());
        existing.setEquipmentDispatched(updates.getEquipmentDispatched());
        existing.setPersonnelDispatched(updates.getPersonnelDispatched());
        existing.setPersonnelNames(updates.getPersonnelNames());
        existing.setOperationCommander(updates.getOperationCommander());
        existing.setSupervisorName(updates.getSupervisorName());
        existing.setOtherSecurityOrgans(updates.getOtherSecurityOrgans());
        existing.setEffectsOnPeople(updates.getEffectsOnPeople());
        existing.setEffectsOnEnvironment(updates.getEffectsOnEnvironment());
        existing.setInjuredPeopleDetails(updates.getInjuredPeopleDetails());
        existing.setDiedPeopleDetails(updates.getDiedPeopleDetails());
        existing.setCasualtyDemographics(updates.getCasualtyDemographics());
        existing.setVillage(updates.getVillage());
        existing.setWard(updates.getWard());
        existing.setRoadLandmark(updates.getRoadLandmark());
        existing.setRoadSymbol(updates.getRoadSymbol());
        existing.setLatitude(updates.getLatitude());
        existing.setLongitude(updates.getLongitude());
        existing.setReportedAt(updates.getReportedAt() != null ? updates.getReportedAt() : existing.getReportedAt());
        existing.setCallReceivedAt(updates.getCallReceivedAt() != null ? updates.getCallReceivedAt() : existing.getCallReceivedAt());
        existing.setArrivalTime(updates.getArrivalTime());
        existing.setDispatchedAt(updates.getDispatchedAt());
        existing.setResolvedAt(updates.getResolvedAt());
        existing.setReportingPerson(updates.getReportingPerson());
        existing.setReportingContact(updates.getReportingContact());
        existing.setRespondingUnit(updates.getRespondingUnit());
        existing.setParentIncidentId(updates.getParentIncidentId());
        existing.setLinkedCallId(updates.getLinkedCallId());
        existing.setWardId(updates.getWardId());
        existing.setVillageStreetId(updates.getVillageStreetId());
        existing.setRoadLandmarkId(updates.getRoadLandmarkId());

        Incident parentIncident = resolveParentIncident(existing);
        EmergencyCall linkedCall = resolveLinkedCall(existing);
        validateIncidentWorkflow(existing, parentIncident, linkedCall, currentUser);
        mergeIncidentWorkflowContext(existing, parentIncident, linkedCall, currentUser);
        geographyService.applyIncidentLocation(existing);

        if (existing.getLocationDetails() == null || existing.getLocationDetails().isBlank()) {
            existing.setLocationDetails(existing.getVillage() != null && !existing.getVillage().isBlank()
                    ? existing.getVillage()
                    : existing.getDistrict() != null ? existing.getDistrict().getName() : "");
        }
        if (existing.getSource() == null || existing.getSource().isBlank()) {
            existing.setSource(resolveIncidentSource(null, existing.getReportingMeans()));
        }
        if (existing.getReportingMeans() == null || existing.getReportingMeans().isBlank()) {
            existing.setReportingMeans(resolveReportingMeans(null, null));
        }
        existing.setApprovalStatus("DRAFT");
        existing.setApprovalCurrentLevel(normalizeRole(currentUser.getRole()));
        existing.setApprovalLastComment("Updated by initiator and resubmitted for approval.");

        Incident saved = incidentRepository.save(existing);
        saved = enrichRouteMetadata(saved);
        return operationalApprovalService.initializeIncidentApproval(saved, currentUser);
    }

    public EmergencyResponse dispatchResponse(Long incidentId, EmergencyResponse response, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_DISPATCH_INCIDENTS);
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        incident.setStatus("RESPONDING");
        if (incident.getDispatchedAt() == null) {
            incident.setDispatchedAt(LocalDateTime.now());
        }
        incidentRepository.save(incident);
        response.setIncident(incident);
        return emergencyResponseRepository.save(response);
    }

    public Incident completeIncident(Long incidentId, Map<String, Object> payload, User currentUser) {
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_COMPLETE_INCIDENT_REPORT);
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        if (roleAccessService.visibleIncidents(currentUser, List.of(incident)).isEmpty()) {
            throw new IllegalArgumentException("You cannot complete an incident outside your scope");
        }

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        if (payload != null) {
            if (payload.get("outcome") != null) {
                incident.setOutcome(clean(payload.get("outcome").toString()));
            }
            if (payload.get("actionTaken") != null) {
                incident.setActionTaken(clean(payload.get("actionTaken").toString()));
            }
            if (payload.get("operationDurationMinutes") instanceof Number number) {
                incident.setOperationDurationMinutes(number.intValue());
            }
        }

        Incident saved = incidentRepository.save(incident);
        signalHandler.broadcast("INCIDENT_COMPLETED", incidentEventPayload(saved));
        return saved;
    }

    public Map<String, Object> dashboardMetrics() {
        return dashboardMetrics(incidentRepository.findAll());
    }

    public Map<String, Object> dashboardMetrics(List<Incident> incidents) {
        List<EmergencyResponse> responses = emergencyResponseRepository.findAll();
        long active = incidents.stream().filter(incident -> "ACTIVE".equalsIgnoreCase(incident.getStatus())).count();
        long responding = incidents.stream().filter(incident -> "RESPONDING".equalsIgnoreCase(incident.getStatus())).count();
        long resolved = incidents.stream().filter(incident -> "RESOLVED".equalsIgnoreCase(incident.getStatus())).count();
        double averageResponseTime = incidents.stream()
                .filter(incident -> incident.getResponseTimeMinutes() != null)
                .mapToInt(Incident::getResponseTimeMinutes)
                .average()
                .orElse(0);

        Map<String, Long> hotspotCounts = new LinkedHashMap<>();
        incidents.stream()
                .filter(incident -> incident.getDistrict() != null)
                .forEach(incident -> hotspotCounts.merge(incident.getDistrict().getName(), 1L, Long::sum));

        return Map.of(
                "totalIncidents", incidents.size(),
                "activeEmergencies", active,
                "responding", responding,
                "resolved", resolved,
                "averageResponseTime", averageResponseTime,
                "activeResponses", responses.stream().filter(response -> !"COMPLETED".equalsIgnoreCase(response.getStatus())).count(),
                "highRiskAreas", hotspotCounts
        );
    }

    public List<Recommendation> latestRecommendations() {
        return recommendationRepository.findTop10ByOrderByGeneratedAtDesc();
    }

    private Incident resolveParentIncident(Incident incident) {
        if (incident.getParentIncidentId() == null) {
            return null;
        }
        return incidentRepository.findById(incident.getParentIncidentId())
                .orElseThrow(() -> new IllegalArgumentException("Selected initial report was not found"));
    }

    private EmergencyCall resolveLinkedCall(Incident incident) {
        if (incident.getLinkedCallId() == null) {
            return null;
        }
        return emergencyCallRepository.findById(incident.getLinkedCallId())
                .orElseThrow(() -> new IllegalArgumentException("Selected system report was not found"));
    }

    private void validateIncidentWorkflow(Incident incident, Incident parentIncident, EmergencyCall linkedCall, User currentUser) {
        String reportLevel = normalizeReportLevel(incident.getReportLevel(), currentUser);
        if ("FULL".equals(reportLevel)) {
            if (parentIncident == null) {
                throw new IllegalArgumentException("A full report must be linked to an initial report");
            }
            if (!"INITIAL".equalsIgnoreCase(parentIncident.getReportLevel())) {
                throw new IllegalArgumentException("Only initial reports can be used as the source for a full report");
            }
            if (incidentRepository.existsByParentIncident_IdAndReportLevelIgnoreCase(parentIncident.getId(), "FULL")) {
                throw new IllegalArgumentException("A full report already exists for the selected initial report");
            }
        }
        if ("INITIAL".equals(reportLevel) && parentIncident != null) {
            throw new IllegalArgumentException("Initial reports cannot be linked to another incident report");
        }
        if ("INITIAL".equals(reportLevel) && linkedCall != null) {
            if (linkedCall.getRoutedStation() == null) {
                throw new IllegalArgumentException("The selected system report has not been routed to a station");
            }
            if (currentUser != null && currentUser.getStation() != null
                    && !currentUser.getStation().getId().equals(linkedCall.getRoutedStation().getId())) {
                throw new IllegalArgumentException("You can only register system reports routed to your station");
            }
            if (incidentRepository.findFirstByEmergencyCallIdAndReportLevelIgnoreCase(linkedCall.getId(), "INITIAL").isPresent()) {
                throw new IllegalArgumentException("An initial report already exists for the selected system report");
            }
        }
    }

    private void mergeIncidentWorkflowContext(Incident incident, Incident parentIncident, EmergencyCall linkedCall, User currentUser) {
        if (parentIncident != null) {
            incident.setParentIncident(parentIncident);
            incident.setEmergencyCall(parentIncident.getEmergencyCall());
            incident.setSource(parentIncident.getSource());
            incident.setSourceReference(parentIncident.getSourceReference());
            if (isBlank(incident.getReportingMeans())) {
                incident.setReportingMeans(parentIncident.getReportingMeans());
            }
            if (isBlank(incident.getReportingPerson())) {
                incident.setReportingPerson(parentIncident.getReportingPerson());
            }
            if (isBlank(incident.getReportingContact())) {
                incident.setReportingContact(parentIncident.getReportingContact());
            }
            if (incident.getStation() == null) {
                incident.setStation(parentIncident.getStation());
            }
            if (incident.getDistrict() == null) {
                incident.setDistrict(parentIncident.getDistrict());
            }
            if (incident.getRegion() == null) {
                incident.setRegion(parentIncident.getRegion());
            }
            if (incident.getLocationDetails() == null || incident.getLocationDetails().isBlank()) {
                incident.setLocationDetails(parentIncident.getLocationDetails());
            }
            if (incident.getVillage() == null || incident.getVillage().isBlank()) {
                incident.setVillage(parentIncident.getVillage());
            }
            if (incident.getWard() == null || incident.getWard().isBlank()) {
                incident.setWard(parentIncident.getWard());
            }
            if (incident.getRoadLandmark() == null || incident.getRoadLandmark().isBlank()) {
                incident.setRoadLandmark(parentIncident.getRoadLandmark());
            }
            if (incident.getRoadSymbol() == null || incident.getRoadSymbol().isBlank()) {
                incident.setRoadSymbol(parentIncident.getRoadSymbol());
            }
            if (incident.getCallReceivedAt() == null) {
                incident.setCallReceivedAt(parentIncident.getCallReceivedAt());
            }
        }

        if (linkedCall != null) {
            incident.setEmergencyCall(linkedCall);
            incident.setSource(resolveIncidentSource(linkedCall.getSourceChannel(), incident.getReportingMeans()));
            incident.setSourceReference(linkedCall.getReportNumber());
            incident.setReportingMeans(resolveReportingMeans(linkedCall.getSourceChannel(), incident.getReportingMeans()));
            if (isBlank(incident.getReportingPerson())) {
                incident.setReportingPerson(linkedCall.getCallerName());
            }
            if (isBlank(incident.getReportingContact())) {
                incident.setReportingContact(linkedCall.getCallerNumber());
            }
            if (incident.getRegion() == null) {
                incident.setRegion(linkedCall.getRegion());
            }
            if (incident.getDistrict() == null) {
                incident.setDistrict(linkedCall.getDistrict());
            }
            if (incident.getStation() == null) {
                incident.setStation(linkedCall.getRoutedStation());
            }
            if (incident.getLocationDetails() == null || incident.getLocationDetails().isBlank()) {
                incident.setLocationDetails(linkedCall.getLocationText());
            }
            if (incident.getVillage() == null || incident.getVillage().isBlank()) {
                incident.setVillage(linkedCall.getVillage());
            }
            if (incident.getWard() == null || incident.getWard().isBlank()) {
                incident.setWard(linkedCall.getWard());
            }
            if (incident.getRoadLandmark() == null || incident.getRoadLandmark().isBlank()) {
                incident.setRoadLandmark(linkedCall.getLocationText());
            }
            if (incident.getRoadSymbol() == null || incident.getRoadSymbol().isBlank()) {
                incident.setRoadSymbol(linkedCall.getRoadSymbol());
            }
            if (incident.getLatitude() == null) {
                incident.setLatitude(linkedCall.getLatitude());
            }
            if (incident.getLongitude() == null) {
                incident.setLongitude(linkedCall.getLongitude());
            }
            if (incident.getCallReceivedAt() == null) {
                incident.setCallReceivedAt(linkedCall.getCallTime());
            }
        }

        if (currentUser != null && currentUser.getStation() != null) {
            if (incident.getStation() == null) {
                incident.setStation(currentUser.getStation());
            }
            if (incident.getDistrict() == null) {
                incident.setDistrict(currentUser.getStation().getDistrict());
            }
            if (incident.getRegion() == null && currentUser.getStation().getDistrict() != null) {
                incident.setRegion(currentUser.getStation().getDistrict().getRegion());
            }
        }
    }

    private void normalizeIncident(Incident incident) {
        if (incident == null) {
            return;
        }
        incident.setIncidentType(normalizeUpper(incident.getIncidentType()));
        incident.setSeverity(normalizeUpper(incident.getSeverity()));
        incident.setStatus(normalizeUpper(incident.getStatus()));
        incident.setReportLevel(normalizeUpper(incident.getReportLevel()));
        incident.setOperationCategory(normalizeUpper(incident.getOperationCategory()));
        incident.setReportingMeans(normalizeUpper(incident.getReportingMeans()));
        incident.setSource(clean(incident.getSource()));
        incident.setDescription(clean(incident.getDescription()));
        incident.setLocationDetails(clean(incident.getLocationDetails()));
        incident.setVillage(clean(incident.getVillage()));
        incident.setWard(clean(incident.getWard()));
        incident.setRoadLandmark(clean(incident.getRoadLandmark()));
        incident.setRoadSymbol(clean(incident.getRoadSymbol()));
        incident.setCause(clean(incident.getCause()));
        incident.setActionTaken(clean(incident.getActionTaken()));
        incident.setOutcome(clean(incident.getOutcome()));
        incident.setSourceReference(clean(incident.getSourceReference()));
        incident.setReportingPerson(clean(incident.getReportingPerson()));
        incident.setReportingContact(clean(incident.getReportingContact()));
        incident.setRespondingUnit(clean(incident.getRespondingUnit()));
        incident.setResourcesUsed(clean(incident.getResourcesUsed()));
        incident.setEquipmentDispatched(clean(incident.getEquipmentDispatched()));
        incident.setPersonnelDispatched(clean(incident.getPersonnelDispatched()));
        incident.setPersonnelNames(clean(incident.getPersonnelNames()));
        incident.setOperationCommander(clean(incident.getOperationCommander()));
        incident.setSupervisorName(clean(incident.getSupervisorName()));
        incident.setOtherSecurityOrgans(clean(incident.getOtherSecurityOrgans()));
        incident.setEffectsOnPeople(clean(incident.getEffectsOnPeople()));
        incident.setEffectsOnEnvironment(clean(incident.getEffectsOnEnvironment()));
        incident.setInjuredPeopleDetails(clean(incident.getInjuredPeopleDetails()));
        incident.setDiedPeopleDetails(clean(incident.getDiedPeopleDetails()));
        incident.setCasualtyDemographics(clean(incident.getCasualtyDemographics()));
    }

    private Map<String, Object> incidentEventPayload(Incident incident) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", incident.getId());
        payload.put("incidentNumber", incident.getIncidentNumber());
        payload.put("incidentType", incident.getIncidentType());
        payload.put("severity", incident.getSeverity());
        payload.put("status", incident.getStatus());
        payload.put("location", incident.getDistrict() != null ? incident.getDistrict().getName() : incident.getVillage());
        payload.put("ward", incident.getWard());
        payload.put("roadLandmark", incident.getRoadLandmark());
        payload.put("stationId", incident.getStation() != null ? incident.getStation().getId() : null);
        payload.put("districtId", incident.getDistrict() != null ? incident.getDistrict().getId()
                : incident.getStation() != null && incident.getStation().getDistrict() != null ? incident.getStation().getDistrict().getId() : null);
        payload.put("regionId", incident.getRegion() != null ? incident.getRegion().getId()
                : incident.getDistrict() != null && incident.getDistrict().getRegion() != null ? incident.getDistrict().getRegion().getId()
                : incident.getStation() != null && incident.getStation().getDistrict() != null
                && incident.getStation().getDistrict().getRegion() != null ? incident.getStation().getDistrict().getRegion().getId() : null);
        return payload;
    }

    private Recommendation generateRecommendations(Incident incident) {
        Recommendation recommendation = new Recommendation();
        recommendation.setIncident(incident);
        recommendation.setStation(incident.getStation());
        var routePreview = aiRouteService.previewForIncident(incident);
        long districtIncidentCount = incidentRepository.findAll().stream()
                .filter(item -> item.getDistrict() != null && incident.getDistrict() != null)
                .filter(item -> incident.getDistrict().getId().equals(item.getDistrict().getId()))
                .count();
        double districtAverageResponse = incidentRepository.findAll().stream()
                .filter(item -> item.getDistrict() != null && incident.getDistrict() != null)
                .filter(item -> incident.getDistrict().getId().equals(item.getDistrict().getId()))
                .filter(item -> item.getResponseTimeMinutes() != null && item.getResponseTimeMinutes() > 0)
                .mapToInt(Incident::getResponseTimeMinutes)
                .average()
                .orElse(0.0);
        Map<String, Object> recommendationPayload = new LinkedHashMap<>();
        recommendationPayload.put("incidentId", incident.getId());
        recommendationPayload.put("incidentNumber", value(incident.getIncidentNumber()));
        recommendationPayload.put("incidentType", value(incident.getIncidentType()));
        recommendationPayload.put("severity", value(incident.getSeverity()));
        recommendationPayload.put("status", value(incident.getStatus()));
        recommendationPayload.put("details", value(incident.getDescription()));
        recommendationPayload.put("resourcesUsed", value(incident.getResourcesUsed()));
        recommendationPayload.put("ward", value(incident.getWard()));
        recommendationPayload.put("village", value(incident.getVillage()));
        recommendationPayload.put("roadLandmark", value(incident.getRoadLandmark()));
        recommendationPayload.put("roadSymbol", value(incident.getRoadSymbol()));
        recommendationPayload.put("location", value(incident.getLocationDetails()));
        recommendationPayload.put("distanceKm", routePreview.map(AiRouteService.RouteRecommendation::roadDistanceKm).orElse(incident.getDistanceKm() == null ? 0.0 : incident.getDistanceKm()));
        recommendationPayload.put("etaMinutes", routePreview.map(AiRouteService.RouteRecommendation::etaMinutes).orElse(incident.getResponseTimeMinutes() == null ? 0 : incident.getResponseTimeMinutes()));
        recommendationPayload.put("districtIncidentCount", districtIncidentCount);
        recommendationPayload.put("districtAverageResponseTime", districtAverageResponse);
        recommendationPayload.put("approvalStatus", value(incident.getApprovalStatus()));
        var aiRecommendation = pythonAiService.recommendIncident(recommendationPayload);

        if (aiRecommendation.isPresent()) {
            Map<String, Object> payload = aiRecommendation.get();
            recommendation.setRecommendationType("PYTHON_AI");
            recommendation.setRiskLevel(value(payload.get("priority"), "MEDIUM"));
            recommendation.setTitle("AI operational recommendation for " + incident.getIncidentType());
            recommendation.setSummary(buildAiRecommendationSummary(payload));
            recommendation.setRationale(buildAiRecommendationRationale(payload));
        } else {
            recommendation.setRecommendationType("DEPLOYMENT");
            recommendation.setRiskLevel("HIGH".equalsIgnoreCase(incident.getSeverity()) ? "HIGH" : "MEDIUM");
            recommendation.setTitle("Operational recommendation for " + incident.getIncidentType());
            recommendation.setSummary(buildRecommendationSummary(incident));
            recommendation.setRationale("Generated from incident severity, type, and station coverage at report time.");
        }
        return recommendationRepository.save(recommendation);
    }

    private String buildAiRecommendationSummary(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Object> actions = (List<Object>) payload.getOrDefault("recommendedActions", List.of());
        if (actions.isEmpty()) {
            return "Python AI did not return recommended actions, so the incident should be reviewed manually.";
        }
        return actions.stream()
                .map(String::valueOf)
                .limit(3)
                .reduce((left, right) -> left + "; " + right)
                .orElse("Review the incident manually.");
    }

    private String buildAiRecommendationRationale(Map<String, Object> payload) {
        String model = value(payload.get("model"), "python-heuristic");
        String riskScore = value(payload.get("riskScore"), "0");
        String severityScore = value(payload.get("severityScore"), "0");
        @SuppressWarnings("unchecked")
        List<Object> improvements = (List<Object>) payload.getOrDefault("operationalImprovements", List.of());
        String improvementSummary = improvements.stream()
                .map(String::valueOf)
                .limit(3)
                .reduce((left, right) -> left + "; " + right)
                .orElse("No operational improvement hints were returned.");
        return "Generated by " + model + " with risk score " + riskScore + " and severity score " + severityScore + ". " + improvementSummary;
    }

    private String buildRecommendationSummary(Incident incident) {
        if ("HIGH".equalsIgnoreCase(incident.getSeverity())) {
            return "Escalate command visibility, deploy nearest capable unit immediately, and monitor response delays closely.";
        }
        if ("FIRE".equalsIgnoreCase(incident.getIncidentType())) {
            return "Assess nearest fire suppression equipment, confirm water support, and route the fastest available station.";
        }
        if ("RESCUE".equalsIgnoreCase(incident.getIncidentType())) {
            return "Dispatch rescue-trained personnel and prepare medical coordination if casualty risk is present.";
        }
        return "Review local unit readiness, assign the nearest station, and keep the incident under active monitoring.";
    }

    private void enforceIncidentRegistrationPermission(User currentUser, String reportLevel) {
        if ("FULL".equalsIgnoreCase(reportLevel)) {
            roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_COMPLETE_INCIDENT_REPORT);
            return;
        }
        roleAccessService.enforceAction(currentUser, RoleResponsibilityService.ACTION_REGISTER_INITIAL_INCIDENT);
    }

    private String normalizeReportLevel(String requestedLevel, User currentUser) {
        if (requestedLevel == null || requestedLevel.isBlank()) {
            return roleAccessService.canCompleteIncidentReport(currentUser) ? "FULL" : "INITIAL";
        }
        return requestedLevel.trim().toUpperCase();
    }

    private String resolveIncidentSource(String sourceChannel, String reportingMeans) {
        String normalizedMeans = resolveReportingMeans(sourceChannel, reportingMeans);
        return switch (normalizedMeans) {
            case "114_CALL" -> "114_CALL";
            case "PUBLIC_PORTAL" -> "PUBLIC_PORTAL";
            case "PHYSICAL" -> "PHYSICAL_REPORT";
            case "RADIO" -> "RADIO_REPORT";
            case "SYSTEM" -> "SYSTEM_REPORTED";
            default -> "STATION";
        };
    }

    private String resolveReportingMeans(String sourceChannel, String reportingMeans) {
        if (!isBlank(reportingMeans)) {
            String normalized = reportingMeans.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            return switch (normalized) {
                case "114", "CALL_114", "114_CALL", "HOTLINE_114" -> "114_CALL";
                case "PUBLIC", "PUBLIC_PORTAL", "PORTAL" -> "PUBLIC_PORTAL";
                case "SYSTEM", "SYSTEM_REPORTED" -> "SYSTEM";
                case "PHYSICAL", "WALK_IN", "PHYSICAL_REPORT" -> "PHYSICAL";
                case "RADIO", "RADIO_REPORT" -> "RADIO";
                default -> normalized;
            };
        }
        if (!isBlank(sourceChannel)) {
            String normalizedChannel = sourceChannel.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            if ("114_CALL".equals(normalizedChannel)) {
                return "114_CALL";
            }
            if ("PUBLIC_PORTAL".equals(normalizedChannel)) {
                return "PUBLIC_PORTAL";
            }
        }
        return "PHYSICAL";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpper(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toUpperCase();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String value(Object value, String fallback) {
        String normalized = value(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalizeRole(String role) {
        return OperationRole.normalizeRole(role);
    }

    private Incident enrichRouteMetadata(Incident incident) {
        if (incident == null) {
            return null;
        }
        return aiRouteService.previewForIncident(incident)
                .map(route -> {
                    incident.setDistanceKm(route.roadDistanceKm());
                    return incidentRepository.save(incident);
                })
                .orElse(incident);
    }
}
