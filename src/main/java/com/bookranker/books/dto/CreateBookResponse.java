package com.bookranker.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after adding a book")
public record CreateBookResponse(
    @Schema(description = "Created book ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String bookId) {}
