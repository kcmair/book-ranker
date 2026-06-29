package com.bookranker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Ranking {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String studentId;

  private String bookId;

  private int rank;
}
