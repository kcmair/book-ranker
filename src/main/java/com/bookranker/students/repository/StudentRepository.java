package com.bookranker.students.repository;

import com.bookranker.students.model.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {

  List<Student> findByClassPeriodId(String classPeriodId);

  Optional<Student> findByClassPeriodIdAndUsername(String classPeriodId, String username);
}
