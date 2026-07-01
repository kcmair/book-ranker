package com.bookranker.books.dto;

public record BookResponse(
    String id,
    String title,
    int capacity
) {
}
