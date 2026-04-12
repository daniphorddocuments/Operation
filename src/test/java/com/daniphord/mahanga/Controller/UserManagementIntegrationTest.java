package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Repositories.EmergencyCallRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.UserService;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private EmergencyCallRepository emergencyCallRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Test
    void superAdminCanAccessUserManagementApi() throws Exception {
        mockMvc.perform(get("/api/users")
                        .session(sessionFor(99L, "system.admin", OperationRole.SUPER_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void controlRoomOperatorCannotAccessUserManagementApi() throws Exception {
        mockMvc.perform(get("/api/users")
                        .session(sessionFor(99L, "control.operator", OperationRole.CONTROL_ROOM_OPERATOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void startupCleanupLeavesOnlyAdminSeedAccounts() {
        assertTrue(userRepository.findByUsername("cgf.command").isEmpty());
        assertTrue(userRepository.findByUsername("ops.command").isEmpty());
        assertTrue(userRepository.findByUsername("tele.support").isEmpty());
    }

    @Test
    void legacyCleanupDoesNotDeleteRealCreatedUsers() {
        User createdUser = createUserWithRole("real.created.user", OperationRole.STATION_OPERATION_OFFICER);

        int deletedUsers = userService.deleteLegacyBootstrapUsers(java.util.List.of("cgf.command", "ops.command", "tele.support"));

        assertTrue(deletedUsers >= 0);
        assertTrue(userRepository.findByUsername(createdUser.getUsername()).isPresent());
    }

    @Test
    void unauthenticatedDashboardRequestRendersErrorPageWithoutTemplateFailure() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 401)
                        .requestAttr("status", 401)
                        .requestAttr("error", "Unauthorized")
                        .requestAttr("message", "No authenticated session was found for this protected request.")
                        .requestAttr("path", "/dashboard")
                        .requestAttr("nextStep", "Sign in first, then open the requested page again."))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Request Failed")))
                .andExpect(content().string(not(containsString("Could not parse as expression"))));
    }

    @Test
    void dashboardRendersModalUserManagementWorkspaceForSuperAdmin() throws Exception {
        User adminUser = userRepository.findByUsername("Mahanga")
                .orElseGet(this::createSuperAdminUser);

                mockMvc.perform(get("/dashboard")
                                .session(sessionFor(adminUser.getId(), adminUser.getUsername(), OperationRole.SUPER_ADMIN)))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("id=\"userListModal\"")))
                        .andExpect(content().string(containsString("id=\"userFormModal\"")))
                        .andExpect(content().string(containsString("id=\"userManagementForm\"")))
                        .andExpect(content().string(containsString("id=\"userSearchInput\"")))
                        .andExpect(content().string(not(containsString("id=\"map-panel\""))))
                        .andExpect(content().string(containsString("id=\"system-test-module\"")))
                        .andExpect(content().string(containsString("id=\"documentation-module\"")))
                        .andExpect(content().string(containsString("Responsibility Boundaries")))
                        .andExpect(content().string(containsString("FROMS System Architecture Document")))
                        .andExpect(content().string(containsString("FROMS System Design Document")))
                        .andExpect(content().string(containsString("FROMS System Requirements Specification")))
                        .andExpect(content().string(containsString("scroll-margin-top: 104px")))
                        .andExpect(content().string(org.hamcrest.Matchers.not(containsString(".ui-hide-center { display: none !important; }"))));
    }

    @Test
    void dashboardRendersDocumentationModuleForAdminWithoutSystemTestModule() throws Exception {
        User adminUser = userRepository.findByUsername("documentation.admin")
                .orElseGet(this::createAdminUser);

        mockMvc.perform(get("/dashboard")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), OperationRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"documentation-module\"")))
                .andExpect(content().string(containsString("FROMS System Architecture Document")))
                .andExpect(content().string(containsString("FROMS System Design Document")))
                .andExpect(content().string(containsString("FROMS System Requirements Specification")))
                .andExpect(content().string(not(containsString("id=\"system-test-module\""))));
    }

    @Test
    void creatingUserWithBlankStationParamStillPersistsRecord() throws Exception {
        User adminUser = userRepository.findByUsername("Mahanga")
                .orElseGet(this::createSuperAdminUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "CHK-1001",
                                  "checkNumber": "CHK-1001",
                                  "fullName": "Popup Test User",
                                  "email": "popup@example.com",
                                  "role": "UNASSIGNED",
                                  "password": "StrongPassword123!"
                                }
                                """)
                        .param("stationId", "")
                        .session(sessionFor(adminUser.getId(), adminUser.getUsername(), OperationRole.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("CHK-1001"));
    }

    @Test
    void operationsDashboardRendersLinkedSections() throws Exception {
        User operationsUser = createUserWithRole("operations.viewer", OperationRole.COMMISSIONER_OPERATIONS);

        mockMvc.perform(get("/operations/dashboard")
                        .session(sessionFor(operationsUser.getId(), operationsUser.getUsername(), operationsUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"operations-analytics-panel\"")))
                .andExpect(content().string(containsString("id=\"notification-center\"")))
                .andExpect(content().string(not(containsString("body[data-page='operations-dashboard'] .ui-hide-center { display: none !important; }"))));
    }

    @Test
    void districtOperationsOfficerCanAccessOperationsDashboard() throws Exception {
        User districtUser = createStationScopedUserWithRole("district.operations.viewer", OperationRole.DISTRICT_OPERATION_OFFICER);

        mockMvc.perform(get("/operations/dashboard")
                        .session(sessionFor(districtUser.getId(), districtUser.getUsername(), districtUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"operations-analytics-panel\"")));
    }

    @Test
    void districtFireOfficerCanAccessOperationsDashboardWithDistrictScopedMenu() throws Exception {
        User districtUser = createStationScopedUserWithRole("district.fire.viewer", OperationRole.DISTRICT_FIRE_OFFICER);

        mockMvc.perform(get("/operations/dashboard")
                        .session(sessionFor(districtUser.getId(), districtUser.getUsername(), districtUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"operations-analytics-panel\"")))
                .andExpect(content().string(containsString("Detailed Incident Desk")))
                .andExpect(content().string(containsString("Notifications")));
    }

    @Test
    void operationsDashboardHidesUnavailableActionsForInvestigationHead() throws Exception {
        User investigationHead = createUserWithRole("investigation.head", OperationRole.FIRE_INVESTIGATION_HOD);

        mockMvc.perform(get("/operations/dashboard")
                        .session(sessionFor(investigationHead.getId(), investigationHead.getUsername(), investigationHead.getRole())))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Request Failed")))
                .andExpect(content().string(containsString("Action not allowed for your role")));
    }

    @Test
    void controlRoomDashboardRendersLinkedSections() throws Exception {
        User controlRoomUser = createUserWithRole("control.room.viewer", OperationRole.CONTROL_ROOM_ATTENDANT);

        mockMvc.perform(get("/control-room/dashboard")
                        .session(sessionFor(controlRoomUser.getId(), controlRoomUser.getUsername(), controlRoomUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"control-analytics-panel\"")))
                .andExpect(content().string(containsString("id=\"notification-center\"")))
                .andExpect(content().string(not(containsString("body[data-page='control-room'] .ui-hide-center { display: none !important; }"))));
    }

    @Test
    void controlRoomAttendantDashboardShowsLivePublisherControls() throws Exception {
        User controlRoomUser = createUserWithRole("control.room.publisher.ui", OperationRole.CONTROL_ROOM_ATTENDANT);

        mockMvc.perform(get("/control-room/dashboard")
                        .session(sessionFor(controlRoomUser.getId(), controlRoomUser.getUsername(), controlRoomUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-start-stream")))
                .andExpect(content().string(containsString("id=\"localVideo\"")))
                .andExpect(content().string(containsString("Awaiting live video stream")));
    }

    @Test
    void controlRoomOperatorDashboardHidesUnavailableActions() throws Exception {
        User controlRoomOperator = createUserWithRole("control.room.operator.ui", OperationRole.CONTROL_ROOM_OPERATOR);

        mockMvc.perform(get("/control-room/dashboard")
                        .session(sessionFor(controlRoomOperator.getId(), controlRoomOperator.getUsername(), controlRoomOperator.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/operations/dashboard\""))))
                .andExpect(content().string(not(containsString("data-start-stream"))))
                .andExpect(content().string(not(containsString("id=\"localVideo\""))));
    }

    @Test
    void cgfControlRoomDashboardHidesStationOnlyChatAction() throws Exception {
        createRoutedCall("cgf-hidden-chat");
        User cgfUser = createUserWithRole("cgf.chat.viewer", OperationRole.CGF);

        mockMvc.perform(get("/control-room/dashboard")
                        .session(sessionFor(cgfUser.getId(), cgfUser.getUsername(), cgfUser.getRole())))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Action not allowed for your role")));
    }

    @Test
    void stationOperationOfficerControlRoomDashboardShowsChatForOwnStation() throws Exception {
        User stationUser = createStationScopedUserWithRole("station.chat.viewer", OperationRole.STATION_OPERATION_OFFICER);

        mockMvc.perform(get("/control-room/dashboard")
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Action not allowed for your role")));
    }

    @Test
    void stationOperationOfficerCanAccessDashboard() throws Exception {
        User stationUser = createStationScopedUserWithRole("station.operator.viewer", OperationRole.STATION_OPERATION_OFFICER);

        mockMvc.perform(get("/dashboard")
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"user-management-module\""))))
                .andExpect(content().string(containsString("canStartLiveVideo: true")));
    }

    @Test
    void stationFireOperationOfficerCanAccessDashboard() throws Exception {
        User stationUser = createStationScopedUserWithRole("station.fire.ops.viewer", OperationRole.STATION_FIRE_OPERATION_OFFICER);

        mockMvc.perform(get("/dashboard")
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"user-management-module\""))))
                .andExpect(content().string(containsString("canStartLiveVideo: false")));
    }

    @Test
    void prefixedStationOperationOfficerRoleStillGetsPublisherFlagOnDashboard() throws Exception {
        User stationUser = createStationScopedUserWithRole("prefixed.station.operator", OperationRole.STATION_OPERATION_OFFICER);

        mockMvc.perform(get("/dashboard")
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), "ROLE_" + OperationRole.STATION_OPERATION_OFFICER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("canStartLiveVideo: true")));
    }

    @Test
    void stationOperationOfficerCanStartLiveVideoSession() throws Exception {
        User stationUser = createStationScopedUserWithRole("station.operator.publisher", OperationRole.STATION_OPERATION_OFFICER);

        mockMvc.perform(post("/api/video/sessions/start")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("locationLabel", "Station yard")
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void controlRoomAttendantCanStartLiveVideoSession() throws Exception {
        User controlRoomUser = createUserWithRole("control.room.publisher.api", OperationRole.CONTROL_ROOM_ATTENDANT);

        mockMvc.perform(post("/api/video/sessions/start")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("locationLabel", "Control room desk")
                        .session(sessionFor(controlRoomUser.getId(), controlRoomUser.getUsername(), controlRoomUser.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void stationFireOperationOfficerCannotStartLiveVideoSession() throws Exception {
        User stationUser = createStationScopedUserWithRole("station.fire.ops.viewer2", OperationRole.STATION_FIRE_OPERATION_OFFICER);

        mockMvc.perform(post("/api/video/sessions/start")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("locationLabel", "Station yard")
                        .session(sessionFor(stationUser.getId(), stationUser.getUsername(), stationUser.getRole())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only Control Room Attendants and Station Operation Officers can start live streams"));
    }

    @Test
    void publicReporterCanStartVideoSessionWithoutAuthentication() throws Exception {
        EmergencyCall call = createPublicCall("public.reporter.video");

        mockMvc.perform(post("/api/video/public/reports/{reportNumber}/sessions/start", call.getReportNumber())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", call.getPublicAccessToken())
                        .param("locationLabel", "Public scene"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"))
                .andExpect(jsonPath("$.participantType").value("PUBLIC"))
                .andExpect(jsonPath("$.callId").value(call.getId()));
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
        user.setUsername("dashboard.super.admin");
        user.setPassword(passwordEncoder.encode("changeMeNow123!"));
        user.setRole(OperationRole.SUPER_ADMIN);
        user.setFullName("Dashboard Super Admin");
        user.setDesignation(OperationRole.SUPER_ADMIN);
        return userRepository.save(user);
    }

    private User createAdminUser() {
        User user = new User();
        user.setUsername("documentation.admin");
        user.setPassword(passwordEncoder.encode("changeMeNow123!"));
        user.setRole(OperationRole.ADMIN);
        user.setFullName("Documentation Admin");
        user.setDesignation(OperationRole.ADMIN);
        return userRepository.save(user);
    }

    private User createUserWithRole(String username, String role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode("changeMeNow123!"));
                    user.setRole(role);
                    user.setFullName(username);
                    user.setDesignation(role);
                    return userRepository.save(user);
                });
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

    private EmergencyCall createPublicCall(String callerName) {
        EmergencyCall call = new EmergencyCall();
        call.setCallerName(callerName);
        call.setCallerNumber("255700111222");
        call.setIncidentType("RESCUE");
        call.setSourceChannel("PUBLIC_PORTAL");
        call.setStatus("REPORTED");
        call.setDetails("Public video start integration test");
        return emergencyCallRepository.save(call);
    }

    private EmergencyCall createRoutedCall(String callerName) {
        Station station = stationRepository.findAll().stream().findFirst().orElseThrow();
        return createRoutedCallForStation(callerName, station);
    }

    private EmergencyCall createRoutedCallForStation(String callerName, Station station) {
        EmergencyCall call = new EmergencyCall();
        call.setCallerName(callerName);
        call.setCallerNumber("255700000000");
        call.setIncidentType("FIRE");
        call.setStatus("ROUTED");
        call.setSourceChannel("TEST");
        call.setDetails("Integration test call");
        call.setRoutedStation(station);
        return emergencyCallRepository.save(call);
    }
}
