package com.bookranker.assignment.service;

import com.bookranker.algorithm.AssignmentResult;
import com.bookranker.algorithm.BookAssignmentSolver;
import com.bookranker.algorithm.ClassState;
import com.bookranker.algorithm.RankingCost;
import com.bookranker.assignment.dto.AssignmentHistoryResponse;
import com.bookranker.assignment.dto.AssignmentResultsResponse;
import com.bookranker.assignment.dto.ReassignStudentRequest;
import com.bookranker.assignment.dto.RunAssignmentResponse;
import com.bookranker.assignment.model.Assignment;
import com.bookranker.assignment.model.AssignmentRun;
import com.bookranker.assignment.model.AssignmentRunStatus;
import com.bookranker.assignment.repository.AssignmentRepository;
import com.bookranker.assignment.repository.AssignmentRunRepository;
import com.bookranker.books.model.Book;
import com.bookranker.books.repository.BookRepository;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.service.ClassPeriodService;
import com.bookranker.rankings.model.Ranking;
import com.bookranker.rankings.repository.RankingRepository;
import com.bookranker.students.model.Student;
import com.bookranker.students.repository.StudentRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssignmentService {

  private static final String ALGORITHM_VERSION = "greedy-ranked-choice-v1";

  private final ClassPeriodService classPeriodService;
  private final ClassStateBuilder classStateBuilder;
  private final AssignmentResponseMapper assignmentResponseMapper;
  private final AssignmentRunRepository assignmentRunRepository;
  private final AssignmentRepository assignmentRepository;
  private final RankingRepository rankingRepository;
  private final StudentRepository studentRepository;
  private final BookRepository bookRepository;
  private final BookAssignmentSolver solver = new BookAssignmentSolver();

  public AssignmentService(
      ClassPeriodService classPeriodService,
      ClassStateBuilder classStateBuilder,
      AssignmentResponseMapper assignmentResponseMapper,
      AssignmentRunRepository assignmentRunRepository,
      AssignmentRepository assignmentRepository,
      RankingRepository rankingRepository,
      StudentRepository studentRepository,
      BookRepository bookRepository) {
    this.classPeriodService = classPeriodService;
    this.classStateBuilder = classStateBuilder;
    this.assignmentResponseMapper = assignmentResponseMapper;
    this.assignmentRunRepository = assignmentRunRepository;
    this.assignmentRepository = assignmentRepository;
    this.rankingRepository = rankingRepository;
    this.studentRepository = studentRepository;
    this.bookRepository = bookRepository;
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
    return assignmentResponseMapper.toRunAssignmentResponse(saved);
  }

  @Transactional(readOnly = true)
  public AssignmentResultsResponse getLatestAssignmentResults(
      String classPeriodId, String teacherEmail) {
    classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);
    AssignmentRun assignmentRun = findLatestCompletedRun(classPeriodId);

    return toAssignmentResultsResponse(classPeriodId, assignmentRun);
  }

  @Transactional
  public AssignmentResultsResponse reassignStudent(
      String classPeriodId, String studentId, ReassignStudentRequest request, String teacherEmail) {
    classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);
    AssignmentRun assignmentRun = findLatestCompletedRun(classPeriodId);
    Student student =
        studentRepository
            .findByIdAndClassPeriodId(studentId, classPeriodId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    Book book =
        bookRepository
            .findByIdAndClassPeriodId(request.bookId(), classPeriodId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

    Assignment assignment =
        assignmentRepository
            .findByAssignmentRunIdAndStudentId(assignmentRun.getId(), student.getId())
            .orElseGet(
                () -> {
                  Assignment newAssignment = new Assignment();
                  newAssignment.setAssignmentRun(assignmentRun);
                  newAssignment.setStudent(student);
                  return newAssignment;
                });

    assignment.setBook(book);
    assignmentRepository.save(assignment);
    recalculateMetrics(assignmentRun, classPeriodId);
    AssignmentRun saved = assignmentRunRepository.save(assignmentRun);

    return toAssignmentResultsResponse(classPeriodId, saved);
  }

  @Transactional(readOnly = true)
  public AssignmentHistoryResponse getAssignmentHistory(String classPeriodId, String teacherEmail) {
    classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);

    return new AssignmentHistoryResponse(
        assignmentRunRepository.findByClassPeriodIdOrderByCreatedAtDesc(classPeriodId).stream()
            .map(assignmentResponseMapper::toRunSummaryResponse)
            .toList());
  }

  private AssignmentResultsResponse toAssignmentResultsResponse(
      String classPeriodId, AssignmentRun assignmentRun) {
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
            .map(assignmentResponseMapper::toResultItemResponse)
            .toList(),
        assignmentResponseMapper.toStudentRankings(
            rankingRepository.findByStudentClassPeriodId(classPeriodId)));
  }

  private AssignmentRun findLatestCompletedRun(String classPeriodId) {
    return assignmentRunRepository
        .findFirstByClassPeriodIdAndStatusOrderByCreatedAtDesc(
            classPeriodId, AssignmentRunStatus.COMPLETE)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment run not found"));
  }

  private void recalculateMetrics(AssignmentRun assignmentRun, String classPeriodId) {
    List<Assignment> assignments =
        assignmentRepository.findByAssignmentRunId(assignmentRun.getId());
    int bookCount = bookRepository.findByClassPeriodId(classPeriodId).size();
    int studentCount = studentRepository.findByClassPeriodId(classPeriodId).size();
    Map<String, Map<String, Integer>> ranksByStudentAndBook =
        rankingRepository.findByStudentClassPeriodId(classPeriodId).stream()
            .collect(
                Collectors.groupingBy(
                    ranking -> ranking.getStudent().getId(),
                    Collectors.toMap(
                        ranking -> ranking.getBook().getId(),
                        Ranking::getRank,
                        (left, right) -> left)));

    Map<Student, Integer> assignedRanks = new HashMap<>();
    int totalCost = 0;
    for (Assignment assignment : assignments) {
      int rank =
          ranksByStudentAndBook
              .getOrDefault(assignment.getStudent().getId(), Map.of())
              .getOrDefault(assignment.getBook().getId(), bookCount + 1);
      totalCost += RankingCost.fromRank(rank);
      assignedRanks.put(assignment.getStudent(), rank);
    }

    AssignmentResult.SatisfactionDistribution distribution =
        satisfactionDistribution(assignedRanks);
    assignmentRun.setTotalCost(totalCost);
    assignmentRun.setSatisfactionScore(satisfactionScore(totalCost, assignments.size(), bookCount));
    assignmentRun.setFirstChoiceCount(distribution.firstChoiceCount());
    assignmentRun.setTopThreeCount(distribution.topThreeCount());
    assignmentRun.setWorseThanThirdCount(distribution.worseThanThirdCount());
    assignmentRun.setUnassignedStudentCount(studentCount - assignments.size());
  }

  private double satisfactionScore(int totalCost, int assignmentCount, int bookCount) {
    if (assignmentCount == 0 || bookCount <= 1) {
      return assignmentCount == 0 ? 0.0 : 1.0;
    }

    int maxPossibleCost = assignmentCount * bookCount;
    return Math.max(0.0, 1.0 - ((double) totalCost / maxPossibleCost));
  }

  private AssignmentResult.SatisfactionDistribution satisfactionDistribution(
      Map<Student, Integer> assignedRanks) {
    int firstChoice = 0;
    int topThree = 0;
    int worseThanThird = 0;

    for (int rank : assignedRanks.values()) {
      if (rank == 1) {
        firstChoice++;
      }
      if (rank <= 3) {
        topThree++;
      } else {
        worseThanThird++;
      }
    }

    return new AssignmentResult.SatisfactionDistribution(firstChoice, topThree, worseThanThird);
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
