package com.bookranker.assignment.dto;

public record RunAssignmentResponse(
    String assignmentRunId,
    String status,
    int totalCost,
    double satisfactionScore,
    int firstChoiceCount,
    int topThreeCount,
    int worseThanThirdCount,
    int unassignedStudentCount
) {
}
