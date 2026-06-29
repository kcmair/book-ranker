package com.bookranker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Book {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String title;

  private int capacity;

  @ManyToOne
  private ClassEntity classEntity;
}
