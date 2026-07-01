package com.bookranker.classperiods.model;

import com.bookranker.auth.model.Teacher;
import com.bookranker.books.model.Book;
import com.bookranker.students.model.Student;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
public class ClassPeriod {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "teacher_id", nullable = false)
  private Teacher teacher;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true, length = 10)
  private String joinCode;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "classPeriod", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Book> books = new ArrayList<>();

  @OneToMany(mappedBy = "classPeriod", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Student> students = new ArrayList<>();
}
