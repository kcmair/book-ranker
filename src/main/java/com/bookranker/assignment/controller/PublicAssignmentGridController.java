package com.bookranker.assignment.controller;

import com.bookranker.assignment.dto.PublicAssignmentGridResponse;
import com.bookranker.assignment.service.PublicAssignmentGridService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/classes/{joinCode}/assignment-grid")
@Tag(name = "Public Assignments", description = "Public assignment grid APIs for student viewing")
public class PublicAssignmentGridController {

  private final PublicAssignmentGridService publicAssignmentGridService;

  public PublicAssignmentGridController(PublicAssignmentGridService publicAssignmentGridService) {
    this.publicAssignmentGridService = publicAssignmentGridService;
  }

  @GetMapping
  @Operation(
      summary = "Get public assignment grid",
      description = "Returns a spreadsheet-shaped view of the latest completed assignment run for each class "
          + "owned by the teacher associated with the supplied join code."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Public assignment grid returned successfully"),
      @ApiResponse(responseCode = "404", description = "Join code not found")
  })
  public PublicAssignmentGridResponse getPublicAssignmentGrid(
      @Parameter(description = "Class join code used to locate the teacher's public assignment grid", required = true)
      @PathVariable String joinCode
  ) {
    return publicAssignmentGridService.getPublicGrid(joinCode);
  }
}
