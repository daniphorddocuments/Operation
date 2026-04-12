package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Equipment;
import com.daniphord.mahanga.Model.Incident;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.EquipmentRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCenterServiceTest {

    @Mock
    private OperationsService operationsService;

    @Mock
    private RoleAccessService roleAccessService;

    @Mock
    private InvestigationWorkflowService investigationWorkflowService;

    @Mock
    private DashboardDefinitionService dashboardDefinitionService;

    @Mock
    private PdfBrandingService pdfBrandingService;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private UserManualService userManualService;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private ReportCenterService reportCenterService;

    @Test
    void equipmentReportUsesVisibleDataEvenWhenCreatedAtIsMissing() {
        User user = new User();
        user.setRole(OperationRole.DISTRICT_FIRE_OFFICER);

        Station station = new Station();
        station.setId(1L);
        station.setName("Dodoma HQ Station");

        Equipment equipment = new Equipment();
        equipment.setName("Tender 1");
        equipment.setType("FIRE_TENDER");
        equipment.setOperationalStatus("AVAILABLE");
        equipment.setStation(station);
        equipment.setCreatedAt(null);
        equipment.setUpdatedAt(null);
        equipment.setLastServicedAt(null);
        equipment.setPurchaseDate(LocalDate.of(2026, 4, 10));

        when(equipmentRepository.findAll()).thenReturn(List.of(equipment));
        when(roleAccessService.visibleEquipment(user, List.of(equipment))).thenReturn(List.of(equipment));
        when(roleAccessService.visibleStations(user, List.of(station))).thenReturn(List.of(station));
        when(stationRepository.findAll()).thenReturn(List.of(station));
        when(pdfBrandingService.generatePdf(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("pdf".getBytes());

        byte[] pdf = reportCenterService.generateReport("equipment-summary", user, "en", null, null);

        assertFalse(pdf.length == 0);
    }

    @Test
    void incidentReportIncludesLegacyRowsWithoutReportedAtWhenNoDateRangeIsSelected() {
        User user = new User();
        user.setRole(OperationRole.DISTRICT_FIRE_OFFICER);

        Incident incident = new Incident();
        incident.setIncidentNumber("INC-1");
        incident.setIncidentType("FIRE");
        incident.setSeverity("HIGH");
        incident.setStatus("ACTIVE");
        incident.setReportedAt(null);
        incident.setCallReceivedAt(null);

        when(operationsService.getAllIncidents()).thenReturn(List.of(incident));
        when(roleAccessService.visibleIncidents(user, List.of(incident))).thenReturn(List.of(incident));
        when(roleAccessService.visibleStations(user, List.of())).thenReturn(List.of());
        when(stationRepository.findAll()).thenReturn(List.of());
        when(pdfBrandingService.generatePdf(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("pdf".getBytes());

        byte[] pdf = reportCenterService.generateReport("fire-fighting", user, "en", null, null);

        assertFalse(pdf.length == 0);
    }
}
