package com.bookranker.rankings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Single book ranking item")
public record RankingItemRequest(
    @Schema(description = "Ranked book ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank
        String bookId,
    @Schema(description = "Student preference rank, starting at 1", example = "1") @Min(1)
        int rank) {}
