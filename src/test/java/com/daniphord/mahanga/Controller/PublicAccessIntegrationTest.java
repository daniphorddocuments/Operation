package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.VillageStreet;
import com.daniphord.mahanga.Model.Ward;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.EmergencyCallRepository;
import com.daniphord.mahanga.Repositories.RegionRepository;
import com.daniphord.mahanga.Repositories.RoadLandmarkRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.VillageStreetRepository;
import com.daniphord.mahanga.Repositories.WardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private VillageStreetRepository villageStreetRepository;

    @Autowired
    private RoadLandmarkRepository roadLandmarkRepository;

    @Autowired
    private EmergencyCallRepository emergencyCallRepository;

    @Test
    void loginPageIncludesCsrfTokenForProtectedPost() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void rootRendersPublicLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fire and Rescue Force Operation Management System")))
                .andExpect(content().string(containsString("Public Emergency Access")));
    }

    @Test
    void publicEmergencyReportPageIncludesCsrfTokenForProtectedPost() throws Exception {
        mockMvc.perform(get("/public/emergency/report"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void publicGeographyEndpointsLoadRegionDistrictStationAndHierarchyOptions() throws Exception {
        Region dodoma = regionRepository.findByNameIgnoreCase("Dodoma").orElseThrow();
        District district = districtRepository.findByRegionIdOrderByNameAsc(dodoma.getId()).stream()
                .filter(item -> "Bahi District Council".equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseThrow();
        Station station = stationRepository.findByDistrictIdOrderByNameAsc(district.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .findFirst()
                .orElseThrow();
        Ward ward = wardRepository.findByDistrictIdOrderByNameAsc(district.getId()).stream().findFirst().orElseThrow();
        VillageStreet villageStreet = villageStreetRepository.findByWardIdOrderByNameAsc(ward.getId()).stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/geography/regions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dodoma")));

        mockMvc.perform(get("/api/geography/regions/{regionId}/districts", dodoma.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(district.getName())));

        mockMvc.perform(get("/api/geography/districts/{districtId}/stations", district.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(station.getName())));

        mockMvc.perform(get("/api/geography/districts/{districtId}/wards", district.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ward.getName())));

        mockMvc.perform(get("/api/geography/wards/{wardId}/villages-streets", ward.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(villageStreet.getName())));

        mockMvc.perform(get("/api/geography/villages-streets/{villageStreetId}/road-landmarks", villageStreet.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Junction")));
    }

    @Test
    void publicEmergencyReportSubmissionPersistsRecordToDatabase() throws Exception {
        Region dodoma = regionRepository.findByNameIgnoreCase("Dodoma").orElseThrow();
        District district = districtRepository.findByRegionIdOrderByNameAsc(dodoma.getId()).stream()
                .filter(item -> "Bahi District Council".equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseThrow();
        Station station = stationRepository.findByDistrictIdOrderByNameAsc(district.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .findFirst()
                .orElseThrow();
        Ward ward = wardRepository.findByDistrictIdOrderByNameAsc(district.getId()).stream().findFirst().orElseThrow();
        VillageStreet villageStreet = villageStreetRepository.findByWardIdOrderByNameAsc(ward.getId()).stream().findFirst().orElseThrow();
        var landmark = roadLandmarkRepository.findByVillageStreetIdOrderByNameAsc(villageStreet.getId()).stream().findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("publicCaptchaAnswer", "4");
        session.setAttribute("publicCaptchaQuestion", "What is 2 + 2?");

        long callsBefore = emergencyCallRepository.count();

        mockMvc.perform(post("/public/emergency/report")
                        .session(session)
                        .param("callerName", "Public Test Reporter")
                        .param("callerNumber", "255700123456")
                        .param("incidentType", "FIRE")
                        .param("regionId", dodoma.getId().toString())
                        .param("districtId", district.getId().toString())
                        .param("stationId", station.getId().toString())
                        .param("wardId", ward.getId().toString())
                        .param("villageStreetId", villageStreet.getId().toString())
                        .param("roadLandmarkId", landmark.getId().toString())
                        .param("details", "Integration test public report persistence")
                        .param("captchaAnswer", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/public/emergency/report/REP-*?token=*"));

        assertEquals(callsBefore + 1, emergencyCallRepository.count());

        EmergencyCall saved = emergencyCallRepository.findTop20ByOrderByCallTimeDesc().stream()
                .filter(call -> "Public Test Reporter".equals(call.getCallerName()))
                .findFirst()
                .orElseThrow();

        assertEquals(dodoma.getId(), saved.getRegion().getId());
        assertEquals(district.getId(), saved.getDistrict().getId());
        assertEquals(station.getId(), saved.getRoutedStation().getId());
        assertEquals(ward.getName(), saved.getWard());
        assertEquals(villageStreet.getName(), saved.getVillage());
        assertEquals(landmark.getName(), saved.getLocationText());
        assertNotNull(saved.getPublicAccessToken());
    }

    @Test
    void publicEmergencyReportSubmissionAcceptsManualLandmarkWhenExactOptionIsNotSelected() throws Exception {
        Region dodoma = regionRepository.findByNameIgnoreCase("Dodoma").orElseThrow();
        District district = districtRepository.findByRegionIdOrderByNameAsc(dodoma.getId()).stream()
                .filter(item -> "Bahi District Council".equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseThrow();
        Station station = stationRepository.findByDistrictIdOrderByNameAsc(district.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .findFirst()
                .orElseThrow();
        Ward ward = wardRepository.findByDistrictIdOrderByNameAsc(district.getId()).stream().findFirst().orElseThrow();
        VillageStreet villageStreet = villageStreetRepository.findByWardIdOrderByNameAsc(ward.getId()).stream().findFirst().orElseThrow();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("publicCaptchaAnswer", "4");
        session.setAttribute("publicCaptchaQuestion", "What is 2 + 2?");

        mockMvc.perform(post("/public/emergency/report")
                        .session(session)
                        .param("callerName", "Manual Landmark Reporter")
                        .param("callerNumber", "255700987654")
                        .param("incidentType", "RESCUE")
                        .param("regionId", dodoma.getId().toString())
                        .param("districtId", district.getId().toString())
                        .param("stationId", station.getId().toString())
                        .param("wardId", ward.getId().toString())
                        .param("villageStreetId", villageStreet.getId().toString())
                        .param("manualLocationText", "Near old market bridge")
                        .param("details", "Manual landmark fallback integration test")
                        .param("captchaAnswer", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/public/emergency/report/REP-*?token=*"));

        EmergencyCall saved = emergencyCallRepository.findTop20ByOrderByCallTimeDesc().stream()
                .filter(call -> "Manual Landmark Reporter".equals(call.getCallerName()))
                .findFirst()
                .orElseThrow();

        assertEquals("Near old market bridge", saved.getLocationText());
        assertEquals(ward.getName(), saved.getWard());
        assertEquals(villageStreet.getName(), saved.getVillage());
    }
}
