package com.bookranker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a teacher account")
public record RegisterTeacherRequest(
    @Schema(description = "Teacher email address", example = "teacher@example.com") @Email @NotBlank
        String email,
    @Schema(description = "Teacher password", example = "password123", format = "password")
        @NotBlank
        @Size(min = 8)
        String password) {}
