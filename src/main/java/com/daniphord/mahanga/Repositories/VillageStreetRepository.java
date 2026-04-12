package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.VillageStreet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VillageStreetRepository extends JpaRepository<VillageStreet, Long> {
    List<VillageStreet> findByWardIdOrderByNameAsc(Long wardId);
}
