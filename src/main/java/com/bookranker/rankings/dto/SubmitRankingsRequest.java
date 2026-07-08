package com.bookranker.rankings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Request to submit student book rankings")
public record SubmitRankingsRequest(
    @Schema(
            description =
                "Ranked books for the student's class period. The list must meet the class minimum ranking count.")
        @NotEmpty
        List<@Valid RankingItemRequest> rankings) {}
