package com.zhiplatform.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JpaConfig {

    /**
     * 显式声明默认的 JPA 事务管理器。
     * Spring Boot 自动配置的 {@code JpaBaseConfiguration#transactionManager} 带有
     * {@code @ConditionalOnMissingBean(TransactionManager.class)}：一旦容器中存在任意
     * TransactionManager Bean（例如 Neo4j 事务管理器），JPA 事务管理器就会被抑制。
     * 这里以默认名 {@code transactionManager} 显式注册 JPA 事务管理器，
     * 确保所有 MySQL 仓库调用处于真实 JPA 事务中，且不再受 Neo4j 可用性影响。
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
