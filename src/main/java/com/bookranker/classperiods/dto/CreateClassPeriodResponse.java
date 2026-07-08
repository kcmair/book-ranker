package com.bookranker.classperiods.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after creating a class period")
public record CreateClassPeriodResponse(
    @Schema(
            description = "Created class period ID",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String classId,
    @Schema(description = "Student join code for the class period", example = "K9X42M")
        String joinCode) {}
