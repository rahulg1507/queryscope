package com.queryscope.backend.config;

import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;
import com.queryscope.backend.engine.storage.TableSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Development-only data. The in-memory database resets when the backend restarts. */
@Component
public class DemoDatabaseSeeder {

    private final Database database;

    public DemoDatabaseSeeder(Database database) {
        this.database = database;
    }

    @PostConstruct
    public void seed() {
        TableSchema usersSchema = new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("name", DataType.STRING),
                new ColumnDefinition("age", DataType.INTEGER),
                new ColumnDefinition("active", DataType.BOOLEAN)
        ));
        Table users = new Table("users", usersSchema);
        users.insert(Map.of("id", 1, "name", "Rahul", "age", 19, "active", true));
        users.insert(Map.of("id", 2, "name", "Aayan", "age", 21, "active", true));
        users.insert(Map.of("id", 3, "name", "John", "age", 17, "active", false));
        users.insert(Map.of("id", 4, "name", "Maya", "age", 25, "active", true));
        database.createTable(users);
    }
}
