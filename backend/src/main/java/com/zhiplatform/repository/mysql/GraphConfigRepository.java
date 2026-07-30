package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.GraphConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphConfigRepository extends JpaRepository<GraphConfig, Long> {

    List<GraphConfig> findByUserId(Long userId);
}
