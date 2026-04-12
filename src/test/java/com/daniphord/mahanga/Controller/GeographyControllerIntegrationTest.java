package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.RegionRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GeographyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void regionsEndpointReturnsOnlyMainlandRegions() throws Exception {
        mockMvc.perform(get("/api/geography/regions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dodoma")))
                .andExpect(content().string(not(containsString("Zanzibar"))));
    }

    @Test
    void districtsEndpointReturnsOnlyDistrictsForSelectedRegion() throws Exception {
        Region dodoma = regionRepository.findByNameIgnoreCase("Dodoma").orElseThrow();

        mockMvc.perform(get("/api/geography/regions/{regionId}/districts", dodoma.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bahi District Council")))
                .andExpect(content().string(not(containsString("Monduli District Council"))));
    }

    @Test
    void mainlandDistrictEndpointFiltersOutLegacyDistricts() throws Exception {
        Region dodoma = regionRepository.findByNameIgnoreCase("Dodoma").orElseThrow();

        District legacyDistrict = new District();
        legacyDistrict.setName("Dodoma Urban Legacy");
        legacyDistrict.setRegion(dodoma);
        districtRepository.save(legacyDistrict);

        mockMvc.perform(get("/api/geography/regions/{regionId}/districts", dodoma.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dodoma City Council")))
                .andExpect(content().string(not(containsString(legacyDistrict.getName()))));
    }

    @Test
    void stationsEndpointReturnsCanonicalStationForSelectedDistrict() throws Exception {
        Region dodoma = regionRepository.findByNameIgnoreCase("Dodoma").orElseThrow();
        District district = districtRepository.findByRegionIdOrderByNameAsc(dodoma.getId()).stream()
                .filter(item -> "Bahi District Council".equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/geography/districts/{districtId}/stations", district.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bahi District Council Fire Station")));
    }

    @Test
    void hierarchyEndpointsReturnWardVillageStreetAndRoadLandmarkOptions() throws Exception {
        Region dodoma = regionRepository.findByNameIgnoreCase("Dodoma").orElseThrow();
        District district = districtRepository.findByRegionIdOrderByNameAsc(dodoma.getId()).stream()
                .filter(item -> "Bahi District Council".equalsIgnoreCase(item.getName()))
                .findFirst()
                .orElseThrow();

        String wardsPayload = mockMvc.perform(get("/api/geography/districts/{districtId}/wards", district.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bahi District Council Central Ward")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String wardId = wardsPayload.replaceAll(".*\"id\":(\\d+),\"name\":\"Bahi District Council Central Ward\".*", "$1");

        String villagePayload = mockMvc.perform(get("/api/geography/wards/{wardId}/villages-streets", Long.parseLong(wardId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bahi District Council Central Ward Market Street")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String villageStreetId = villagePayload.replaceAll(".*\"id\":(\\d+),\"name\":\"Bahi District Council Central Ward Market Street\".*", "$1");

        mockMvc.perform(get("/api/geography/villages-streets/{villageStreetId}/road-landmarks", Long.parseLong(villageStreetId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bahi District Council Central Ward Market Street Junction")))
                .andExpect(content().string(containsString("\"symbol\":\"JCT\"")));
    }

    @Test
    void superAdminDashboardRendersStationRegistrationSelectors() throws Exception {
        User adminUser = userRepository.findByUsername("Mahanga")
                .orElseGet(this::createSuperAdminUser);

        mockMvc.perform(get("/dashboard")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), OperationRole.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"geography-module\"")))
                .andExpect(content().string(containsString("id=\"stationModal\"")))
                .andExpect(content().string(containsString("id=\"stationRegionId\"")))
                .andExpect(content().string(containsString("id=\"stationDistrictId\"")))
                .andExpect(content().string(containsString("geographyApiUrl")));
    }

    private MockHttpSession sessionFor(Long userId, String username, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setAttribute("username", username);
        session.setAttribute("role", role);
        return session;
    }

    private User createSuperAdminUser() {
        User user = new User();
        user.setUsername("geography.super.admin");
        user.setPassword(passwordEncoder.encode("changeMeNow123!"));
        user.setRole(OperationRole.SUPER_ADMIN);
        user.setFullName("Geography Super Admin");
        user.setDesignation(OperationRole.SUPER_ADMIN);
        return userRepository.save(user);
    }
}
