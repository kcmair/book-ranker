package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Public class-specific assignment grid for student viewing")
public record PublicClassAssignmentGridResponse(
    @Schema(description = "Class period identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    String classId,
    @Schema(description = "Class period display name", example = "English 12")
    String className,
    @Schema(description = "Class join code used to request the grid", example = "K9X42M")
    String joinCode,
    @Schema(description = "Latest completed assignment run identifier")
    String assignmentRunId,
    @ArraySchema(schema = @Schema(description = "Book assignment row"))
    List<PublicClassAssignmentRowResponse> rows
) {
}
