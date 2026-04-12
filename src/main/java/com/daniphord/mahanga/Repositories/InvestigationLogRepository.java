package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.InvestigationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestigationLogRepository extends JpaRepository<InvestigationLog, Long> {
    List<InvestigationLog> findByReportIdOrderByTimestampAsc(Long reportId);
}
