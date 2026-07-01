# BookRanker Deployment Guide

## 1. Purpose

This document defines how BookRanker is built, deployed, and hosted in a **free-tier production setup**.

The goal is:

* zero cost hosting
* simple CI/CD
* public demo availability
* minimal infrastructure overhead

---

## 2. System Overview

BookRanker consists of:

### Backend

* Spring Boot (Java 21)
* Maven build
* PostgreSQL database
* REST API

### Frontend

* React (Vite + TypeScript)
* Static SPA

---

## 3. Target Architecture (Free Tier)

```text id="deploy_arch_1"
[ React Frontend ]
        ↓
   (Static Hosting)
        ↓
[ Spring Boot API ]
        ↓
[ PostgreSQL DB ]
```

---

## 4. Hosting Strategy (FREE TIER STACK)

### 4.1 Frontend Hosting

Recommended:

* **Vercel** (preferred)
* or Netlify

Why:

* free tier
* automatic GitHub deploys
* zero configuration
* HTTPS included

Deployment:

* build: `npm run build`
* output: `/dist`

---

### 4.2 Backend Hosting

Recommended options:

#### Option A (Recommended): Render

* free tier web services
* supports Docker or native Java
* auto deploy from GitHub

#### Option B: Railway

* simple Git-based deployment
* free tier credits

#### Option C: Fly.io

* lightweight container hosting
* good for Java services

---

### Backend Requirements

* expose port via `SERVER_PORT`
* configure CORS for frontend domain
* externalize DB config via env variables

---

### 4.3 Database Hosting

Recommended:

* **Supabase (Postgres)**
* or Neon (PostgreSQL)

Why:

* free tier
* managed database hosting
* easy connection strings
* no local infra needed

---

## 5. Environment Configuration

### Backend environment variables

```env id="env_backend"
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
JWT_SECRET=
```

---

### Frontend environment variables

```env id="env_frontend"
VITE_API_BASE_URL=
```

---

## 6. Local Development Setup

### Backend

Maven is the only supported build tool.

```bash id="local_backend"
mvn clean install
mvn spring-boot:run
```

Runs on:

```
http://localhost:8080
```

By default, `mvn spring-boot:run` activates the `local` Spring profile from the Maven Spring Boot plugin configuration. That profile uses the in-memory H2 database in `application-local.yaml`, so local development does not require PostgreSQL to be running.

To run against PostgreSQL, use the default application configuration with PostgreSQL environment variables or a production-oriented profile.

---

### Frontend

```bash id="local_frontend"
npm install
npm run dev
```

Runs on:

```
http://localhost:5173
```

---

## 7. CORS Configuration

Backend must allow:

* local frontend (localhost:5173)
* production frontend (Vercel/Netlify domain)

---

## 8. CI/CD Pipeline (GitHub-based)

### Frontend (Vercel)

* auto deploy on push to `main`

### Backend (Render/Railway)

* auto deploy on push to `main` or `feature/release`

---

## 9. Deployment Strategy

### Recommended workflow:

1. Develop on feature branches
2. Merge into `main`
3. Frontend auto deploys
4. Backend auto deploys
5. Database remains managed (Supabase/Neon)

---

## 10. Build & Release Process

### Step 1 — Backend build

```bash id="build_backend"
mvn clean package
```

---

### Step 2 — Frontend build

```bash id="build_frontend"
npm run build
```

---

### Step 3 — Verify API integration

* Swagger must be accessible in production
* `/api/**` routes must be live

---

## 11. Production Considerations

### Security

* never expose DB credentials in repo
* JWT secret must be environment variable
* enable HTTPS only in production

---

### Performance

* stateless backend (horizontal scaling ready)
* frontend is fully static
* database is managed service

---

## 12. Cost Model

Target cost:

| Component                          | Cost |
| ---------------------------------- | ---- |
| Frontend (Vercel)                  | $0   |
| Backend (Render/Railway free tier) | $0   |
| PostgreSQL (Supabase/Neon)         | $0   |

> Total: $0/month (within free tier limits)

---

## 13. Deployment Goals

This system is designed to:

* be publicly demoable
* require no paid infrastructure
* support portfolio showcasing
* scale later if needed

---

## 14. Failure Recovery Strategy

If a deployment fails:

1. rollback via Git revert
2. redeploy from last stable commit
3. verify env variables
4. re-run build pipeline

---

## 15. Final Deployment State

A successful deployment means:

* frontend accessible via public URL
* backend API reachable via HTTPS
* database connected and persistent
* full user flow operational:

    * teacher login
    * class creation
    * student ranking
    * assignment execution
    * results display

---
