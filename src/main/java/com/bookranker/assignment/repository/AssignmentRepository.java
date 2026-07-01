package com.bookranker.assignment.repository;

import com.bookranker.assignment.model.Assignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, String> {

  List<Assignment> findByAssignmentRunId(String assignmentRunId);
}
