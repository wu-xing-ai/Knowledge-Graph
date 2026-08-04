package com.zhiplatform.repository.neo4j;

import com.zhiplatform.entity.neo4j.KnowledgeNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeNodeRepository extends Neo4jRepository<KnowledgeNode, Long> {

    List<KnowledgeNode> findBySubject(String subject);

    List<KnowledgeNode> findBySubjectId(Long subjectId);

    List<KnowledgeNode> findByNameContaining(String name);

    List<KnowledgeNode> findByName(String name);

    // 问题文本中包含某知识点名称（最精准匹配）
    @Query("MATCH (n:Knowledge) WHERE $question CONTAINS n.name RETURN n LIMIT 20")
    List<KnowledgeNode> findByNameInQuestion(@Param("question") String question);

    // 关键词在名称/描述/标签中模糊匹配
    @Query("MATCH (n:Knowledge) WHERE n.name CONTAINS $keyword OR n.description CONTAINS $keyword OR n.tags CONTAINS $keyword RETURN n")
    List<KnowledgeNode> findByKeywordAnywhere(@Param("keyword") String keyword);

    List<KnowledgeNode> findByType(String type);

    List<KnowledgeNode> findByIsCross(Boolean isCross);

    @Query("MATCH (n:Knowledge) WHERE n.subjectId IN $subjectIds RETURN n")
    List<KnowledgeNode> findBySubjectIds(@Param("subjectIds") List<Long> subjectIds);

    @Query("MATCH (n:Knowledge) WHERE n.tags CONTAINS $tag RETURN n")
    List<KnowledgeNode> findByTag(@Param("tag") String tag);

    @Query("MATCH (n:Knowledge)-[r]-(m:Knowledge) WHERE id(n) = $nodeId RETURN m, r")
    List<KnowledgeNode> findRelatedNodes(@Param("nodeId") Long nodeId);
}



