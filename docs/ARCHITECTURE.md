# BookRanker System Architecture

## 1. System Overview

This document describes the current architecture for BookRanker as it moves from implementation into deployment.

BookRanker is a classroom assignment platform that allows teachers to create a list of books, define capacity constraints, and collect ranked preferences from students. The system computes a deterministic ranked-choice assignment while respecting book capacity limits.

The core problem is modeled as a **capacitated ranked-choice assignment problem**.

---

## 2. Key Goals

* Allow teachers to create classes and manage books
* Allow students to join using a class-specific poll URL and submit rankings
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
[ Assignment Engine (Ranked-Choice Algorithm) ]
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
* H2 for local development and tests
* Flyway database migrations
* Lombok
* Bean Validation

### Frontend

* React
* TypeScript
* Vite

### Hosting

* Frontend and DNS: Cloudflare Pages
* Backend: Render free Docker web service
* Database: Neon PostgreSQL

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
* minimum_ranking_count
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

This is a **capacitated ranked-choice assignment problem**.

### Approach

We use a deterministic greedy ranked-choice pass. Each student is processed once
in class roster order and assigned to the first ranked book with remaining
capacity. Students without rankings, or whose ranked books are full, remain
unassigned.

### Cost Function

| Rank | Cost |
| ---- | ---- |
| 1    | 0    |
| 2    | 1    |
| 3    | 2    |
| n    | n-1  |

The cost function is used for reporting satisfaction metrics after assignments
and manual reassignments.

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

DTOs follow the package-by-feature conventions used throughout the current backend codebase.

---

## 9. Authentication Model

### Teacher Authentication

* Email + password
* JWT-based authentication
* Teachers own all classes they create

### Student Authentication

* No authentication
* Students join via the public poll URL `/poll/{joinCode}`
* Identified only by username within a class

---

## 10. Student Workflow

1. Student receives or opens the class poll URL
2. Enters username
3. Drags books from the book list into the rankings column
4. Reorders ranked books from highest to lowest
5. Submits at least the class's minimum number of ranked books
6. Can submit revised rankings before the teacher runs the assignment
7. After the teacher runs the assignment, the same poll URL shows the public class assignment spreadsheet instead of the ranking form

---

## 11. Teacher Workflow

1. Create account
2. Create class
3. Add books with capacities
4. Set the minimum rankings per student if the default should be lower than the number of books
5. Share the class poll URL
6. Monitor joined students
7. Run assignment algorithm
8. View results, satisfaction metrics, student ranking columns, and assignment spreadsheets

---

## 12. Assignment Rules

* Students must submit at least the class's minimum number of ranked books before assignment
* Students below the ranking minimum are excluded from run
* Book capacities must not be exceeded
* Each student receives exactly one book (if possible)
* If total eligible students exceed total book capacity, unassigned students are reported in the run metrics and UI
* Assignment result views include each student's submitted ranking order with the assigned book highlighted

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

* Historical agent roles, branch strategy, isolation rules, and merge rules are archived in `docs/archive/AI_WORKFLOW.md`
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

* Email notifications
* Real-time updates (WebSockets)
* Class analytics dashboard
* CSV export of results
* Multi-assignment comparison tools

---
