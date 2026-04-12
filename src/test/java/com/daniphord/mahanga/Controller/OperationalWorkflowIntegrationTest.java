package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.EquipmentRepository;
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

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationalWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void stationOperationOfficerCanCreateIncidentWithDefaultWorkflowValues() throws Exception {
        User stationUser = createStationScopedUserWithRole("workflow.incident.creator", OperationRole.STATION_OPERATION_OFFICER);

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentType": "fire",
                                  "severity": "high",
                                  "description": "Warehouse smoke seen near the bus stand"
                                }
                                """)
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentType").value("FIRE"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.reportLevel").value("INITIAL"))
                .andExpect(jsonPath("$.reportingMeans").value("PHYSICAL"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.approvalCurrentLevel").value(OperationRole.DISTRICT_OPERATION_OFFICER));

        Incident savedIncident = incidentRepository.findAll().stream()
                .filter(item -> item.getCreatedBy() != null && stationUser.getId().equals(item.getCreatedBy().getId()))
                .max(Comparator.comparing(Incident::getId))
                .orElseThrow();

        assertNotNull(savedIncident.getId());
        assertEquals(stationUser.getStation().getId(), savedIncident.getStation().getId());
        assertEquals("PHYSICAL", savedIncident.getReportingMeans());
        assertEquals("PHYSICAL_REPORT", savedIncident.getSource());
        assertEquals("PENDING", savedIncident.getApprovalStatus());
        assertEquals(OperationRole.DISTRICT_OPERATION_OFFICER, savedIncident.getApprovalCurrentLevel());
    }

    @Test
    void stationOperationOfficerCanSaveMultipleEquipmentRecordsWithBlankSerialNumbers() throws Exception {
        User stationUser = createStationScopedUserWithRole("workflow.equipment.creator", OperationRole.STATION_OPERATION_OFFICER);
        String firstName = "Blank Serial Pump A";
        String secondName = "Blank Serial Pump B";

        mockMvc.perform(post("/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blank Serial Pump A",
                                  "type": "pump",
                                  "serialNumber": "",
                                  "conditionStatus": "",
                                  "operationalStatus": ""
                                }
                                """)
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(firstName));

        mockMvc.perform(post("/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blank Serial Pump B",
                                  "type": "pump",
                                  "serialNumber": "",
                                  "conditionStatus": "",
                                  "operationalStatus": ""
                                }
                                """)
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(secondName));

        Equipment firstSaved = equipmentRepository.findAll().stream()
                .filter(item -> firstName.equals(item.getName()))
                .max(Comparator.comparing(Equipment::getId))
                .orElseThrow();
        Equipment secondSaved = equipmentRepository.findAll().stream()
                .filter(item -> secondName.equals(item.getName()))
                .max(Comparator.comparing(Equipment::getId))
                .orElseThrow();

        assertEquals(stationUser.getStation().getId(), firstSaved.getStation().getId());
        assertEquals(stationUser.getStation().getId(), secondSaved.getStation().getId());
        assertNull(firstSaved.getSerialNumber());
        assertNull(secondSaved.getSerialNumber());
        assertEquals("GOOD", firstSaved.getConditionStatus());
        assertEquals("AVAILABLE", secondSaved.getOperationalStatus());
    }

    @Test
    void deniedIncidentCanBeEditedAndResubmittedByInitiator() throws Exception {
        User stationUser = createStationScopedUserWithRole("workflow.incident.resubmit", OperationRole.STATION_OPERATION_OFFICER);
        User districtUser = createStationScopedUserWithRole("workflow.district.approver", OperationRole.DISTRICT_OPERATION_OFFICER);

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentType": "fire",
                                  "severity": "medium",
                                  "description": "Initial denied incident"
                                }
                                """)
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isOk());

        Incident savedIncident = incidentRepository.findAll().stream()
                .filter(item -> item.getCreatedBy() != null && stationUser.getId().equals(item.getCreatedBy().getId()))
                .max(Comparator.comparing(Incident::getId))
                .orElseThrow();

        mockMvc.perform(post("/api/incidents/" + savedIncident.getId() + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approve": false,
                                  "comment": "Needs correction"
                                }
                                """)
                        .session(sessionFor(districtUser.getId(), districtUser.getUsername(), districtUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("DENIED"));

        mockMvc.perform(post("/api/incidents/" + savedIncident.getId() + "/resubmit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportLevel": "INITIAL",
                                  "incidentType": "fire",
                                  "severity": "high",
                                  "status": "ACTIVE",
                                  "reportingMeans": "PHYSICAL",
                                  "description": "Corrected incident after return",
                                  "locationDetails": "District yard"
                                }
                                """)
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.approvalCurrentLevel").value(OperationRole.DISTRICT_OPERATION_OFFICER))
                .andExpect(jsonPath("$.description").value("Corrected incident after return"));

        Incident resubmittedIncident = incidentRepository.findById(savedIncident.getId()).orElseThrow();
        assertEquals("PENDING", resubmittedIncident.getApprovalStatus());
        assertEquals(OperationRole.DISTRICT_OPERATION_OFFICER, resubmittedIncident.getApprovalCurrentLevel());
        assertTrue(resubmittedIncident.getApprovalLastComment().contains("Submitted"));
    }

    @Test
    void districtOperationOfficerSubmissionMovesToDistrictFireOfficerThenRegionalOperationOfficer() throws Exception {
        User districtOperationsUser = createStationScopedUserWithRole("workflow.doo.creator", OperationRole.DISTRICT_OPERATION_OFFICER);
        User districtFireOfficer = createStationScopedUserWithRole("workflow.dfo.approver", OperationRole.DISTRICT_FIRE_OFFICER);
        User regionalOperationsOfficer = createRegionalScopedUser("workflow.roo.approver", OperationRole.REGIONAL_OPERATION_OFFICER, districtOperationsUser.getStation());

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentType": "fire",
                                  "severity": "high",
                                  "description": "District operational submission"
                                }
                                """)
                        .session(sessionFor(districtOperationsUser.getId(), districtOperationsUser.getUsername(), districtOperationsUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalCurrentLevel").value(OperationRole.DISTRICT_FIRE_OFFICER));

        Incident savedIncident = incidentRepository.findAll().stream()
                .filter(item -> item.getCreatedBy() != null && districtOperationsUser.getId().equals(item.getCreatedBy().getId()))
                .max(Comparator.comparing(Incident::getId))
                .orElseThrow();

        mockMvc.perform(post("/api/incidents/" + savedIncident.getId() + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approve": true,
                                  "comment": "District fire review approved"
                                }
                                """)
                        .session(sessionFor(districtFireOfficer.getId(), districtFireOfficer.getUsername(), districtFireOfficer.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalCurrentLevel").value(OperationRole.REGIONAL_OPERATION_OFFICER))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"));

        Incident forwardedIncident = incidentRepository.findById(savedIncident.getId()).orElseThrow();
        assertEquals(OperationRole.REGIONAL_OPERATION_OFFICER, forwardedIncident.getApprovalCurrentLevel());
        assertFalse("APPROVED".equalsIgnoreCase(forwardedIncident.getApprovalStatus()));

        mockMvc.perform(post("/api/incidents/" + savedIncident.getId() + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approve": true,
                                  "comment": "Regional operations approved"
                                }
                                """)
                        .session(sessionFor(regionalOperationsOfficer.getId(), regionalOperationsOfficer.getUsername(), regionalOperationsOfficer.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
    }

    private MockHttpSession sessionFor(Long userId, String username, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setAttribute("username", username);
        session.setAttribute("role", role);
        return session;
    }

    private User createStationScopedUserWithRole(String username, String role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    Station station = stationRepository.findAll().stream().findFirst().orElseThrow();
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode("changeMeNow123!"));
                    user.setRole(role);
                    user.setFullName(username);
                    user.setDesignation(role);
                    user.setStation(station);
                    user.setActive(true);
                    return userRepository.save(user);
                });
    }

    private User createRegionalScopedUser(String username, String role, Station sourceStation) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode("changeMeNow123!"));
                    user.setRole(role);
                    user.setFullName(username);
                    user.setDesignation(role);
                    user.setStation(sourceStation);
                    user.setActive(true);
                    return userRepository.save(user);
                });
    }
}
