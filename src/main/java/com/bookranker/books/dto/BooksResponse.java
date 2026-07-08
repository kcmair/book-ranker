package com.bookranker.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response containing books in a class period")
public record BooksResponse(
    @Schema(description = "Class period display name", example = "English 12") String className,
    @Schema(description = "Minimum number of book rankings each student must submit", example = "5")
        int minimumRankingCount,
    @Schema(description = "Books available in the class period") List<BookResponse> books) {}
