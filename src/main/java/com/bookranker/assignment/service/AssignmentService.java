package com.bookranker.assignment.service;

import com.bookranker.algorithm.AssignmentResult;
import com.bookranker.algorithm.BookAssignmentSolver;
import com.bookranker.algorithm.ClassState;
import com.bookranker.assignment.dto.AssignmentHistoryResponse;
import com.bookranker.assignment.dto.AssignmentRankingItemResponse;
import com.bookranker.assignment.dto.AssignmentResultItemResponse;
import com.bookranker.assignment.dto.AssignmentResultsResponse;
import com.bookranker.assignment.dto.AssignmentRunSummaryResponse;
import com.bookranker.assignment.dto.RunAssignmentResponse;
import com.bookranker.assignment.dto.StudentAssignmentRankingsResponse;
import com.bookranker.assignment.model.Assignment;
import com.bookranker.assignment.model.AssignmentRun;
import com.bookranker.assignment.model.AssignmentRunStatus;
import com.bookranker.assignment.repository.AssignmentRepository;
import com.bookranker.assignment.repository.AssignmentRunRepository;
import com.bookranker.books.model.Book;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.service.ClassPeriodService;
import com.bookranker.rankings.repository.RankingRepository;
import com.bookranker.students.model.Student;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssignmentService {

  private static final String ALGORITHM_VERSION = "mcmf-v1";

  private final ClassPeriodService classPeriodService;
  private final ClassStateBuilder classStateBuilder;
  private final AssignmentRunRepository assignmentRunRepository;
  private final AssignmentRepository assignmentRepository;
  private final RankingRepository rankingRepository;
  private final BookAssignmentSolver solver = new BookAssignmentSolver();

  public AssignmentService(
      ClassPeriodService classPeriodService,
      ClassStateBuilder classStateBuilder,
      AssignmentRunRepository assignmentRunRepository,
      AssignmentRepository assignmentRepository,
      RankingRepository rankingRepository) {
    this.classPeriodService = classPeriodService;
    this.classStateBuilder = classStateBuilder;
    this.assignmentRunRepository = assignmentRunRepository;
    this.assignmentRepository = assignmentRepository;
    this.rankingRepository = rankingRepository;
  }

  @Transactional
  public RunAssignmentResponse runAssignment(String classPeriodId, String teacherEmail) {
    ClassPeriod classPeriod = classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);
    ClassStateBuilder.BuiltClassState builtClassState = classStateBuilder.build(classPeriodId);

    AssignmentRun assignmentRun = new AssignmentRun();
    assignmentRun.setClassPeriod(classPeriod);
    assignmentRun.setAlgorithmVersion(ALGORITHM_VERSION);
    assignmentRun.setStatus(AssignmentRunStatus.PENDING);
    assignmentRun = assignmentRunRepository.save(assignmentRun);

    AssignmentResult result = solver.solve(builtClassState.classState());
    applyMetrics(assignmentRun, result);
    persistAssignments(assignmentRun, result, builtClassState);
    assignmentRun.setStatus(AssignmentRunStatus.COMPLETE);

    AssignmentRun saved = assignmentRunRepository.save(assignmentRun);
    return new RunAssignmentResponse(
        saved.getId(),
        saved.getStatus().name(),
        saved.getTotalCost(),
        saved.getSatisfactionScore(),
        saved.getFirstChoiceCount(),
        saved.getTopThreeCount(),
        saved.getWorseThanThirdCount(),
        saved.getUnassignedStudentCount());
  }

  @Transactional(readOnly = true)
  public AssignmentResultsResponse getLatestAssignmentResults(
      String classPeriodId, String teacherEmail) {
    classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);
    AssignmentRun assignmentRun =
        assignmentRunRepository
            .findFirstByClassPeriodIdOrderByCreatedAtDesc(classPeriodId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment run not found"));

    return new AssignmentResultsResponse(
        assignmentRun.getId(),
        assignmentRun.getTotalCost(),
        assignmentRun.getSatisfactionScore(),
        assignmentRun.getFirstChoiceCount(),
        assignmentRun.getTopThreeCount(),
        assignmentRun.getWorseThanThirdCount(),
        assignmentRun.getUnassignedStudentCount(),
        assignmentRepository.findByAssignmentRunId(assignmentRun.getId()).stream()
            .sorted(Comparator.comparing(assignment -> assignment.getStudent().getId()))
            .map(
                assignment ->
                    new AssignmentResultItemResponse(
                        assignment.getStudent().getId(), assignment.getBook().getId()))
            .toList(),
        buildStudentRankings(classPeriodId));
  }

  @Transactional(readOnly = true)
  public AssignmentHistoryResponse getAssignmentHistory(String classPeriodId, String teacherEmail) {
    classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);

    List<AssignmentRunSummaryResponse> runs =
        assignmentRunRepository.findByClassPeriodIdOrderByCreatedAtDesc(classPeriodId).stream()
            .map(
                run ->
                    new AssignmentRunSummaryResponse(
                        run.getId(),
                        run.getCreatedAt(),
                        run.getStatus().name(),
                        run.getTotalCost(),
                        run.getSatisfactionScore(),
                        run.getFirstChoiceCount(),
                        run.getTopThreeCount(),
                        run.getWorseThanThirdCount(),
                        run.getUnassignedStudentCount()))
            .toList();

    return new AssignmentHistoryResponse(runs);
  }

  private void applyMetrics(AssignmentRun assignmentRun, AssignmentResult result) {
    AssignmentResult.SatisfactionDistribution distribution = result.satisfactionDistribution();

    assignmentRun.setTotalCost(result.totalCost());
    assignmentRun.setSatisfactionScore(result.satisfactionScore());
    assignmentRun.setFirstChoiceCount(distribution.firstChoiceCount());
    assignmentRun.setTopThreeCount(distribution.topThreeCount());
    assignmentRun.setWorseThanThirdCount(distribution.worseThanThirdCount());
    assignmentRun.setUnassignedStudentCount(result.unassignedStudents().size());
  }

  private List<StudentAssignmentRankingsResponse> buildStudentRankings(String classPeriodId) {
    return rankingRepository.findByStudentClassPeriodId(classPeriodId).stream()
        .collect(Collectors.groupingBy(ranking -> ranking.getStudent().getId()))
        .entrySet()
        .stream()
        .map(
            entry ->
                new StudentAssignmentRankingsResponse(
                    entry.getKey(),
                    entry.getValue().stream()
                        .sorted(Comparator.comparingInt(ranking -> ranking.getRank()))
                        .map(
                            ranking ->
                                new AssignmentRankingItemResponse(
                                    ranking.getBook().getId(), ranking.getRank()))
                        .toList()))
        .sorted(Comparator.comparing(StudentAssignmentRankingsResponse::studentId))
        .toList();
  }

  private void persistAssignments(
      AssignmentRun assignmentRun,
      AssignmentResult result,
      ClassStateBuilder.BuiltClassState builtClassState) {
    Map<String, Student> studentsById = builtClassState.domainStudentsById();
    Map<String, Book> booksById = builtClassState.domainBooksById();

    for (Map.Entry<ClassState.Student, ClassState.Book> entry : result.assignments().entrySet()) {
      Student student = studentsById.get(entry.getKey().id());
      Book book = booksById.get(entry.getValue().id());
      if (student == null || book == null) {
        continue;
      }

      Assignment assignment = new Assignment();
      assignment.setAssignmentRun(assignmentRun);
      assignment.setStudent(student);
      assignment.setBook(book);
      assignmentRepository.save(assignment);
    }
  }
}
