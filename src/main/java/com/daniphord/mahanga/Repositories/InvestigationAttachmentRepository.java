package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.InvestigationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestigationAttachmentRepository extends JpaRepository<InvestigationAttachment, Long> {
    List<InvestigationAttachment> findByReportIdOrderByUploadedAtAsc(Long reportId);
}
