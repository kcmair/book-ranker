package com.bookranker.assignment.dto;

import java.time.Instant;

public record AssignmentRunSummaryResponse(
    String runId,
    Instant createdAt,
    String status,
    int totalCost,
    double satisfactionScore,
    int firstChoiceCount,
    int topThreeCount,
    int worseThanThirdCount,
    int unassignedStudentCount
) {
}
