package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    List<StudyRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<StudyRecord> findByUserIdAndActionType(Long userId, String actionType);

    // 按类型查询用户记录并按时间倒序（用于问答历史）
    List<StudyRecord> findByUserIdAndActionTypeOrderByCreateTimeDesc(Long userId, String actionType);

    // 按行为类型查询全部记录（用于作答统计聚合）
    List<StudyRecord> findByActionType(String actionType);
}
