package com.queryscope.backend.engine.optimizer;

/**
 * Small deterministic educational cost model. Costs are abstract work units,
 * never wall-clock measurements, so plans are reproducible across machines.
 */
public final class CostModel {

    private static final double TABLE_SCAN_PER_ROW = 1.0;
    private static final double INDEX_LOOKUP = 1.0;
    private static final double INDEX_TRAVERSAL_PER_LEVEL = 0.25;
    private static final double INDEX_ROW_FETCH = 1.0;
    private static final double FILTER_PER_ROW = 0.25;
    private static final double PROJECTION_PER_ROW = 0.10;
    private static final double NESTED_LOOP_COMPARISON = 1.0;
    private static final double HASH_BUILD = 1.0;
    private static final double HASH_PROBE = 1.0;
    private static final double AGGREGATE_PER_ROW = 0.50;

    public double tableScan(double rows) {
        return nonNegative(rows) * TABLE_SCAN_PER_ROW;
    }

    public double indexScan(double indexedRows, double estimatedRows) {
        double levels = Math.log(Math.max(1.0, indexedRows)) / Math.log(2.0);
        return INDEX_LOOKUP + levels * INDEX_TRAVERSAL_PER_LEVEL
                + nonNegative(estimatedRows) * INDEX_ROW_FETCH;
    }

    public double filter(double inputRows) {
        return nonNegative(inputRows) * FILTER_PER_ROW;
    }

    public double projection(double rows) {
        return nonNegative(rows) * PROJECTION_PER_ROW;
    }

    public double nestedLoopJoin(double leftRows, double rightRows) {
        return nonNegative(leftRows) * nonNegative(rightRows) * NESTED_LOOP_COMPARISON;
    }

    public double hashJoin(double leftRows, double rightRows) {
        return nonNegative(leftRows) * HASH_BUILD + nonNegative(rightRows) * HASH_PROBE;
    }

    public double aggregate(double inputRows) {
        return nonNegative(inputRows) * AGGREGATE_PER_ROW;
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
