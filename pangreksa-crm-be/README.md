# pangreksa-crm-be — Pangreksa CRM Backend (Spring Boot REST API)

The backend for Pangreksa CRM: a **Spring Boot REST API** (Kotlin) over PostgreSQL. It replaces the
Vaadin Hilla monolith (kept as legacy in `../pangreksa-crm`). The frontend (`../pangreksa-crm-fe`)
consumes this API over JSON.

> Status: **documented skeleton** — no application code yet. This README is the contract/spec the
> implementation (and code generation) should follow. See `../CLAUDE.md`, `../PRD.md`, `../schema.md`.

## Stack
- **Kotlin** + **Spring Boot** (Web / REST)
- **Spring Data JPA** + **PostgreSQL**
- **Liquibase** (DB migrations)
- **springdoc-openapi** (publishes OpenAPI/Swagger — the FE generates its typed client from this)
- **Bean Validation** (jakarta.validation)
- **Spring Security** (CORS now; full RBAC per `PRD.md` FR-SEC-* later)
- Runs on **JDK 25** (bytecode target 24 if Kotlin's max applies)

## Responsibilities
- Owns **all business logic and validation in the service layer** (per CLAUDE.md).
- Exposes JSON REST endpoints under `/api/**`; publishes OpenAPI at `/v3/api-docs` + Swagger UI.
- Enables **CORS** for the frontend dev origin (e.g. `http://localhost:3000`).

## Suggested structure (feature-based)
```
src/main/kotlin/com/pangreksa/crm/
  CrmApiApplication.kt
  config/        # SecurityConfig (CORS, permitAll for now), OpenApiConfig
  base/          # BaseEntity (id, audit, jsonb customFields), error handling (@ControllerAdvice)
  account/  contact/  lead/  deal/  ...   # per module:
    domain/      # @Entity + JpaRepository
    service/     # business logic + validation (throws domain exceptions)
    web/         # @RestController (maps to /api/<module>), request/response DTOs
src/main/resources/
  application.yaml
  db/changelog/db.changelog-master.yaml    # Liquibase (accounts, contacts, leads, deals, ...)
```

## REST contract (conventions)
- **List (infinite scroll, no pagination):** `GET /api/leads?offset=0&limit=50&sort=lastName,asc&q=...`
  → `200` JSON array of DTOs; total count returned via header `X-Total-Count` (for the FE's infinite query).
- **Get one:** `GET /api/leads/{id}` → DTO or `404`.
- **Create:** `POST /api/leads` (JSON body) → `201` + created DTO; validation failure → `400` with a
  field-error payload (`{ field, message }[]`) the FE can show inline.
- **Update:** `PATCH /api/leads/{id}`; **Delete (soft):** `DELETE /api/leads/{id}` → Recycle Bin.
- **Pick-lists / metadata:** e.g. `GET /api/deals/stages`, `GET /api/leads/lead-sources`.
- Errors use a consistent shape; picklist values, lookups (by id), and derived fields (e.g. deal
  probability/expected revenue) are computed server-side.

## Database
- `jdbc:postgresql://localhost:5432/pangreksa_crm` (user/pass per environment; do not commit secrets).
- Schema owned by Liquibase; `custom_fields jsonb` on entities for flexible data.

## Run (once implemented)
```
JAVA_HOME="<jdk25>" ./mvnw spring-boot:run     # http://localhost:8080 ; OpenAPI at /v3/api-docs
```

## Mapping to the spec
Entities/fields → `schema.md`; behaviour/validation → `PRD.md` (`FR-*`) & `BDD/*.feature`.
