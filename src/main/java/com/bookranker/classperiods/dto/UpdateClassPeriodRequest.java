package com.bookranker.classperiods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update a class period")
public record UpdateClassPeriodRequest(
    @Schema(description = "Updated class period name", example = "English 12 Honors")
    @NotBlank String name
) {
}
