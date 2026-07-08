package com.bookranker.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to add a book to a class period")
public record CreateBookRequest(
    @Schema(description = "Book title", example = "To Kill a Mockingbird") @NotBlank String title,
    @Schema(description = "Maximum number of students that can receive this book", example = "10")
        @Min(1)
        int capacity) {}
