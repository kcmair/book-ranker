| Date       | Decision                                    | Why                                                                                                     |
| ---------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| 2026-06-30 | Use Spring Boot REST instead of GraphQL     | Simpler architecture, aligns with project goals.                                                        |
| 2026-06-30 | Use Render + Vercel + Supabase              | Meets zero-cost hosting objective.                                                                      |
| 2026-06-30 | Use Minimum-Cost Maximum-Flow               | Produces globally optimal assignments under capacity constraints.                                       |
| 2026-06-30 | Use package-by-feature Spring Boot layout   | Keeps agent ownership and domain boundaries clear as the codebase grows.                                |
| 2026-07-01 | Use Maven as the only Java build tool       | Avoids Maven/Gradle drift and matches deployment documentation.                                         |
| 2026-07-01 | Use ClassPeriod as the Java domain name     | Avoids confusion with `java.lang.Class` while keeping `/api/classes` and `classes` externally concise. |


| ID      | Decision                                        | Status     |
| ------- | ----------------------------------------------- | ---------- |
| ADR-001 | Spring Boot REST API                            | ✅ Accepted |
| ADR-002 | React + TypeScript frontend                     | ✅ Accepted |
| ADR-003 | PostgreSQL database                             | ✅ Accepted |
| ADR-004 | Minimum-Cost Maximum-Flow assignment algorithm  | ✅ Accepted |
| ADR-005 | Teacher accounts required                       | ✅ Accepted |
| ADR-006 | Books remain a concrete entity (no abstraction) | ✅ Accepted |
| ADR-007 | Package-by-feature Spring Boot layout           | ✅ Accepted |
| ADR-008 | Free-tier hosting (Render + Vercel + Supabase)  | ✅ Accepted |
| ADR-009 | Maven-only Java build                           | ✅ Accepted |
| ADR-010 | ClassPeriod Java domain naming                  | ✅ Accepted |
