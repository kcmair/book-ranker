package com.bookranker.classperiods.service;

import com.bookranker.classperiods.model.ClassPeriod;
import org.springframework.stereotype.Component;

@Component
public class ClassPeriodPolicy {

  public int effectiveMinimumRankingCount(ClassPeriod classPeriod) {
    return effectiveMinimumRankingCount(classPeriod, classPeriod.getBooks().size());
  }

  public int effectiveMinimumRankingCount(ClassPeriod classPeriod, int bookCount) {
    if (classPeriod.getMinimumRankingCount() == null) {
      return bookCount;
    }
    return Math.min(classPeriod.getMinimumRankingCount(), bookCount);
  }
}
