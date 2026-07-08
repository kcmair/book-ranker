package com.bookranker.students.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update a student")
public record UpdateStudentRequest(
    @Schema(description = "Updated student display username", example = "student123") @NotBlank
        String username) {}
