| Date       | Decision                                    | Why                                                                                                     |
| ---------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| 2026-06-30 | Use Spring Boot REST instead of GraphQL     | Simpler architecture, aligns with project goals.                                                        |
| 2026-06-30 | Use managed free-tier hosting               | Meets the initial low-usage hosting objective.                                                          |
| 2026-06-30 | Use Minimum-Cost Maximum-Flow               | Superseded by the ranked-choice assignment rule.                                                        |
| 2026-06-30 | Use package-by-feature Spring Boot layout   | Keeps agent ownership and domain boundaries clear as the codebase grows.                                |
| 2026-07-01 | Use Maven as the only Java build tool       | Avoids Maven/Gradle drift and matches deployment documentation.                                         |
| 2026-07-01 | Use ClassPeriod as the Java domain name     | Avoids confusion with `java.lang.Class` while keeping `/api/classes` and `classes` externally concise. |
| 2026-07-20 | Use Cloudflare Pages + Render + Neon        | Uses the purchased Cloudflare domain and keeps intermittent-use hosting inexpensive.                    |
| 2026-07-20 | Manage the production schema with Flyway    | Makes schema creation and future database changes repeatable and reviewable.                            |
| 2026-07-20 | Use deterministic ranked-choice assignment  | Matches the classroom rule that each student gets their first ranked book with available capacity.       |


| ID      | Decision                                        | Status     |
| ------- | ----------------------------------------------- | ---------- |
| ADR-001 | Spring Boot REST API                            | ✅ Accepted |
| ADR-002 | React + TypeScript frontend                     | ✅ Accepted |
| ADR-003 | PostgreSQL database                             | ✅ Accepted |
| ADR-004 | Minimum-Cost Maximum-Flow assignment algorithm  | Superseded |
| ADR-005 | Teacher accounts required                       | ✅ Accepted |
| ADR-006 | Books remain a concrete entity (no abstraction) | ✅ Accepted |
| ADR-007 | Package-by-feature Spring Boot layout           | ✅ Accepted |
| ADR-008 | Free-tier hosting for intermittent use          | ✅ Accepted |
| ADR-009 | Maven-only Java build                           | ✅ Accepted |
| ADR-010 | ClassPeriod Java domain naming                  | ✅ Accepted |
| ADR-011 | Cloudflare Pages + Render + Neon deployment     | ✅ Accepted |
| ADR-012 | Flyway-managed production schema                | ✅ Accepted |
| ADR-013 | Deterministic ranked-choice assignment          | ✅ Accepted |
