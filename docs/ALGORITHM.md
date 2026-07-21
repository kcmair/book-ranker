# BookRanker Assignment Algorithm

## 1. Overview

BookRanker assigns students to books with a deterministic ranked-choice pass.
Each student is considered once. If the student has submitted any rankings, the
solver assigns that student to the first ranked book in their list that still has
available capacity.

The algorithm intentionally does not assign a student to an unranked fallback
book. Students with no rankings, or whose ranked books are already full, remain
unassigned.

---

## 2. Problem Definition

Given:

* A set of students `S`
* A set of books `B`
* A capacity function `cap(b)` for each book
* A ranking function `rank(s, b)` for ranked student-book pairs

Find an assignment function:

```text id="alg1"
f: S_assigned -> B
```

such that:

* Each assigned student is assigned exactly one book
* Each assigned book was ranked by that student
* For each book `b`:

```text id="alg2"
|{ s in S : f(s) = b }| <= cap(b)
```

---

## 3. Assignment Rule

For each student, in class roster order:

1. Read the student's submitted rankings.
2. Sort ranked books from highest preference to lowest.
3. Assign the first ranked book with remaining capacity.
4. If no ranked book has remaining capacity, leave the student unassigned.

This means any student who has made ranking choices can be assigned with a
partial ranking list. The class minimum ranking count remains useful as UI and
validation guidance, but the assignment solver does not require a complete
ranking list.

---

## 4. Cost and Metrics

Rankings are converted into costs for reporting:

| Rank | Cost |
| ---- | ---- |
| 1    | 0    |
| 2    | 1    |
| 3    | 2    |
| n    | n-1  |

The assignment run reports:

* Total cost
* Satisfaction score
* First-choice count
* Top-three count
* Worse-than-third count
* Unassigned student count

Manual teacher reassignments recalculate these metrics for the latest completed
assignment run. Manual reassignments may intentionally exceed book capacity
after teacher confirmation; the automatic solver still respects capacity.

---

## 5. Expected Complexity

Let:

* `S = number of students`
* `R = total submitted ranking rows`

The solver sorts each student's rankings and scans them once:

```text id="alg3"
O(R log R_student)
```

For normal classroom sizes this is effectively instantaneous.

---

## 6. Input Representation

Before running the algorithm, the service constructs:

```java id="in1"
class ClassState {
    List<Student> students;
    List<Book> books;
    Map<Student, Map<Book, Integer>> rankings;
    Map<Book, Integer> capacities;
}
```

---

## 7. Output Representation

```java id="out1"
class AssignmentResult {
    Map<Student, Book> assignments;
    Set<Student> unassignedStudents;
    int totalCost;
    double satisfactionScore;
}
```

---

## 8. Edge Cases

### 8.1 Not enough capacity

If total ranked demand exceeds available book capacity:

* Some students remain unassigned
* These are explicitly returned as unassigned

### 8.2 No submitted rankings

Students with no submitted rankings:

* Are not assigned
* Are counted as unassigned

### 8.3 Equal choices

The solver is deterministic because it processes students in the supplied class
state order and books in submitted ranking order.

---

## 9. Implementation Requirements

The algorithm MUST:

* Be implemented in pure Java with no Spring dependencies
* Be deterministic
* Be unit-testable in isolation
* Accept a fully materialized `ClassState` object
* Return an `AssignmentResult` object

---

## 10. Integration Boundary

### Allowed

* Input: `ClassState` from the service layer
* Output: `AssignmentResult`

### Not allowed

* Direct database access
* Spring annotations
* HTTP awareness
