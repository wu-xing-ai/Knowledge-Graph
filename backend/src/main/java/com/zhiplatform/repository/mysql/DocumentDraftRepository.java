package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.DocumentDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentDraftRepository extends JpaRepository<DocumentDraft, Long> {
    List<DocumentDraft> findByUserIdOrderByCreateTimeDesc(Long userId);
}
