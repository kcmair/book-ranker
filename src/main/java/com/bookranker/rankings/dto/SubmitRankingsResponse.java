package com.bookranker.rankings.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after submitting rankings")
public record SubmitRankingsResponse(
    @Schema(description = "Ranking submission status", example = "submitted")
    String status
) {
}
