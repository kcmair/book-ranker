package com.bookranker.assignment.controller;

import com.bookranker.assignment.dto.AssignmentHistoryResponse;
import com.bookranker.assignment.dto.AssignmentResultsResponse;
import com.bookranker.assignment.dto.RunAssignmentResponse;
import com.bookranker.assignment.service.AssignmentService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes/{classId}")
public class AssignmentController {

  private final AssignmentService assignmentService;

  public AssignmentController(AssignmentService assignmentService) {
    this.assignmentService = assignmentService;
  }

  @PostMapping("/assign")
  public RunAssignmentResponse runAssignment(
      @PathVariable String classId,
      Principal principal
  ) {
    return assignmentService.runAssignment(classId, principal.getName());
  }

  @GetMapping("/assignments/latest")
  public AssignmentResultsResponse getLatestAssignmentResults(
      @PathVariable String classId,
      Principal principal
  ) {
    return assignmentService.getLatestAssignmentResults(classId, principal.getName());
  }

  @GetMapping("/assignments")
  public AssignmentHistoryResponse getAssignmentHistory(
      @PathVariable String classId,
      Principal principal
  ) {
    return assignmentService.getAssignmentHistory(classId, principal.getName());
  }
}
