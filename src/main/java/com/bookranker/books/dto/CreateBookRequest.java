package com.bookranker.books.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(
    @NotBlank String title,
    @Min(1) int capacity
) {
}
