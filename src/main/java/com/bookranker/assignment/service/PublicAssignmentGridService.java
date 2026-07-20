package com.bookranker.assignment.service;

import com.bookranker.assignment.dto.PublicClassAssignmentGridResponse;
import com.bookranker.assignment.dto.PublicClassAssignmentRowResponse;
import com.bookranker.assignment.model.Assignment;
import com.bookranker.assignment.model.AssignmentRun;
import com.bookranker.assignment.model.AssignmentRunStatus;
import com.bookranker.assignment.repository.AssignmentRepository;
import com.bookranker.assignment.repository.AssignmentRunRepository;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.service.ClassPeriodService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicAssignmentGridService {

  private final ClassPeriodService classPeriodService;
  private final AssignmentRunRepository assignmentRunRepository;
  private final AssignmentRepository assignmentRepository;

  public PublicAssignmentGridService(
      ClassPeriodService classPeriodService,
      AssignmentRunRepository assignmentRunRepository,
      AssignmentRepository assignmentRepository) {
    this.classPeriodService = classPeriodService;
    this.assignmentRunRepository = assignmentRunRepository;
    this.assignmentRepository = assignmentRepository;
  }

  @Transactional(readOnly = true)
  public PublicClassAssignmentGridResponse getPublicClassGrid(String joinCode) {
    ClassPeriod classPeriod = classPeriodService.findByJoinCode(joinCode);
    Optional<AssignmentRun> latestRun =
        assignmentRunRepository.findFirstByClassPeriodIdAndStatusOrderByCreatedAtDesc(
            classPeriod.getId(), AssignmentRunStatus.COMPLETE);

    return new PublicClassAssignmentGridResponse(
        classPeriod.getId(),
        classPeriod.getName(),
        classPeriod.getJoinCode(),
        latestRun.map(AssignmentRun::getId).orElse(null),
        buildPublicClassRows(classPeriod, latestRun));
  }

  private List<PublicClassAssignmentRowResponse> buildPublicClassRows(
      ClassPeriod classPeriod, Optional<AssignmentRun> latestRun) {
    Map<String, List<String>> studentsByBookTitle = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    classPeriod.getBooks().stream()
        .sorted(Comparator.comparing(book -> normalizedBookTitle(book.getTitle())))
        .forEach(book -> studentsByBookTitle.putIfAbsent(book.getTitle(), new ArrayList<>()));

    latestRun.ifPresent(
        assignmentRun ->
            assignmentRepository.findByAssignmentRunId(assignmentRun.getId()).stream()
                .sorted(
                    Comparator.comparing(
                            (Assignment assignment) ->
                                normalizedBookTitle(assignment.getBook().getTitle()))
                        .thenComparing(assignment -> assignment.getBook().getTitle())
                        .thenComparing(
                            assignment -> assignment.getStudent().getUsername(),
                            String.CASE_INSENSITIVE_ORDER))
                .forEach(
                    assignment ->
                        studentsByBookTitle
                            .computeIfAbsent(
                                assignment.getBook().getTitle(), ignored -> new ArrayList<>())
                            .add(assignment.getStudent().getUsername())));

    return studentsByBookTitle.entrySet().stream()
        .map(
            entry ->
                new PublicClassAssignmentRowResponse(entry.getKey(), List.copyOf(entry.getValue())))
        .toList();
  }

  private String normalizedBookTitle(String title) {
    return title.trim().toLowerCase(Locale.ROOT);
  }
}
