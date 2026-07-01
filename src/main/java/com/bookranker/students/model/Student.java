package com.bookranker.students.model;

import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.rankings.model.Ranking;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "students",
    uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "username"})
)
@Getter
@Setter
@NoArgsConstructor
public class Student {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_id", nullable = false)
  private ClassPeriod classPeriod;

  @Column(nullable = false, length = 100)
  private String username;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Ranking> rankings = new ArrayList<>();
}
