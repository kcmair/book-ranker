package com.bookranker.classperiods.dto;

import com.bookranker.books.dto.BookResponse;
import com.bookranker.students.dto.StudentResponse;
import java.util.List;

public record ClassPeriodDetailsResponse(
    String id,
    String name,
    String joinCode,
    List<BookResponse> books,
    List<StudentResponse> students
) {
}
