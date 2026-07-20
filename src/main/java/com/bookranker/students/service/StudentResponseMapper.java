package com.bookranker.students.service;

import com.bookranker.students.dto.StudentResponse;
import com.bookranker.students.model.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentResponseMapper {

  public StudentResponse toResponse(Student student) {
    return new StudentResponse(student.getId(), student.getUsername());
  }
}
