package com.bookranker.classperiods.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClassPeriodRequest(
    @NotBlank String name
) {
}
