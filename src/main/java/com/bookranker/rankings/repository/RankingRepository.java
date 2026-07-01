package com.bookranker.rankings.repository;

import com.bookranker.rankings.model.Ranking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingRepository extends JpaRepository<Ranking, String> {

  List<Ranking> findByStudentId(String studentId);

  long countByStudentId(String studentId);

  void deleteByStudentId(String studentId);
}
