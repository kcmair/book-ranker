package com.bookranker.assignment.controller;

import com.bookranker.assignment.dto.PublicClassAssignmentGridResponse;
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
      summary = "Get public class assignment grid",
      description = "Returns a public class-specific view of the latest completed assignment run for the class "
          + "identified by the supplied join code. This endpoint is public and does not use bearer authentication."
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Public class assignment grid returned successfully"),
      @ApiResponse(responseCode = "404", description = "Join code not found")
  })
  public PublicClassAssignmentGridResponse getPublicAssignmentGrid(
      @Parameter(description = "Class join code used to locate the public class assignment grid", required = true)
      @PathVariable String joinCode
  ) {
    return publicAssignmentGridService.getPublicClassGrid(joinCode);
  }
}
