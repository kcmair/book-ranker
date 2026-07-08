package com.bookranker.rankings.model;

import com.bookranker.books.model.Book;
import com.bookranker.students.model.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "rankings",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"student_id", "book_id"}),
      @UniqueConstraint(columnNames = {"student_id", "rank_value"})
    })
@Getter
@Setter
@NoArgsConstructor
public class Ranking {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "rank_value", nullable = false)
  private int rank;

  @Column(nullable = false)
  private Instant submittedAt = Instant.now();
}
