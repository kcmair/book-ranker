package com.bookranker.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to manually reassign a student to a different book")
public record ReassignStudentRequest(
    @NotBlank @Schema(description = "Book identifier to assign to the student") String bookId) {}
