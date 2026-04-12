package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.RoutePlan;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Repositories.EmergencyCallRepository;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.RoutePlanRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiRouteService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double BASE_ROAD_MULTIPLIER = 1.18;
    private static final double BASE_SPEED_KPH = 42.0;

    private final StationRepository stationRepository;
    private final EmergencyCallRepository emergencyCallRepository;
    private final IncidentRepository incidentRepository;
    private final RoutePlanRepository routePlanRepository;

    public AiRouteService(
            StationRepository stationRepository,
            EmergencyCallRepository emergencyCallRepository,
            IncidentRepository incidentRepository,
            RoutePlanRepository routePlanRepository
    ) {
        this.stationRepository = stationRepository;
        this.emergencyCallRepository = emergencyCallRepository;
        this.incidentRepository = incidentRepository;
        this.routePlanRepository = routePlanRepository;
    }

    public Optional<RouteRecommendation> previewForCall(Long callId) {
        EmergencyCall call = emergencyCallRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Call not found"));
        return previewForCall(call);
    }

    public Optional<RouteRecommendation> previewForIncident(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        return previewForIncident(incident);
    }

    public Optional<RouteRecommendation> previewForCall(EmergencyCall call) {
        if (call == null || call.getLatitude() == null || call.getLongitude() == null) {
            return Optional.empty();
        }
        return candidateStations(call.getDistrict() != null ? call.getDistrict().getId() : null).stream()
                .filter(this::hasCoordinates)
                .map(station -> buildRecommendation(
                        "CALL",
                        call.getId(),
                        call.getReportNumber(),
                        call.getIncidentType(),
                        station,
                        call.getLatitude().doubleValue(),
                        call.getLongitude().doubleValue(),
                        originLabel(call),
                        historicalLearningFactor(station)
                ))
                .min(Comparator.comparingInt(RouteRecommendation::etaMinutes));
    }

    public Optional<RouteRecommendation> previewForIncident(Incident incident) {
        if (incident == null || incident.getLatitude() == null || incident.getLongitude() == null) {
            return Optional.empty();
        }
        return candidateStations(incident.getDistrict() != null ? incident.getDistrict().getId() : null).stream()
                .filter(this::hasCoordinates)
                .map(station -> buildRecommendation(
                        "INCIDENT",
                        incident.getId(),
                        incident.getIncidentNumber(),
                        incident.getIncidentType(),
                        station,
                        incident.getLatitude().doubleValue(),
                        incident.getLongitude().doubleValue(),
                        originLabel(incident),
                        historicalLearningFactor(station)
                ))
                .min(Comparator.comparingInt(RouteRecommendation::etaMinutes));
    }

    public RouteRecommendation generateAndStoreForCall(Long callId) {
        EmergencyCall call = emergencyCallRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Call not found"));
        RouteRecommendation recommendation = previewForCall(call)
                .orElseThrow(() -> new IllegalArgumentException("Call does not contain GPS coordinates for route generation"));
        persist(call, null, recommendation);
        return recommendation;
    }

    public RouteRecommendation generateAndStoreForIncident(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        RouteRecommendation recommendation = previewForIncident(incident)
                .orElseThrow(() -> new IllegalArgumentException("Incident does not contain GPS coordinates for route generation"));
        persist(null, incident, recommendation);
        return recommendation;
    }

    private void persist(EmergencyCall call, Incident incident, RouteRecommendation recommendation) {
        RoutePlan plan = new RoutePlan();
        plan.setEmergencyCall(call);
        plan.setIncident(incident);
        plan.setStation(stationRepository.findById(recommendation.stationId())
                .orElseThrow(() -> new IllegalArgumentException("Recommended station not found")));
        plan.setStraightDistanceKm(roundDecimal(recommendation.straightDistanceKm()));
        plan.setRoadDistanceKm(roundDecimal(recommendation.roadDistanceKm()));
        plan.setEtaMinutes(recommendation.etaMinutes());
        plan.setLearningFactor(roundDecimal(recommendation.learningFactor()));
        plan.setDirectionsText(String.join("\n", recommendation.directions()));
        plan.setCreatedAt(LocalDateTime.now());
        routePlanRepository.save(plan);
    }

    private RouteRecommendation buildRecommendation(
            String sourceType,
            Long sourceId,
            String sourceReference,
            String incidentType,
            Station station,
            double originLat,
            double originLon,
            String originLabel,
            double learningFactor
    ) {
        double destinationLat = station.getLatitude().doubleValue();
        double destinationLon = station.getLongitude().doubleValue();
        double straightDistanceKm = haversine(originLat, originLon, destinationLat, destinationLon);
        double roadDistanceKm = straightDistanceKm * roadMultiplierFor(station, learningFactor);
        int etaMinutes = Math.max(3, (int) Math.round((roadDistanceKm / BASE_SPEED_KPH) * 60.0 * learningFactor));
        double bearing = bearing(destinationLat, destinationLon, originLat, originLon);
        String direction = cardinalDirection(bearing);
        List<String> directions = directionsFor(station, originLabel, roadDistanceKm, etaMinutes, direction, incidentType);
        return new RouteRecommendation(
                sourceType,
                sourceId,
                sourceReference == null ? "" : sourceReference,
                station.getId(),
                station.getName(),
                station.getDistrict() != null ? station.getDistrict().getName() : "",
                round(straightDistanceKm),
                round(roadDistanceKm),
                etaMinutes,
                round(learningFactor),
                directions,
                embedUrl(destinationLat, destinationLon, originLat, originLon),
                directionsUrl(destinationLat, destinationLon, originLat, originLon)
        );
    }

    private List<Station> candidateStations(Long districtId) {
        List<Station> districtStations = districtId == null
                ? List.of()
                : stationRepository.findByDistrictIdOrderByNameAsc(districtId).stream()
                .filter(station -> Boolean.TRUE.equals(station.getActive()))
                .toList();
        if (!districtStations.isEmpty()) {
            return districtStations;
        }
        return stationRepository.findByActiveTrue();
    }

    private boolean hasCoordinates(Station station) {
        return station != null && station.getLatitude() != null && station.getLongitude() != null;
    }

    private double historicalLearningFactor(Station station) {
        double averageResponse = incidentRepository.findAll().stream()
                .filter(incident -> incident.getStation() != null)
                .filter(incident -> station.getId().equals(incident.getStation().getId()))
                .filter(incident -> incident.getResponseTimeMinutes() != null && incident.getResponseTimeMinutes() > 0)
                .mapToInt(Incident::getResponseTimeMinutes)
                .average()
                .orElse(15.0);
        return Math.max(0.9, Math.min(1.35, averageResponse / 15.0));
    }

    private double roadMultiplierFor(Station station, double learningFactor) {
        double ruralAdjustment = station.getVillage() == null || station.getVillage().isBlank() ? 0.0 : 0.04;
        return BASE_ROAD_MULTIPLIER + ruralAdjustment + Math.max(0.0, learningFactor - 1.0) * 0.15;
    }

    private List<String> directionsFor(Station station, String originLabel, double roadDistanceKm, int etaMinutes, String direction, String incidentType) {
        List<String> directions = new ArrayList<>();
        directions.add("Depart " + station.getName() + " station and begin response deployment immediately.");
        directions.add("Travel " + direction + " toward " + originLabel + ".");
        directions.add("Estimated road distance is " + formatDouble(roadDistanceKm) + " km with ETA " + etaMinutes + " minutes.");
        if ("FIRE".equalsIgnoreCase(incidentType)) {
            directions.add("Confirm hydrant and suppression equipment readiness before final approach.");
        } else if ("RESCUE".equalsIgnoreCase(incidentType)) {
            directions.add("Prepare rescue equipment and casualty handling support during the final approach.");
        } else {
            directions.add("Maintain tele-support contact and update the control room on arrival.");
        }
        return directions;
    }

    private String originLabel(EmergencyCall call) {
        String primary = firstNonBlank(call.getLocationText(), call.getVillage(), call.getWard(), call.getDistrict() != null ? call.getDistrict().getName() : null);
        return primary == null ? "reported scene" : primary;
    }

    private String originLabel(Incident incident) {
        String primary = firstNonBlank(incident.getVillage(), incident.getDistrict() != null ? incident.getDistrict().getName() : null);
        return primary == null ? "incident scene" : primary;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private double bearing(double startLat, double startLon, double endLat, double endLon) {
        double startLatRad = Math.toRadians(startLat);
        double startLonRad = Math.toRadians(startLon);
        double endLatRad = Math.toRadians(endLat);
        double endLonRad = Math.toRadians(endLon);
        double y = Math.sin(endLonRad - startLonRad) * Math.cos(endLatRad);
        double x = Math.cos(startLatRad) * Math.sin(endLatRad)
                - Math.sin(startLatRad) * Math.cos(endLatRad) * Math.cos(endLonRad - startLonRad);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    private String cardinalDirection(double bearing) {
        if (bearing >= 337.5 || bearing < 22.5) {
            return "north";
        }
        if (bearing < 67.5) {
            return "north-east";
        }
        if (bearing < 112.5) {
            return "east";
        }
        if (bearing < 157.5) {
            return "south-east";
        }
        if (bearing < 202.5) {
            return "south";
        }
        if (bearing < 247.5) {
            return "south-west";
        }
        if (bearing < 292.5) {
            return "west";
        }
        return "north-west";
    }

    private String embedUrl(double stationLat, double stationLon, double originLat, double originLon) {
        double minLon = Math.min(stationLon, originLon) - 0.08;
        double maxLon = Math.max(stationLon, originLon) + 0.08;
        double minLat = Math.min(stationLat, originLat) - 0.08;
        double maxLat = Math.max(stationLat, originLat) + 0.08;
        return String.format(Locale.ENGLISH,
                "https://www.openstreetmap.org/export/embed.html?bbox=%f%%2C%f%%2C%f%%2C%f&layer=mapnik&marker=%f%%2C%f",
                minLon, minLat, maxLon, maxLat, originLat, originLon);
    }

    private String directionsUrl(double stationLat, double stationLon, double originLat, double originLon) {
        String route = String.format(Locale.ENGLISH, "%f,%f;%f,%f", stationLat, stationLon, originLat, originLon);
        return "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route="
                + URLEncoder.encode(route, StandardCharsets.UTF_8);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal roundDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatDouble(double value) {
        return String.format(Locale.ENGLISH, "%.2f", value);
    }

    public record RouteRecommendation(
            String sourceType,
            Long sourceId,
            String sourceReference,
            Long stationId,
            String stationName,
            String stationDistrict,
            double straightDistanceKm,
            double roadDistanceKm,
            int etaMinutes,
            double learningFactor,
            List<String> directions,
            String embedUrl,
            String directionsUrl
    ) {
    }
}
