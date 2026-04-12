package com.daniphord.mahanga.Repositories;

import com.daniphord.mahanga.Model.Approval;
import com.daniphord.mahanga.Model.Approval.ApprovalTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByTargetTypeAndTargetIdOrderByApprovalLevelAsc(ApprovalTargetType targetType, Long targetId);
}
