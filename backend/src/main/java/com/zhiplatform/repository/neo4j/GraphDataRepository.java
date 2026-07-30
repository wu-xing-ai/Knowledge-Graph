package com.zhiplatform.repository.neo4j;

import com.zhiplatform.entity.neo4j.KnowledgeNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface GraphDataRepository extends Neo4jRepository<KnowledgeNode, Long> {

    @Query("MATCH (n:Knowledge)-[r]->(m:Knowledge) " +
           "WHERE ($subjectIds IS NULL OR n.subjectId IN $subjectIds) " +
           "RETURN n, r, m LIMIT $limit")
    List<Map<String, Object>> getGraphData(@Param("subjectIds") List<Long> subjectIds,
                                           @Param("limit") int limit);

    @Query("MATCH (n:Knowledge)-[r]-(m:Knowledge) " +
           "WHERE id(n) = $nodeId " +
           "RETURN n, r, m")
    List<Map<String, Object>> expandNode(@Param("nodeId") Long nodeId);

    @Query("MATCH (n:Knowledge)-[r*1..2]-(m:Knowledge) " +
           "WHERE id(n) = $nodeId " +
           "RETURN n, r, m LIMIT 50")
    List<Map<String, Object>> expandNodeDepth2(@Param("nodeId") Long nodeId);

    @Query("MATCH (n:Knowledge)-[r]->(m:Knowledge) RETURN n, r, m LIMIT $limit")
    List<Map<String, Object>> getAllGraphData(@Param("limit") int limit);

    @Query("MATCH (n:Knowledge) WHERE n.name CONTAINS $keyword OR n.description CONTAINS $keyword " +
           "MATCH (n)-[r]-(m:Knowledge) RETURN n, r, m LIMIT 20")
    List<Map<String, Object>> searchByKeyword(@Param("keyword") String keyword);
}
