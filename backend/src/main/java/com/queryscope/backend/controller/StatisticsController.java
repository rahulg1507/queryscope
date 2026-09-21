package com.queryscope.backend.controller;

import com.queryscope.backend.dto.StatisticsColumnDto;
import com.queryscope.backend.dto.StatisticsIndexDto;
import com.queryscope.backend.dto.StatisticsResponse;
import com.queryscope.backend.dto.StatisticsTableDto;
import com.queryscope.backend.engine.statistics.StatisticsManager;
import com.queryscope.backend.engine.statistics.StatisticsSnapshot;
import com.queryscope.backend.engine.statistics.TableStatistics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsManager statisticsManager;

    public StatisticsController(StatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
    }

    @GetMapping
    public StatisticsResponse statistics() {
        StatisticsSnapshot snapshot = statisticsManager.snapshot();
        return new StatisticsResponse(snapshot.tables().values().stream()
                .map(this::toDto)
                .toList());
    }

    private StatisticsTableDto toDto(TableStatistics table) {
        return new StatisticsTableDto(
                table.name(),
                table.rowCount(),
                table.columns().values().stream()
                        .map(column -> new StatisticsColumnDto(
                                column.name(), column.type(), column.distinctValues(), column.min(), column.max()))
                        .toList(),
                table.indexes().stream()
                        .map(index -> new StatisticsIndexDto(
                                index.name(), index.column(), index.distinctKeys(), index.indexedRows()))
                        .toList()
        );
    }
}
