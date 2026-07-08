package com.bookranker.students.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for a student to join a class period")
public record JoinClassPeriodRequest(
    @Schema(description = "Class period join code", example = "K9X42M") @NotBlank String joinCode,
    @Schema(description = "Student display username", example = "student123") @NotBlank
        String username) {}
