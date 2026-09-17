# FinTrack — CODEX Development Contract

| Field | Value |
| --- | --- |
| Product | FinTrack |
| Document | Development instructions for Codex / AI-assisted sessions |
| Version | 1.0.0 |
| Status | Active contract |
| Related documents | `PRD.md`, `SRS.md`, `DECISIONS.md` |
| Last updated | 2026-09-17 |

This file is the **primary development contract** for future implementation sessions. Follow it unless the human explicitly overrides a rule in the current session.

---

## 1. Project Context

FinTrack is an educational personal banking backend MVP:

- Java 17 + Spring Boot microservices.
- PostgreSQL with separate schemas.
- REST APIs for accounts, deposits, and transactions.
- Portfolio target: Cognizant Jr. Software Engineer, Job ID 00068949144.

It is **not** a real bank. Do not claim production-grade banking guarantees.

Authoritative product and technical specs: `PRD.md` and `SRS.md`. Architecture rationale: `DECISIONS.md`.

---

## 2. Development Objectives

1. Deliver a small, working two-service MVP achievable in ~two hours of implementation.
2. Keep Account Service and Transaction Service responsibilities separate.
3. Prefer correctness, clarity, and tests over features.
4. Keep documentation consistent with code (ports, endpoints, data model).

**Mandatory process rule (exact text):**

> Work in small verified slices. After each meaningful slice, run the relevant checks and report the result before continuing. Do not make broad speculative refactors.

---

## 3. Technology Constraints

| Constraint | Rule |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot |
| Money | `BigDecimal` only for balances and amounts — never `double`/`float` |
| Persistence | PostgreSQL; one instance; separate schemas |
| Account Service port | **8081** |
| Transaction Service port | **8082** |
| Inter-service | Synchronous REST only (no Kafka) |

---

## 4. Repository Structure (target)

```
FinTrack/
├── PRD.md
├── SRS.md
├── CODEX.md
├── DECISIONS.md
├── docs/                          # supplementary notes (optional)
├── docker-compose.yml             # PostgreSQL
├── account-service/               # Spring Boot module/app
│   └── src/main/java/...
│   └── src/test/java/...
└── transaction-service/           # Spring Boot module/app
    └── src/main/java/...
    └── src/test/java/...
```

Exact package names may vary; keep each service independently buildable and runnable.

---

## 5. Architecture Rules

1. **Account Service (8081):** create account, get account, maintain balance, deposit.
2. **Transaction Service (8082):** record DEPOSIT/WITHDRAWAL, list by account, validate account via Account Service REST.
3. **Data ownership:** Account Service owns account tables; Transaction Service owns transaction tables. No shared writable tables.
4. **No cross-schema foreign keys required** between services; `account_id` on transactions is a logical reference.
5. **Do not implement Transaction Service until Account Service builds and its basic API tests pass.**
6. Balance updates and transaction inserts are separate operations (ADR-005). Document the limitation; do not fake distributed transactions.

---

## 6. Coding Conventions

- Layering: Controller → Service → Repository; use DTOs for API request/response.
- Entities stay in persistence layer; map to/from DTOs.
- Centralize exception handling (`@ControllerAdvice` or equivalent).
- Prefer clear names over clever abstractions.
- Keep classes focused; avoid premature interfaces/factories for single implementations.
- Inspect existing files before modifying them.
- Match existing style once code exists.

---

## 7. API and Database Conventions

### Endpoints (MVP only)

| Service | Method | Path |
| --- | --- | --- |
| Account | POST | `/api/accounts` |
| Account | GET | `/api/accounts/{id}` |
| Account | POST | `/api/accounts/{id}/deposit` |
| Transaction | POST | `/api/transactions` |
| Transaction | GET | `/api/transactions/account/{accountId}` |

Do not add endpoints unless the human marks them as in-scope.

### Data model

**Accounts:** `id`, `account_number`, `account_holder`, `balance`, `created_at`  
**Transactions:** `id`, `account_id`, `type`, `amount`, `created_at`

Use `NUMERIC(19,2)` (or equivalent) for money columns. Index `account_id` on transactions; unique `account_number` on accounts.

JSON field names may be camelCase in APIs (`accountNumber`, `accountHolder`, `createdAt`) while DB columns remain snake_case.

---

## 8. Financial Data Handling Rules

1. Use `BigDecimal` for all monetary values in Java.
2. Normalize scale to 2 decimal places with an explicit rounding mode (recommend `RoundingMode.HALF_UP`).
3. Reject amounts that are null or `<= 0` for deposits and recorded transactions.
4. Never use floating-point types for money.
5. Do not invent interest, FX, or fee calculations in the MVP.

---

## 9. Testing Rules

1. Write/adjust tests in the same slice as the behavior they cover.
2. Account Service basic API tests must pass before starting Transaction Service.
3. Cover happy path and key validation / not-found cases.
4. Run relevant tests after every meaningful implementation slice.
5. Do not claim tests or APIs exist before implementation.

---

## 10. Verification Commands

Adapt to the chosen build tool. Examples:

```bash
# From account-service/
./mvnw test
# or: mvn test

# From transaction-service/
./mvnw test

# Run services (after DB is up)
./mvnw spring-boot:run
```

PostgreSQL via Docker Compose from repo root:

```bash
docker compose up -d
```

After each slice: report which command ran and whether it passed or failed.

---

## 11. Git Workflow

- Commit only when the human asks.
- Prefer small commits after a verified slice.
- Do not commit secrets (`.env` with credentials, private keys).
- Do not rewrite published history unless explicitly requested.

---

## 12. Definition of Done (MVP slice)

A slice is done when:

1. Code compiles.
2. Relevant automated tests pass.
3. Behavior matches `SRS.md` for the touched endpoints.
4. No out-of-scope infrastructure was introduced.
5. Results of verification commands are reported before the next slice.

MVP overall done when `PRD.md` acceptance criteria AC-1 through AC-8 are met.

---

## 13. Explicit Prohibitions (Anti-Overengineering)

Do **not**:

- Add Kafka, Kubernetes, service mesh, or unrelated infrastructure.
- Add a frontend.
- Add JWT / OAuth / Spring Security unless the human explicitly expands scope.
- Implement distributed transactions, sagas, or two-phase commit for the MVP.
- Split into three or more services.
- Add CQRS, event sourcing, or hexagonal “purity” that slows the two-hour MVP.
- Invent extra endpoints, entities, or “nice to have” modules.
- Perform broad speculative refactors.
- Claim production-grade banking, compliance, or correctness guarantees.

When unsure, implement the smaller option that still satisfies `PRD.md` / `SRS.md`.

---

## 14. Recommended Two-Hour Development Sequence

| Slice | Time (approx.) | Work | Verify |
| --- | --- | --- | --- |
| 0 | 10 min | Docker Compose PostgreSQL; schemas; confirm DB reachable | `docker compose up -d` |
| 1 | 25 min | Scaffold Account Service; entity/repo; create + get account | `mvn test` (create/get) |
| 2 | 20 min | Deposit endpoint + validation + tests | `mvn test` (Account Service) |
| 3 | 15 min | Error handling polish; unique account number if time | Account Service tests green |
| 4 | 25 min | Scaffold Transaction Service; REST client to Account Service; create transaction | `mvn test` |
| 5 | 15 min | List transactions by account + tests | Transaction tests green |
| 6 | 10 min | Manual smoke of primary workflow; align README notes if present | curl/Postman checklist |

**Gate:** Do not start Slice 4 until Account Service builds and basic API tests pass.

---

## 15. Unresolved / Defaulted Choices

If not yet decided in code, use these defaults and record deviations in `DECISIONS.md`:

| Topic | Default |
| --- | --- |
| Build tool | Maven |
| HTTP client | Spring `RestClient` (Boot 3.2+) or `RestTemplate` if needed |
| Duplicate account number | HTTP `409 Conflict` if easy; otherwise `400` |
| Timestamp type | `Instant` or `LocalDateTime` — pick one per service and stay consistent |
| Schema bootstrap | `spring.jpa.hibernate.ddl-auto` for MVP **or** Flyway — prefer Flyway if time allows |

---

## Document Control

Update this contract when ports, endpoints, or hard constraints change. Keep `PRD.md`, `SRS.md`, and `DECISIONS.md` in sync.
