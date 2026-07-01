package com.bookranker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTeacherRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password
) {
}
