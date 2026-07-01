package com.bookranker.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ClassState(
    List<Student> students,
    List<Book> books,
    Map<Student, Map<Book, Integer>> rankings,
    Map<Book, Integer> capacities
) {

  public ClassState {
    students = List.copyOf(Objects.requireNonNull(students, "students"));
    books = List.copyOf(Objects.requireNonNull(books, "books"));
    rankings = Map.copyOf(Objects.requireNonNull(rankings, "rankings"));
    capacities = Map.copyOf(Objects.requireNonNull(capacities, "capacities"));

    for (Book book : books) {
      int capacity = capacities.getOrDefault(book, 0);
      if (capacity < 0) {
        throw new IllegalArgumentException("Book capacity cannot be negative");
      }
    }
  }

  public record Student(String id) {
    public Student {
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("Student id is required");
      }
    }
  }

  public record Book(String id) {
    public Book {
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("Book id is required");
      }
    }
  }
}
