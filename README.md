# BookRanker

BookRanker is a classroom book-assignment tool. Teachers create class periods,
add books with capacity limits, share a student poll URL, collect ranked student
preferences, and run an assignment algorithm that matches students to books while
respecting capacity constraints.

The backend is a Spring Boot REST API. The frontend is a Vite React application.
The assignment engine is implemented in Java as a standalone algorithm module
inside the backend codebase.

## Tech Stack

- Backend: Java 21, Spring Boot, Maven, Spring Web, Spring Security, Spring Data
  JPA, PostgreSQL, H2 for local development and tests
- Frontend: React, TypeScript, Vite
- Assignment algorithm: Minimum-Cost Maximum-Flow
- API docs: Springdoc OpenAPI / Swagger UI
- Deployment: Cloudflare Pages, Render, Neon PostgreSQL, Flyway

## Repository Layout

```text
.
├── src/main/java/com/bookranker     Backend source
├── src/main/resources               Backend configuration
├── src/test/java/com/bookranker     Backend tests
├── frontend                         React/Vite frontend
├── docs                             Architecture, API, workflow, deployment docs
├── scripts                          Utility scripts
├── Dockerfile                       Production backend image
├── render.yaml                      Render service definition
├── pom.xml                          Maven backend build
└── compose.yaml                     Local service support
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 26.5+
- npm

## Local Development

### Backend

Maven is the supported backend build tool.

```bash
mvn spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

`mvn spring-boot:run` activates the `local` Spring profile through the Maven
Spring Boot plugin. The local profile uses an in-memory H2 database, so
PostgreSQL is not required for normal local development.

Swagger/OpenAPI:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/swagger-ui.html
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at:

```text
http://localhost:5173
```

By default, the frontend calls:

```text
http://localhost:8080
```

Override the backend URL with:

```bash
VITE_API_BASE_URL=http://127.0.0.1:8080 npm run dev
```

## Common Commands

### Backend

```bash
mvn test
mvn spotless:check checkstyle:check test
mvn spotless:apply
mvn clean package
```

### Frontend

```bash
cd frontend
npm run lint
npm run build
npm run format:check
npm run format
```

## Demo Data

The demo seed script creates a teacher, classes, books, students, rankings, and
assignment results against a running local backend.

```bash
node scripts/seed-demo-data.mjs
```

Run it after starting the backend. Because the local H2 database is in-memory,
seeded data is lost when the backend restarts.

## Main User Flows

### Teacher

1. Create a teacher account.
2. Log in.
3. Create a class period.
4. Add books and capacity limits.
5. Copy the student poll URL.
6. Review joined students.
7. Run the assignment.
8. Review class results and all-class assignment spreadsheets.

### Student

1. Open the poll URL.
2. Enter a username.
3. Drag books into ranked order.
4. Submit rankings.
5. After the teacher runs the assignment, revisit the poll URL to see the public
   class assignment spreadsheet.

## Authentication

Teacher management endpoints require a JWT bearer token returned by:

```text
POST /api/teachers/login
```

Use the token as:

```text
Authorization: Bearer <token>
```

Public student poll and assignment-grid endpoints do not require teacher
authentication.

## Configuration

Important backend environment variables for non-local environments:

```env
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
BOOKRANKER_JWT_SECRET=
BOOKRANKER_CORS_ALLOWED_ORIGINS=
```

Production deployments use Flyway migrations and Hibernate schema validation.
See `docs/DEPLOYMENT.md` for the Cloudflare Pages, Render, Neon, custom-domain,
and release-verification steps.

Important frontend environment variables:

```env
VITE_API_BASE_URL=
VITE_USE_MOCKS=false
```

## Documentation

- `docs/ARCHITECTURE.md`: system architecture and domain model
- `docs/API.md`: REST API contract
- `docs/ALGORITHM.md`: assignment algorithm design
- `docs/DATABASE.md`: target database schema
- `docs/DEPLOYMENT.md`: deployment and environment guidance
- `docs/DECISIONS.md`: architectural decision record
- `docs/archive/`: historical implementation-planning docs from the build phase

## Notes

- This is a Maven project. Gradle files are intentionally not part of the build.
- Local backend data is ephemeral when using the default local H2 profile.
- The public API uses `/api/classes`, while the Java domain entity is
  `ClassPeriod`.
