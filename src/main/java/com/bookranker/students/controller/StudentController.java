package com.bookranker.students.controller;

import com.bookranker.students.dto.JoinClassPeriodRequest;
import com.bookranker.students.dto.JoinClassPeriodResponse;
import com.bookranker.students.dto.StudentStatusResponse;
import com.bookranker.students.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Student APIs", description = "Public APIs for student class participation")
public class StudentController {

  private final StudentService studentService;

  public StudentController(StudentService studentService) {
    this.studentService = studentService;
  }

  @PostMapping("/classes/join")
  @Operation(summary = "Join a class period")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Student joined class period successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "Class period not found")
  })
  public JoinClassPeriodResponse joinClassPeriod(@Valid @RequestBody JoinClassPeriodRequest request) {
    return studentService.joinClassPeriod(request);
  }

  @GetMapping("/students/{studentId}/status")
  @Operation(summary = "Get student ranking status")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Student status returned successfully"),
      @ApiResponse(responseCode = "404", description = "Student not found")
  })
  public StudentStatusResponse getStudentStatus(
      @Parameter(description = "Student ID", example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable String studentId
  ) {
    return studentService.getStatus(studentId);
  }
}
