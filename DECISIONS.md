# FinTrack — Architecture Decision Records (DECISIONS)

| Field | Value |
| --- | --- |
| Product | FinTrack |
| Document | Architecture Decision Log |
| Version | 1.0.0 |
| Status | Active |
| Related documents | `PRD.md`, `SRS.md`, `CODEX.md` |
| Last updated | 2026-09-17 |

Each record uses ADR format: Status, Context, Decision, Alternatives considered, Consequences.

---

## ADR-001: Two Microservices

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-09-17 |

### Context

The project must demonstrate microservice-based backend development suitable for a junior software engineering portfolio, while remaining small enough for an approximately two-hour MVP.

### Decision

Separate the system into two independently runnable Spring Boot services:

- **Account Service** on port **8081** — accounts, balances, deposits.
- **Transaction Service** on port **8082** — transaction records and history; validates account existence via Account Service.

### Alternatives considered

- **Single monolithic Spring Boot application** — simpler operations, weaker demonstration of service boundaries.
- **Three or more services** — clearer theoretical decomposition, but excessive for a two-hour MVP.

### Consequences

- Demonstrates service boundaries and REST communication.
- Adds operational complexity (two processes, configuration, failure modes).
- Requires service-to-service error handling in Transaction Service.

---

## ADR-002: PostgreSQL with Separate Schemas

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-09-17 |

### Context

The MVP requires relational persistence, SQL discussion topics (joins, indexing), and clear service data ownership without heavy infrastructure.

### Decision

Use **one PostgreSQL instance** (typically one Docker container) with **separate schemas** for Account Service and Transaction Service data (e.g., `account_service` and `transaction_service`).

### Alternatives considered

- **Separate database containers per service** — stronger isolation; more local setup cost for the MVP.
- **Shared tables across services** — breaks ownership boundaries; couples deployments.
- **Embedded H2 only** — faster unit tests possible, but weaker demonstration of PostgreSQL and containerized relational setup for the main demo path.

### Consequences

- Simple local setup.
- Demonstrates relational modeling and indexing.
- Does not provide complete infrastructure isolation (shared database server).

---

## ADR-003: BigDecimal for Monetary Values

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-09-17 |

### Context

Floating-point arithmetic (`float` / `double`) is unsuitable for financial amounts due to binary rounding errors.

### Decision

Use `java.math.BigDecimal` for account balances and transaction amounts in application code, mapped to `NUMERIC` (e.g., `NUMERIC(19, 2)`) in PostgreSQL.

Normalize to scale 2 with an explicit rounding mode (recommended: `HALF_UP`) at API boundaries.

### Alternatives considered

- `double` for convenience — incorrect for money.
- Integer minor units (cents) only — valid approach, but `BigDecimal` is the direct Spring/JPA teaching choice for this MVP.
- Third-party money libraries — unnecessary dependency for scope.

### Consequences

- Predictable decimal arithmetic.
- Requires explicit scale and rounding decisions in validation and mapping.
- Slightly more verbose than primitive numeric types.

---

## ADR-004: REST for Inter-Service Communication

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-09-17 |

### Context

Transaction Service must validate that an `accountId` exists before recording a transaction. Services must not share writable tables.

### Decision

Use **synchronous REST** communication: Transaction Service calls Account Service `GET /api/accounts/{id}` when creating a transaction.

### Alternatives considered

- **Kafka (or other) events** — useful for async architectures; out of scope and too heavy for the MVP.
- **Shared database access** — Transaction Service reading Account Service tables — violates data ownership and couples schemas.

### Consequences

- Simple implementation and easy to demo.
- Introduces network dependency and potential partial failures (timeouts, Account Service down).
- Call failures should map to appropriate HTTP errors (e.g., `503`/`502`), not silent success.

---

## ADR-005: Simplified Transaction Consistency

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-09-17 |

### Context

The two-hour MVP does not implement distributed transactions. Clients may deposit via Account Service and record history via Transaction Service as separate steps.

### Decision

Treat **account balance updates** and **transaction recording** as **separate operations**. Do not implement 2PC, sagas, or transactional outbox in the MVP.

Document that balance and history can diverge if one step fails.

### Alternatives considered

- Distributed transaction protocols — too complex for MVP timebox.
- Single service owning both balance and ledger — simpler consistency, weaker microservice demo.
- Eventual consistency with messaging — deferred to future enhancements.

### Consequences

- The system may experience balance/history inconsistency if one operation fails.
- This limitation must remain documented in `PRD.md` / `SRS.md`.
- Future production design would require stronger consistency guarantees.

---

## ADR-006: Scope and Time Constraint

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-09-17 |

### Context

The MVP must be completed in approximately two hours of implementation while remaining credible for portfolio and interview discussion.

### Decision

Implement **only** the core account and transaction workflows defined in `PRD.md` and `SRS.md`:

- Account create, get, deposit.
- Transaction create, list by account.
- PostgreSQL + tests + documentation.

Explicitly exclude frontend, authentication, Kafka, Kubernetes, and advanced financial features.

### Alternatives considered

- Larger “production-like” feature set — risks incomplete demo.
- Tiny single-endpoint sample — insufficient for microservice and SQL discussion.

### Consequences

- No frontend, authentication, event streaming, or advanced financial features.
- Prioritize working APIs and tests.
- Future work is listed as enhancements, not implied as delivered.

---

## Decision Index

| ID | Title | Status |
| --- | --- | --- |
| ADR-001 | Two Microservices | Accepted |
| ADR-002 | PostgreSQL with Separate Schemas | Accepted |
| ADR-003 | BigDecimal for Monetary Values | Accepted |
| ADR-004 | REST for Inter-Service Communication | Accepted |
| ADR-005 | Simplified Transaction Consistency | Accepted |
| ADR-006 | Scope and Time Constraint | Accepted |

---

## Open Items (non-blocking)

These are defaults for implementation, not architecture blockers. Record a new ADR if a choice materializes differently:

| Topic | Tentative default | Owner doc |
| --- | --- | --- |
| Build tool | Maven | `CODEX.md` |
| Schema migrations | Flyway if time; else JPA DDL for MVP | `CODEX.md` |
| Duplicate `account_number` HTTP status | Prefer `409` | `SRS.md` / `CODEX.md` |
| Exact Spring Boot minor version | Choose current stable Boot 3.x compatible with Java 17 at implementation time | Implementation |

No unresolved decisions block documentation consistency for ports, endpoints, or the data model.
