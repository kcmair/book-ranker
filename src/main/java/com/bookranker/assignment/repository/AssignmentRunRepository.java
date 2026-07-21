package com.bookranker.assignment.repository;

import com.bookranker.assignment.model.AssignmentRun;
import com.bookranker.assignment.model.AssignmentRunStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRunRepository extends JpaRepository<AssignmentRun, String> {

  Optional<AssignmentRun> findFirstByClassPeriodIdAndStatusOrderByCreatedAtDesc(
      String classPeriodId, AssignmentRunStatus status);

  Optional<AssignmentRun> findByIdAndClassPeriodId(String id, String classPeriodId);

  List<AssignmentRun> findByClassPeriodIdOrderByCreatedAtDesc(String classPeriodId);

  void deleteByClassPeriodId(String classPeriodId);
}
