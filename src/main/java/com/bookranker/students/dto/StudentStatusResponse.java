package com.bookranker.students.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student ranking submission status")
public record StudentStatusResponse(
    @Schema(description = "Whether the student has submitted the required minimum number of rankings", example = "true")
    boolean submitted,
    @Schema(description = "Number of rankings submitted by the student", example = "5")
    long rankCount,
    @Schema(description = "Total number of books in the student's class period", example = "5")
    long totalBooks,
    @Schema(description = "Minimum number of rankings required for submission", example = "3")
    long minimumRankingCount
) {
}
