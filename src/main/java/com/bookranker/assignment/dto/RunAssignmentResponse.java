package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after executing an assignment run")
public record RunAssignmentResponse(
    @Schema(
            description = "Assignment run identifier",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String assignmentRunId,
    @Schema(description = "Assignment run status", example = "COMPLETE") String status,
    @Schema(description = "Total ranking cost produced by the assignment algorithm", example = "2")
        int totalCost,
    @Schema(description = "Normalized satisfaction score from 0.0 to 1.0", example = "0.92")
        double satisfactionScore,
    @Schema(description = "Number of students assigned their first choice", example = "18")
        int firstChoiceCount,
    @Schema(
            description = "Number of students assigned one of their top three choices",
            example = "24")
        int topThreeCount,
    @Schema(
            description = "Number of students assigned worse than their third choice",
            example = "1")
        int worseThanThirdCount,
    @Schema(description = "Number of students not assigned in this run", example = "0")
        int unassignedStudentCount) {}
