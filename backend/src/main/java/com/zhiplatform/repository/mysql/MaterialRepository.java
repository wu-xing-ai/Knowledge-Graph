package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    /** 按班级查询资料，新的在前 */
    List<Material> findByClazzIdOrderByCreateTimeDesc(Long clazzId);

    /** 批量查询多个班级的资料（学生端用） */
    List<Material> findByClazzIdIn(List<Long> clazzIds);
}
