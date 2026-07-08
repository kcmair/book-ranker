package com.bookranker.classperiods.dto;

import com.bookranker.books.dto.BookResponse;
import com.bookranker.students.dto.StudentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Detailed class period response")
public record ClassPeriodDetailsResponse(
    @Schema(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,
    @Schema(description = "Name of the class period", example = "English 12") String name,
    @Schema(description = "Student join code for the class period", example = "K9X42M")
        String joinCode,
    @Schema(description = "Minimum number of book rankings each student must submit", example = "5")
        int minimumRankingCount,
    @Schema(
            description = "Whether the minimum ranking count was explicitly set by the teacher",
            example = "false")
        boolean minimumRankingCountExplicit,
    @Schema(description = "Books available in the class period") List<BookResponse> books,
    @Schema(description = "Students who have joined the class period")
        List<StudentResponse> students) {}
