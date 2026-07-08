package com.bookranker.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update a book")
public record UpdateBookRequest(
    @Schema(description = "Updated book title", example = "To Kill a Mockingbird") @NotBlank
        String title,
    @Schema(
            description = "Updated maximum number of students that can receive this book",
            example = "12")
        @Min(1)
        int capacity) {}
