package com.bookranker.assignment.service;

import com.bookranker.assignment.dto.AssignmentRankingItemResponse;
import com.bookranker.assignment.dto.AssignmentResultItemResponse;
import com.bookranker.assignment.dto.AssignmentRunSummaryResponse;
import com.bookranker.assignment.dto.RunAssignmentResponse;
import com.bookranker.assignment.dto.StudentAssignmentRankingsResponse;
import com.bookranker.assignment.model.Assignment;
import com.bookranker.assignment.model.AssignmentRun;
import com.bookranker.rankings.model.Ranking;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AssignmentResponseMapper {

  public RunAssignmentResponse toRunAssignmentResponse(AssignmentRun assignmentRun) {
    return new RunAssignmentResponse(
        assignmentRun.getId(),
        assignmentRun.getStatus().name(),
        assignmentRun.getTotalCost(),
        assignmentRun.getSatisfactionScore(),
        assignmentRun.getFirstChoiceCount(),
        assignmentRun.getTopThreeCount(),
        assignmentRun.getWorseThanThirdCount(),
        assignmentRun.getUnassignedStudentCount());
  }

  public AssignmentRunSummaryResponse toRunSummaryResponse(AssignmentRun assignmentRun) {
    return new AssignmentRunSummaryResponse(
        assignmentRun.getId(),
        assignmentRun.getCreatedAt(),
        assignmentRun.getStatus().name(),
        assignmentRun.getTotalCost(),
        assignmentRun.getSatisfactionScore(),
        assignmentRun.getFirstChoiceCount(),
        assignmentRun.getTopThreeCount(),
        assignmentRun.getWorseThanThirdCount(),
        assignmentRun.getUnassignedStudentCount());
  }

  public AssignmentResultItemResponse toResultItemResponse(Assignment assignment) {
    return new AssignmentResultItemResponse(
        assignment.getStudent().getId(), assignment.getBook().getId());
  }

  public List<StudentAssignmentRankingsResponse> toStudentRankings(List<Ranking> rankings) {
    return rankings.stream()
        .collect(Collectors.groupingBy(ranking -> ranking.getStudent().getId()))
        .entrySet()
        .stream()
        .map(
            entry ->
                new StudentAssignmentRankingsResponse(
                    entry.getKey(),
                    entry.getValue().stream()
                        .sorted(Comparator.comparingInt(Ranking::getRank))
                        .map(
                            ranking ->
                                new AssignmentRankingItemResponse(
                                    ranking.getBook().getId(), ranking.getRank()))
                        .toList()))
        .sorted(Comparator.comparing(StudentAssignmentRankingsResponse::studentId))
        .toList();
  }
}
