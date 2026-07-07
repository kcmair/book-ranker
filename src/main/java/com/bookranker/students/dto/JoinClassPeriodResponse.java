package com.bookranker.students.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after a student joins a class period")
public record JoinClassPeriodResponse(
    @Schema(description = "Created student ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String studentId,
    @Schema(description = "Joined class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String classId,
    @Schema(description = "Whether this username was already joined to the class period", example = "false")
    boolean existingMember
) {
}
