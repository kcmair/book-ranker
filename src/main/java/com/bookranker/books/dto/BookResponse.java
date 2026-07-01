package com.bookranker.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Book available for student ranking")
public record BookResponse(
    @Schema(description = "Book ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String id,
    @Schema(description = "Book title", example = "To Kill a Mockingbird")
    String title,
    @Schema(description = "Maximum number of students that can receive this book", example = "10")
    int capacity
) {
}
