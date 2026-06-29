package com.bookranker.repository;

import com.bookranker.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, String> {
  List<Student> findByClassEntityId(String classId);
}
