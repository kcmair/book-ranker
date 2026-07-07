package com.bookranker.classperiods.repository;

import com.bookranker.classperiods.model.ClassPeriod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassPeriodRepository extends JpaRepository<ClassPeriod, String> {

  List<ClassPeriod> findByTeacher_EmailOrderByCreatedAtDesc(String teacherEmail);

  List<ClassPeriod> findByTeacherIdOrderByCreatedAtAsc(String teacherId);

  Optional<ClassPeriod> findByJoinCode(String joinCode);

  boolean existsByJoinCode(String joinCode);
}
