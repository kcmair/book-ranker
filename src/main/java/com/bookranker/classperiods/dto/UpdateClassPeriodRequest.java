package com.bookranker.classperiods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update a class period")
public record UpdateClassPeriodRequest(
    @Schema(description = "Updated class period name", example = "English 12 Honors") @NotBlank
        String name,
    @Schema(description = "Minimum number of book rankings each student must submit", example = "3")
        @Min(1)
        Integer minimumRankingCount) {}
