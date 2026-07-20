package com.bookranker.assignment.service;

import java.util.Locale;

final class AssignmentGridSupport {

  private AssignmentGridSupport() {}

  static String normalizedBookTitle(String title) {
    return title.trim().toLowerCase(Locale.ROOT);
  }
}
