package com.bookranker.assignment.controller;

import com.bookranker.assignment.dto.TeacherAssignmentGridResponse;
import com.bookranker.assignment.service.PublicAssignmentGridService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers/me/assignment-grid")
@Tag(
    name = "Teacher Assignment Grid",
    description = "Authenticated teacher assignment spreadsheet APIs")
@SecurityRequirement(name = "bearerAuth")
public class TeacherAssignmentGridController {

  private final PublicAssignmentGridService publicAssignmentGridService;

  public TeacherAssignmentGridController(PublicAssignmentGridService publicAssignmentGridService) {
    this.publicAssignmentGridService = publicAssignmentGridService;
  }

  @GetMapping
  @Operation(
      summary = "Get teacher assignment spreadsheet grid",
      description =
          "Returns spreadsheet-shaped JSON for all classes owned by the currently authenticated teacher, "
              + "using each class's latest completed assignment run. Requires the Swagger Authorize bearer token or an "
              + "Authorization: Bearer <token> header.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Teacher assignment grid returned successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
      })
  public TeacherAssignmentGridResponse getTeacherAssignmentGrid(Principal principal) {
    return publicAssignmentGridService.getTeacherGrid(principal.getName());
  }
}
