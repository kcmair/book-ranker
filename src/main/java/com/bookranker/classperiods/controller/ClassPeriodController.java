package com.bookranker.classperiods.controller;

import com.bookranker.classperiods.dto.ClassPeriodDetailsResponse;
import com.bookranker.classperiods.dto.CreateClassPeriodRequest;
import com.bookranker.classperiods.dto.CreateClassPeriodResponse;
import com.bookranker.classperiods.service.ClassPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @PostMapping
  @Operation(summary = "Create a class period")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Class period created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT")
  })
  public CreateClassPeriodResponse createClassPeriod(
      @Valid @RequestBody CreateClassPeriodRequest request,
      Principal principal
  ) {
    return classPeriodService.createClassPeriod(request, principal.getName());
  }

  @GetMapping("/{classId}")
  @Operation(summary = "Get class period details")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Class period details returned successfully"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
      @ApiResponse(responseCode = "404", description = "Class period not found")
  })
  public ClassPeriodDetailsResponse getClassPeriod(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable String classId,
      Principal principal
  ) {
    return classPeriodService.getClassPeriod(classId, principal.getName());
  }
}
