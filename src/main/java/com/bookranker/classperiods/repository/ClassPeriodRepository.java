package com.bookranker.classperiods.repository;

import com.bookranker.classperiods.model.ClassPeriod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassPeriodRepository extends JpaRepository<ClassPeriod, String> {

  Optional<ClassPeriod> findByJoinCode(String joinCode);

  boolean existsByJoinCode(String joinCode);
}
