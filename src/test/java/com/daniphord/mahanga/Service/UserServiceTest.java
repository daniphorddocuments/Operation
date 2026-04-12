package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.District;
import com.daniphord.mahanga.Model.Region;
import com.daniphord.mahanga.Model.Station;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.DistrictRepository;
import com.daniphord.mahanga.Repositories.StationRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Util.OperationRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserDoesNotRequireStationForNationalRoles() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("StrongPassword123!");
        user.setRole(OperationRole.SUPER_ADMIN);

        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("encoded-password");
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> userService.createUser(user, null, null, null));
    }

    @Test
    void createUserAssignsRegionalScopeFromFirstActiveStation() {
        User user = new User();
        user.setUsername("regional.user");
        user.setPassword("StrongPassword123!");
        user.setRole(OperationRole.REGIONAL_OPERATION_OFFICER);

        Region region = new Region();
        region.setId(15L);

        District district = new District();
        district.setId(20L);
        district.setRegion(region);

        Station station = new Station();
        station.setId(25L);
        station.setName("Dodoma Station");
        station.setDistrict(district);
        station.setActive(true);

        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("encoded-password");
        when(userRepository.findByUsername("regional.user")).thenReturn(java.util.Optional.empty());
        when(districtRepository.findByRegionIdOrderByNameAsc(15L)).thenReturn(java.util.List.of(district));
        when(stationRepository.findByDistrictIdOrderByNameAsc(20L)).thenReturn(java.util.List.of(station));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> userService.createUser(user, null, null, 15L));
        assertEquals(25L, savedUser.getStation().getId());
    }
}
