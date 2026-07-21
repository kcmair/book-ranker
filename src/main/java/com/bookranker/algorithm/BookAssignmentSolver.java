package com.bookranker.algorithm;

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

    Map<ClassState.Book, Integer> remainingCapacity = new HashMap<>();
    for (ClassState.Book book : classState.books()) {
      remainingCapacity.put(book, classState.capacities().getOrDefault(book, 0));
    }
    Map<ClassState.Student, ClassState.Book> assignments = new LinkedHashMap<>();
    Map<ClassState.Student, Integer> assignedRanks = new HashMap<>();
    int totalCost = 0;

    for (ClassState.Student student : classState.students()) {
      Map<ClassState.Book, Integer> studentRankings = classState.rankings().get(student);
      if (studentRankings == null || studentRankings.isEmpty()) {
        continue;
      }

      for (Map.Entry<ClassState.Book, Integer> candidate : sortedRankings(studentRankings)) {
        ClassState.Book book = candidate.getKey();
        int capacity = remainingCapacity.getOrDefault(book, 0);
        if (capacity <= 0) {
          continue;
        }

        assignments.put(student, book);
        assignedRanks.put(student, candidate.getValue());
        remainingCapacity.put(book, capacity - 1);
        totalCost += RankingCost.fromRank(candidate.getValue());
        break;
      }
    }

    Set<ClassState.Student> unassigned = new LinkedHashSet<>(classState.students());
    unassigned.removeAll(assignments.keySet());

    return new AssignmentResult(
        assignments,
        unassigned,
        totalCost,
        satisfactionScore(totalCost, assignments.size(), classState.books().size()),
        satisfactionDistribution(assignedRanks));
  }

  private List<Map.Entry<ClassState.Book, Integer>> sortedRankings(
      Map<ClassState.Book, Integer> studentRankings) {
    return studentRankings.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
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
}
