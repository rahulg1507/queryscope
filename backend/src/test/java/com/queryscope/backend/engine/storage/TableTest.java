package com.queryscope.backend.engine.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TableTest {

    @Test
    void storesRowsInInsertionOrderAndPreservesSchema() {
        TableSchema schema = new TableSchema(List.of(new ColumnDefinition("id", DataType.INTEGER)));
        Table table = new Table("events", schema);
        table.insert(Map.of("id", 2));
        table.insert(Map.of("id", 1));

        assertThat(table.schema()).isSameAs(schema);
        assertThat(table.rows()).extracting(row -> row.get("id")).containsExactly(2L, 1L);
    }
}
