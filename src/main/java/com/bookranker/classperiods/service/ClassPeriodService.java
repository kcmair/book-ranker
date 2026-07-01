package com.bookranker.classperiods.service;

import com.bookranker.auth.model.Teacher;
import com.bookranker.auth.repository.TeacherRepository;
import com.bookranker.books.dto.BookResponse;
import com.bookranker.classperiods.dto.ClassPeriodDetailsResponse;
import com.bookranker.classperiods.dto.CreateClassPeriodRequest;
import com.bookranker.classperiods.dto.CreateClassPeriodResponse;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.repository.ClassPeriodRepository;
import com.bookranker.students.dto.StudentResponse;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClassPeriodService {

  private static final String JOIN_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int JOIN_CODE_LENGTH = 6;
  private final SecureRandom secureRandom = new SecureRandom();

  private final ClassPeriodRepository classPeriodRepository;
  private final TeacherRepository teacherRepository;

  public ClassPeriodService(
      ClassPeriodRepository classPeriodRepository,
      TeacherRepository teacherRepository
  ) {
    this.classPeriodRepository = classPeriodRepository;
    this.teacherRepository = teacherRepository;
  }

  @Transactional
  public CreateClassPeriodResponse createClassPeriod(CreateClassPeriodRequest request, String teacherEmail) {
    Teacher teacher = findTeacherByEmail(teacherEmail);
    ClassPeriod classPeriod = new ClassPeriod();
    classPeriod.setTeacher(teacher);
    classPeriod.setName(request.name());
    classPeriod.setJoinCode(generateUniqueJoinCode());

    ClassPeriod saved = classPeriodRepository.save(classPeriod);
    return new CreateClassPeriodResponse(saved.getId(), saved.getJoinCode());
  }

  @Transactional(readOnly = true)
  public ClassPeriodDetailsResponse getClassPeriod(String classPeriodId, String teacherEmail) {
    ClassPeriod classPeriod = findOwnedClassPeriod(classPeriodId, teacherEmail);

    return new ClassPeriodDetailsResponse(
        classPeriod.getId(),
        classPeriod.getName(),
        classPeriod.getJoinCode(),
        classPeriod.getBooks().stream()
            .map(book -> new BookResponse(book.getId(), book.getTitle(), book.getCapacity()))
            .toList(),
        classPeriod.getStudents().stream()
            .map(student -> new StudentResponse(student.getId(), student.getUsername()))
            .toList()
    );
  }

  @Transactional(readOnly = true)
  public ClassPeriod findOwnedClassPeriod(String classPeriodId, String teacherEmail) {
    ClassPeriod classPeriod = classPeriodRepository.findById(classPeriodId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class period not found"));

    if (!classPeriod.getTeacher().getEmail().equals(teacherEmail)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Class period does not belong to teacher");
    }

    return classPeriod;
  }

  @Transactional(readOnly = true)
  public ClassPeriod findByJoinCode(String joinCode) {
    return classPeriodRepository.findByJoinCode(normalizeJoinCode(joinCode))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class period not found"));
  }

  private Teacher findTeacherByEmail(String teacherEmail) {
    return teacherRepository.findByEmail(teacherEmail)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Teacher not found"));
  }

  private String generateUniqueJoinCode() {
    String joinCode;
    do {
      joinCode = randomJoinCode();
    } while (classPeriodRepository.existsByJoinCode(joinCode));
    return joinCode;
  }

  private String randomJoinCode() {
    StringBuilder builder = new StringBuilder(JOIN_CODE_LENGTH);
    for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
      builder.append(JOIN_CODE_CHARS.charAt(secureRandom.nextInt(JOIN_CODE_CHARS.length())));
    }
    return builder.toString();
  }

  private String normalizeJoinCode(String joinCode) {
    return joinCode.trim().toUpperCase(Locale.ROOT);
  }
}
