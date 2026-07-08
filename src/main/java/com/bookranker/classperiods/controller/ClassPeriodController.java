package com.bookranker.classperiods.controller;

import com.bookranker.classperiods.dto.ClassPeriodDetailsResponse;
import com.bookranker.classperiods.dto.ClassPeriodSummaryResponse;
import com.bookranker.classperiods.dto.ClassPeriodsResponse;
import com.bookranker.classperiods.dto.CreateClassPeriodRequest;
import com.bookranker.classperiods.dto.CreateClassPeriodResponse;
import com.bookranker.classperiods.dto.UpdateClassPeriodRequest;
import com.bookranker.classperiods.service.ClassPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
@Tag(name = "Class Period APIs", description = "APIs for managing teacher-owned class periods")
@SecurityRequirement(name = "bearerAuth")
public class ClassPeriodController {

  private final ClassPeriodService classPeriodService;

  public ClassPeriodController(ClassPeriodService classPeriodService) {
    this.classPeriodService = classPeriodService;
  }

  @GetMapping
  @Operation(summary = "List class periods for the authenticated teacher")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Class periods returned successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT")
      })
  public ClassPeriodsResponse listClassPeriods(Principal principal) {
    return classPeriodService.listClassPeriods(principal.getName());
  }

  @PostMapping
  @Operation(summary = "Create a class period")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Class period created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT")
      })
  public CreateClassPeriodResponse createClassPeriod(
      @Valid @RequestBody CreateClassPeriodRequest request, Principal principal) {
    return classPeriodService.createClassPeriod(request, principal.getName());
  }

  @GetMapping("/{classId}")
  @Operation(summary = "Get class period details")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Class period details returned successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
        @ApiResponse(responseCode = "404", description = "Class period not found")
      })
  public ClassPeriodDetailsResponse getClassPeriod(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String classId,
      Principal principal) {
    return classPeriodService.getClassPeriod(classId, principal.getName());
  }

  @PatchMapping("/{classId}")
  @Operation(summary = "Update a class period")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Class period updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
        @ApiResponse(responseCode = "403", description = "Class period belongs to another teacher"),
        @ApiResponse(responseCode = "404", description = "Class period not found")
      })
  public ClassPeriodSummaryResponse updateClassPeriod(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String classId,
      @Valid @RequestBody UpdateClassPeriodRequest request,
      Principal principal) {
    return classPeriodService.updateClassPeriod(classId, request, principal.getName());
  }

  @DeleteMapping("/{classId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a class period")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Class period deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
        @ApiResponse(responseCode = "403", description = "Class period belongs to another teacher"),
        @ApiResponse(responseCode = "404", description = "Class period not found")
      })
  public void deleteClassPeriod(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String classId,
      Principal principal) {
    classPeriodService.deleteClassPeriod(classId, principal.getName());
  }

  @DeleteMapping("/{classId}/students")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Clear student data from a class period",
      description =
          "Removes all students, rankings, assignment runs, and assignment results for the class period "
              + "while preserving the class period, join code, and books.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Class student data cleared successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
        @ApiResponse(responseCode = "403", description = "Class period belongs to another teacher"),
        @ApiResponse(responseCode = "404", description = "Class period not found")
      })
  public void clearStudentData(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String classId,
      Principal principal) {
    classPeriodService.clearStudentData(classId, principal.getName());
  }
}
