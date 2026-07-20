package com.bookranker.classperiods.service;

import com.bookranker.books.service.BookResponseMapper;
import com.bookranker.classperiods.dto.ClassPeriodDetailsResponse;
import com.bookranker.classperiods.dto.ClassPeriodSummaryResponse;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.students.service.StudentResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class ClassPeriodResponseMapper {

  private final BookResponseMapper bookResponseMapper;
  private final ClassPeriodPolicy classPeriodPolicy;
  private final StudentResponseMapper studentResponseMapper;

  public ClassPeriodResponseMapper(
      BookResponseMapper bookResponseMapper,
      ClassPeriodPolicy classPeriodPolicy,
      StudentResponseMapper studentResponseMapper) {
    this.bookResponseMapper = bookResponseMapper;
    this.classPeriodPolicy = classPeriodPolicy;
    this.studentResponseMapper = studentResponseMapper;
  }

  public ClassPeriodSummaryResponse toSummaryResponse(ClassPeriod classPeriod) {
    return new ClassPeriodSummaryResponse(
        classPeriod.getId(), classPeriod.getName(), classPeriod.getJoinCode());
  }

  public ClassPeriodDetailsResponse toDetailsResponse(ClassPeriod classPeriod) {
    return new ClassPeriodDetailsResponse(
        classPeriod.getId(),
        classPeriod.getName(),
        classPeriod.getJoinCode(),
        classPeriodPolicy.effectiveMinimumRankingCount(classPeriod),
        classPeriod.getMinimumRankingCount() != null,
        classPeriod.getBooks().stream().map(bookResponseMapper::toResponse).toList(),
        classPeriod.getStudents().stream().map(studentResponseMapper::toResponse).toList());
  }
}
