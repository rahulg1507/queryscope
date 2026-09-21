package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.plan.ExecutionPlan;
import com.queryscope.backend.engine.plan.ExecutionPlanNode;
import com.queryscope.backend.engine.plan.FilterPlan;
import com.queryscope.backend.engine.plan.NestedLoopJoinPlan;
import com.queryscope.backend.engine.plan.ProjectionPlan;
import com.queryscope.backend.engine.plan.TableScanPlan;
import com.queryscope.backend.engine.storage.Database;

public final class ExecutionPlanExecutor {

    private final Database database;

    public ExecutionPlanExecutor(Database database) {
        this.database = database;
    }

    public OperatorResult execute(ExecutionPlan plan) {
        return operatorFor(plan.root()).execute();
    }

    private QueryOperator operatorFor(ExecutionPlanNode node) {
        if (node instanceof TableScanPlan tableScan) {
            return new TableScanOperator(database, tableScan.table());
        }
        if (node instanceof FilterPlan filter) {
            return new FilterOperator(operatorFor(filter.child()), filter.condition(), filter.table());
        }
        if (node instanceof NestedLoopJoinPlan join) {
            return new NestedLoopJoinOperator(
                    operatorFor(join.left()),
                    operatorFor(join.right()),
                    join.condition(),
                    join.leftTable(),
                    join.rightTable()
            );
        }
        if (node instanceof ProjectionPlan projection) {
            return new ProjectionOperator(operatorFor(projection.child()), projection.columns(), projection.table());
        }
        throw new QueryExecutionException("Unsupported execution plan node: " + node.type());
    }
}
