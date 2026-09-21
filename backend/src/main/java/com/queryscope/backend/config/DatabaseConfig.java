package com.queryscope.backend.config;

import com.queryscope.backend.engine.execution.ExecutionPlanExecutor;
import com.queryscope.backend.engine.execution.InMemoryQueryExecutor;
import com.queryscope.backend.engine.execution.QueryExecutor;
import com.queryscope.backend.engine.plan.ExecutionPlanBuilder;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.statistics.StatisticsManager;
import com.queryscope.backend.engine.benchmark.BenchmarkService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {

    @Bean
    public Database database() {
        return new Database();
    }

    @Bean
    public StatisticsManager statisticsManager(Database database) {
        return new StatisticsManager(database);
    }

    @Bean
    public BenchmarkService benchmarkService() {
        return new BenchmarkService();
    }

    @Bean
    public ExecutionPlanBuilder executionPlanBuilder() {
        return new ExecutionPlanBuilder();
    }

    @Bean
    public ExecutionPlanExecutor executionPlanExecutor(Database database) {
        return new ExecutionPlanExecutor(database);
    }

    @Bean
    public QueryExecutor queryExecutor(
            Database database,
            ExecutionPlanBuilder planBuilder,
            ExecutionPlanExecutor planExecutor
    ) {
        return new InMemoryQueryExecutor(database, planBuilder, planExecutor);
    }
}
