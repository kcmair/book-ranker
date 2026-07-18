package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Submitted ranking order for a student in assignment results")
public record StudentAssignmentRankingsResponse(
    @Schema(description = "Student identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String studentId,
    @ArraySchema(schema = @Schema(description = "Submitted ranking item ordered by rank"))
        List<AssignmentRankingItemResponse> rankings) {}
