# BookRanker System Architecture

## 1. System Overview

This document describes the target architecture for BookRanker. The implementation may lag behind this document while the project is under active development; use `IMPLEMENTATION_ALIGNMENT.md` to track known alignment work.

BookRanker is a classroom assignment optimization platform that allows teachers to create a list of books, define capacity constraints, and collect ranked preferences from students. The system computes an optimal assignment that maximizes student satisfaction while respecting book capacity limits.

The core problem is modeled as a **capacitated preference optimization problem**, solved using a **Minimum-Cost Maximum-Flow (MCMF)** algorithm.

---

## 2. Key Goals

* Allow teachers to create classes and manage books
* Allow students to join using a class code and submit rankings
* Ensure fair and optimal assignment of books to students
* Respect book capacity constraints
* Provide assignment transparency and satisfaction metrics
* Maintain privacy by using usernames only (no real names)

---

## 3. High-Level Architecture

```
[ React Frontend ]
        |
        v
[ Spring Boot REST API ]
        |
        v
[ Service Layer ]
        |
        v
[ Repository Layer ]
        |
        v
[ PostgreSQL Database ]

        |
        v
[ Assignment Engine (MCMF Algorithm) ]
```

The assignment engine is a standalone Java module that is not dependent on Spring.

---

## 4. Technology Stack

### Backend

* Java 21
* Spring Boot 3.x
* Maven
* Spring Web
* Spring Data JPA
* PostgreSQL
* Lombok
* Bean Validation

### Frontend

* React
* TypeScript
* Vite

### Hosting (Free Tier)

* Frontend: Vercel
* Backend: Render
* Database: Supabase PostgreSQL

---

## 5. Core Domain Model

### Teacher

Represents an authenticated user who owns classes.

* id (UUID)
* email
* password_hash

Relationships:

* One teacher → many classes

---

### ClassPeriod

Represents a classroom grouping for a specific class period.

Implementation note:

* The Java entity should be named `ClassPeriod` to avoid confusion with `java.lang.Class`.
* API routes may still use `/api/classes` as the public resource name.
* The database table may remain `classes` unless migrations choose to rename it later.

* id (UUID)
* name
* join_code
* teacher_id

Relationships:

* One class period → many books
* One class period → many students
* One class period → many assignment runs

---

### Book

Represents an assignable item.

* id (UUID)
* title
* capacity
* class_id

Constraints:

* capacity ≥ 1

---

### Student

Represents a participant in a class.

* id (UUID)
* username
* class_id

Notes:

* No real names are stored
* Username must be unique per class

---

### Ranking

Represents a student's preference ordering.

* id (UUID)
* student_id
* book_id
* rank (1 = highest preference)

Rules:

* Each student must submit at least the class's minimum number of ranked books
* The default minimum is the current number of books unless the teacher sets a lower minimum
* Rank values must be unique per student

---

### AssignmentRun

Represents a single execution of the assignment algorithm.

* id (UUID)
* class_id
* created_at
* status (PENDING, COMPLETE, FAILED)
* algorithm_version

Purpose:

* Preserves historical assignment results
* Enables reruns and comparisons

---

### Assignment

Represents final book allocation for a given run.

* id (UUID)
* assignment_run_id
* student_id
* book_id

---

## 6. Core Algorithm

### Problem Type

This is a **capacitated assignment optimization problem**.

### Approach

We use a **Minimum-Cost Maximum-Flow (MCMF)** algorithm.

### Graph Model

* Source → Students (capacity = 1, cost = 0)
* Students → Books (capacity = 1, cost = ranking score)
* Books → Sink (capacity = book capacity)

### Cost Function

| Rank | Cost |
| ---- | ---- |
| 1    | 0    |
| 2    | 1    |
| 3    | 2    |
| n    | n-1  |

Objective:

* Minimize total cost (maximize satisfaction)

---

## 7. Backend Layer Responsibilities

### Controller Layer

* Handles HTTP requests
* Performs request validation
* Delegates to services
* Returns DTOs only

### Service Layer

* Contains business orchestration logic
* Coordinates repositories
* Calls assignment engine
* Manages transactional boundaries

### Repository Layer

* Pure data access
* No business logic
* Spring Data JPA interfaces

### Algorithm Layer

* Pure Java implementation
* No Spring dependencies
* Stateless computation
* Accepts input graph, returns assignments

---

## 7.1 Backend Package Layout

Use package-by-feature under `com.bookranker`.

Target packages:

```text
com.bookranker.auth
com.bookranker.classperiods
com.bookranker.books
com.bookranker.students
com.bookranker.rankings
com.bookranker.assignment
com.bookranker.algorithm
com.bookranker.shared
```

New implementation work should use these feature packages. Existing generic packages may be migrated gradually as their features are brought into contract alignment.

---

## 8. API Principles

* REST-based design
* JSON request/response
* DTOs used for all external communication
* No entity exposure to frontend
* Consistent error format

DTO naming conventions are defined in `IMPLEMENTATION_ALIGNMENT.md`.

---

## 9. Authentication Model

### Teacher Authentication

* Email + password
* JWT-based authentication
* Teachers own all classes they create

### Student Authentication

* No authentication
* Students join via class code
* Identified only by username within a class

---

## 10. Student Workflow

1. Student receives class join code
2. Enters username
3. Submits at least the class's minimum number of ranked books
4. Can update ranking until class is locked
5. Receives assigned book after algorithm run

---

## 11. Teacher Workflow

1. Create account
2. Create class
3. Add books with capacities
4. Share join code
5. Monitor student participation
6. Run assignment algorithm
7. View results and satisfaction metrics

---

## 12. Assignment Rules

* Students must submit at least the class's minimum number of ranked books before assignment
* Students below the ranking minimum are excluded from run
* Book capacities must not be exceeded
* Each student receives exactly one book (if possible)

---

## 13. Data Privacy Rules

* No real names stored for students
* Only usernames are persisted
* No email or identity data for students
* Teacher data is protected via authentication

---

## 14. Non-Functional Requirements

* System must handle 100–2000 students efficiently
* Assignment algorithm should complete in under 2 seconds for typical class sizes
* API must be stateless
* System must be horizontally scalable (future consideration)

---

## 15. Logging & Observability

* Structured logging (JSON preferred later)
* Log assignment runs with input size and duration
* Log failed assignment attempts

---

## 16. Error Handling

Standard API error format:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Validation Error",
  "message": "Rankings must include at least the required minimum number of books",
  "path": "/api/rankings"
}
```

---

## 17. AI Development Workflow

This project is designed for AI-assisted development using Codex-style agents.

### Workflow Principles

* Architecture is the source of truth

* Agent roles, branch strategy, isolation rules, and merge rules are defined in `AI_WORKFLOW.md`
* The canonical execution model uses five agents: Auth, Domain, Algorithm, Assignment, and Frontend

* No agent may modify multiple domains without explicit instruction

* All changes must conform to this architecture

---

## 18. Definition of Done

A feature is complete only when:

* Code compiles
* Unit tests pass
* API contracts match specification
* No architectural violations exist
* Assignment engine produces correct results
* Manual test flow succeeds end-to-end

---

## 19. Future Extensions (not implemented yet)

* Partial ranking support
* Email notifications
* Real-time updates (WebSockets)
* Class analytics dashboard
* CSV export of results
* Multi-assignment comparison tools

---
