package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.QaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QaTemplateRepository extends JpaRepository<QaTemplate, Long> {

    List<QaTemplate> findByCategory(String category);
}
