package com.queryscope.backend.engine.statistics;

/** Centralized assumptions for the educational cardinality estimator. */
public final class StatisticsConfig {

    public static final double DEFAULT_FILTER_SELECTIVITY = 0.25d;
    public static final double DEFAULT_JOIN_SELECTIVITY = 0.10d;

    private StatisticsConfig() {
    }
}
