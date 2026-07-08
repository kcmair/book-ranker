package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A class column in the teacher assignment spreadsheet grid")
public record TeacherAssignmentGridColumnResponse(
    @Schema(
            description = "Class period identifier",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String classId,
    @Schema(description = "Class period display name", example = "English 12") String className) {}
