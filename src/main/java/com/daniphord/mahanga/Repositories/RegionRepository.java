package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByNameIgnoreCase(String name);
    List<Region> findAllByOrderByNameAsc();
}
