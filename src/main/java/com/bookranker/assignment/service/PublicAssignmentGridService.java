package com.bookranker.assignment.service;

import com.bookranker.assignment.dto.PublicAssignmentGridColumnResponse;
import com.bookranker.assignment.dto.PublicAssignmentGridResponse;
import com.bookranker.assignment.dto.PublicAssignmentGridRowResponse;
import com.bookranker.assignment.model.Assignment;
import com.bookranker.assignment.model.AssignmentRun;
import com.bookranker.assignment.model.AssignmentRunStatus;
import com.bookranker.assignment.repository.AssignmentRepository;
import com.bookranker.assignment.repository.AssignmentRunRepository;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.repository.ClassPeriodRepository;
import com.bookranker.classperiods.service.ClassPeriodService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicAssignmentGridService {

  private final ClassPeriodService classPeriodService;
  private final ClassPeriodRepository classPeriodRepository;
  private final AssignmentRunRepository assignmentRunRepository;
  private final AssignmentRepository assignmentRepository;

  public PublicAssignmentGridService(
      ClassPeriodService classPeriodService,
      ClassPeriodRepository classPeriodRepository,
      AssignmentRunRepository assignmentRunRepository,
      AssignmentRepository assignmentRepository
  ) {
    this.classPeriodService = classPeriodService;
    this.classPeriodRepository = classPeriodRepository;
    this.assignmentRunRepository = assignmentRunRepository;
    this.assignmentRepository = assignmentRepository;
  }

  @Transactional(readOnly = true)
  public PublicAssignmentGridResponse getPublicGrid(String joinCode) {
    ClassPeriod sourceClassPeriod = classPeriodService.findByJoinCode(joinCode);
    String teacherId = sourceClassPeriod.getTeacher().getId();
    List<ClassPeriod> classPeriods = classPeriodRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId);

    Map<String, AssignmentRun> latestRunsByClassId = latestCompletedRunsByClassId(classPeriods);
    Map<String, Map<String, List<String>>> studentsByBookAndClass = emptyGridForTeacherBooks(classPeriods);
    addAssignedStudents(studentsByBookAndClass, latestRunsByClassId);

    return new PublicAssignmentGridResponse(
        sourceClassPeriod.getJoinCode(),
        teacherId,
        classPeriods.stream()
            .map(classPeriod -> new PublicAssignmentGridColumnResponse(classPeriod.getId(), classPeriod.getName()))
            .toList(),
        buildRows(classPeriods, studentsByBookAndClass)
    );
  }

  private Map<String, AssignmentRun> latestCompletedRunsByClassId(List<ClassPeriod> classPeriods) {
    Map<String, AssignmentRun> runsByClassId = new LinkedHashMap<>();
    for (ClassPeriod classPeriod : classPeriods) {
      Optional<AssignmentRun> run = assignmentRunRepository
          .findFirstByClassPeriodIdAndStatusOrderByCreatedAtDesc(
              classPeriod.getId(),
              AssignmentRunStatus.COMPLETE
          );
      run.ifPresent(assignmentRun -> runsByClassId.put(classPeriod.getId(), assignmentRun));
    }
    return runsByClassId;
  }

  private Map<String, Map<String, List<String>>> emptyGridForTeacherBooks(List<ClassPeriod> classPeriods) {
    Map<String, Map<String, List<String>>> grid = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (ClassPeriod classPeriod : classPeriods) {
      classPeriod.getBooks().stream()
          .sorted(Comparator.comparing(book -> normalizedBookTitle(book.getTitle())))
          .forEach(book -> grid.computeIfAbsent(book.getTitle(), ignored -> new LinkedHashMap<>()));
    }
    return grid;
  }

  private void addAssignedStudents(
      Map<String, Map<String, List<String>>> grid,
      Map<String, AssignmentRun> latestRunsByClassId
  ) {
    Map<String, String> classIdByRunId = new LinkedHashMap<>();
    for (Map.Entry<String, AssignmentRun> entry : latestRunsByClassId.entrySet()) {
      classIdByRunId.put(entry.getValue().getId(), entry.getKey());
    }

    if (classIdByRunId.isEmpty()) {
      return;
    }

    List<Assignment> assignments = assignmentRepository.findByAssignmentRunIdIn(
        List.copyOf(classIdByRunId.keySet())
    );
    assignments.stream()
        .sorted(Comparator
            .comparing((Assignment assignment) -> normalizedBookTitle(assignment.getBook().getTitle()))
            .thenComparing(assignment -> assignment.getBook().getTitle())
            .thenComparing(assignment -> classIdByRunId.get(assignment.getAssignmentRun().getId()))
            .thenComparing(assignment -> assignment.getStudent().getUsername(), String.CASE_INSENSITIVE_ORDER)
        )
        .forEach(assignment -> {
          String classId = classIdByRunId.get(assignment.getAssignmentRun().getId());
          if (classId == null) {
            return;
          }
          String bookTitle = assignment.getBook().getTitle();
          grid.computeIfAbsent(bookTitle, ignored -> new LinkedHashMap<>())
              .computeIfAbsent(classId, ignored -> new ArrayList<>())
              .add(assignment.getStudent().getUsername());
        });
  }

  private List<PublicAssignmentGridRowResponse> buildRows(
      List<ClassPeriod> classPeriods,
      Map<String, Map<String, List<String>>> studentsByBookAndClass
  ) {
    List<PublicAssignmentGridRowResponse> rows = new ArrayList<>();
    for (Map.Entry<String, Map<String, List<String>>> bookEntry : studentsByBookAndClass.entrySet()) {
      int rowCount = classPeriods.stream()
          .map(classPeriod -> bookEntry.getValue().get(classPeriod.getId()))
          .filter(Objects::nonNull)
          .mapToInt(List::size)
          .max()
          .orElse(1);

      for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
        Map<String, String> cells = new LinkedHashMap<>();
        for (ClassPeriod classPeriod : classPeriods) {
          List<String> students = bookEntry.getValue().getOrDefault(classPeriod.getId(), List.of());
          cells.put(classPeriod.getId(), rowIndex < students.size() ? students.get(rowIndex) : "");
        }
        rows.add(new PublicAssignmentGridRowResponse(rowIndex == 0 ? bookEntry.getKey() : "", cells));
      }
    }
    return rows;
  }

  private String normalizedBookTitle(String title) {
    return title.trim().toLowerCase(Locale.ROOT);
  }
}
