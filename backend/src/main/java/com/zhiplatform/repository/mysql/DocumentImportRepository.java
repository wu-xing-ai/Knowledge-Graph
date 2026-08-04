package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.DocumentImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentImportRepository extends JpaRepository<DocumentImport, Long> {
    List<DocumentImport> findByUserIdOrderByCreateTimeDesc(Long userId);
}
