package com.bookranker.rankings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Request to submit a complete set of student book rankings")
public record SubmitRankingsRequest(
    @Schema(description = "Complete list of ranked books for the student's class period")
    @NotEmpty List<@Valid RankingItemRequest> rankings
) {
}
