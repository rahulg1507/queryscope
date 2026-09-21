package com.queryscope.backend.engine.statistics;

import com.queryscope.backend.engine.plan.FilterCondition;
import com.queryscope.backend.engine.plan.PlanComparisonOperator;

public final class CardinalityEstimator {

    public double filterRows(TableStatistics table, FilterCondition condition) {
        if (table.rowCount() == 0) {
            return 0d;
        }
        ColumnStatistics column = table.column(condition.column());
        double selectivity = selectivity(column, condition);
        return Math.max(0d, Math.min(table.rowCount(), table.rowCount() * selectivity));
    }

    public double equalityJoinRows(TableStatistics left, String leftColumn, TableStatistics right, String rightColumn) {
        ColumnStatistics leftStats = left.column(leftColumn);
        ColumnStatistics rightStats = right.column(rightColumn);
        if (leftStats == null || rightStats == null
                || leftStats.distinctValues() == 0 || rightStats.distinctValues() == 0) {
            return left.rowCount() * right.rowCount() * StatisticsConfig.DEFAULT_JOIN_SELECTIVITY;
        }
        return (double) left.rowCount() * right.rowCount()
                / Math.max(leftStats.distinctValues(), rightStats.distinctValues());
    }

    public double selectivity(ColumnStatistics column, FilterCondition condition) {
        if (column == null) {
            return StatisticsConfig.DEFAULT_FILTER_SELECTIVITY;
        }
        if (condition.operator() == PlanComparisonOperator.EQUAL) {
            return column.distinctValues() > 0 ? 1d / column.distinctValues()
                    : StatisticsConfig.DEFAULT_FILTER_SELECTIVITY;
        }
        if (condition.operator() == PlanComparisonOperator.NOT_EQUAL) {
            return column.distinctValues() > 0 ? 1d - (1d / column.distinctValues())
                    : 1d - StatisticsConfig.DEFAULT_FILTER_SELECTIVITY;
        }
        if (!(column.min() instanceof Number min) || !(column.max() instanceof Number max)
                || !(condition.value() instanceof Number value) || max.doubleValue() <= min.doubleValue()) {
            return StatisticsConfig.DEFAULT_FILTER_SELECTIVITY;
        }
        double range = max.doubleValue() - min.doubleValue();
        double position = (value.doubleValue() - min.doubleValue()) / range;
        double selectivity = switch (condition.operator()) {
            case GREATER_THAN -> 1d - position;
            case GREATER_EQUAL -> 1d - position;
            case LESS_THAN -> position;
            case LESS_EQUAL -> position;
            default -> StatisticsConfig.DEFAULT_FILTER_SELECTIVITY;
        };
        return Math.max(0d, Math.min(1d, selectivity));
    }
}
