package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A submitted book ranking included with assignment results")
public record AssignmentRankingItemResponse(
    @Schema(
            description = "Ranked book identifier",
            example = "550e8400-e29b-41d4-a716-446655440001")
        String bookId,
    @Schema(description = "Student preference rank, starting at 1", example = "1") int rank) {}
