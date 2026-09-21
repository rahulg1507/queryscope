package com.queryscope.backend.config;

import com.queryscope.backend.engine.execution.InMemoryQueryExecutor;
import com.queryscope.backend.engine.execution.QueryExecutor;
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
    public QueryExecutor queryExecutor(Database database) {
        return new InMemoryQueryExecutor(database);
    }
}
