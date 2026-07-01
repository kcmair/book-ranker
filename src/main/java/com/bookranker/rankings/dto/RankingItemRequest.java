package com.bookranker.rankings.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RankingItemRequest(
    @NotBlank String bookId,
    @Min(1) int rank
) {
}
