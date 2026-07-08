package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Assigned students for one book in a public class grid")
public record PublicClassAssignmentRowResponse(
    @Schema(description = "Book title", example = "The Hobbit") String bookTitle,
    @ArraySchema(schema = @Schema(description = "Student usernames assigned to this book"))
        List<String> students) {}
