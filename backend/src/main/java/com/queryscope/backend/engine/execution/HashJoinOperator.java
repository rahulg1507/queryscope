package com.queryscope.backend.engine.execution;

import com.queryscope.backend.engine.plan.JoinCondition;
import com.queryscope.backend.engine.storage.ColumnResolution;
import com.queryscope.backend.engine.storage.Row;
import com.queryscope.backend.engine.storage.TableSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HashJoinOperator implements QueryOperator {

    private final QueryOperator left;
    private final QueryOperator right;
    private final JoinCondition condition;
    private final String leftTable;
    private final String rightTable;

    public HashJoinOperator(
            QueryOperator left,
            QueryOperator right,
            JoinCondition condition,
            String leftTable,
            String rightTable
    ) {
        this.left = left;
        this.right = right;
        this.condition = condition;
        this.leftTable = leftTable;
        this.rightTable = rightTable;
    }

    @Override
    public OperatorResult execute() {
        OperatorResult leftResult = left.execute();
        OperatorResult rightResult = right.execute();
        JoinOperatorSupport.ResolvedColumns columns = JoinOperatorSupport.resolve(
                leftResult, rightResult, condition, leftTable, rightTable
        );
        TableSchema joinedSchema = JoinOperatorSupport.joinedSchema(
                leftResult.schema(), rightResult.schema(), leftTable, rightTable
        );

        boolean buildLeft = leftResult.rows().size() <= rightResult.rows().size();
        OperatorResult buildResult = buildLeft ? leftResult : rightResult;
        OperatorResult probeResult = buildLeft ? rightResult : leftResult;
        ColumnResolution buildColumn = buildLeft ? columns.left() : columns.right();
        ColumnResolution probeColumn = buildLeft ? columns.right() : columns.left();
        Map<HashKey, List<BuildEntry>> buckets = new LinkedHashMap<>();
        int rowsInserted = 0;
        for (int index = 0; index < buildResult.rows().size(); index++) {
            Row row = buildResult.rows().get(index);
            buckets.computeIfAbsent(HashKey.of(row.get(buildColumn.actualName())), ignored -> new ArrayList<>())
                    .add(new BuildEntry(index, row));
            rowsInserted++;
        }

        List<Row> joinedRows = new ArrayList<>();
        List<List<Row>> leftMatches = buildLeft
                ? new ArrayList<>(Collections.nCopies(leftResult.rows().size(), null))
                : List.of();
        int hashLookups = 0;
        int matches = 0;
        for (Row probeRow : probeResult.rows()) {
            hashLookups++;
            List<BuildEntry> matchesForKey = buckets.get(HashKey.of(probeRow.get(probeColumn.actualName())));
            if (matchesForKey == null) {
                continue;
            }
            for (BuildEntry buildEntry : matchesForKey) {
                Row buildRow = buildEntry.row();
                Row leftRow = buildLeft ? buildRow : probeRow;
                Row rightRow = buildLeft ? probeRow : buildRow;
                if (buildLeft) {
                    List<Row> rightMatches = leftMatches.get(buildEntry.index());
                    if (rightMatches == null) {
                        rightMatches = new ArrayList<>();
                        leftMatches.set(buildEntry.index(), rightMatches);
                    }
                    rightMatches.add(rightRow);
                } else {
                    joinedRows.add(JoinOperatorSupport.combine(
                            joinedSchema, leftRow, rightRow, leftResult.schema(), rightResult.schema(), leftTable, rightTable
                    ));
                }
                matches++;
            }
        }
        if (buildLeft) {
            for (int leftIndex = 0; leftIndex < leftResult.rows().size(); leftIndex++) {
                List<Row> rightRows = leftMatches.get(leftIndex);
                if (rightRows == null) {
                    continue;
                }
                for (Row rightRow : rightRows) {
                    joinedRows.add(JoinOperatorSupport.combine(
                            joinedSchema, leftResult.rows().get(leftIndex), rightRow,
                            leftResult.schema(), rightResult.schema(), leftTable, rightTable
                    ));
                }
            }
        }

        Map<String, Object> details = Map.of(
                "buildSide", buildLeft ? "LEFT" : "RIGHT",
                "probeSide", buildLeft ? "RIGHT" : "LEFT",
                "buildRows", buildResult.rows().size(),
                "probeRows", probeResult.rows().size(),
                "rowsInserted", rowsInserted,
                "hashLookups", hashLookups,
                "hashBuckets", buckets.size(),
                "matches", matches
        );
        return new OperatorResult(
                joinedSchema,
                joinedRows,
                new OperatorMetrics("HashJoin", leftResult.rows().size() + rightResult.rows().size(),
                        joinedRows.size(), List.of(leftResult.metrics(), rightResult.metrics()), details)
        );
    }

    private record HashKey(Object value) {
        private static HashKey of(Object value) {
            return new HashKey(value);
        }
    }

    private record BuildEntry(int index, Row row) {
    }
}
