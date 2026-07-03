package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Public spreadsheet-shaped assignment grid for a teacher")
public record PublicAssignmentGridResponse(
    @Schema(description = "Join code used to request the grid", example = "K9X42M")
    String sourceJoinCode,
    @Schema(description = "Teacher identifier for the classes represented in the grid")
    String teacherId,
    @ArraySchema(schema = @Schema(description = "Class column"))
    List<PublicAssignmentGridColumnResponse> columns,
    @ArraySchema(schema = @Schema(description = "Grid row"))
    List<PublicAssignmentGridRowResponse> rows
) {
}
