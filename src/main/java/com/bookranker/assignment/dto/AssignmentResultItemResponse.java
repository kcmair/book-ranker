package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single student-to-book assignment")
public record AssignmentResultItemResponse(
    @Schema(
            description = "Assigned student identifier",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String studentId,
    @Schema(
            description = "Assigned book identifier",
            example = "550e8400-e29b-41d4-a716-446655440001")
        String bookId) {}
