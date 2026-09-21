package com.queryscope.backend.engine.index;

import com.queryscope.backend.engine.execution.QueryExecutionException;
import com.queryscope.backend.engine.plan.FilterCondition;
import com.queryscope.backend.engine.plan.PlanComparisonOperator;
import com.queryscope.backend.engine.storage.DataType;

import java.util.Comparator;

public final class TableIndex {

    public static final int DEFAULT_ORDER = 4;

    private final String name;
    private final String tableName;
    private final String columnName;
    private final DataType keyType;
    private final BPlusTree<Object, Integer> tree;

    public TableIndex(String name, String tableName, String columnName, DataType keyType) {
        if (name == null || name.isBlank() || tableName == null || tableName.isBlank()
                || columnName == null || columnName.isBlank() || keyType == null) {
            throw new IllegalArgumentException("An index requires a name, table, column, and key type.");
        }
        if (keyType != DataType.INTEGER && keyType != DataType.STRING) {
            throw new QueryExecutionException("Indexes support INTEGER and STRING columns only; '"
                    + columnName + "' is " + keyType + ".");
        }
        this.name = name;
        this.tableName = tableName;
        this.columnName = columnName;
        this.keyType = keyType;
        this.tree = new BPlusTree<>(DEFAULT_ORDER, comparator(keyType));
    }

    public String name() {
        return name;
    }

    public String tableName() {
        return tableName;
    }

    public String columnName() {
        return columnName;
    }

    public DataType keyType() {
        return keyType;
    }

    public void insert(Object key, int rowPosition) {
        validateKey(key);
        tree.insert(key, rowPosition);
    }

    public TreeLookupResult<Integer> lookup(FilterCondition condition) {
        if (condition.valueType() != keyType) {
            throw new QueryExecutionException("Type mismatch: index '" + name + "' is on " + keyType
                    + " but predicate uses " + condition.valueType() + ".");
        }
        validateKey(condition.value());
        return switch (condition.operator()) {
            case EQUAL -> tree.exact(condition.value());
            case GREATER_THAN -> tree.range(condition.value(), false, null, false);
            case GREATER_EQUAL -> tree.range(condition.value(), true, null, false);
            case LESS_THAN -> tree.range(null, false, condition.value(), false);
            case LESS_EQUAL -> tree.range(null, false, condition.value(), true);
            case NOT_EQUAL -> throw new QueryExecutionException("Index '" + name
                    + "' cannot satisfy a NOT_EQUAL predicate.");
        };
    }

    public int distinctKeyCount() {
        return tree.distinctKeyCount();
    }

    public int entryCount() {
        return tree.entryCount();
    }

    public void validateInvariants() {
        tree.validateInvariants();
    }

    private void validateKey(Object key) {
        if (key == null) {
            throw new QueryExecutionException("Index keys must not be null.");
        }
        boolean valid = switch (keyType) {
            case INTEGER -> key instanceof Long || key instanceof Integer;
            case STRING -> key instanceof String;
            default -> false;
        };
        if (!valid) {
            throw new QueryExecutionException("Invalid key for index '" + name + "': expected " + keyType + ".");
        }
    }

    private static Comparator<Object> comparator(DataType type) {
        return switch (type) {
            case INTEGER -> (left, right) -> Long.compare(((Number) left).longValue(), ((Number) right).longValue());
            case STRING -> (left, right) -> ((String) left).compareTo((String) right);
            default -> throw new QueryExecutionException("Indexes support INTEGER and STRING columns only.");
        };
    }
}
