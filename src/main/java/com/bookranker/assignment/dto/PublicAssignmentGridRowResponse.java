package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "A display row in the public assignment grid")
public record PublicAssignmentGridRowResponse(
    @Schema(description = "Book title shown once for the first row in a book group", example = "The Hobbit")
    String bookTitle,
    @Schema(description = "Student username by class id for this row")
    Map<String, String> cells
) {
}
