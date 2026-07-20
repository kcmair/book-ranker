# BookRanker Deployment Guide

## Production Architecture

BookRanker uses an inexpensive, Git-based deployment suitable for intermittent use:

```text
book-ranker.net              Cloudflare Pages (React SPA)
        |
        v
api.book-ranker.net          Render free web service (Spring Boot container)
        |
        v
Managed PostgreSQL           Neon
```

Render's free service may sleep after inactivity. The first request after sleeping can take about a minute while the service starts. Upgrade the Render service if usage later requires an always-on API.

## Repository Deployment Files

* `Dockerfile` builds and runs the Java 21 backend as a non-root user.
* `render.yaml` defines the Render web service, health check, custom domain, and environment variable contract.
* `src/main/resources/db/migration/` contains Flyway database migrations.
* `frontend/public/_redirects` preserves client-side React routes on Cloudflare Pages.

## Prerequisites

* The production changes are merged to the GitHub `main` branch.
* `book-ranker.net` is active in Cloudflare.
* A Neon, Render, and Cloudflare account is available.
* Render and Cloudflare have access to the GitHub repository.

## 1. Create the Neon Database

1. Create a Neon project in a region near the Render service.
2. Create or select the production database and application role.
3. Copy the connection details from Neon's Connect dialog.
4. Convert the hostname and database to a JDBC URL:

   ```text
   jdbc:postgresql://HOST/DATABASE?sslmode=require
   ```

5. Keep the JDBC URL, username, and password out of source control.

Flyway creates the schema during the backend's first startup. Hibernate then validates the migrated schema instead of changing it.

## 2. Deploy the Backend to Render

1. In Render, create a Blueprint from the GitHub repository.
2. Render reads `render.yaml` and creates the `book-ranker-api` Docker web service.
3. Supply the prompted secret values:

   ```env
   SPRING_DATASOURCE_URL=jdbc:postgresql://HOST/DATABASE?sslmode=require
   SPRING_DATASOURCE_USERNAME=...
   SPRING_DATASOURCE_PASSWORD=...
   ```

4. Render generates `BOOKRANKER_JWT_SECRET` and sets these non-secret values from the Blueprint:

   ```env
   SPRING_JPA_HIBERNATE_DDL_AUTO=validate
   BOOKRANKER_CORS_ALLOWED_ORIGINS=https://book-ranker.net,https://www.book-ranker.net
   ```

5. Wait for the image build, Flyway migration, and health check to succeed.
6. Verify the temporary Render URL:

   ```text
   GET https://SERVICE.onrender.com/actuator/health
   GET https://SERVICE.onrender.com/v3/api-docs
   GET https://SERVICE.onrender.com/swagger-ui.html
   ```

The application reads Render's assigned `PORT` and exposes only the Actuator health endpoint. Health details are not public.

## 3. Configure the API Domain

The Blueprint declares `api.book-ranker.net` as the backend custom domain. Follow Render's domain verification instructions and create the requested Cloudflare DNS record.

Use DNS-only mode while Render verifies and provisions its certificate if Render requests it. After HTTPS works directly through Render, the record may be proxied through Cloudflare. Do not cache `/api/*`, `/actuator/*`, `/v3/api-docs`, or `/swagger-ui/*` responses.

Verify:

```text
https://api.book-ranker.net/actuator/health
https://api.book-ranker.net/swagger-ui.html
```

## 4. Deploy the Frontend to Cloudflare Pages

1. In Cloudflare, open **Workers & Pages** and create a Pages project connected to the GitHub repository.
2. Use these build settings:

   ```text
   Production branch: main
   Root directory: frontend
   Build command: npm ci && npm run build
   Build output directory: dist
   ```

3. Under **Settings > Build > Build watch paths**, set:

   ```text
   Include paths: frontend/*
   Exclude paths: (empty)
   ```

   This prevents backend-only and documentation-only commits from rebuilding the frontend. Cloudflare's `*` wildcard includes nested paths.

4. Set production variables:

   ```env
   NODE_VERSION=26.5.0
   VITE_API_BASE_URL=https://api.book-ranker.net
   VITE_USE_MOCKS=false
   ```

5. Deploy and verify the generated `pages.dev` URL.
6. Add `book-ranker.net` under the Pages project's custom domains.
7. Configure `www.book-ranker.net` to redirect permanently to `https://book-ranker.net` while preserving paths and query strings.

The `_redirects` file ensures direct visits to routes such as `/poll/{joinCode}` load the React application.

## 5. Production Verification

Run the complete workflow against the public domains:

1. Confirm `GET /actuator/health` returns `200` and `{"status":"UP"}`.
2. Confirm Swagger loads and offers JWT bearer authorization.
3. Register and log in a new teacher.
4. Create multiple classes and books.
5. Open a class poll URL in a logged-out browser session.
6. Join as a student and submit rankings.
7. Run the assignment as the teacher.
8. Reopen the poll URL and confirm public results appear.
9. Confirm teacher class and all-class result grids load correctly.
10. Confirm unknown resources, invalid ranking submissions, and unauthorized teacher requests return the documented status codes.
11. Confirm browser requests contain no CORS or mixed-content errors.
12. Confirm a direct refresh of a nested frontend route still loads the application.

## Configuration Reference

### Backend

| Variable | Required | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Yes | PostgreSQL JDBC URL with TLS enabled |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database application role |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Yes | Must be `validate` in production |
| `BOOKRANKER_JWT_SECRET` | Yes | Strong JWT signing secret |
| `BOOKRANKER_CORS_ALLOWED_ORIGINS` | Yes | Comma-separated exact frontend origins |
| `PORT` | Set by Render | HTTP listener port |

### Frontend

| Variable | Required | Purpose |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Yes | Public HTTPS API origin |
| `VITE_USE_MOCKS` | Yes | Must be `false` in production |
| `NODE_VERSION` | Build setting | Node version matching `.nvmrc` |

Vite variables are compiled into the frontend bundle and are not secrets.

## Security and Operations

* Never commit database credentials or JWT secrets.
* Keep CORS restricted to the deployed frontend origins.
* Keep Hibernate schema mutation disabled in production.
* Apply all schema changes through versioned Flyway migrations.
* Keep H2 and its console limited to the local profile.
* Use HTTPS for every production browser and API request.
* Review Render and Neon usage limits periodically.
* Use Neon backups or restore features before risky schema or data changes.
* Roll back application failures by reverting the release commit and redeploying. Do not reverse an applied migration by deleting its Flyway history entry.

## Automatic Deployment Filters

Both production services follow `main`, but each rebuilds only for relevant changes:

* Render reads `buildFilter.paths` from `render.yaml` and rebuilds for backend source, configuration, Maven, or Docker build-input changes. Render always processes changes to `render.yaml` itself.
* Cloudflare Pages watches `frontend/*`, including nested frontend files.
* Documentation-only commits trigger neither application build.
* Manual deployments and platform configuration changes can still force a build regardless of these filters.

## Local Builds

```bash
mvn clean package

cd frontend
npm ci
npm run build
```

Local `mvn spring-boot:run` still activates the `local` profile with an ephemeral H2 database. Flyway is disabled only in that local profile; production uses PostgreSQL and Flyway.
