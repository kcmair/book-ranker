package com.bookranker.classperiods.service;

import com.bookranker.auth.model.Teacher;
import com.bookranker.auth.repository.TeacherRepository;
import com.bookranker.classperiods.dto.ClassPeriodDetailsResponse;
import com.bookranker.classperiods.dto.ClassPeriodSummaryResponse;
import com.bookranker.classperiods.dto.ClassPeriodsResponse;
import com.bookranker.classperiods.dto.CreateClassPeriodRequest;
import com.bookranker.classperiods.dto.CreateClassPeriodResponse;
import com.bookranker.classperiods.dto.UpdateClassPeriodRequest;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.repository.ClassPeriodRepository;
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
  private final ClassPeriodCleanupService classPeriodCleanupService;
  private final ClassPeriodPolicy classPeriodPolicy;
  private final ClassPeriodResponseMapper classPeriodResponseMapper;
  private final TeacherRepository teacherRepository;

  public ClassPeriodService(
      ClassPeriodRepository classPeriodRepository,
      ClassPeriodCleanupService classPeriodCleanupService,
      ClassPeriodPolicy classPeriodPolicy,
      ClassPeriodResponseMapper classPeriodResponseMapper,
      TeacherRepository teacherRepository) {
    this.classPeriodRepository = classPeriodRepository;
    this.classPeriodCleanupService = classPeriodCleanupService;
    this.classPeriodPolicy = classPeriodPolicy;
    this.classPeriodResponseMapper = classPeriodResponseMapper;
    this.teacherRepository = teacherRepository;
  }

  @Transactional
  public CreateClassPeriodResponse createClassPeriod(
      CreateClassPeriodRequest request, String teacherEmail) {
    Teacher teacher = findTeacherByEmail(teacherEmail);
    ClassPeriod classPeriod = new ClassPeriod();
    classPeriod.setTeacher(teacher);
    classPeriod.setName(request.name());
    classPeriod.setJoinCode(generateUniqueJoinCode());

    ClassPeriod saved = classPeriodRepository.save(classPeriod);
    return new CreateClassPeriodResponse(saved.getId(), saved.getJoinCode());
  }

  @Transactional(readOnly = true)
  public ClassPeriodsResponse listClassPeriods(String teacherEmail) {
    Teacher teacher = findTeacherByEmail(teacherEmail);
    return new ClassPeriodsResponse(
        classPeriodRepository.findByTeacher_EmailOrderByCreatedAtDesc(teacher.getEmail()).stream()
            .map(classPeriodResponseMapper::toSummaryResponse)
            .toList());
  }

  @Transactional(readOnly = true)
  public ClassPeriodDetailsResponse getClassPeriod(String classPeriodId, String teacherEmail) {
    ClassPeriod classPeriod = findOwnedClassPeriod(classPeriodId, teacherEmail);
    return classPeriodResponseMapper.toDetailsResponse(classPeriod);
  }

  @Transactional
  public ClassPeriodSummaryResponse updateClassPeriod(
      String classPeriodId, UpdateClassPeriodRequest request, String teacherEmail) {
    ClassPeriod classPeriod = findOwnedClassPeriod(classPeriodId, teacherEmail);
    classPeriod.setName(request.name());

    if (request.minimumRankingCount() != null) {
      int bookCount = classPeriod.getBooks().size();
      if (request.minimumRankingCount() > bookCount) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Minimum ranking count cannot exceed number of books in the class");
      }
      classPeriod.setMinimumRankingCount(request.minimumRankingCount());
    }

    return classPeriodResponseMapper.toSummaryResponse(classPeriod);
  }

  @Transactional
  public void deleteClassPeriod(String classPeriodId, String teacherEmail) {
    ClassPeriod classPeriod = findOwnedClassPeriod(classPeriodId, teacherEmail);
    classPeriodCleanupService.clearAssignmentData(classPeriod.getId());
    classPeriodRepository.delete(classPeriod);
  }

  @Transactional
  public void clearStudentData(String classPeriodId, String teacherEmail) {
    ClassPeriod classPeriod = findOwnedClassPeriod(classPeriodId, teacherEmail);
    classPeriodCleanupService.clearStudentData(classPeriod.getId());
  }

  @Transactional(readOnly = true)
  public ClassPeriod findOwnedClassPeriod(String classPeriodId, String teacherEmail) {
    ClassPeriod classPeriod =
        classPeriodRepository
            .findById(classPeriodId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class period not found"));

    if (!classPeriod.getTeacher().getEmail().equals(teacherEmail)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Class period does not belong to teacher");
    }

    return classPeriod;
  }

  @Transactional(readOnly = true)
  public ClassPeriod findByJoinCode(String joinCode) {
    return classPeriodRepository
        .findByJoinCode(normalizeJoinCode(joinCode))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class period not found"));
  }

  public int effectiveMinimumRankingCount(ClassPeriod classPeriod) {
    return classPeriodPolicy.effectiveMinimumRankingCount(classPeriod);
  }

  public int effectiveMinimumRankingCount(ClassPeriod classPeriod, int bookCount) {
    return classPeriodPolicy.effectiveMinimumRankingCount(classPeriod, bookCount);
  }

  private Teacher findTeacherByEmail(String teacherEmail) {
    return teacherRepository
        .findByEmail(teacherEmail)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Teacher not found"));
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
