package com.bookranker.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BookAssignmentSolver {

  public AssignmentResult solve(ClassState classState) {
    Objects.requireNonNull(classState, "classState");

    List<ClassState.Student> eligibleStudents = eligibleStudents(classState);
    List<ClassState.Book> books = classState.books();

    int source = 0;
    int studentOffset = 1;
    int bookOffset = studentOffset + eligibleStudents.size();
    int sink = bookOffset + books.size();
    MinCostFlowGraph graph = new MinCostFlowGraph(sink + 1);

    for (int studentIndex = 0; studentIndex < eligibleStudents.size(); studentIndex++) {
      graph.addEdge(source, studentOffset + studentIndex, 1, 0);
    }

    List<StudentBookEdge> studentBookEdges = new ArrayList<>();
    for (int studentIndex = 0; studentIndex < eligibleStudents.size(); studentIndex++) {
      ClassState.Student student = eligibleStudents.get(studentIndex);
      Map<ClassState.Book, Integer> studentRankings = classState.rankings().get(student);

      for (int bookIndex = 0; bookIndex < books.size(); bookIndex++) {
        ClassState.Book book = books.get(bookIndex);
        int rank = studentRankings.getOrDefault(book, books.size() + 1);
        int cost = RankingCost.fromRank(rank);
        MinCostFlowGraph.Edge edge =
            graph.addEdge(studentOffset + studentIndex, bookOffset + bookIndex, 1, cost);
        studentBookEdges.add(new StudentBookEdge(student, book, rank, edge));
      }
    }

    for (int bookIndex = 0; bookIndex < books.size(); bookIndex++) {
      ClassState.Book book = books.get(bookIndex);
      int capacity = classState.capacities().getOrDefault(book, 0);
      graph.addEdge(bookOffset + bookIndex, sink, capacity, 0);
    }

    MinCostFlowGraph.FlowResult flowResult =
        graph.minCostMaxFlow(source, sink, eligibleStudents.size());

    Map<ClassState.Student, ClassState.Book> assignments = new LinkedHashMap<>();
    Map<ClassState.Student, Integer> assignedRanks = new HashMap<>();
    for (StudentBookEdge candidate : studentBookEdges) {
      if (candidate.edge().flow() > 0) {
        assignments.put(candidate.student(), candidate.book());
        assignedRanks.put(candidate.student(), candidate.rank());
      }
    }

    Set<ClassState.Student> unassigned = new LinkedHashSet<>(classState.students());
    unassigned.removeAll(assignments.keySet());

    return new AssignmentResult(
        assignments,
        unassigned,
        flowResult.cost(),
        satisfactionScore(flowResult.cost(), assignments.size(), books.size()),
        satisfactionDistribution(assignedRanks));
  }

  private List<ClassState.Student> eligibleStudents(ClassState classState) {
    List<ClassState.Student> eligible = new ArrayList<>();
    for (ClassState.Student student : classState.students()) {
      Map<ClassState.Book, Integer> studentRankings = classState.rankings().get(student);
      if (studentRankings == null || studentRankings.size() < classState.minimumRankingCount()) {
        continue;
      }
      eligible.add(student);
    }
    return eligible;
  }

  private double satisfactionScore(int totalCost, int assignmentCount, int bookCount) {
    if (assignmentCount == 0 || bookCount <= 1) {
      return assignmentCount == 0 ? 0.0 : 1.0;
    }

    int maxPossibleCost = assignmentCount * bookCount;
    return Math.max(0.0, 1.0 - ((double) totalCost / maxPossibleCost));
  }

  private AssignmentResult.SatisfactionDistribution satisfactionDistribution(
      Map<ClassState.Student, Integer> assignedRanks) {
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

  private record StudentBookEdge(
      ClassState.Student student, ClassState.Book book, int rank, MinCostFlowGraph.Edge edge) {}
}
