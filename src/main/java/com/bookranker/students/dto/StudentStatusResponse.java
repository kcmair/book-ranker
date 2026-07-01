package com.bookranker.students.dto;

public record StudentStatusResponse(
    boolean submitted,
    long rankCount,
    long totalBooks
) {
}
