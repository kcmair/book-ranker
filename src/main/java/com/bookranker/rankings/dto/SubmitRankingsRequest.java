package com.bookranker.rankings.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitRankingsRequest(
    @NotEmpty List<@Valid RankingItemRequest> rankings
) {
}
