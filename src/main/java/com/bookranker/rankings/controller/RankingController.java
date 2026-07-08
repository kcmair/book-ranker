package com.bookranker.rankings.controller;

import com.bookranker.rankings.dto.SubmitRankingsRequest;
import com.bookranker.rankings.dto.SubmitRankingsResponse;
import com.bookranker.rankings.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/rankings")
@Tag(name = "Student APIs", description = "Public APIs for student class participation")
public class RankingController {

  private final RankingService rankingService;

  public RankingController(RankingService rankingService) {
    this.rankingService = rankingService;
  }

  @PostMapping
  @Operation(summary = "Submit student book rankings")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Rankings submitted successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid ranking request or ranking count below class minimum"),
        @ApiResponse(responseCode = "404", description = "Student not found")
      })
  public SubmitRankingsResponse submitRankings(
      @Parameter(description = "Student ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String studentId,
      @Valid @RequestBody SubmitRankingsRequest request) {
    return rankingService.submitRankings(studentId, request);
  }
}
