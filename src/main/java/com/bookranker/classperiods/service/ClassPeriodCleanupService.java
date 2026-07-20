package com.bookranker.classperiods.service;

import com.bookranker.assignment.repository.AssignmentRepository;
import com.bookranker.assignment.repository.AssignmentRunRepository;
import com.bookranker.rankings.repository.RankingRepository;
import com.bookranker.students.repository.StudentRepository;
import org.springframework.stereotype.Service;

@Service
public class ClassPeriodCleanupService {

  private final AssignmentRepository assignmentRepository;
  private final AssignmentRunRepository assignmentRunRepository;
  private final RankingRepository rankingRepository;
  private final StudentRepository studentRepository;

  public ClassPeriodCleanupService(
      AssignmentRepository assignmentRepository,
      AssignmentRunRepository assignmentRunRepository,
      RankingRepository rankingRepository,
      StudentRepository studentRepository) {
    this.assignmentRepository = assignmentRepository;
    this.assignmentRunRepository = assignmentRunRepository;
    this.rankingRepository = rankingRepository;
    this.studentRepository = studentRepository;
  }

  public void clearAssignmentData(String classPeriodId) {
    assignmentRepository.deleteByAssignmentRunClassPeriodId(classPeriodId);
    assignmentRunRepository.deleteByClassPeriodId(classPeriodId);
    rankingRepository.deleteByStudentClassPeriodId(classPeriodId);
  }

  public void clearStudentData(String classPeriodId) {
    clearAssignmentData(classPeriodId);
    studentRepository.deleteByClassPeriodId(classPeriodId);
  }
}
