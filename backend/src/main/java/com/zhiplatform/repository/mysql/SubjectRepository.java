package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByType(String type);

    List<Subject> findByParentId(Long parentId);
}
