package com.bookranker.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BookAssignmentSolverTest {

  private final BookAssignmentSolver solver = new BookAssignmentSolver();

  @Test
  void assignsPerfectCapacityMatchAtMinimumCost() {
    ClassState.Student s1 = student("s1");
    ClassState.Student s2 = student("s2");
    ClassState.Book b1 = book("b1");
    ClassState.Book b2 = book("b2");

    AssignmentResult result =
        solver.solve(
            state(
                List.of(s1, s2),
                List.of(b1, b2),
                capacities(Map.of(b1, 1, b2, 1)),
                ranks(s1, Map.of(b1, 1, b2, 2), s2, Map.of(b1, 2, b2, 1))));

    assertEquals(Map.of(s1, b1, s2, b2), result.assignments());
    assertTrue(result.unassignedStudents().isEmpty());
    assertEquals(0, result.totalCost());
    assertEquals(1.0, result.satisfactionScore());
    assertEquals(2, result.satisfactionDistribution().firstChoiceCount());
  }

  @Test
  void leavesStudentsUnassignedWhenCapacityIsInsufficient() {
    ClassState.Student s1 = student("s1");
    ClassState.Student s2 = student("s2");
    ClassState.Student s3 = student("s3");
    ClassState.Book b1 = book("b1");
    ClassState.Book b2 = book("b2");

    AssignmentResult result =
        solver.solve(
            state(
                List.of(s1, s2, s3),
                List.of(b1, b2),
                capacities(Map.of(b1, 1, b2, 1)),
                ranks(
                    s1, Map.of(b1, 1, b2, 2), s2, Map.of(b1, 1, b2, 2), s3, Map.of(b1, 2, b2, 1))));

    assertEquals(2, result.assignments().size());
    assertEquals(1, result.unassignedStudents().size());
    assertEquals(2, assignedCount(result, b1) + assignedCount(result, b2));
    assertTrue(assignedCount(result, b1) <= 1);
    assertTrue(assignedCount(result, b2) <= 1);
  }

  @Test
  void allowsUnusedBookCapacity() {
    ClassState.Student s1 = student("s1");
    ClassState.Book b1 = book("b1");
    ClassState.Book b2 = book("b2");

    AssignmentResult result =
        solver.solve(
            state(
                List.of(s1),
                List.of(b1, b2),
                capacities(Map.of(b1, 5, b2, 5)),
                ranks(s1, Map.of(b1, 1, b2, 2))));

    assertEquals(Map.of(s1, b1), result.assignments());
    assertTrue(result.unassignedStudents().isEmpty());
  }

  @Test
  void handlesSkewedPreferencesWithoutExceedingCapacity() {
    ClassState.Student s1 = student("s1");
    ClassState.Student s2 = student("s2");
    ClassState.Student s3 = student("s3");
    ClassState.Book popular = book("popular");
    ClassState.Book backup = book("backup");

    AssignmentResult result =
        solver.solve(
            state(
                List.of(s1, s2, s3),
                List.of(popular, backup),
                capacities(Map.of(popular, 1, backup, 2)),
                ranks(
                    s1,
                    Map.of(popular, 1, backup, 2),
                    s2,
                    Map.of(popular, 1, backup, 2),
                    s3,
                    Map.of(popular, 1, backup, 2))));

    assertEquals(3, result.assignments().size());
    assertEquals(1, assignedCount(result, popular));
    assertEquals(2, assignedCount(result, backup));
    assertEquals(2, result.totalCost());
  }

  @Test
  void assignsStudentsWithAnyRankingToFirstAvailableRankedBook() {
    ClassState.Student first = student("first");
    ClassState.Student second = student("second");
    ClassState.Book b1 = book("b1");
    ClassState.Book b2 = book("b2");

    AssignmentResult result =
        solver.solve(
            state(
                List.of(first, second),
                List.of(b1, b2),
                capacities(Map.of(b1, 1, b2, 1)),
                ranks(first, Map.of(b1, 1), second, Map.of(b1, 1, b2, 2))));

    assertEquals(Map.of(first, b1, second, b2), result.assignments());
    assertTrue(result.unassignedStudents().isEmpty());
  }

  @Test
  void leavesStudentUnassignedWhenAllRankedBooksAreFull() {
    ClassState.Student partial = student("partial");
    ClassState.Book b1 = book("b1");
    ClassState.Book b2 = book("b2");
    ClassState.Book b3 = book("b3");

    AssignmentResult result =
        solver.solve(
            new ClassState(
                List.of(partial),
                List.of(b1, b2, b3),
                ranks(partial, Map.of(b1, 1, b2, 2)),
                capacities(Map.of(b1, 0, b2, 0, b3, 2)),
                2));

    assertTrue(result.assignments().isEmpty());
    assertTrue(result.unassignedStudents().contains(partial));
    assertEquals(0.0, result.satisfactionScore());
  }

  @Test
  void returnsDeterministicAssignmentsForEqualOptimalSolutions() {
    ClassState.Student s1 = student("s1");
    ClassState.Student s2 = student("s2");
    ClassState.Book b1 = book("b1");
    ClassState.Book b2 = book("b2");
    ClassState state =
        state(
            List.of(s1, s2),
            List.of(b1, b2),
            capacities(Map.of(b1, 1, b2, 1)),
            ranks(s1, Map.of(b1, 1, b2, 1), s2, Map.of(b1, 1, b2, 1)));

    AssignmentResult first = solver.solve(state);
    AssignmentResult second = solver.solve(state);

    assertEquals(first.assignments(), second.assignments());
  }

  @Test
  void solvesModeratelyLargeDatasetWithinCorrectnessConstraints() {
    int studentCount = 200;
    int bookCount = 20;
    List<ClassState.Student> students = new ArrayList<>();
    List<ClassState.Book> books = new ArrayList<>();
    Map<ClassState.Book, Integer> capacities = new LinkedHashMap<>();
    Map<ClassState.Student, Map<ClassState.Book, Integer>> rankings = new LinkedHashMap<>();

    for (int bookIndex = 0; bookIndex < bookCount; bookIndex++) {
      ClassState.Book book = book("b" + bookIndex);
      books.add(book);
      capacities.put(book, 10);
    }

    for (int studentIndex = 0; studentIndex < studentCount; studentIndex++) {
      ClassState.Student student = student("s" + studentIndex);
      students.add(student);
      Map<ClassState.Book, Integer> studentRanks = new LinkedHashMap<>();
      for (int bookIndex = 0; bookIndex < bookCount; bookIndex++) {
        studentRanks.put(books.get(bookIndex), ((studentIndex + bookIndex) % bookCount) + 1);
      }
      rankings.put(student, studentRanks);
    }

    AssignmentResult result = solver.solve(new ClassState(students, books, rankings, capacities));

    assertEquals(studentCount, result.assignments().size());
    assertTrue(result.unassignedStudents().isEmpty());
    for (ClassState.Book book : books) {
      assertTrue(assignedCount(result, book) <= capacities.get(book));
    }
    for (ClassState.Book assignedBook : result.assignments().values()) {
      assertFalse(assignedBook.id().isBlank());
    }
  }

  private ClassState state(
      List<ClassState.Student> students,
      List<ClassState.Book> books,
      Map<ClassState.Book, Integer> capacities,
      Map<ClassState.Student, Map<ClassState.Book, Integer>> rankings) {
    return new ClassState(students, books, rankings, capacities);
  }

  private ClassState.Student student(String id) {
    return new ClassState.Student(id);
  }

  private ClassState.Book book(String id) {
    return new ClassState.Book(id);
  }

  private Map<ClassState.Book, Integer> capacities(Map<ClassState.Book, Integer> capacities) {
    return new LinkedHashMap<>(capacities);
  }

  @SafeVarargs
  private final Map<ClassState.Student, Map<ClassState.Book, Integer>> ranks(
      Object... studentRankingPairs) {
    Map<ClassState.Student, Map<ClassState.Book, Integer>> result = new LinkedHashMap<>();
    for (int i = 0; i < studentRankingPairs.length; i += 2) {
      ClassState.Student student = (ClassState.Student) studentRankingPairs[i];
      @SuppressWarnings("unchecked")
      Map<ClassState.Book, Integer> rankings =
          (Map<ClassState.Book, Integer>) studentRankingPairs[i + 1];
      result.put(student, new LinkedHashMap<>(rankings));
    }
    return result;
  }

  private int assignedCount(AssignmentResult result, ClassState.Book book) {
    int count = 0;
    for (ClassState.Book assignedBook : result.assignments().values()) {
      if (assignedBook.equals(book)) {
        count++;
      }
    }
    return count;
  }
}
