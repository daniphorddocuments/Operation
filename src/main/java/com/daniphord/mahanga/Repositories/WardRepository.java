package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WardRepository extends JpaRepository<Ward, Long> {
    List<Ward> findByDistrictIdOrderByNameAsc(Long districtId);
}
