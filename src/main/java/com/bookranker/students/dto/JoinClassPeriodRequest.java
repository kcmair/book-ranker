package com.bookranker.students.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinClassPeriodRequest(
    @NotBlank String joinCode,
    @NotBlank String username
) {
}
