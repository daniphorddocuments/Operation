package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.SystemTestReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemTestReportRepository extends JpaRepository<SystemTestReport, Long> {
    List<SystemTestReport> findTop20ByOrderByStartedAtDesc();
    SystemTestReport findFirstByCompletedAtIsNotNullOrderByCompletedAtDesc();
}
