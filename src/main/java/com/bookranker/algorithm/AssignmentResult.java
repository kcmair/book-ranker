package com.bookranker.algorithm;

import java.util.Map;
import java.util.Set;

public record AssignmentResult(
    Map<ClassState.Student, ClassState.Book> assignments,
    Set<ClassState.Student> unassignedStudents,
    int totalCost,
    double satisfactionScore,
    SatisfactionDistribution satisfactionDistribution) {

  public AssignmentResult {
    assignments = Map.copyOf(assignments);
    unassignedStudents = Set.copyOf(unassignedStudents);
  }

  public record SatisfactionDistribution(
      int firstChoiceCount, int topThreeCount, int worseThanThirdCount) {}
}
