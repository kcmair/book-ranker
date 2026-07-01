package com.bookranker.assignment.repository;

import com.bookranker.assignment.model.AssignmentRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRunRepository extends JpaRepository<AssignmentRun, String> {

  Optional<AssignmentRun> findFirstByClassPeriodIdOrderByCreatedAtDesc(String classPeriodId);

  List<AssignmentRun> findByClassPeriodIdOrderByCreatedAtDesc(String classPeriodId);
}
