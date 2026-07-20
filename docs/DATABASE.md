# BookRanker Database Design

## 1. Overview

This document defines the target PostgreSQL schema. The current implementation may be incomplete while the project is under active development; use `IMPLEMENTATION_ALIGNMENT.md` for known alignment work.

This database stores teachers, classes, books, students, rankings, assignment runs, and final assignments.

It is designed for PostgreSQL and optimized for:

* Relational integrity
* Fast lookup by class
* Efficient assignment computation input aggregation

---

## 2. Core Design Principles

* All primary keys are UUIDs
* No student personally identifiable information (username only)
* All relationships enforced via foreign keys
* Ranking data is immutable per submission (latest submission wins)
* Assignment runs are historical and never overwritten
* Denormalization is avoided except where needed for performance

---

## 3. Entity Relationship Summary

```text id="er1"
Teacher → Class → Book
               → Student → Ranking
               → AssignmentRun → Assignment
```

---

## 4. Tables

---

## 4.1 teachers

Stores authenticated teachers.

```sql id="t1"
CREATE TABLE teachers (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 4.2 classes

Represents a class period created by a teacher.

Implementation note:

* The Java entity is `ClassPeriod`.
* The table name remains `classes` for concise SQL and existing API alignment.
* Java fields use camelCase and map to snake_case columns where needed.

```sql id="t2"
CREATE TABLE classes (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    join_code VARCHAR(10) UNIQUE NOT NULL,
    minimum_ranking_count INT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

Indexes:

* teacher_id
* join_code (lookup for student join)

---

## 4.3 books

Books available for assignment within a class.

```sql id="t3"
CREATE TABLE books (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    created_at TIMESTAMP DEFAULT NOW()
);
```

Indexes:

* class_id

---

## 4.4 students

Represents a participant in a class (username only).

```sql id="t4"
CREATE TABLE students (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    username VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(class_id, username)
);
```

Indexes:

* class_id
* (class_id, username)

---

## 4.5 rankings

Stores student preferences.

Each row represents a ranked book.

```sql id="t5"
CREATE TABLE rankings (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    rank INT NOT NULL,
    submitted_at TIMESTAMP DEFAULT NOW()
);
```

Constraints:

* A student must rank at least the class's minimum number of books
* Rank values must be unique per student

Recommended index:

```sql id="t6"
CREATE INDEX idx_rankings_student ON rankings(student_id);
```

---

## 4.6 assignment_runs

Represents execution of the assignment algorithm.

```sql id="t7"
CREATE TABLE assignment_runs (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL, -- PENDING, COMPLETE, FAILED
    algorithm_version VARCHAR(50),
    total_cost INTEGER NOT NULL DEFAULT 0,
    satisfaction_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    first_choice_count INTEGER NOT NULL DEFAULT 0,
    top_three_count INTEGER NOT NULL DEFAULT 0,
    worse_than_third_count INTEGER NOT NULL DEFAULT 0,
    unassigned_student_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
```

Indexes:

* class_id

---

## 4.7 assignments

Final output of the algorithm.

```sql id="t8"
CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    assignment_run_id UUID NOT NULL REFERENCES assignment_runs(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(assignment_run_id, student_id)
);
```

---

## 5. Data Integrity Rules

### Student constraints

* Username must be unique per class
* Students cannot exist outside a class

---

### Ranking constraints

* Each student must submit exactly one ranking per book in the class
* Rank values must be continuous (1..N)
* No duplicate ranks per student

(Enforced at service layer; not fully enforceable in SQL without triggers)

---

### Assignment constraints

* Each student appears once per assignment run
* Book capacity must never be exceeded
* Assignments must belong to a valid assignment run

---

## 6. Query Patterns Optimized For

### Common queries:

* Get all books in a class
* Get all students in a class
* Get rankings for a student
* Load full class state for assignment engine
* Retrieve assignment results per run

---

## 7. Performance Considerations

* Index all foreign keys
* Keep rankings table append-heavy
* Assignment runs are read-heavy after creation
* Avoid joins in hot paths of algorithm execution (preload into memory)

---

## 8. Algorithm Input Shape

Before running the assignment engine, the service will construct:

```text id="algo1"
ClassState {
    students[]
    books[]
    rankings[student][book] -> rank
    capacities[book]
}
```

This is loaded once per assignment run and passed into the MCMF solver.

---

## 9. Migration Strategy (future)

Initial development:

* Local H2 uses Hibernate `ddl-auto=create-drop`
* The default PostgreSQL configuration uses `ddl-auto=validate`

Production:

* Add Flyway migrations before deploying schema changes

---

## 10. Future Enhancements (not implemented yet)

* Soft delete for classes
* Audit log table for assignment runs
* Materialized views for analytics
* Partitioning rankings table for very large classes
* Caching class state for repeated runs

---
