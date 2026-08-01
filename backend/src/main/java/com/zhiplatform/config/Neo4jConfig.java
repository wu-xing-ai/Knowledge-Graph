package com.zhiplatform.config;

import com.zhiplatform.repository.neo4j.GraphDataRepository;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
// 显式声明 Neo4j 仓库的事务管理器，避免占用默认名为 transactionManager 的 Bean，
// 从而把默认事务管理器留给 Spring Data JPA（否则 JPA 层将失去真实事务语义）。
@EnableNeo4jRepositories(
    basePackageClasses = { KnowledgeNodeRepository.class, GraphDataRepository.class },
    transactionManagerRef = "neo4jTransactionManager"
)
public class Neo4jConfig {

    @Bean
    public PlatformTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}
