package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findTop10ByOrderByGeneratedAtDesc();
}
