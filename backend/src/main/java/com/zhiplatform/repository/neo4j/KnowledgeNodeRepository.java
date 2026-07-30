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

    List<KnowledgeNode> findByType(String type);

    List<KnowledgeNode> findByIsCross(Boolean isCross);

    @Query("MATCH (n:Knowledge) WHERE n.subjectId IN $subjectIds RETURN n")
    List<KnowledgeNode> findBySubjectIds(@Param("subjectIds") List<Long> subjectIds);

    @Query("MATCH (n:Knowledge) WHERE n.tags CONTAINS $tag RETURN n")
    List<KnowledgeNode> findByTag(@Param("tag") String tag);

    @Query("MATCH (n:Knowledge)-[r]-(m:Knowledge) WHERE id(n) = $nodeId RETURN m, r")
    List<KnowledgeNode> findRelatedNodes(@Param("nodeId") Long nodeId);
}
