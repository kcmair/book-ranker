package com.bookranker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to log in as a teacher")
public record LoginRequest(
    @Schema(description = "Teacher email address", example = "teacher@example.com")
    @Email @NotBlank String email,

    @Schema(description = "Teacher password", example = "password123", format = "password")
    @NotBlank String password
) {
}
