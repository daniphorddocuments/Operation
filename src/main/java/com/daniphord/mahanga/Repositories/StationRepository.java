package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {
    List<Station> findByDistrictId(Long districtId);
    List<Station> findByDistrictIdOrderByNameAsc(Long districtId);
    List<Station> findByActiveTrue();
}
