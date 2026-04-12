package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.RoadLandmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadLandmarkRepository extends JpaRepository<RoadLandmark, Long> {
    List<RoadLandmark> findByVillageStreetIdOrderByNameAsc(Long villageStreetId);
}
