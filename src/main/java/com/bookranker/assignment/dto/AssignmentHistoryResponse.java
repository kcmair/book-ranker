package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Assignment run history for a class period")
public record AssignmentHistoryResponse(
    @ArraySchema(schema = @Schema(description = "Assignment run summary"))
    List<AssignmentRunSummaryResponse> runs
) {
}
