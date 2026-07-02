package com.bookranker.classperiods.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Class period summary response")
public record ClassPeriodSummaryResponse(
    @Schema(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String id,
    @Schema(description = "Name of the class period", example = "English 12")
    String name,
    @Schema(description = "Student join code for the class period", example = "K9X42M")
    String joinCode
) {
}
