# BookRanker Assignment Algorithm

## 1. Overview

BookRanker uses a **Minimum-Cost Maximum-Flow (MCMF)** algorithm to assign students to books under capacity constraints while maximizing overall student satisfaction.

This is a constrained optimization problem:

* Each eligible student gets exactly one book when total capacity allows
* Each book has a maximum capacity
* Each eligible student provides at least the class's minimum number of ranked books
* Goal: maximize total preference satisfaction

---

## 2. Problem Definition

Given:

* A set of students `S`
* A set of books `B`
* A capacity function `cap(b)` for each book
* A ranking function `rank(s, b)` for ranked student-book pairs
* A class minimum ranking count `minRankings`

Find an assignment function for the maximum feasible set of eligible students:

```
f: S_assigned → B
```

such that:

* Each assigned student is assigned exactly one book

* For each book `b`:

  ```
  |{ s ∈ S : f(s) = b }| ≤ cap(b)
  ```

* Total cost is minimized.

---

## 3. Optimization Objective

We convert rankings into costs:

| Rank | Cost |
| ---- | ---- |
| 1    | 0    |
| 2    | 1    |
| 3    | 2    |
| n    | n-1  |

### Objective:

Minimize:

```
Σ cost(rank(s, f(s)))
```

This is equivalent to maximizing satisfaction.

---

## 4. Graph Model (Flow Network)

We construct a directed graph:

### Nodes:

* Source node `S`
* Student nodes `s ∈ S`
* Book nodes `b ∈ B`
* Sink node `T`

---

### Edges:

#### 1. Source → Students

```
S → s
capacity = 1
cost = 0
```

Each student can only be assigned once.

---

#### 2. Students → Books

```
s → b
capacity = 1
cost = rankCost(s, b)
```

This encodes preferences.

---

#### 3. Books → Sink

```
b → T
capacity = cap(b)
cost = 0
```

This enforces book capacity constraints.

---

## 5. Algorithm Choice

We use:

> **Successive Shortest Path Min-Cost Max-Flow**

### Why:

* Simple to implement in Java
* Efficient for typical classroom sizes
* Deterministic output
* Well-understood for interviews

---

## 6. Expected Complexity

Let:

* `V = number of students + books + 2`
* `E = student-book edges + others`

Complexity:

```
O(F × E log V)
```

Where:

* `F = number of students`

For typical class sizes (≤ 2000 students), this is fast enough in practice.

---

## 7. Input Representation

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

## 8. Output Representation

```java id="out1"
class AssignmentResult {
    Map<Student, Book> assignments;
    int totalCost;
    double satisfactionScore;
}
```

---

## 9. Edge Cases

### 9.1 Not enough capacity

If:

```
Σ cap(book) < number of students
```

Then:

* Some students remain unassigned
* These are explicitly returned as “unassigned”

---

### 9.2 Rankings below the class minimum

Students below the class minimum ranking count:

* Excluded from graph only when their submitted ranking count is below `minRankings`
* Reported in assignment run summary

---

### 9.3 Equal optimal solutions

If multiple optimal solutions exist:

* The algorithm will return the first found path-based solution
* No additional tie-breaking required in v1

---

## 10. Satisfaction Metrics

After solving, we compute:

### Average satisfaction score

```
avg = 1 - (totalCost / maxPossibleCost)
```

### Distribution

* % receiving 1st choice
* % receiving top 3 choices
* % receiving worse than 3rd choice

---

## 11. Implementation Requirements

The algorithm MUST:

* Be implemented in pure Java (no Spring dependencies)
* Be deterministic
* Be unit-testable in isolation
* Accept a fully materialized ClassState object
* Return an AssignmentResult object

---

## 12. Integration Boundary

### Allowed:

* Input: ClassState (from service layer)
* Output: AssignmentResult

### NOT allowed:

* Direct database access
* Spring annotations
* HTTP awareness
* DTO usage

---

## 13. Testing Strategy

Unit tests must cover:

### Core cases:

* Perfect capacity match
* Overcapacity (more students than slots)
* Undercapacity (unused book slots)
* Skewed preferences (everyone wants same book)
* Random large dataset simulation

### Correctness checks:

* No student assigned twice
* No book exceeds capacity
* All assignments respect rankings

---

## 14. Performance Expectations

Target performance:

| Students | Runtime target |
| -------- | -------------- |
| 100      | < 50ms         |
| 500      | < 200ms        |
| 2000     | < 2 seconds    |

---

## 15. Future Improvements (not in v1)

* Partial ranking support (top-k preferences)
* Weighted satisfaction curves (non-linear scoring)
* Fairness constraints (avoid always-last assignment)
* Multi-book assignments per student
* Real-time incremental recomputation

---

## 16. Design Principle

> The algorithm is the single source of truth for assignment fairness.

Everything else in the system exists only to:

* collect inputs
* execute the solver
* display outputs

---
