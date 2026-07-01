package com.bookranker.assignment.dto;

import java.util.List;

public record AssignmentResultsResponse(
    String runId,
    int totalCost,
    double satisfactionScore,
    int firstChoiceCount,
    int topThreeCount,
    int worseThanThirdCount,
    int unassignedStudentCount,
    List<AssignmentResultItemResponse> results
) {
}
