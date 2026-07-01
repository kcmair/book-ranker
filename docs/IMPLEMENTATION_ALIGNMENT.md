# BookRanker Implementation Alignment

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

## 7. Known Alignment Work

### API Surface

* Change class period endpoints from `/api/class` to `/api/classes`
* Replace request params with JSON request DTOs
* Return response DTOs instead of JPA entities
* Apply the standard error response shape

### Domain Model

* Use `username` for students instead of `name`
* Add teacher ownership to class periods
* Add timestamps and active flags where documented
* Map ranking relationships to `Student` and `Book`

### Auth

* Add teacher registration and login
* Add password hashing
* Add JWT creation and validation
* Protect teacher and management endpoints

### Assignment

* Add assignment run and assignment entities
* Add algorithm input/output models
* Add assignment orchestration service
* Persist historical assignment results

### API Documentation

* Add Springdoc/OpenAPI after the first stable DTO/controller pattern is implemented
* Annotate backend controllers once endpoint shapes are stable

---

## 8. Swagger Timing

Swagger/OpenAPI is required for the target system, but should be added after the first stable DTO/controller pattern exists.

Do not add Swagger annotations to prototype endpoints that are about to be reshaped. First align one representative endpoint with:

* JSON request DTO
* response DTO
* validation annotations
* standard error handling

Then add Springdoc and use that endpoint as the pattern for the rest of the backend.

---

## 9. Local Runtime Database

Local Maven runs use the `local` Spring profile and H2 by default.

This supports fast local development without requiring PostgreSQL.

Production and deployment environments use PostgreSQL through externalized datasource settings.

