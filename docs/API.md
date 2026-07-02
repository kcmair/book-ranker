# BookRanker API Design

## 1. Overview

This document defines the target REST API for BookRanker.
All endpoints follow REST conventions and return JSON.

The current implementation may not expose every endpoint yet. Known alignment work is tracked in `IMPLEMENTATION_ALIGNMENT.md`.

Base URL:

```
/api
```

---

## 2. Authentication Model

### Teacher

* Uses JWT authentication
* Required for all `/api/teachers/**` and `/api/classes/**` management endpoints

### Student

* No authentication
* Identified via `class_code + username`

---

## 3. Standard Response Format

### Success

```json id="r1"
{
  "data": {},
  "timestamp": "2026-01-01T00:00:00Z"
}
```

### Error

```json id="r2"
{
  "status": 400,
  "error": "Validation Error",
  "message": "Rankings must include all books in the class",
  "path": "/api/rankings"
}
```

---

## 4. Teacher APIs

---

### 4.1 Create Teacher Account

```
POST /api/teachers/register
```

Request:

```json id="t1"
{
  "email": "teacher@example.com",
  "password": "password123"
}
```

Response:

```json id="t2"
{
  "teacherId": "uuid"
}
```

---

### 4.2 Login

```
POST /api/teachers/login
```

Request:

```json id="t3"
{
  "email": "teacher@example.com",
  "password": "password123"
}
```

Response:

```json id="t4"
{
  "token": "jwt-token"
}
```

---

## 5. Class Period APIs

The public API uses `/api/classes` as the concise resource path, while the backend domain entity is `ClassPeriod`.

---

### 5.1 Create Class Period

```
POST /api/classes
```

Headers:

```
Authorization: Bearer <token>
```

Request:

```json id="c1"
{
  "name": "English 12"
}
```

Response:

```json id="c2"
{
  "classId": "uuid",
  "joinCode": "K9X42M"
}
```

---

### 5.2 List Class Periods

```
GET /api/classes
```

Headers:

```
Authorization: Bearer <token>
```

Response:

```json id="c3"
{
  "classes": [
    {
      "id": "uuid",
      "name": "English 12",
      "joinCode": "K9X42M"
    }
  ]
}
```

---

### 5.3 Get Class Period Details

```
GET /api/classes/{classId}
```

Response:

```json id="c4"
{
  "id": "uuid",
  "name": "English 12",
  "joinCode": "K9X42M",
  "books": [],
  "students": []
}
```

---

### 5.4 Update Class Period

```
PATCH /api/classes/{classId}
```

Headers:

```
Authorization: Bearer <token>
```

Request:

```json id="c5"
{
  "name": "English 12 Honors"
}
```

Response:

```json id="c6"
{
  "id": "uuid",
  "name": "English 12 Honors",
  "joinCode": "K9X42M"
}
```

---

### 5.5 Delete Class Period

```
DELETE /api/classes/{classId}
```

Headers:

```
Authorization: Bearer <token>
```

Response:

```text id="c7"
204 No Content
```

---

## 6. Book APIs

---

### 6.1 Add Book to Class Period

```
POST /api/classes/{classId}/books
```

Request:

```json id="b1"
{
  "title": "To Kill a Mockingbird",
  "capacity": 10
}
```

Response:

```json id="b2"
{
  "bookId": "uuid"
}
```

---

### 6.2 Get Books in Class Period

```
GET /api/classes/{classId}/books
```

Response:

```json id="b3"
{
  "books": [
    {
      "id": "uuid",
      "title": "Book Title",
      "capacity": 10
    }
  ]
}
```

---

### 6.3 Update Book

```
PATCH /api/classes/{classId}/books/{bookId}
```

Headers:

```
Authorization: Bearer <token>
```

Request:

```json id="b4"
{
  "title": "To Kill a Mockingbird",
  "capacity": 12
}
```

Response:

```json id="b5"
{
  "id": "uuid",
  "title": "To Kill a Mockingbird",
  "capacity": 12
}
```

---

### 6.4 Delete Book

```
DELETE /api/classes/{classId}/books/{bookId}
```

Headers:

```
Authorization: Bearer <token>
```

Response:

```text id="b6"
204 No Content
```

Deleting a book also removes rankings for that book.

---

## 7. Student APIs

---

### 7.1 Join Class Period

```
POST /api/classes/join
```

Request:

```json id="s1"
{
  "joinCode": "K9X42M",
  "username": "student123"
}
```

Response:

```json id="s2"
{
  "studentId": "uuid",
  "classId": "uuid"
}
```

---

### 7.2 Submit Rankings

```
POST /api/students/{studentId}/rankings
```

Request:

```json id="s3"
{
  "rankings": [
    {
      "bookId": "uuid",
      "rank": 1
    },
    {
      "bookId": "uuid",
      "rank": 2
    }
  ]
}
```

Rules:

* Must include ALL books in class
* Ranks must be unique and contiguous

Response:

```json id="s4"
{
  "status": "submitted"
}
```

---

### 7.3 Get Student Status

```
GET /api/students/{studentId}/status
```

Response:

```json id="s5"
{
  "submitted": true,
  "rankCount": 5,
  "totalBooks": 5
}
```

---

### 7.4 Update Student

```
PATCH /api/classes/{classId}/students/{studentId}
```

Headers:

```
Authorization: Bearer <token>
```

Request:

```json id="s6"
{
  "username": "student456"
}
```

Response:

```json id="s7"
{
  "id": "uuid",
  "username": "student456"
}
```

---

### 7.5 Delete Student

```
DELETE /api/classes/{classId}/students/{studentId}
```

Headers:

```
Authorization: Bearer <token>
```

Response:

```text id="s8"
204 No Content
```

Deleting a student also removes that student's rankings.

---

## 8. Assignment APIs

---

### 8.1 Run Assignment

```
POST /api/classes/{classId}/assign
```

Description:
Runs the MCMF algorithm.

Response:

```json id="a1"
{
  "assignmentRunId": "uuid",
  "status": "COMPLETE",
  "totalCost": 2,
  "satisfactionScore": 0.92,
  "firstChoiceCount": 18,
  "topThreeCount": 24,
  "worseThanThirdCount": 1,
  "unassignedStudentCount": 0
}
```

---

### 8.2 Get Assignment Results

```
GET /api/classes/{classId}/assignments/latest
```

Response:

```json id="a2"
{
  "runId": "uuid",
  "totalCost": 2,
  "satisfactionScore": 0.92,
  "firstChoiceCount": 18,
  "topThreeCount": 24,
  "worseThanThirdCount": 1,
  "unassignedStudentCount": 0,
  "results": [
    {
      "studentId": "uuid",
      "bookId": "uuid"
    }
  ]
}
```

---

### 8.3 Get Assignment History

```
GET /api/classes/{classId}/assignments
```

Response:

```json id="a3"
{
  "runs": [
    {
      "runId": "uuid",
      "createdAt": "timestamp",
      "status": "COMPLETE",
      "totalCost": 2,
      "satisfactionScore": 0.92,
      "firstChoiceCount": 18,
      "topThreeCount": 24,
      "worseThanThirdCount": 1,
      "unassignedStudentCount": 0
    }
  ]
}
```

---

## 9. Validation Rules

### Students

* Username required
* Unique per class

### Rankings

* Must include all books in class
* Rank must be 1..N with no duplicates

### Books

* Capacity must be > 0

---

## 10. Error Codes

| Code | Meaning          |
| ---- | ---------------- |
| 400  | Validation error |
| 401  | Unauthorized     |
| 403  | Forbidden        |
| 404  | Not found        |
| 500  | Server error     |

---

## 11. API Design Principles

* RESTful endpoints
* No entity exposure
* DTO-only responses
* Stateless services
* Deterministic assignment outputs

---

## 11.1 DTO Naming Conventions

Controllers must use request and response DTOs rather than JPA entities.

Naming rules:

* Request DTOs end in `Request`
* Response DTOs end in `Response`
* DTO names use the domain concept, such as `ClassPeriod`, even when the URL uses `/api/classes`

Examples:

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

---

## 12. Future Extensions (not implemented yet)

* Partial ranking support
* WebSocket live updates for classroom dashboard
* CSV import/export
* Email notifications
* Re-run comparison analytics
* Teacher analytics dashboard

---

## 13. Swagger / OpenAPI Documentation

BookRanker will expose an interactive API specification using **Springdoc OpenAPI (Swagger UI)**.

Swagger/OpenAPI is required for the target system, but it should be added after the first stable DTO/controller pattern has been implemented. Do not annotate prototype endpoints that are expected to be reshaped.

This ensures:

* Live API exploration in browser
* Auto-generated request/response schemas
* Alignment between implementation and contract
* Easier testing for students, teachers, and Codex agents

---

## 13.1 Dependency

After the first stable DTO/controller pattern is implemented, add the following dependency to `pom.xml`:

```xml id="sw1"
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

---

## 13.2 Swagger UI Access

Once the backend is running, Swagger UI will be available at:

```text id="sw2"
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```text id="sw3"
http://localhost:8080/v3/api-docs
```

---

## 13.3 API Documentation Strategy

All endpoints defined in this document MUST be annotated using:

### Controller annotations

```java id="sw4"
@Tag(name = "Class Period Management", description = "APIs for managing class periods")
```

### Endpoint annotations

```java id="sw5"
@Operation(summary = "Create a new class")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Class period created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid request")
})
```

---

## 13.4 DTO Documentation

All request and response DTOs MUST include schema descriptions:

```java id="sw6"
@Schema(description = "Request to create a new class")
public class CreateClassRequest {

    @Schema(description = "Name of the class", example = "English 12")
    private String name;
}
```

---

## 13.5 Swagger Grouping Strategy

Swagger UI will be grouped by domain:

* Teacher APIs
* Class Period APIs
* Book APIs
* Student APIs
* Assignment APIs

This aligns with the service boundaries in the codebase.

---

## 13.6 Security in Swagger

JWT authentication will be enabled in Swagger UI:

### Add Security Scheme

```java id="sw7"
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
```

### Apply to secured endpoints

```java id="sw8"
@SecurityRequirement(name = "bearerAuth")
```

---

## 13.7 Why Swagger is Required for This Project

Swagger is not optional in this architecture because:

1. **Frontend alignment**

    * React app will be built directly against OpenAPI spec

2. **Codex agent alignment**

    * Agents will reference Swagger schema for correctness

3. **Debugging**

    * Teachers and developers can test flows without Postman

4. **Portfolio value**

    * Demonstrates production-grade API design practices

---

## 13.8 Rule for Implementation

All backend controllers MUST:

* Be fully annotated with Swagger/OpenAPI metadata
* Match this API.md exactly
* Never expose JPA entities directly
* Always return DTOs with documented schemas

---

## 13.9 Future Enhancement

Later we may extend Swagger with:

* Example request generators for classrooms
* Seed data “Try it out” scenarios
* Role-based endpoint visibility (Teacher vs Student view)

---
