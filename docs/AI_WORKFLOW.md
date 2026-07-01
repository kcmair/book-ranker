# BookRanker AI Workflow

## 1. Purpose

This document defines how Codex or other AI coding sessions work on BookRanker without drifting from the architecture, duplicating logic, or blocking one another.

It combines the project workflow, orchestration rules, and branch strategy into one canonical protocol.

---

## 2. Core Principle

Agents work in isolation and communicate only through shared contracts.

The shared contracts are:

* `ARCHITECTURE.md`
* `API.md`
* `DATABASE.md`
* `ALGORITHM.md`
* `TASKS.md`
* `IMPLEMENTATION_ALIGNMENT.md`

No agent may assume behavior, paths, endpoints, data shapes, or dependencies that are not defined by these documents or by its assigned task.

---

## 3. Coordination Authority

### Architect (Human Only)

The Architect is the only coordination authority.

Responsibilities:

* assign task bundles to agents
* create or approve branch assignments
* validate outputs against project contracts
* update `TASKS.md`
* resolve conflicts
* approve pull requests
* merge into `main`

Worker agents do not coordinate with one another directly.

---

## 4. Agent Model

BookRanker uses five execution agents.

| Agent | Branch | Scope |
| --- | --- | --- |
| Auth Agent | `feature/auth` | Teacher authentication, JWT, security configuration |
| Domain Agent | `feature/domain` | Class periods, books, students, rankings |
| Algorithm Agent | `feature/algorithm` | Pure Java MCMF assignment engine |
| Assignment Agent | `feature/assignment` | Assignment run lifecycle, orchestration, persistence, metrics |
| Frontend Agent | `feature/frontend` | React + TypeScript frontend |

---

## 5. Agent Responsibilities

### Auth Agent

Owns:

* Teacher entity and repository
* registration and login
* password hashing
* JWT creation and validation
* Spring Security configuration

Rules:

* Must follow `API.md` teacher auth endpoints exactly
* Must not implement class, book, student, ranking, assignment, algorithm, or frontend logic
* Must expose auth state to other backend modules through explicit security contracts only

---

### Domain Agent

Owns:

* Class periods
* Books
* Students
* Rankings
* related DTOs, controllers, services, repositories, and validation

Rules:

* Must follow `API.md` for all domain endpoints
* Must follow `DATABASE.md` for persistence relationships and constraints
* Must return DTOs only
* Must not compute assignments
* Must not implement algorithm logic
* Must not modify auth/security beyond required integration points

---

### Algorithm Agent

Owns:

* graph model
* Minimum-Cost Maximum-Flow implementation
* ranking cost function
* deterministic tie behavior
* algorithm unit tests

Rules:

* Must follow `ALGORITHM.md`
* Must be pure Java
* Must not use Spring annotations
* Must not access the database
* Must not use HTTP, controller DTOs, or repositories
* Must accept a fully materialized input model and return an assignment result

---

### Assignment Agent

Owns:

* `AssignmentRun` lifecycle
* assignment orchestration service
* `ClassState` construction from domain data
* invocation of the Algorithm Agent output
* persistence of assignment results
* satisfaction metrics
* assignment-related API endpoints

Rules:

* Must not modify the algorithm implementation
* Must not duplicate ranking validation owned by the Domain Agent except where required to validate assignment readiness
* Must persist historical runs without overwriting previous results
* Must follow `API.md`, `DATABASE.md`, and `ALGORITHM.md`

---

### Frontend Agent

Owns:

* React + TypeScript app
* UI state management
* API integration layer
* teacher workflows
* student workflows
* assignment result views

Rules:

* Must consume backend APIs only through `API.md`
* May mock API responses for frontend-first development
* Must not implement backend business rules as an alternate source of truth
* Must not bypass API contracts

---

## 6. Contract Boundaries

All cross-agent communication happens through explicit contracts:

* Frontend to Backend: `API.md`
* Backend to Database: `DATABASE.md`
* Assignment to Algorithm: `ClassState` and `AssignmentResult` from `ALGORITHM.md`
* Work planning and state: `TASKS.md`

No implicit coupling is allowed.

Examples of forbidden coupling:

* frontend reading database schema instead of API responses
* algorithm importing JPA entities or repositories
* assignment service reimplementing MCMF logic
* domain controller returning JPA entities directly

---

## 7. Path Isolation

Each agent may only edit files in its assigned scope.

| Agent | Primary allowed paths |
| --- | --- |
| Auth Agent | `src/main/java/com/bookranker/auth/**`, security config, auth DTOs/tests |
| Domain Agent | `src/main/java/com/bookranker/{classperiods,books,students,rankings}/**`, shared domain DTOs/tests |
| Algorithm Agent | `src/main/java/com/bookranker/algorithm/**`, algorithm tests |
| Assignment Agent | `src/main/java/com/bookranker/assignment/**`, assignment tests |
| Frontend Agent | `frontend/**` |

Shared configuration, application bootstrap, and docs may be changed only when the assigned task explicitly requires it.

If the current codebase has not yet been split into these package paths, agents should create the appropriate package for their scope rather than expanding unrelated existing classes.

---

## 8. Dependency Rules

An agent may begin work only when its dependencies are satisfied:

| Agent | Dependencies |
| --- | --- |
| Auth Agent | none |
| Domain Agent | Auth contracts for secured teacher flows; database contract |
| Algorithm Agent | none |
| Assignment Agent | Domain data model + Algorithm output |
| Frontend Agent | `API.md`; mocks allowed before backend completion |

Parallel work is allowed when agents do not share paths and their dependencies are met.

Safe early parallel work:

* Auth Agent
* Algorithm Agent
* Frontend Agent using mocked API responses

---

## 9. Branch Lifecycle

Each worker branch follows this lifecycle:

```text
CREATE -> ASSIGN -> IMPLEMENT -> TEST -> PR -> REVIEW -> MERGE -> DELETE
```

Steps:

* CREATE: branch is created from `main`
* ASSIGN: Architect assigns a scoped task bundle
* IMPLEMENT: worker modifies only allowed files
* TEST: build and relevant tests pass
* PR: worker opens a pull request
* REVIEW: Architect validates contracts and isolation
* MERGE: Architect merges into `main`
* DELETE: branch is optionally removed

Workers may not commit directly to `main`.

---

## 10. Worker Prompt Template

Each worker session should start with:

```text
You are working on BookRanker as an isolated worker.
Assigned agent: <agent name>
Assigned branch: feature/<scope>
Assigned tasks: <TASKS.md task IDs>

Read and follow:
- docs/ARCHITECTURE.md
- docs/API.md
- docs/DATABASE.md
- docs/ALGORITHM.md
- docs/TASKS.md
- docs/IMPLEMENTATION_ALIGNMENT.md
- docs/AI_WORKFLOW.md

Only modify files in your assigned scope.
Do not change API contracts unless explicitly instructed by the Architect.
Return compilable code, tests where appropriate, and notes on assumptions.
```

---

## 11. Execution Workflow

All agents follow the same workflow:

1. Read required project contracts
2. Confirm assigned task IDs and allowed paths
3. Implement only assigned scope
4. Add or update focused tests
5. Verify build/test commands relevant to the change
6. Report assumptions, integration points, and remaining risks

Backend tasks must also keep Swagger/OpenAPI annotations aligned with `API.md` once Swagger is introduced.

---

## 12. Merge Requirements

A pull request may be merged only when:

* code compiles
* relevant tests pass
* implementation matches `API.md`, `DATABASE.md`, and `ALGORITHM.md`
* no agent edited another agent's scope without explicit approval
* no JPA entities are exposed from API controllers
* no business logic is hidden in controllers
* no algorithm logic exists outside the Algorithm Agent scope
* Swagger/OpenAPI documentation is updated for backend API changes once available

---

## 13. Conflict Resolution

When two workers produce conflicting output:

1. Compare each implementation against the contract docs
2. Prefer the solution that is simplest, most contract-compliant, and least coupled
3. Select one implementation
4. Reject or rework the non-compliant implementation

Do not blend conflicting implementations unless the Architect explicitly designs an integration path.

---

## 14. State Management

`TASKS.md` is the only global task state.

Task statuses:

* Not Started
* In Progress
* Completed

Agents may report progress, but only the Architect updates the canonical task board unless explicitly delegated.

---

## 15. Guardrails

Never allowed:

* cross-agent edits without explicit instruction
* direct worker-to-worker coordination as a substitute for contracts
* API behavior not documented in `API.md`
* database relationships not documented in `DATABASE.md`
* Spring or database access inside the algorithm module
* frontend reimplementation of backend assignment rules
* direct commits to `main` from worker agents

---

## 16. System Goal

BookRanker is successful when five isolated agents can build their portions independently and the Architect can integrate them with predictable, contract-compliant results.
