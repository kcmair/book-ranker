# BookRanker Implementation Alignment

> Archived: This document is historical code/docs alignment material from the build phase. Remaining deployment-relevant guidance has been folded into the active README, deployment, API, architecture, database, and algorithm docs.

## 1. Purpose

This document tracks how the current codebase should be brought into alignment with the target architecture and contracts.

The docs describe the desired finished system. The current codebase may be incomplete while still being valid work in progress. Use this checklist to distinguish:

* not implemented yet
* implemented but out of contract
* intentionally different from a generic convention

---

## 2. Build System

Maven is the only build system for this project.

Canonical commands:

```bash
mvn test
mvn install
mvn spring-boot:run
```

Do not add Gradle files, Gradle wrapper scripts, or Gradle-specific documentation unless the project explicitly reopens that decision.

---

## 3. Naming Decisions

### ClassPeriod

Use `ClassPeriod` for the Java/domain concept.

Rationale:

* avoids confusion with `java.lang.Class`
* better reflects a teacher's class period or classroom grouping
* keeps the domain language more precise

External names:

* API resource path: `/api/classes`
* database table: `classes`
* Java entity: `ClassPeriod`
* DTO prefix: `ClassPeriod`

---

## 4. Backend Package Layout

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

Within each feature package, use subpackages only when the package grows enough to need them:

```text
controller
service
repository
dto
model
```

Example:

```text
com.bookranker.classperiods.controller
com.bookranker.classperiods.dto
com.bookranker.classperiods.model
com.bookranker.classperiods.repository
com.bookranker.classperiods.service
```

Do not expand the current generic `controller`, `service`, `repository`, and `model` packages for new work. Move existing code gradually when touching that feature.

---

## 5. DTO Naming Conventions

Controllers must not expose JPA entities.

Use request and response DTOs named by action and domain concept:

```text
CreateClassPeriodRequest
ClassPeriodResponse
ClassPeriodDetailsResponse
CreateBookRequest
BookResponse
JoinClassPeriodRequest
JoinClassPeriodResponse
SubmitRankingsRequest
StudentStatusResponse
RunAssignmentResponse
AssignmentResultsResponse
AssignmentHistoryResponse
```

Guidelines:

* Request DTOs end in `Request`
* Response DTOs end in `Response`
* List responses should name the aggregate, such as `BooksResponse`
* DTO field names use API-facing camelCase, such as `joinCode`
* Persistence names use database snake_case, such as `join_code`

---

## 6. Persistence Mapping Conventions

Java entities use camelCase fields and JPA mappings for SQL names.

Examples:

| Java | Database |
| --- | --- |
| `ClassPeriod` | `classes` |
| `joinCode` | `join_code` |
| `teacher` or `teacherId` | `teacher_id` |
| `classPeriod` | `class_id` |
| `submittedAt` | `submitted_at` |
| `createdAt` | `created_at` |

Prefer relationships over raw foreign-key strings when the entity relationship is part of the domain model.

Examples:

* `Ranking` should reference `Student` and `Book`, not only string IDs.
* `Book` should reference `ClassPeriod`.
* `Student` should reference `ClassPeriod`.

---

## 7. Alignment Status

### Completed Alignment

The current codebase has been brought into alignment on these items:

* Class period endpoints use `/api/classes`
* Controllers use request and response DTOs rather than exposing JPA entities
* Teacher registration and login are implemented
* Password hashing is implemented
* JWT creation and validation are implemented
* Teacher-owned management endpoints are protected
* Class periods, books, students, rankings, assignment runs, and assignments are represented in the domain model
* Assignment orchestration persists historical runs and assignment results
* Springdoc/OpenAPI is installed and secured endpoints expose bearer-auth metadata
* The frontend uses a single API client layer for backend communication
* The student poll flow uses `/poll/{joinCode}` and drag-and-drop rankings
* Public class assignment grids and authenticated teacher assignment grids are implemented

### Remaining/Future Alignment Work

* Apply the standard project-wide error envelope if the project decides to replace Spring's default error responses
* Add timestamps and active flags everywhere they are documented but not yet exposed by DTOs
* Add production-grade schema migrations before deployment
* Harden production defaults for datasource, SQL logging, H2 console access, and frame options

---

## 8. Swagger Status

Swagger/OpenAPI is part of the current backend implementation.

Backend API changes should keep controller and DTO schema annotations aligned with `API.md`. Verify Swagger behavior against:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

Secured endpoints should require the `bearerAuth` scheme in the generated OpenAPI spec.

---

## 9. Local Runtime Database

Local Maven runs use the `local` Spring profile and H2 by default.

This supports fast local development without requiring PostgreSQL.

Production and deployment environments use PostgreSQL through externalized datasource settings.
