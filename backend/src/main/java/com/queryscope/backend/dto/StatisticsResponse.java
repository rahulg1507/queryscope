package com.queryscope.backend.dto;

import java.util.List;

public record StatisticsResponse(List<StatisticsTableDto> tables) {
}
