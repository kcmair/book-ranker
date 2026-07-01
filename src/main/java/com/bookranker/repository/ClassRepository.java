package com.bookranker.repository;

import com.bookranker.model.ClassPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<ClassPeriod, String> {
}
