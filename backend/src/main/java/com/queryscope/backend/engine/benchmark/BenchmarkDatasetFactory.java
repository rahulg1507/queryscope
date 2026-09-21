package com.queryscope.backend.engine.benchmark;

import com.queryscope.backend.engine.storage.ColumnDefinition;
import com.queryscope.backend.engine.storage.DataType;
import com.queryscope.backend.engine.storage.Database;
import com.queryscope.backend.engine.storage.Table;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.List;
import java.util.Map;

/** Creates isolated, repeatable benchmark data without touching the application database. */
public final class BenchmarkDatasetFactory {

    public Database create(BenchmarkDatasetSize size, boolean amountIndex) {
        Database database = new Database();
        Table users = new Table("users", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("name", DataType.STRING),
                new ColumnDefinition("age", DataType.INTEGER),
                new ColumnDefinition("active", DataType.BOOLEAN)
        )));
        for (int id = 1; id <= size.userRows(); id++) {
            users.insert(Map.of(
                    "id", id,
                    "name", "User-" + id,
                    "age", 18 + (id % 50),
                    "active", id % 2 == 0
            ));
        }

        Table expenses = new Table("expenses", new TableSchema(List.of(
                new ColumnDefinition("id", DataType.INTEGER),
                new ColumnDefinition("user_id", DataType.INTEGER),
                new ColumnDefinition("amount", DataType.INTEGER),
                new ColumnDefinition("description", DataType.STRING)
        )));
        for (int id = 1; id <= size.expenseRows(); id++) {
            int amount = ((id * 37) % 500) + 1;
            expenses.insert(Map.of(
                    "id", id,
                    "user_id", ((id - 1) % size.userRows()) + 1,
                    "amount", amount,
                    "description", "Expense-" + id
            ));
        }

        database.createTable(users);
        database.createTable(expenses);
        if (amountIndex) {
            database.createIndex("idx_benchmark_amount", "expenses", "amount");
        }
        return database;
    }
}
