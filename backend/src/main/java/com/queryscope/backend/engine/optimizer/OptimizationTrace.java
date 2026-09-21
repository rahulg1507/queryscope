package com.queryscope.backend.engine.optimizer;

public record OptimizationTrace(
        String rule,
        String decision,
        String reason
) {
    public OptimizationTrace {
        if (rule == null || rule.isBlank() || decision == null || decision.isBlank()
                || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("An optimization trace requires a rule, decision, and reason.");
        }
    }
}
