package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.EmergencyCall;
import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleAccessServiceTest {

    private final RoleAccessService roleAccessService = new RoleAccessService();

    @Test
    void canManageUsersAllowsConfiguredAdminRoles() {
        assertTrue(roleAccessService.canManageUsers(userWithRole(OperationRole.SUPER_ADMIN)));
        assertTrue(roleAccessService.canManageUsers(userWithRole(OperationRole.ADMIN)));
    }

    @Test
    void canManageUsersRejectsNonAdminRoles() {
        assertFalse(roleAccessService.canManageUsers(userWithRole(OperationRole.CGF)));
        assertFalse(roleAccessService.canManageUsers(userWithRole(OperationRole.STATION_OPERATION_OFFICER)));
        assertFalse(roleAccessService.canManageUsers(userWithRole(OperationRole.OPERATION_OFFICER)));
        assertFalse(roleAccessService.canManageUsers(userWithRole(OperationRole.TELE_SUPPORT_PERSONNEL)));
    }

    @Test
    void canPublishLiveVideoAllowsControlRoomAttendantAndStationOperationOfficer() {
        assertTrue(roleAccessService.canPublishLiveVideo(userWithRole(OperationRole.STATION_OPERATION_OFFICER)));
        assertTrue(roleAccessService.canPublishLiveVideo(userWithRole("ROLE_" + OperationRole.STATION_OPERATION_OFFICER)));
        assertTrue(roleAccessService.canPublishLiveVideo(userWithRole(OperationRole.CONTROL_ROOM_ATTENDANT)));
        assertFalse(roleAccessService.canPublishLiveVideo(userWithRole(OperationRole.STATION_FIRE_OPERATION_OFFICER)));
    }

    @Test
    void dashboardAccessHelpersMatchExpectedRoles() {
        assertTrue(roleAccessService.canAccessOperationsDashboard(userWithRole(OperationRole.CGF)));
        assertTrue(roleAccessService.canAccessOperationsDashboard(userWithRole(OperationRole.DISTRICT_FIRE_OFFICER)));
        assertFalse(roleAccessService.canAccessOperationsDashboard(userWithRole(OperationRole.CONTROL_ROOM_ATTENDANT)));
        assertTrue(roleAccessService.canAccessControlRoomDashboard(userWithRole(OperationRole.CONTROL_ROOM_ATTENDANT)));
        assertFalse(roleAccessService.canAccessControlRoomDashboard(userWithRole(OperationRole.FIRE_INVESTIGATION_HOD)));
    }

    @Test
    void districtFireOfficerGetsRegionalStyleOperationalModulesAtDistrictScope() {
        User districtFireOfficer = userWithRole(OperationRole.DISTRICT_FIRE_OFFICER);
        districtFireOfficer.setStation(station(71L, district(9L, "Dodoma City", region(10L, "Dodoma"))));

        assertTrue(roleAccessService.canViewMap(districtFireOfficer));
        assertTrue(roleAccessService.canViewLiveVideo(districtFireOfficer));
        assertTrue(roleAccessService.canApproveInvestigations(districtFireOfficer));
    }

    @Test
    void controlRoomChatRequiresMatchingStation() {
        Station station = new Station();
        station.setId(7L);
        Station otherStation = new Station();
        otherStation.setId(11L);

        User matchedUser = userWithRole(OperationRole.CONTROL_ROOM_ATTENDANT);
        matchedUser.setStation(station);

        EmergencyCall matchedCall = new EmergencyCall();
        matchedCall.setRoutedStation(station);

        EmergencyCall otherCall = new EmergencyCall();
        otherCall.setRoutedStation(otherStation);

        assertTrue(roleAccessService.canAccessControlRoomChat(matchedUser, matchedCall));
        assertFalse(roleAccessService.canAccessControlRoomChat(matchedUser, otherCall));
        assertFalse(roleAccessService.canAccessControlRoomChat(userWithRole(OperationRole.CGF), matchedCall));
    }

    @Test
    void canAccessAdminDocumentsAllowsAdminButNotOperationalRoles() {
        assertTrue(roleAccessService.canAccessAdminDocuments(userWithRole(OperationRole.SUPER_ADMIN)));
        assertTrue(roleAccessService.canAccessAdminDocuments(userWithRole(OperationRole.ADMIN)));
        assertFalse(roleAccessService.canAccessAdminDocuments(userWithRole(OperationRole.CGF)));
        assertFalse(roleAccessService.canAccessAdminDocuments(userWithRole(OperationRole.OPERATION_OFFICER)));
    }

    @Test
    void regionalUserOnlySeesIncidentsAndEquipmentFromOwnRegion() {
        Station kageraStation = station(11L, district(3L, "Bukoba", region(1L, "Kagera")));
        Station kigomaStation = station(12L, district(4L, "Kasulu", region(2L, "Kigoma")));

        User regionalUser = userWithRole(OperationRole.REGIONAL_OPERATION_OFFICER);
        regionalUser.setStation(kageraStation);

        Incident kageraIncident = incident(21L, "INC-KAG", kageraStation);
        Incident kigomaIncident = incident(22L, "INC-KIG", kigomaStation);
        Equipment kageraEquipment = equipment(31L, "Pump Alpha", kageraStation);
        Equipment kigomaEquipment = equipment(32L, "Pump Beta", kigomaStation);

        assertEquals(List.of(kageraIncident), roleAccessService.visibleIncidents(regionalUser, List.of(kageraIncident, kigomaIncident)));
        assertEquals(List.of(kageraEquipment), roleAccessService.visibleEquipment(regionalUser, List.of(kageraEquipment, kigomaEquipment)));
    }

    @Test
    void headquartersRolesCanSeeAllRegionalIncidentsAndEquipment() {
        Station kageraStation = station(41L, district(5L, "Muleba", region(7L, "Kagera")));
        Station kigomaStation = station(42L, district(6L, "Ujiji", region(8L, "Kigoma")));

        User headquartersUser = userWithRole(OperationRole.FIRE_INVESTIGATION_HOD);
        Incident kageraIncident = incident(51L, "INC-HQ-KAG", kageraStation);
        Incident kigomaIncident = incident(52L, "INC-HQ-KIG", kigomaStation);
        Equipment kageraEquipment = equipment(61L, "Rescue Set", kageraStation);
        Equipment kigomaEquipment = equipment(62L, "Foam Kit", kigomaStation);

        assertEquals(List.of(kageraIncident, kigomaIncident), roleAccessService.visibleIncidents(headquartersUser, List.of(kageraIncident, kigomaIncident)));
        assertEquals(List.of(kageraEquipment, kigomaEquipment), roleAccessService.visibleEquipment(headquartersUser, List.of(kageraEquipment, kigomaEquipment)));
    }

    private User userWithRole(String role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    private Region region(Long id, String name) {
        Region region = new Region();
        region.setId(id);
        region.setName(name);
        return region;
    }

    private District district(Long id, String name, Region region) {
        District district = new District();
        district.setId(id);
        district.setName(name);
        district.setRegion(region);
        return district;
    }

    private Station station(Long id, District district) {
        Station station = new Station();
        station.setId(id);
        station.setName("Station " + id);
        station.setDistrict(district);
        return station;
    }

    private Incident incident(Long id, String number, Station station) {
        Incident incident = new Incident();
        incident.setId(id);
        incident.setIncidentNumber(number);
        incident.setIncidentType("FIRE");
        incident.setSeverity("HIGH");
        incident.setSource("TEST");
        incident.setStation(station);
        incident.setDistrict(station.getDistrict());
        incident.setRegion(station.getDistrict().getRegion());
        return incident;
    }

    private Equipment equipment(Long id, String name, Station station) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setName(name);
        equipment.setType("PUMP");
        equipment.setStation(station);
        return equipment;
    }
}
