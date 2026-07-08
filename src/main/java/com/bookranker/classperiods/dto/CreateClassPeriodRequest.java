package com.bookranker.classperiods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to create a new class period")
public record CreateClassPeriodRequest(
    @Schema(description = "Name of the class period", example = "English 12") @NotBlank
        String name) {}
