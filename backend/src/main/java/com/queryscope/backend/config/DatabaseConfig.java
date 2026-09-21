package com.queryscope.backend.config;

import com.queryscope.backend.engine.execution.ExecutionPlanExecutor;
import com.queryscope.backend.engine.execution.InMemoryQueryExecutor;
import com.queryscope.backend.engine.execution.QueryExecutor;
import com.queryscope.backend.engine.plan.ExecutionPlanBuilder;
import com.queryscope.backend.engine.storage.Database;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {

    @Bean
    public Database database() {
        return new Database();
    }

    @Bean
    public ExecutionPlanBuilder executionPlanBuilder(Database database) {
        return new ExecutionPlanBuilder(database);
    }

    @Bean
    public ExecutionPlanExecutor executionPlanExecutor(Database database) {
        return new ExecutionPlanExecutor(database);
    }

    @Bean
    public QueryExecutor queryExecutor(ExecutionPlanBuilder planBuilder, ExecutionPlanExecutor planExecutor) {
        return new InMemoryQueryExecutor(planBuilder, planExecutor);
    }
}
