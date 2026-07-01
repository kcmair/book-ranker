package com.bookranker.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RankingCostTest {

  @Test
  void mapsRankToZeroBasedCost() {
    assertEquals(0, RankingCost.fromRank(1));
    assertEquals(1, RankingCost.fromRank(2));
    assertEquals(4, RankingCost.fromRank(5));
  }

  @Test
  void rejectsInvalidRank() {
    assertThrows(IllegalArgumentException.class, () -> RankingCost.fromRank(0));
  }
}
