package com.bookranker.assignment.repository;

import com.bookranker.assignment.model.Assignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, String> {

  List<Assignment> findByAssignmentRunId(String assignmentRunId);

  List<Assignment> findByAssignmentRunIdIn(List<String> assignmentRunIds);

  Optional<Assignment> findByAssignmentRunIdAndStudentId(String assignmentRunId, String studentId);

  void deleteByAssignmentRunId(String assignmentRunId);

  void deleteByAssignmentRunClassPeriodId(String classPeriodId);
}
