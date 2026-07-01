package com.bookranker.assignment.dto;

import java.util.List;

public record AssignmentHistoryResponse(
    List<AssignmentRunSummaryResponse> runs
) {
}
