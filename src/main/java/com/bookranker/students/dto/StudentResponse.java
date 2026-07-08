package com.bookranker.students.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student summary response")
public record StudentResponse(
    @Schema(description = "Student ID", example = "550e8400-e29b-41d4-a716-446655440000") String id,
    @Schema(description = "Student display username", example = "student123") String username) {}
