package com.queryscope.backend.engine.storage;

import com.queryscope.backend.engine.execution.QueryExecutionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseTest {

    @Test
    void createsRetrievesAndListsTablesCaseInsensitively() {
        Database database = new Database();
        database.createTable(table("users"));
        database.createTable(table("expenses"));

        assertThat(database.hasTable("USERS")).isTrue();
        assertThat(database.getTable("Users").name()).isEqualTo("users");
        assertThat(database.tableNames()).containsExactly("users", "expenses");
    }

    @Test
    void rejectsDuplicateAndMissingTables() {
        Database database = new Database();
        database.createTable(table("users"));

        assertThatThrownBy(() -> database.createTable(table("USERS")))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessageContaining("already exists");
        assertThatThrownBy(() -> database.requireTable("customers"))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessage("Unknown table 'customers'.");
    }

    private static Table table(String name) {
        return new Table(name, new TableSchema(List.of(new ColumnDefinition("id", DataType.INTEGER))));
    }
}
