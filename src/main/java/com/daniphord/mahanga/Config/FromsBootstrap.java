package com.daniphord.mahanga.Config;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.RegionRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class FromsBootstrap {

    private static final Logger log = LoggerFactory.getLogger(FromsBootstrap.class);

    @Bean
    @Order(2)
    CommandLineRunner bootstrapMainlandStations(
            MainlandGeographyCatalog mainlandGeographyCatalog,
            RegionRepository regionRepository,
            DistrictRepository districtRepository,
            StationRepository stationRepository
    ) {
        return args -> {
            mainlandGeographyCatalog.geography().forEach((regionName, districtNames) -> {
                regionRepository.findByNameIgnoreCase(regionName).ifPresent(region -> {
                    List<District> districts = districtRepository.findByRegionIdOrderByNameAsc(region.getId());
                    districtNames.forEach(districtName -> districts.stream()
                            .filter(district -> districtName.equalsIgnoreCase(district.getName()))
                            .findFirst()
                            .ifPresent(district -> ensureCanonicalStation(stationRepository, mainlandGeographyCatalog, district)));
                });
            });
            log.info("Ensured canonical fire stations for Tanzania Mainland districts");
        };
    }

    private void ensureCanonicalStation(
            StationRepository stationRepository,
            MainlandGeographyCatalog mainlandGeographyCatalog,
            District district
    ) {
        String canonicalName = mainlandGeographyCatalog.canonicalStationName(district.getName());
        List<Station> districtStations = stationRepository.findByDistrictIdOrderByNameAsc(district.getId());

        Station station = districtStations.stream()
                .filter(existing -> canonicalName.equalsIgnoreCase(existing.getName()))
                .findFirst()
                .orElseGet(() -> districtStations.size() == 1 ? districtStations.get(0) : new Station());

        station.setName(canonicalName);
        station.setDistrict(district);
        if (station.getVillage() == null || station.getVillage().isBlank()) {
            station.setVillage(district.getName());
        }
        station.setActive(true);
        stationRepository.save(station);
    }
}
