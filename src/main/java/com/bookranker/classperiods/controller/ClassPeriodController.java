package com.bookranker.classperiods.controller;

import com.bookranker.classperiods.dto.ClassPeriodDetailsResponse;
import com.bookranker.classperiods.dto.CreateClassPeriodRequest;
import com.bookranker.classperiods.dto.CreateClassPeriodResponse;
import com.bookranker.classperiods.service.ClassPeriodService;
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
public class ClassPeriodController {

  private final ClassPeriodService classPeriodService;

  public ClassPeriodController(ClassPeriodService classPeriodService) {
    this.classPeriodService = classPeriodService;
  }

  @PostMapping
  public CreateClassPeriodResponse createClassPeriod(
      @Valid @RequestBody CreateClassPeriodRequest request,
      Principal principal
  ) {
    return classPeriodService.createClassPeriod(request, principal.getName());
  }

  @GetMapping("/{classId}")
  public ClassPeriodDetailsResponse getClassPeriod(
      @PathVariable String classId,
      Principal principal
  ) {
    return classPeriodService.getClassPeriod(classId, principal.getName());
  }
}
