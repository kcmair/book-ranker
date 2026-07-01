package com.bookranker.books.dto;

import java.util.List;

public record BooksResponse(
    List<BookResponse> books
) {
}
