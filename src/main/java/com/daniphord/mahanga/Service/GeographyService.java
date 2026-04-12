package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Config.MainlandGeographyCatalog;
import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.RoadLandmark;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Model.VillageStreet;
import com.daniphord.mahanga.Model.Ward;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.RegionRepository;
import com.daniphord.mahanga.Repositories.RoadLandmarkRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.VillageStreetRepository;
import com.daniphord.mahanga.Repositories.WardRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GeographyService {

    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final StationRepository stationRepository;
    private final WardRepository wardRepository;
    private final VillageStreetRepository villageStreetRepository;
    private final RoadLandmarkRepository roadLandmarkRepository;
    private final MainlandGeographyCatalog mainlandGeographyCatalog;

    public GeographyService(
            RegionRepository regionRepository,
            DistrictRepository districtRepository,
            StationRepository stationRepository,
            WardRepository wardRepository,
            VillageStreetRepository villageStreetRepository,
            RoadLandmarkRepository roadLandmarkRepository,
            MainlandGeographyCatalog mainlandGeographyCatalog
    ) {
        this.regionRepository = regionRepository;
        this.districtRepository = districtRepository;
        this.stationRepository = stationRepository;
        this.wardRepository = wardRepository;
        this.villageStreetRepository = villageStreetRepository;
        this.roadLandmarkRepository = roadLandmarkRepository;
        this.mainlandGeographyCatalog = mainlandGeographyCatalog;
    }

    public List<Region> regions() {
        return regionRepository.findAllByOrderByNameAsc().stream()
                .filter(region -> mainlandGeographyCatalog.isMainlandRegion(region.getName()))
                .toList();
    }

    public List<District> districts(Long regionId) {
        if (regionId == null) {
            return List.of();
        }
        Region region = regionRepository.findById(regionId).orElse(null);
        if (region == null || !mainlandGeographyCatalog.isMainlandRegion(region.getName())) {
            return List.of();
        }
        return districtRepository.findByRegionIdOrderByNameAsc(regionId).stream()
                .filter(district -> mainlandGeographyCatalog.isMainlandDistrict(region.getName(), district.getName()))
                .toList();
    }

    public List<Station> stations(Long districtId) {
        if (districtId == null) {
            return List.of();
        }
        District district = districtRepository.findById(districtId).orElse(null);
        if (district == null || district.getRegion() == null || !mainlandGeographyCatalog.isMainlandDistrict(district.getRegion().getName(), district.getName())) {
            return List.of();
        }
        return stationRepository.findByDistrictIdOrderByNameAsc(districtId).stream()
                .filter(station -> Boolean.TRUE.equals(station.getActive()))
                .toList();
    }

    public List<Ward> wards(Long districtId) {
        if (districtId == null) {
            return List.of();
        }
        District district = mainlandDistrict(districtId);
        return wardRepository.findByDistrictIdOrderByNameAsc(district.getId());
    }

    public List<VillageStreet> villagesAndStreets(Long wardId) {
        if (wardId == null) {
            return List.of();
        }
        return villageStreetRepository.findByWardIdOrderByNameAsc(wardId);
    }

    public List<RoadLandmark> roadLandmarks(Long villageStreetId) {
        if (villageStreetId == null) {
            return List.of();
        }
        return roadLandmarkRepository.findByVillageStreetIdOrderByNameAsc(villageStreetId);
    }

    public Region createRegion(String name, String code) {
        String canonicalRegionName = mainlandGeographyCatalog.canonicalRegionName(name);
        Region region = regionRepository.findByNameIgnoreCase(canonicalRegionName).orElseGet(Region::new);
        region.setName(canonicalRegionName);
        region.setCode(resolveRegionCode(canonicalRegionName, code));
        return regionRepository.save(region);
    }

    public Region updateRegion(Long id, String name, String code) {
        Region region = regionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Region not found"));
        String canonicalRegionName = mainlandGeographyCatalog.canonicalRegionName(name);
        region.setName(canonicalRegionName);
        region.setCode(resolveRegionCode(canonicalRegionName, code));
        return regionRepository.save(region);
    }

    public District createDistrict(Long regionId, String name) {
        Region region = mainlandRegion(regionId);
        String canonicalDistrictName = mainlandGeographyCatalog.canonicalDistrictName(region.getName(), name);
        District district = districtRepository.findByRegionIdOrderByNameAsc(region.getId()).stream()
                .filter(existing -> canonicalDistrictName.equalsIgnoreCase(existing.getName()))
                .findFirst()
                .orElseGet(District::new);
        district.setName(canonicalDistrictName);
        district.setRegion(region);
        return districtRepository.save(district);
    }

    public District updateDistrict(Long id, Long regionId, String name) {
        District district = districtRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("District not found"));
        Region region = mainlandRegion(regionId);
        district.setName(mainlandGeographyCatalog.canonicalDistrictName(region.getName(), name));
        district.setRegion(region);
        return districtRepository.save(district);
    }

    public Station createStation(Long districtId, String name, String village, String controlRoomNumber, String phoneNumber) {
        District district = mainlandDistrict(districtId);
        String canonicalStationName = mainlandGeographyCatalog.canonicalStationName(district.getName());
        Station station = stationRepository.findByDistrictIdOrderByNameAsc(district.getId()).stream()
                .filter(existing -> canonicalStationName.equalsIgnoreCase(existing.getName()))
                .findFirst()
                .orElseGet(Station::new);
        station.setName(canonicalStationName);
        station.setVillage(defaultVillage(village, district.getName()));
        station.setDistrict(district);
        station.setControlRoomNumber(controlRoomNumber);
        station.setPhoneNumber(phoneNumber);
        station.setActive(true);
        return stationRepository.save(station);
    }

    public Station updateStation(Long id, Long districtId, String name, String village, String controlRoomNumber, String phoneNumber, Boolean active) {
        Station station = stationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Station not found"));
        District district = mainlandDistrict(districtId);
        station.setName(mainlandGeographyCatalog.canonicalStationName(district.getName()));
        station.setVillage(defaultVillage(village, district.getName()));
        station.setDistrict(district);
        station.setControlRoomNumber(controlRoomNumber);
        station.setPhoneNumber(phoneNumber);
        if (active != null) {
            station.setActive(active);
        }
        return stationRepository.save(station);
    }

    public List<Map<String, Object>> regionViews() {
        return regions().stream()
                .map(region -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", region.getId());
                    view.put("name", region.getName());
                    view.put("code", region.getCode() == null ? "" : region.getCode());
                    return view;
                })
                .toList();
    }

    public List<Map<String, Object>> districtViews(Long regionId) {
        return districts(regionId).stream()
                .map(district -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", district.getId());
                    view.put("name", district.getName());
                    view.put("regionId", district.getRegion().getId());
                    return view;
                })
                .toList();
    }

    public List<Map<String, Object>> stationViews(Long districtId) {
        return stations(districtId).stream()
                .map(station -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", station.getId());
                    view.put("name", station.getName());
                    view.put("districtId", station.getDistrict().getId());
                    view.put("village", station.getVillage() == null ? "" : station.getVillage());
                    view.put("controlRoomNumber", station.getControlRoomNumber() == null ? "" : station.getControlRoomNumber());
                    view.put("phoneNumber", station.getPhoneNumber() == null ? "" : station.getPhoneNumber());
                    view.put("active", station.getActive());
                    return view;
                })
                .toList();
    }

    public List<Map<String, Object>> wardViews(Long districtId) {
        return wards(districtId).stream()
                .map(ward -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", ward.getId());
                    view.put("name", ward.getName());
                    view.put("districtId", ward.getDistrict().getId());
                    return view;
                })
                .toList();
    }

    public List<Map<String, Object>> villageStreetViews(Long wardId) {
        return villagesAndStreets(wardId).stream()
                .map(villageStreet -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", villageStreet.getId());
                    view.put("name", villageStreet.getName());
                    view.put("wardId", villageStreet.getWard().getId());
                    view.put("entryType", villageStreet.getEntryType().name());
                    return view;
                })
                .toList();
    }

    public List<Map<String, Object>> roadLandmarkViews(Long villageStreetId) {
        return roadLandmarks(villageStreetId).stream()
                .map(roadLandmark -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", roadLandmark.getId());
                    view.put("name", roadLandmark.getName());
                    view.put("symbol", roadLandmark.getSymbol() == null ? "" : roadLandmark.getSymbol());
                    view.put("villageStreetId", roadLandmark.getVillageStreet().getId());
                    return view;
                })
                .toList();
    }

    public void applyEmergencyCallLocation(EmergencyCall emergencyCall) {
        if (emergencyCall == null) {
            return;
        }
        HierarchyLocation location = resolveHierarchy(
                emergencyCall.getWardId(),
                emergencyCall.getVillageStreetId(),
                emergencyCall.getRoadLandmarkId()
        );
        if (location == null) {
            return;
        }
        if (emergencyCall.getDistrict() != null && location.districtId() != null
                && !emergencyCall.getDistrict().getId().equals(location.districtId())) {
            throw new IllegalArgumentException("Selected ward hierarchy does not belong to the chosen district");
        }
        emergencyCall.setWard(location.wardName());
        emergencyCall.setVillage(location.villageStreetName());
        emergencyCall.setLocationText(location.landmarkName());
        emergencyCall.setRoadSymbol(location.roadSymbol());
        if (emergencyCall.getLatitude() == null) {
            emergencyCall.setLatitude(location.latitude());
        }
        if (emergencyCall.getLongitude() == null) {
            emergencyCall.setLongitude(location.longitude());
        }
    }

    public void applyIncidentLocation(Incident incident) {
        if (incident == null) {
            return;
        }
        HierarchyLocation location = resolveHierarchy(
                incident.getWardId(),
                incident.getVillageStreetId(),
                incident.getRoadLandmarkId()
        );
        if (location == null) {
            return;
        }
        if (incident.getDistrict() != null && location.districtId() != null
                && !incident.getDistrict().getId().equals(location.districtId())) {
            throw new IllegalArgumentException("Selected ward hierarchy does not belong to the chosen district");
        }
        incident.setWard(location.wardName());
        incident.setVillage(location.villageStreetName());
        incident.setRoadLandmark(location.landmarkName());
        incident.setRoadSymbol(location.roadSymbol());
        if (incident.getLocationDetails() == null || incident.getLocationDetails().isBlank()) {
            incident.setLocationDetails(location.fullLabel());
        }
        if (incident.getLatitude() == null) {
            incident.setLatitude(location.latitude());
        }
        if (incident.getLongitude() == null) {
            incident.setLongitude(location.longitude());
        }
    }

    public ScopeMapView scopeMap(User user) {
        String role = OperationRole.normalizeRole(user == null ? null : user.getRole());
        String query;
        String title;
        String summary;

        if (user != null && user.getStation() != null && user.getStation().getDistrict() != null && !OperationRole.NATIONAL_ROLES.contains(role)) {
            if (isRegionalRole(role)) {
                String regionName = user.getStation().getDistrict().getRegion() != null
                        ? user.getStation().getDistrict().getRegion().getName()
                        : "Tanzania Mainland";
                query = regionName + " Region, Tanzania";
                title = regionName + " regional operational map";
                summary = "Shows the current surroundings for the assigned region so this level opens on its own operational area.";
            } else if (isDistrictRole(role)) {
                String districtName = user.getStation().getDistrict().getName();
                query = districtName + " District, Tanzania";
                title = districtName + " district operational map";
                summary = "Shows the current surroundings for the assigned district so district users land on their own command area.";
            } else {
                String stationName = user.getStation().getName();
                String districtName = user.getStation().getDistrict().getName();
                query = stationName + ", " + districtName + ", Tanzania";
                title = stationName + " station surroundings";
                summary = "Shows the immediate surroundings for the assigned station, including nearby routes and landmarks.";
            }
        } else {
            query = "Tanzania Mainland";
            title = "Tanzania Mainland operational map";
            summary = "Shows the national base map when the current role is not limited to one local command area.";
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return new ScopeMapView(
                title,
                summary,
                "https://www.google.com/maps?q=" + encodedQuery + "&output=embed",
                "https://www.google.com/maps/search/?api=1&query=" + encodedQuery,
                query
        );
    }

    private Region mainlandRegion(Long regionId) {
        Region region = regionRepository.findById(regionId).orElseThrow(() -> new IllegalArgumentException("Region not found"));
        if (!mainlandGeographyCatalog.isMainlandRegion(region.getName())) {
            throw new IllegalArgumentException("Only Tanzania Mainland regions are supported");
        }
        return region;
    }

    private District mainlandDistrict(Long districtId) {
        District district = districtRepository.findById(districtId).orElseThrow(() -> new IllegalArgumentException("District not found"));
        if (district.getRegion() == null || !mainlandGeographyCatalog.isMainlandDistrict(district.getRegion().getName(), district.getName())) {
            throw new IllegalArgumentException("Only Tanzania Mainland districts are supported");
        }
        return district;
    }

    private String defaultVillage(String village, String districtName) {
        return village == null || village.isBlank() ? districtName : village;
    }

    private String resolveRegionCode(String regionName, String code) {
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        return regionName.substring(0, Math.min(3, regionName.length())).toUpperCase(Locale.ROOT);
    }

    private boolean isRegionalRole(String role) {
        return List.of(OperationRole.REGIONAL_FIRE_OFFICER, OperationRole.REGIONAL_OPERATION_OFFICER, OperationRole.REGIONAL_INVESTIGATION_OFFICER)
                .contains(role);
    }

    private boolean isDistrictRole(String role) {
        return List.of(OperationRole.DISTRICT_FIRE_OFFICER, OperationRole.DISTRICT_OPERATION_OFFICER, OperationRole.DISTRICT_INVESTIGATION_OFFICER)
                .contains(role);
    }

    private HierarchyLocation resolveHierarchy(Long wardId, Long villageStreetId, Long roadLandmarkId) {
        Ward ward = wardId == null ? null : wardRepository.findById(wardId)
                .orElseThrow(() -> new IllegalArgumentException("Ward not found"));
        VillageStreet villageStreet = villageStreetId == null ? null : villageStreetRepository.findById(villageStreetId)
                .orElseThrow(() -> new IllegalArgumentException("Village or street not found"));
        RoadLandmark roadLandmark = roadLandmarkId == null ? null : roadLandmarkRepository.findById(roadLandmarkId)
                .orElseThrow(() -> new IllegalArgumentException("Road landmark not found"));

        if (villageStreet != null && ward != null && !villageStreet.getWard().getId().equals(ward.getId())) {
            throw new IllegalArgumentException("Selected village or street does not belong to the chosen ward");
        }
        if (roadLandmark != null && villageStreet != null && !roadLandmark.getVillageStreet().getId().equals(villageStreet.getId())) {
            throw new IllegalArgumentException("Selected road landmark does not belong to the chosen village or street");
        }
        if (roadLandmark != null && villageStreet == null) {
            villageStreet = roadLandmark.getVillageStreet();
        }
        if (villageStreet != null && ward == null) {
            ward = villageStreet.getWard();
        }
        if (roadLandmark == null && ward == null && villageStreet == null) {
            return null;
        }

        BigDecimal latitude = roadLandmark != null && roadLandmark.getLatitude() != null ? roadLandmark.getLatitude()
                : villageStreet != null && villageStreet.getLatitude() != null ? villageStreet.getLatitude()
                : ward != null ? ward.getLatitude() : null;
        BigDecimal longitude = roadLandmark != null && roadLandmark.getLongitude() != null ? roadLandmark.getLongitude()
                : villageStreet != null && villageStreet.getLongitude() != null ? villageStreet.getLongitude()
                : ward != null ? ward.getLongitude() : null;

        String wardName = ward == null ? "" : ward.getName();
        String villageStreetName = villageStreet == null ? "" : villageStreet.getName();
        String landmarkName = roadLandmark == null ? "" : roadLandmark.getName();
        String roadSymbol = roadLandmark == null || roadLandmark.getSymbol() == null ? "" : roadLandmark.getSymbol();
        String fullLabel = List.of(landmarkName, villageStreetName, wardName)
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return new HierarchyLocation(
                ward == null || ward.getDistrict() == null ? null : ward.getDistrict().getId(),
                wardName,
                villageStreetName,
                landmarkName,
                roadSymbol,
                latitude,
                longitude,
                fullLabel
        );
    }

    public record ScopeMapView(
            String title,
            String summary,
            String embedUrl,
            String openUrl,
            String queryLabel
    ) {
    }

    private record HierarchyLocation(
            Long districtId,
            String wardName,
            String villageStreetName,
            String landmarkName,
            String roadSymbol,
            BigDecimal latitude,
            BigDecimal longitude,
            String fullLabel
    ) {
    }
}
