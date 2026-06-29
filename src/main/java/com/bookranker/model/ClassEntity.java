package com.bookranker.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ClassEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String name;

  private String joinCode;

  @OneToMany(mappedBy = "classEntity")
  private List<Book> books;

  @OneToMany(mappedBy = "classEntity")
  private List<Student> students;
}
