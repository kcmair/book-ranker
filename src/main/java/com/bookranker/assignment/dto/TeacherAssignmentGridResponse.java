package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Authenticated teacher spreadsheet-shaped assignment grid")
public record TeacherAssignmentGridResponse(
    @ArraySchema(schema = @Schema(description = "Class column"))
        List<TeacherAssignmentGridColumnResponse> columns,
    @ArraySchema(schema = @Schema(description = "Grid row"))
        List<TeacherAssignmentGridRowResponse> rows) {}
