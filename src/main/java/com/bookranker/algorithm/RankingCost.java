package com.bookranker.algorithm;

public final class RankingCost {

  private RankingCost() {
  }

  public static int fromRank(int rank) {
    if (rank < 1) {
      throw new IllegalArgumentException("Rank must be at least 1");
    }
    return rank - 1;
  }
}
