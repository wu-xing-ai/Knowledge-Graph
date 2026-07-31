package com.zhiplatform.config;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class Neo4jConfig {

    // Bean 名必须为 "transactionManager"：@EnableNeo4jRepositories 默认按该名字查找事务管理器
    @Bean
    public PlatformTransactionManager transactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}
