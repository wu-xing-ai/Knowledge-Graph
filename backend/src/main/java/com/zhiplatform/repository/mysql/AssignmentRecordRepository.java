package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.AssignmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRecordRepository extends JpaRepository<AssignmentRecord, Long> {

    List<AssignmentRecord> findByAssignmentId(Long assignmentId);

    List<AssignmentRecord> findByUserId(Long userId);

    Optional<AssignmentRecord> findByAssignmentIdAndUserId(Long assignmentId, Long userId);

    void deleteByAssignmentId(Long assignmentId);
}