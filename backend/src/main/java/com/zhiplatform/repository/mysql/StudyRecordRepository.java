package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    List<StudyRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<StudyRecord> findByUserIdAndActionType(Long userId, String actionType);
}
