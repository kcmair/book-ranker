package com.bookranker.controller;

import com.bookranker.model.ClassPeriod;
import com.bookranker.service.ClassService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/class")
public class ClassController {

  private final ClassService classService;

  public ClassController(ClassService classService) {
    this.classService = classService;
  }

  @PostMapping
  public ClassPeriod createClass(@RequestParam String name) {
    return classService.createClass(name);
  }

  @GetMapping("/{id}")
  public ClassPeriod getClass(@PathVariable String id) {
    return classService.getClass(id);
  }
}
