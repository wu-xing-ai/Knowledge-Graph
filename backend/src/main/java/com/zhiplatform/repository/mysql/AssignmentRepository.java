package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCreateByOrderByCreateTimeDesc(Long createBy);

    List<Assignment> findByTargetTypeAndTargetId(String targetType, Long targetId);

    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);
}