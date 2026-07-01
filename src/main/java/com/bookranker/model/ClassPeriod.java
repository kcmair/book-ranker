package com.bookranker.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "classes")
public class ClassPeriod {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String name;

  private String joinCode;

  @OneToMany(mappedBy = "classPeriod")
  private List<Book> books;

  @OneToMany(mappedBy = "classPeriod")
  private List<Student> students;
}
