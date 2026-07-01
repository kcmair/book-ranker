# BookRanker Implementation Task Board

## 1. Purpose

This document defines all implementation tasks for BookRanker as a **dependency-driven execution system**.

Unlike a sequential plan, tasks may be executed in parallel by AI agents as soon as their dependencies are satisfied.

---

## 2. Execution Model

### Key Principle

> Tasks are not ordered by phases. Tasks are executed when dependencies are met.

---

### Rules

* Multiple agents may work in parallel
* A task is eligible when all dependencies are complete
* No agent may modify another agent’s domain
* API.md is the contract boundary for frontend/backend interaction
* Algorithm module is fully independent
* IMPLEMENTATION_ALIGNMENT.md tracks known code-to-contract alignment work

---

## 3. Agents (Execution Owners)

* 🟦 Auth Agent (teacher authentication, JWT, security)
* 🟪 Domain Agent (Class Periods, Books, Students, Rankings)
* 🟧 Assignment Agent (orchestration + persistence + metrics)
* 🟥 Algorithm Agent (pure MCMF engine)
* 🟩 Frontend Agent (React UI consuming API)

---

## 4. Task Format

Each task includes:

* ID
* Description
* Owner Agent
* Dependencies
* Status

Status values:

* ⬜ Not Started
* 🟨 In Progress
* ✅ Completed

---

# 5. Task Graph

---

# 🔐 AUTH DOMAIN (Foundation)

### TASK A1 — Teacher Entity

* Owner: Auth Agent
* Dependencies: none
* Status: ✅
* Description: Create Teacher entity (id, email, passwordHash)

---

### TASK A2 — Teacher Repository

* Owner: Auth Agent
* Dependencies: A1
* Status: ✅

---

### TASK A3 — Teacher Auth Service

* Owner: Auth Agent
* Dependencies: A1, A2
* Status: ✅
* Description:

    * register
    * login
    * BCrypt hashing

---

### TASK A4 — JWT Security Layer

* Owner: Auth Agent
* Dependencies: A3
* Status: ✅

---

### TASK A5 — Auth Controller

* Owner: Auth Agent
* Dependencies: A3, A4
* Status: ✅
* Endpoints:

    * POST /api/teachers/register
    * POST /api/teachers/login

---

# 🏫 CLASS DOMAIN

### TASK B1 — ClassPeriod Entity

* Owner: Domain Agent
* Dependencies: A1 (teacher exists)
* Status: ⬜

---

### TASK B2 — ClassPeriod Repository

* Owner: Domain Agent
* Dependencies: B1
* Status: ⬜

---

### TASK B3 — ClassPeriod Service

* Owner: Domain Agent
* Dependencies: B1, B2
* Status: ⬜

---

### TASK B4 — ClassPeriod Controller

* Owner: Domain Agent
* Dependencies: B3
* Status: ⬜

---

# 📚 BOOK DOMAIN

### TASK C1 — Book Entity

* Owner: Domain Agent
* Dependencies: B1
* Status: ⬜

---

### TASK C2 — Book Repository

* Owner: Domain Agent
* Dependencies: C1
* Status: ⬜

---

### TASK C3 — Book Service

* Owner: Domain Agent
* Dependencies: C1, C2
* Status: ⬜

---

### TASK C4 — Book Controller

* Owner: Domain Agent
* Dependencies: C3, B1
* Status: ⬜

---

# 👨‍🎓 STUDENT DOMAIN

### TASK D1 — Student Entity

* Owner: Domain Agent
* Dependencies: B1
* Status: ⬜

---

### TASK D2 — Student Repository

* Owner: Domain Agent
* Dependencies: D1
* Status: ⬜

---

### TASK D3 — Student Service (Join Class)

* Owner: Domain Agent
* Dependencies: D1, B1
* Status: ⬜

---

### TASK D4 — Student Controller

* Owner: Domain Agent
* Dependencies: D3
* Status: ⬜

---

# 📝 RANKING DOMAIN

### TASK E1 — Ranking Entity

* Owner: Domain Agent
* Dependencies: D1, C1
* Status: ⬜

---

### TASK E2 — Ranking Service

* Owner: Domain Agent
* Dependencies: E1
* Status: ⬜

---

### TASK E3 — Ranking Controller

* Owner: Domain Agent
* Dependencies: E2
* Status: ⬜

---

# ⚙️ ALGORITHM (INDEPENDENT SYSTEM)

### TASK F1 — Graph Model

* Owner: Algorithm Agent
* Dependencies: none
* Status: ✅

---

### TASK F2 — MCMF Implementation

* Owner: Algorithm Agent
* Dependencies: F1
* Status: ✅

---

### TASK F3 — Cost Function Mapping

* Owner: Algorithm Agent
* Dependencies: F1
* Status: ✅

---

### TASK F4 — Algorithm Tests

* Owner: Algorithm Agent
* Dependencies: F2, F3
* Status: ✅

---

# 🧠 ASSIGNMENT SYSTEM

### TASK G1 — ClassState Builder

* Owner: Assignment Agent
* Dependencies: B1, C1, D1, E1
* Status: ⬜

---

### TASK G2 — AssignmentRun Entity

* Owner: Assignment Agent
* Dependencies: B1
* Status: ⬜

---

### TASK G3 — Assignment Service (Orchestration)

* Owner: Assignment Agent
* Dependencies: F2, G1, G2
* Status: ⬜

---

### TASK G4 — Assignment Controller

* Owner: Assignment Agent
* Dependencies: G3
* Status: ⬜

---

# 🎨 FRONTEND (PARALLEL CONSUMER)

### TASK H1 — React Project Setup

* Owner: Frontend Agent
* Dependencies: API.md stable (assumed)
* Status: ⬜

---

### TASK H2 — Teacher UI

* Owner: Frontend Agent
* Dependencies: G4 (assignment APIs optional)
* Status: ⬜

---

### TASK H3 — Student UI

* Owner: Frontend Agent
* Dependencies: D4, E3
* Status: ⬜

---

### TASK H4 — Results Dashboard

* Owner: Frontend Agent
* Dependencies: G4
* Status: ⬜

---

# 🔗 6. Global Dependency Rules

## Core Rules

* Algorithm Agent is fully independent
* Auth Agent owns authentication and security contracts
* Domain Agent is foundation for DB-backed classroom, book, student, and ranking modules
* Assignment Agent depends on all domain data models
* Frontend depends only on API contracts (not internal implementation)

---

## Parallel Execution Opportunities

These can run immediately:

* F1–F3 (Algorithm Agent)
* A1–A3 (Auth Agent core)
* H1 (Frontend setup)

---

# 🧪 7. Definition of Done

A task is complete only if:

* Code compiles
* Tests pass (if applicable)
* Matches API.md (if backend)
* No cross-agent violations exist
* Swagger updated for backend tasks once the first stable DTO/controller pattern exists

---

# 🚨 8. Agent Enforcement Rules

* No agent may modify another agent’s domain
* No skipping dependencies
* No hidden business logic in controllers
* No algorithm logic outside Algorithm Agent
* All outputs must match specification documents

---

# 📊 9. System Outcome Goal

> BookRanker is successful when all tasks can be executed independently by agents and integrated without conflict.

---
