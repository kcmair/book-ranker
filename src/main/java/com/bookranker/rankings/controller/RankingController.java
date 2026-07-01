package com.bookranker.rankings.controller;

import com.bookranker.rankings.dto.SubmitRankingsRequest;
import com.bookranker.rankings.dto.SubmitRankingsResponse;
import com.bookranker.rankings.service.RankingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/rankings")
public class RankingController {

  private final RankingService rankingService;

  public RankingController(RankingService rankingService) {
    this.rankingService = rankingService;
  }

  @PostMapping
  public SubmitRankingsResponse submitRankings(
      @PathVariable String studentId,
      @Valid @RequestBody SubmitRankingsRequest request
  ) {
    return rankingService.submitRankings(studentId, request);
  }
}
