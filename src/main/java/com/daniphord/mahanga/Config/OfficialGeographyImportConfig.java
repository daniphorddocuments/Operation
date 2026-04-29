package com.daniphord.mahanga.Config;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.RoadLandmark;
import com.daniphord.mahanga.Model.VillageStreet;
import com.daniphord.mahanga.Model.Ward;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.RegionRepository;
import com.daniphord.mahanga.Repositories.RoadLandmarkRepository;
import com.daniphord.mahanga.Repositories.VillageStreetRepository;
import com.daniphord.mahanga.Repositories.WardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class OfficialGeographyImportConfig {

    private static final Logger log = LoggerFactory.getLogger(OfficialGeographyImportConfig.class);
    private static final Path LOCALITIES_FILE = Paths.get("data", "geography", "tz_mainland_localities.csv");

    @Bean
    @Order(2)
    CommandLineRunner importOfficialGeographyData(
            RegionRepository regionRepository,
            DistrictRepository districtRepository,
            WardRepository wardRepository,
            VillageStreetRepository villageStreetRepository,
            RoadLandmarkRepository roadLandmarkRepository,
            MainlandGeographyCatalog mainlandGeographyCatalog
    ) {
        return args -> {
            if (!Files.exists(LOCALITIES_FILE)) {
                log.info("Official geography import skipped because '{}' was not found", LOCALITIES_FILE);
                return;
            }

            ImportSummary summary = new ImportSummary();
            try (BufferedReader reader = Files.newBufferedReader(LOCALITIES_FILE, StandardCharsets.UTF_8)) {
                String headerLine = reader.readLine();
                if (headerLine == null || headerLine.isBlank()) {
                    log.warn("Official geography import skipped because '{}' is empty", LOCALITIES_FILE);
                    return;
                }

                CsvHeader header = CsvHeader.from(headerLine);
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) {
                        continue;
                    }
                    try {
                        CsvRow row = header.row(line);
                        importRow(
                                row,
                                summary,
                                regionRepository,
                                districtRepository,
                                wardRepository,
                                villageStreetRepository,
                                roadLandmarkRepository,
                                mainlandGeographyCatalog
                        );
                    } catch (IllegalArgumentException exception) {
                        log.warn("Skipping geography row {}: {}", lineNumber, exception.getMessage());
                    }
                }
                log.info(
                        "Official geography import completed from '{}': {} regions, {} districts, {} wards, {} localities, {} landmarks",
                        LOCALITIES_FILE,
                        summary.createdRegions,
                        summary.createdDistricts,
                        summary.createdWards,
                        summary.createdLocalities,
                        summary.createdLandmarks
                );
            }
        };
    }

    private void importRow(
            CsvRow row,
            ImportSummary summary,
            RegionRepository regionRepository,
            DistrictRepository districtRepository,
            WardRepository wardRepository,
            VillageStreetRepository villageStreetRepository,
            RoadLandmarkRepository roadLandmarkRepository,
            MainlandGeographyCatalog mainlandGeographyCatalog
    ) {
        String regionName = canonicalRegionName(mainlandGeographyCatalog, row.required("region"));
        String districtName = canonicalDistrictName(mainlandGeographyCatalog, regionName, row.required("district"));
        String wardName = row.required("ward");
        String localityName = row.required("locality_name");
        VillageStreet.EntryType entryType = parseEntryType(row.required("entry_type"));

        Region region = regionRepository.findByNameIgnoreCase(regionName).orElseGet(() -> {
            Region created = new Region();
            created.setName(regionName);
            created.setCode(resolveRegionCode(regionName, row.value("region_code")));
            summary.createdRegions++;
            return regionRepository.save(created);
        });
        if (region.getCode() == null || region.getCode().isBlank()) {
            region.setCode(resolveRegionCode(regionName, row.value("region_code")));
            regionRepository.save(region);
        }

        District district = districtRepository.findByRegionIdOrderByNameAsc(region.getId()).stream()
                .filter(existing -> normalize(existing.getName()).equals(normalize(districtName)))
                .findFirst()
                .orElseGet(() -> {
                    District created = new District();
                    created.setRegion(region);
                    created.setName(districtName);
                    summary.createdDistricts++;
                    return districtRepository.save(created);
                });

        Ward ward = wardRepository.findByDistrictIdOrderByNameAsc(district.getId()).stream()
                .filter(existing -> normalize(existing.getName()).equals(normalize(wardName)))
                .findFirst()
                .orElseGet(() -> {
                    Ward created = new Ward();
                    created.setDistrict(district);
                    created.setName(wardName);
                    created.setLatitude(decimalValue(row.value("ward_latitude")));
                    created.setLongitude(decimalValue(row.value("ward_longitude")));
                    summary.createdWards++;
                    return wardRepository.save(created);
                });
        boolean wardUpdated = false;
        if (ward.getLatitude() == null) {
            BigDecimal latitude = decimalValue(row.value("ward_latitude"));
            if (latitude != null) {
                ward.setLatitude(latitude);
                wardUpdated = true;
            }
        }
        if (ward.getLongitude() == null) {
            BigDecimal longitude = decimalValue(row.value("ward_longitude"));
            if (longitude != null) {
                ward.setLongitude(longitude);
                wardUpdated = true;
            }
        }
        if (wardUpdated) {
            ward = wardRepository.save(ward);
        }

        Ward persistedWard = ward;
        VillageStreet locality = villageStreetRepository.findByWardIdOrderByNameAsc(ward.getId()).stream()
                .filter(existing -> normalize(existing.getName()).equals(normalize(localityName)))
                .findFirst()
                .orElseGet(() -> {
                    VillageStreet created = new VillageStreet();
                    created.setWard(persistedWard);
                    created.setName(localityName);
                    created.setEntryType(entryType);
                    created.setLatitude(decimalValue(row.value("locality_latitude")));
                    created.setLongitude(decimalValue(row.value("locality_longitude")));
                    summary.createdLocalities++;
                    return villageStreetRepository.save(created);
                });
        boolean localityUpdated = false;
        if (locality.getEntryType() == null) {
            locality.setEntryType(entryType);
            localityUpdated = true;
        }
        if (locality.getLatitude() == null) {
            BigDecimal latitude = decimalValue(row.value("locality_latitude"));
            if (latitude != null) {
                locality.setLatitude(latitude);
                localityUpdated = true;
            }
        }
        if (locality.getLongitude() == null) {
            BigDecimal longitude = decimalValue(row.value("locality_longitude"));
            if (longitude != null) {
                locality.setLongitude(longitude);
                localityUpdated = true;
            }
        }
        if (localityUpdated) {
            locality = villageStreetRepository.save(locality);
        }

        String landmarkName = row.value("landmark_name");
        if (landmarkName == null || landmarkName.isBlank()) {
            return;
        }

        VillageStreet persistedLocality = locality;
        roadLandmarkRepository.findByVillageStreetIdOrderByNameAsc(locality.getId()).stream()
                .filter(existing -> normalize(existing.getName()).equals(normalize(landmarkName)))
                .findFirst()
                .orElseGet(() -> {
                    RoadLandmark created = new RoadLandmark();
                    created.setVillageStreet(persistedLocality);
                    created.setName(landmarkName.trim());
                    created.setSymbol(emptyToNull(row.value("landmark_symbol")));
                    created.setLatitude(decimalValue(row.value("landmark_latitude")));
                    created.setLongitude(decimalValue(row.value("landmark_longitude")));
                    summary.createdLandmarks++;
                    return roadLandmarkRepository.save(created);
                });
    }

    private VillageStreet.EntryType parseEntryType(String rawValue) {
        String normalized = normalize(rawValue);
        if (normalized.equals("street") || normalized.equals("mtaa")) {
            return VillageStreet.EntryType.STREET;
        }
        if (normalized.equals("village") || normalized.equals("kijiji")) {
            return VillageStreet.EntryType.VILLAGE;
        }
        throw new IllegalArgumentException("entry_type must be STREET/MTAA or VILLAGE/KIJIJI");
    }

    private String canonicalRegionName(MainlandGeographyCatalog mainlandGeographyCatalog, String regionName) {
        return mainlandGeographyCatalog.canonicalRegionName(regionName);
    }

    private String canonicalDistrictName(MainlandGeographyCatalog mainlandGeographyCatalog, String regionName, String districtName) {
        return mainlandGeographyCatalog.canonicalDistrictName(regionName, districtName);
    }

    private String resolveRegionCode(String regionName, String configuredCode) {
        if (configuredCode != null && !configuredCode.isBlank()) {
            return configuredCode.trim();
        }
        return regionName.substring(0, Math.min(3, regionName.length())).toUpperCase(Locale.ROOT);
    }

    private BigDecimal decimalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return BigDecimal.valueOf(Double.parseDouble(value.trim())).setScale(6, RoundingMode.HALF_UP);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static final class ImportSummary {
        private int createdRegions;
        private int createdDistricts;
        private int createdWards;
        private int createdLocalities;
        private int createdLandmarks;
    }

    private record CsvRow(Map<String, String> values) {
        private String value(String key) {
            return values.getOrDefault(key, "");
        }

        private String required(String key) {
            String value = value(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("missing required column '" + key + "'");
            }
            return value.trim();
        }
    }

    private record CsvHeader(Map<String, Integer> positions) {
        private static CsvHeader from(String headerLine) {
            List<String> columns = parseCsvLine(headerLine);
            Map<String, Integer> positions = new LinkedHashMap<>();
            for (int index = 0; index < columns.size(); index++) {
                positions.put(columns.get(index).trim().toLowerCase(Locale.ROOT), index);
            }
            for (String required : List.of("region", "district", "ward", "entry_type", "locality_name")) {
                if (!positions.containsKey(required)) {
                    throw new IllegalArgumentException("CSV header must include column '" + required + "'");
                }
            }
            return new CsvHeader(positions);
        }

        private CsvRow row(String line) {
            List<String> cells = parseCsvLine(line);
            Map<String, String> values = new LinkedHashMap<>();
            positions.forEach((key, index) -> values.put(key, index < cells.size() ? cells.get(index) : ""));
            return new CsvRow(values);
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (character == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        values.add(current.toString().trim());
        return values;
    }
}
