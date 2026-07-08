package com.bookranker.assignment.controller;

import com.bookranker.assignment.dto.AssignmentHistoryResponse;
import com.bookranker.assignment.dto.AssignmentResultsResponse;
import com.bookranker.assignment.dto.RunAssignmentResponse;
import com.bookranker.assignment.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes/{classId}")
@Tag(
    name = "Assignments",
    description = "Run and inspect book assignment results for a class period")
@SecurityRequirement(name = "bearerAuth")
public class AssignmentController {

  private final AssignmentService assignmentService;

  public AssignmentController(AssignmentService assignmentService) {
    this.assignmentService = assignmentService;
  }

  @PostMapping("/assign")
  @Operation(
      summary = "Run book assignment",
      description =
          "Builds the class assignment state, runs the MCMF solver, persists a historical assignment run, "
              + "and returns run status and satisfaction metrics. Requires the Swagger Authorize bearer token or an "
              + "Authorization: Bearer <token> header.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Assignment run completed successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
        @ApiResponse(
            responseCode = "403",
            description = "Authenticated teacher does not own the class period"),
        @ApiResponse(responseCode = "404", description = "Class period not found")
      })
  public RunAssignmentResponse runAssignment(
      @Parameter(description = "Class period identifier", required = true) @PathVariable
          String classId,
      Principal principal) {
    return assignmentService.runAssignment(classId, principal.getName());
  }

  @GetMapping("/assignments/latest")
  @Operation(
      summary = "Get latest assignment results",
      description =
          "Returns the most recent assignment run for the class period, including persisted assignments "
              + "and satisfaction metrics. Requires the Swagger Authorize bearer token or an Authorization: Bearer "
              + "<token> header.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Latest assignment results returned successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
        @ApiResponse(
            responseCode = "403",
            description = "Authenticated teacher does not own the class period"),
        @ApiResponse(responseCode = "404", description = "Class period or assignment run not found")
      })
  public AssignmentResultsResponse getLatestAssignmentResults(
      @Parameter(description = "Class period identifier", required = true) @PathVariable
          String classId,
      Principal principal) {
    return assignmentService.getLatestAssignmentResults(classId, principal.getName());
  }

  @GetMapping("/assignments")
  @Operation(
      summary = "Get assignment run history",
      description =
          "Returns historical assignment runs for the class period in newest-first order. Requires the "
              + "Swagger Authorize bearer token or an Authorization: Bearer <token> header.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Assignment history returned successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
        @ApiResponse(
            responseCode = "403",
            description = "Authenticated teacher does not own the class period"),
        @ApiResponse(responseCode = "404", description = "Class period not found")
      })
  public AssignmentHistoryResponse getAssignmentHistory(
      @Parameter(description = "Class period identifier", required = true) @PathVariable
          String classId,
      Principal principal) {
    return assignmentService.getAssignmentHistory(classId, principal.getName());
  }
}
