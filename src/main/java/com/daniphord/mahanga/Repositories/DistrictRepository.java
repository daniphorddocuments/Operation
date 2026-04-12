package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByRegionId(Long regionId);
    List<District> findByRegionIdOrderByNameAsc(Long regionId);
    Optional<District> findByNameIgnoreCase(String name);
}
