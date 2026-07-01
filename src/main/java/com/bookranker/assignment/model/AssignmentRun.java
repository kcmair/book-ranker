package com.bookranker.assignment.model;

import com.bookranker.classperiods.model.ClassPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignment_runs")
@Getter
@Setter
@NoArgsConstructor
public class AssignmentRun {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_id", nullable = false)
  private ClassPeriod classPeriod;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AssignmentRunStatus status = AssignmentRunStatus.PENDING;

  @Column(length = 50)
  private String algorithmVersion;

  @Column(nullable = false)
  private int totalCost;

  @Column(nullable = false)
  private double satisfactionScore;

  @Column(nullable = false)
  private int firstChoiceCount;

  @Column(nullable = false)
  private int topThreeCount;

  @Column(nullable = false)
  private int worseThanThirdCount;

  @Column(nullable = false)
  private int unassignedStudentCount;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();
}
