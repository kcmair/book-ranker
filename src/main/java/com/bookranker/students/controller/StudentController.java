package com.bookranker.students.controller;

import com.bookranker.students.dto.JoinClassPeriodRequest;
import com.bookranker.students.dto.JoinClassPeriodResponse;
import com.bookranker.students.dto.StudentStatusResponse;
import com.bookranker.students.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StudentController {

  private final StudentService studentService;

  public StudentController(StudentService studentService) {
    this.studentService = studentService;
  }

  @PostMapping("/classes/join")
  public JoinClassPeriodResponse joinClassPeriod(@Valid @RequestBody JoinClassPeriodRequest request) {
    return studentService.joinClassPeriod(request);
  }

  @GetMapping("/students/{studentId}/status")
  public StudentStatusResponse getStudentStatus(@PathVariable String studentId) {
    return studentService.getStatus(studentId);
  }
}
