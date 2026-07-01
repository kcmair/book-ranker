package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Historical assignment run summary")
public record AssignmentRunSummaryResponse(
    @Schema(description = "Assignment run identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    String runId,
    @Schema(description = "Timestamp when the assignment run was created", example = "2026-07-01T19:00:00Z")
    Instant createdAt,
    @Schema(description = "Assignment run status", example = "COMPLETE")
    String status,
    @Schema(description = "Total ranking cost produced by the assignment algorithm", example = "2")
    int totalCost,
    @Schema(description = "Normalized satisfaction score from 0.0 to 1.0", example = "0.92")
    double satisfactionScore,
    @Schema(description = "Number of students assigned their first choice", example = "18")
    int firstChoiceCount,
    @Schema(description = "Number of students assigned one of their top three choices", example = "24")
    int topThreeCount,
    @Schema(description = "Number of students assigned worse than their third choice", example = "1")
    int worseThanThirdCount,
    @Schema(description = "Number of students not assigned in this run", example = "0")
    int unassignedStudentCount
) {
}
