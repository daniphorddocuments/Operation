package com.daniphord.mahanga.Config;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.RoadLandmark;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.VillageStreet;
import com.daniphord.mahanga.Model.Ward;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.RoadLandmarkRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.VillageStreetRepository;
import com.daniphord.mahanga.Repositories.WardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Configuration
public class GeographyHierarchyBootstrap {

    private static final Logger log = LoggerFactory.getLogger(GeographyHierarchyBootstrap.class);

    @Bean
    @Order(3)
    CommandLineRunner bootstrapGeographyHierarchy(
            DistrictRepository districtRepository,
            StationRepository stationRepository,
            WardRepository wardRepository,
            VillageStreetRepository villageStreetRepository,
            RoadLandmarkRepository roadLandmarkRepository
    ) {
        return args -> {
            List<District> districts = districtRepository.findAll();
            for (int districtIndex = 0; districtIndex < districts.size(); districtIndex++) {
                District district = districts.get(districtIndex);
                GeoPoint districtPoint = pointForDistrict(district, districtIndex);
                ensureStationCoordinates(stationRepository, district, districtPoint);
                ensureHierarchy(wardRepository, villageStreetRepository, roadLandmarkRepository, district, districtPoint);
            }
            log.info("Ensured ward, village/street, landmark hierarchy and fallback coordinates for all districts");
        };
    }

    private void ensureStationCoordinates(StationRepository stationRepository, District district, GeoPoint districtPoint) {
        stationRepository.findByDistrictIdOrderByNameAsc(district.getId()).forEach(station -> {
            if (station.getLatitude() == null) {
                station.setLatitude(districtPoint.latitude());
            }
            if (station.getLongitude() == null) {
                station.setLongitude(districtPoint.longitude());
            }
            stationRepository.save(station);
        });
    }

    private void ensureHierarchy(
            WardRepository wardRepository,
            VillageStreetRepository villageStreetRepository,
            RoadLandmarkRepository roadLandmarkRepository,
            District district,
            GeoPoint districtPoint
    ) {
        List<Ward> wards = wardRepository.findByDistrictIdOrderByNameAsc(district.getId());
        if (wards.isEmpty()) {
            wards = List.of(
                    createWard(wardRepository, district, districtPoint.offset(0.008, -0.006), district.getName() + " Central Ward"),
                    createWard(wardRepository, district, districtPoint.offset(-0.007, 0.005), district.getName() + " East Ward"),
                    createWard(wardRepository, district, districtPoint.offset(0.006, 0.007), district.getName() + " West Ward")
            );
        }

        for (int wardIndex = 0; wardIndex < wards.size(); wardIndex++) {
            Ward ward = wards.get(wardIndex);
            List<VillageStreet> entries = villageStreetRepository.findByWardIdOrderByNameAsc(ward.getId());
            if (entries.isEmpty()) {
                entries = List.of(
                        createVillageStreet(villageStreetRepository, ward, wardIndex, ward.getName() + " Market Street", VillageStreet.EntryType.STREET, 0.003, -0.002),
                        createVillageStreet(villageStreetRepository, ward, wardIndex, ward.getName() + " Primary Village", VillageStreet.EntryType.VILLAGE, -0.002, 0.003)
                );
            }
            for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
                VillageStreet entry = entries.get(entryIndex);
                List<RoadLandmark> landmarks = roadLandmarkRepository.findByVillageStreetIdOrderByNameAsc(entry.getId());
                if (landmarks.isEmpty()) {
                    createLandmark(roadLandmarkRepository, entry, entry.getName() + " Junction", "JCT", 0.001, 0.001);
                    createLandmark(roadLandmarkRepository, entry, entry.getName() + " Health Centre", "HC", -0.001, 0.0015);
                    createLandmark(roadLandmarkRepository, entry, entry.getName() + " School Gate", "SCH", 0.0015, -0.001);
                }
            }
        }
    }

    private Ward createWard(WardRepository wardRepository, District district, GeoPoint point, String name) {
        Ward ward = new Ward();
        ward.setDistrict(district);
        ward.setName(name);
        ward.setLatitude(point.latitude());
        ward.setLongitude(point.longitude());
        return wardRepository.save(ward);
    }

    private VillageStreet createVillageStreet(
            VillageStreetRepository villageStreetRepository,
            Ward ward,
            int wardIndex,
            String name,
            VillageStreet.EntryType entryType,
            double latOffset,
            double lonOffset
    ) {
        VillageStreet entry = new VillageStreet();
        entry.setWard(ward);
        entry.setName(name);
        entry.setEntryType(entryType);
        entry.setLatitude(round(ward.getLatitude().doubleValue() + latOffset + (wardIndex * 0.0004)));
        entry.setLongitude(round(ward.getLongitude().doubleValue() + lonOffset + (wardIndex * 0.0004)));
        return villageStreetRepository.save(entry);
    }

    private void createLandmark(
            RoadLandmarkRepository roadLandmarkRepository,
            VillageStreet villageStreet,
            String name,
            String symbol,
            double latOffset,
            double lonOffset
    ) {
        RoadLandmark landmark = new RoadLandmark();
        landmark.setVillageStreet(villageStreet);
        landmark.setName(name);
        landmark.setSymbol(symbol);
        landmark.setLatitude(round(villageStreet.getLatitude().doubleValue() + latOffset));
        landmark.setLongitude(round(villageStreet.getLongitude().doubleValue() + lonOffset));
        roadLandmarkRepository.save(landmark);
    }

    private GeoPoint pointForDistrict(District district, int districtIndex) {
        Region region = district.getRegion();
        long regionIndex = region == null || region.getId() == null ? 0L : region.getId() % 20;
        double latitude = -4.5 - (regionIndex * 0.22) - ((districtIndex % 6) * 0.04);
        double longitude = 33.0 + (regionIndex * 0.31) + ((districtIndex % 5) * 0.05);
        return new GeoPoint(round(latitude), round(longitude));
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private record GeoPoint(BigDecimal latitude, BigDecimal longitude) {
        private GeoPoint offset(double latitudeOffset, double longitudeOffset) {
            return new GeoPoint(
                    BigDecimal.valueOf(latitude.doubleValue() + latitudeOffset).setScale(6, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(longitude.doubleValue() + longitudeOffset).setScale(6, RoundingMode.HALF_UP)
            );
        }
    }
}
