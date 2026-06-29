package com.bookranker.repository;

import com.bookranker.model.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankingRepository extends JpaRepository<Ranking, String> {
  List<Ranking> findByStudentId(String studentId);
}
