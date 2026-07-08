package com.bookranker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after creating a teacher account")
public record RegisterTeacherResponse(
    @Schema(
            description = "Unique teacher identifier",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String teacherId) {}
