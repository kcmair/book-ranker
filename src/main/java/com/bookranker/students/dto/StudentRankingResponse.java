package com.bookranker.students.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Single submitted student book ranking")
public record StudentRankingResponse(
    @Schema(description = "Ranked book ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String bookId,
    @Schema(description = "Student preference rank, starting at 1", example = "1") int rank) {}
