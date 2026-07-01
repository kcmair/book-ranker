package com.bookranker.students.service;

import com.bookranker.books.repository.BookRepository;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.service.ClassPeriodService;
import com.bookranker.rankings.repository.RankingRepository;
import com.bookranker.students.dto.JoinClassPeriodRequest;
import com.bookranker.students.dto.JoinClassPeriodResponse;
import com.bookranker.students.dto.StudentStatusResponse;
import com.bookranker.students.model.Student;
import com.bookranker.students.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentService {

  private final StudentRepository studentRepository;
  private final ClassPeriodService classPeriodService;
  private final BookRepository bookRepository;
  private final RankingRepository rankingRepository;

  public StudentService(
      StudentRepository studentRepository,
      ClassPeriodService classPeriodService,
      BookRepository bookRepository,
      RankingRepository rankingRepository
  ) {
    this.studentRepository = studentRepository;
    this.classPeriodService = classPeriodService;
    this.bookRepository = bookRepository;
    this.rankingRepository = rankingRepository;
  }

  @Transactional
  public JoinClassPeriodResponse joinClassPeriod(JoinClassPeriodRequest request) {
    ClassPeriod classPeriod = classPeriodService.findByJoinCode(request.joinCode());
    String username = request.username().trim();

    Student student = studentRepository.findByClassPeriodIdAndUsername(classPeriod.getId(), username)
        .orElseGet(() -> {
          Student newStudent = new Student();
          newStudent.setClassPeriod(classPeriod);
          newStudent.setUsername(username);
          return studentRepository.save(newStudent);
        });

    return new JoinClassPeriodResponse(student.getId(), classPeriod.getId());
  }

  @Transactional(readOnly = true)
  public StudentStatusResponse getStatus(String studentId) {
    Student student = findStudent(studentId);
    long totalBooks = bookRepository.countByClassPeriodId(student.getClassPeriod().getId());
    long rankCount = rankingRepository.countByStudentId(studentId);

    return new StudentStatusResponse(totalBooks > 0 && rankCount == totalBooks, rankCount, totalBooks);
  }

  @Transactional(readOnly = true)
  public Student findStudent(String studentId) {
    return studentRepository.findById(studentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
  }
}
