package com.queryscope.backend.engine.storage;

import com.queryscope.backend.engine.execution.QueryExecutionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableSchemaTest {

    private final TableSchema schema = new TableSchema(List.of(
            new ColumnDefinition("id", DataType.INTEGER),
            new ColumnDefinition("name", DataType.STRING),
            new ColumnDefinition("active", DataType.BOOLEAN)
    ));

    @Test
    void preservesColumnOrderAndRejectsDuplicateNames() {
        assertThat(schema.columns()).extracting(ColumnDefinition::name)
                .containsExactly("id", "name", "active");
        assertThatThrownBy(() -> new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("ID", DataType.INTEGER))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate column name");
    }

    @Test
    void acceptsValidRowsAndCoercesIntegerValues() {
        Row row = new Row(schema, Map.of("id", 1, "name", "Rahul", "active", true));

        assertThat(row.get("ID")).isEqualTo(1L);
        assertThat(row.get("name")).isEqualTo("Rahul");
        assertThat(row.get("active")).isEqualTo(true);
    }

    @Test
    void rejectsInvalidMissingAndUnknownValues() {
        assertThatThrownBy(() -> new Row(schema, Map.of("id", 1, "name", 7, "active", true)))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessageContaining("expected STRING");
        assertThatThrownBy(() -> new Row(schema, Map.of("id", 1, "name", "Rahul")))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessageContaining("Missing required column 'active'");
        assertThatThrownBy(() -> new Row(schema, Map.of("id", 1, "name", "Rahul", "active", true, "email", "x")))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessageContaining("Unknown column 'email'");
    }
}
