package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ComprehensiveFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminDocumentationApiIncludesDisasterRecoveryDocuments() throws Exception {
        User superAdmin = createUser("docs-super-admin", OperationRole.SUPER_ADMIN, null);

        mockMvc.perform(get("/api/admin/documents")
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='disaster-recovery-plan')]").exists())
                .andExpect(jsonPath("$[?(@.key=='disaster-recovery-runbook')]").exists());

        mockMvc.perform(get("/api/admin/documents/disaster-recovery-plan")
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("FROMS Disaster Recovery Plan"));

        mockMvc.perform(get("/api/admin/documents/disaster-recovery-plan/pdf")
                        .param("lang", "en")
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("disaster-recovery-plan.pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void brandingAuditAndReportsApisWorkForSuperAdmin() throws Exception {
        User superAdmin = createUser("platform-super-admin", OperationRole.SUPER_ADMIN, null);

        mockMvc.perform(get("/api/admin/branding")
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signatureText").exists());

        mockMvc.perform(put("/api/admin/branding/signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signatureText": "Operations Command Test Signature"
                                }
                                """)
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signatureText").value("Operations Command Test Signature"));

        mockMvc.perform(put("/api/admin/branding/signature-footer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "signatureFooterBase64": "ZHVtbXktc2lnbmF0dXJlLWZvb3Rlcg=="
                                }
                                """)
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signatureFooterDataUri").exists());

        mockMvc.perform(put("/api/admin/branding/logos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fireLogoBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wn7K6kAAAAASUVORK5CYII="
                                }
                                """)
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fireLogoDataUri").exists());

        mockMvc.perform(get("/api/admin/audit/summary")
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$", hasKey("failedEvents")))
                .andExpect(jsonPath("$", hasKey("successfulEvents")));

        mockMvc.perform(get("/api/admin/audit/logs")
                        .param("limit", "10")
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports")
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='user-manual')]").exists());

        mockMvc.perform(get("/api/reports/user-manual/pdf")
                        .param("lang", "en")
                        .session(sessionFor(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void loginSessionLanguageAndPasswordFlowsWork() throws Exception {
        User loginUser = createUser("loginuser1", OperationRole.ADMIN, null);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", loginUser.getUsername())
                        .param("password", "changeMeNow123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession loggedInSession = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/session/keepalive")
                        .session(loggedInSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(post("/api/me/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lang": "sw"
                                }
                                """)
                        .session(loggedInSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lang").value("sw"));

        mockMvc.perform(post("/api/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "changeMeNow123!",
                                  "newPassword": "ChangedPassword123!"
                                }
                                """)
                        .session(loggedInSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        User updatedUser = userRepository.findById(loginUser.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("ChangedPassword123!", updatedUser.getPassword()));

        mockMvc.perform(get("/logout")
                        .session(loggedInSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void routeApisAndPrintableRouteWorkForAuthorizedRole() throws Exception {
        Station station = stationWithCoordinates();
        User routeUser = createUser("route-commander", OperationRole.COMMISSIONER_OPERATIONS, station);
        User blockedUser = createUser("route-blocked", OperationRole.ADMIN, station);
        Incident incident = createIncident("route-case", station, new BigDecimal("-6.810000"), new BigDecimal("39.280000"));

        mockMvc.perform(get("/api/routes/incidents/{incidentId}", incident.getId())
                        .session(sessionFor(routeUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("INCIDENT"))
                .andExpect(jsonPath("$.stationName").isNotEmpty())
                .andExpect(jsonPath("$.directionsUrl").isNotEmpty());

        mockMvc.perform(get("/routes/print/incidents/{incidentId}", incident.getId())
                        .session(sessionFor(routeUser)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Printable Incident Route")));

        mockMvc.perform(get("/api/routes/incidents/{incidentId}", incident.getId())
                        .session(sessionFor(blockedUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Action not allowed for your role"));
    }

    @Test
    void equipmentAndHydrantFlowsWorkForSuperAdmin() throws Exception {
        User stationOperator = createUser("equipment-station-operator", OperationRole.STATION_OPERATION_OFFICER, stationWithCoordinates());

        MvcResult equipmentResult = mockMvc.perform(post("/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Main Pump Unit",
                                  "type": "FIRE_TENDER",
                                  "serialNumber": "EQ-COMP-1001",
                                  "conditionStatus": "GOOD",
                                  "operationalStatus": "AVAILABLE",
                                  "purchaseDate": "2025-01-10",
                                  "maintenanceDueDate": "2026-06-01",
                                  "quantityInStore": 1
                                }
                                """)
                        .session(sessionFor(stationOperator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Main Pump Unit"))
                .andReturn();

        String equipmentId = com.jayway.jsonpath.JsonPath.read(equipmentResult.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(get("/api/equipment")
                        .session(sessionFor(stationOperator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.serialNumber=='EQ-COMP-1001')]").exists());

        mockMvc.perform(post("/api/equipment/{equipmentId}/maintenance", equipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceDate": "2026-04-21",
                                  "notes": "Routine service",
                                  "servicedBy": "Workshop Team",
                                  "nextServiceDate": "2026-10-21"
                                }
                                """)
                        .session(sessionFor(stationOperator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment.id").value(Long.parseLong(equipmentId)));

        mockMvc.perform(post("/api/equipment/hydrants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "working": 5,
                                  "notWorking": 1,
                                  "lowPressure": 2,
                                  "remarks": "Quarterly hydrant review",
                                  "locations": [
                                    {
                                      "name": "City Center",
                                      "status": "WORKING",
                                      "pressure": "GOOD"
                                    }
                                  ]
                                }
                                """)
                        .session(sessionFor(stationOperator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHydrants").value(8));

        mockMvc.perform(get("/api/equipment/hydrants")
                        .session(sessionFor(stationOperator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locations").isArray());
    }

    @Test
    void investigationWorkflowCanBeStartedReviewedAndExported() throws Exception {
        Station station = stationWithCoordinates();
        User dio = createUser("district-investigator", OperationRole.DISTRICT_INVESTIGATION_OFFICER, station);
        User dfo = createUser("district-fire-reviewer", OperationRole.DISTRICT_FIRE_OFFICER, station);
        Incident incident = createIncident("investigation-case", station, new BigDecimal("-6.802000"), new BigDecimal("39.295000"));

        MvcResult createResult = mockMvc.perform(multipart("/api/investigations")
                        .param("incidentId", incident.getId().toString())
                        .param("incidentDetails", "Warehouse fire investigation details")
                        .param("causeAnalysis", "Electrical fault suspected")
                        .param("witnessStatements", "Witnesses saw sparks before smoke")
                        .session(sessionFor(dio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incident.getId()))
                .andExpect(jsonPath("$.currentLevel").value(OperationRole.DISTRICT_FIRE_OFFICER))
                .andReturn();

        Long reportId = Long.valueOf(com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.id").toString());

        mockMvc.perform(get("/api/investigations")
                        .session(sessionFor(dfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==%s)]", reportId).exists());

        mockMvc.perform(post("/api/investigations/{reportId}/decision", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approve": true,
                                  "comment": "Forward to the next level"
                                }
                                """)
                        .session(sessionFor(dfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REGIONAL_INVESTIGATION_OFFICER"));

        mockMvc.perform(get("/api/investigations/{reportId}/logs", reportId)
                        .session(sessionFor(dfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));

        mockMvc.perform(get("/api/investigations/{reportId}/attachments", reportId)
                        .session(sessionFor(dfo)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/investigations/{reportId}/pdf", reportId)
                        .param("lang", "en")
                        .session(sessionFor(dfo)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());
        if (user.getStation() != null) {
            session.setAttribute("stationId", user.getStation().getId());
        }
        return session;
    }

    private User createUser(String username, String role, Station station) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode("changeMeNow123!"));
                    user.setRole(role);
                    user.setFullName(username);
                    user.setDesignation(role);
                    user.setActive(true);
                    user.setStation(station);
                    return userRepository.save(user);
                });
    }

    private Station stationWithCoordinates() {
        Station station = stationRepository.findAll().stream().findFirst().orElseThrow();
        boolean updated = false;
        if (station.getLatitude() == null) {
            station.setLatitude(new BigDecimal("-6.792354"));
            updated = true;
        }
        if (station.getLongitude() == null) {
            station.setLongitude(new BigDecimal("39.208328"));
            updated = true;
        }
        if (!Boolean.TRUE.equals(station.getActive())) {
            station.setActive(true);
            updated = true;
        }
        return updated ? stationRepository.save(station) : station;
    }

    private Incident createIncident(String sourceReference, Station station, BigDecimal latitude, BigDecimal longitude) {
        Incident incident = new Incident();
        incident.setIncidentType("FIRE");
        incident.setSource("SYSTEM_TEST");
        incident.setSeverity("HIGH");
        incident.setStatus("ACTIVE");
        incident.setSourceReference(sourceReference + "-" + System.nanoTime());
        incident.setDescription("Integration verification incident");
        incident.setDistrict(station.getDistrict());
        incident.setStation(station);
        incident.setVillage("Verification Village");
        incident.setLatitude(latitude);
        incident.setLongitude(longitude);
        incident.setReportedAt(LocalDateTime.now());
        incident.setCallReceivedAt(LocalDateTime.now());
        incident.setResponseTimeMinutes(12);
        incident.setReportingPerson("System Test");
        incident.setReportingContact("255700000001");
        incident.setDispatchedAt(LocalDateTime.now());
        incident.setResolvedAt(LocalDateTime.now().plusMinutes(45));
        incident.setApprovalStatus("APPROVED");
        return incidentRepository.save(incident);
    }
}
