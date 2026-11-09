package com.zhiplatform.repository.mysql;

import com.zhiplatform.entity.mysql.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreateTimeDesc(Long userId);

    boolean existsByUserIdAndKnowledgeId(Long userId, Long knowledgeId);

    Optional<Favorite> findByUserIdAndKnowledgeId(Long userId, Long knowledgeId);
}
