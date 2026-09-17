# FinTrack — Software Requirements Specification (SRS)

| Field | Value |
| --- | --- |
| Product | FinTrack |
| Document | Software Requirements Specification |
| Version | 1.0.0 |
| Status | Draft — documentation foundation |
| Related documents | `PRD.md`, `CODEX.md`, `DECISIONS.md` |
| Last updated | 2026-09-17 |

---

## 1. Purpose and Scope

### 1.1 Purpose

This SRS defines the software requirements for the FinTrack educational banking MVP: two Spring Boot microservices, PostgreSQL persistence, REST APIs, validation, error handling, and testing expectations.

### 1.2 Scope

**In scope:** Account Service, Transaction Service, PostgreSQL with separate schemas, the endpoints listed in Section 8, and automated tests for core behavior.

**Out of scope:** Frontend, authentication, Kafka, Kubernetes, distributed transactions, and advanced banking features. See `PRD.md` Section 7.

**Disclaimer:** FinTrack is an educational system, not a production banking platform.

---

## 2. System Overview

FinTrack exposes REST APIs for personal banking–style operations:

1. Create and retrieve accounts; maintain balances; accept deposits (Account Service).
2. Record deposits/withdrawals and retrieve history; validate account existence via Account Service (Transaction Service).

Clients interact with each service independently. The primary workflow is:

**Create account → Deposit money → Record transaction → View balance and transaction history.**

---

## 3. Architecture

Two independently runnable Spring Boot services:

| Service | Port | Responsibility |
| --- | --- | --- |
| Account Service | **8081** | Accounts, balances, deposits |
| Transaction Service | **8082** | Transaction records; account existence check via REST |

```
┌─────────────┐     REST      ┌────────────────────┐
│   Client    │──────────────▶│  Account Service   │
│             │               │  :8081             │
│             │──────────────▶│  Transaction Svc   │
│             │               │  :8082             │
└─────────────┘               └─────────┬──────────┘
                                        │ REST (validate account)
                                        ▼
                              ┌────────────────────┐
                              │  Account Service   │
                              └─────────┬──────────┘
                                        │
              ┌─────────────────────────┴─────────────────────────┐
              │              PostgreSQL (one container)           │
              │   schema: account_service | transaction_service   │
              └───────────────────────────────────────────────────┘
```

Architecture decisions are recorded in `DECISIONS.md` (ADR-001 through ADR-006).

---

## 4. Service Responsibilities

### 4.1 Account Service (port 8081)

- Create accounts.
- Retrieve accounts by id.
- Maintain account balances.
- Accept deposit requests and update balances.

**Does not:** store transaction history or call Transaction Service in the MVP.

### 4.2 Transaction Service (port 8082)

- Record transactions of type `DEPOSIT` or `WITHDRAWAL`.
- Retrieve transaction history by `accountId`.
- Validate that the referenced account exists by calling Account Service (`GET /api/accounts/{id}`).

**Does not:** update account balances in the MVP (balance changes remain Account Service’s responsibility).

---

## 5. Technology Stack

| Layer | Choice |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot (Web, Data JPA, Validation) |
| Build | Maven (preferred) or Gradle — pick one and keep consistent |
| Database | PostgreSQL |
| Local DB | Docker Compose recommended |
| Money type | `java.math.BigDecimal` |
| HTTP client (service-to-service) | Spring `RestClient` / `WebClient` / `RestTemplate` (choose one) |
| Tests | JUnit 5, Spring Boot Test, MockMvc or TestRestTemplate |

Do **not** add Kafka, Kubernetes, a frontend, or JWT authentication for the MVP.

---

## 6. Functional Requirements

| ID | Requirement | Service |
| --- | --- | --- |
| SRS-FR-1 | Create account with `accountNumber`, `accountHolder`; initial balance `0.00`. | Account |
| SRS-FR-2 | Retrieve account by id; return `404` if not found. | Account |
| SRS-FR-3 | Deposit positive amount; update balance; return updated account. | Account |
| SRS-FR-4 | Reject deposit when account missing (`404`) or amount invalid (`400`). | Account |
| SRS-FR-5 | Create transaction with `accountId`, `type`, `amount` after account validation. | Transaction |
| SRS-FR-6 | Reject transaction when account does not exist (`404` or mapped client error). | Transaction |
| SRS-FR-7 | List transactions for an account id (empty list if none). | Transaction |
| SRS-FR-8 | Use `BigDecimal` for balances and amounts. | Both |

---

## 7. Non-Functional Requirements

| ID | Requirement |
| --- | --- |
| SRS-NFR-1 | Each service starts independently and binds to its assigned port. |
| SRS-NFR-2 | Service data ownership is enforced via separate PostgreSQL schemas. |
| SRS-NFR-3 | Request validation occurs at the API boundary (Bean Validation). |
| SRS-NFR-4 | Errors return structured JSON with HTTP status and a clear message. |
| SRS-NFR-5 | Core API paths are covered by automated tests. |
| SRS-NFR-6 | Implementation remains within approximately two hours for the MVP. |
| SRS-NFR-7 | Documentation stays aligned with ports, endpoints, and data model. |

---

## 8. API Specifications

Only the endpoints below are required for the MVP. Do not invent additional endpoints unless marked as future scope.

### 8.1 Account Service — Base URL `http://localhost:8081`

#### POST `/api/accounts`

Creates an account.

**Request body:**

```json
{
  "accountNumber": "ACC-1001",
  "accountHolder": "Ada Lovelace"
}
```

**Success response:** `201 Created`

```json
{
  "id": 1,
  "accountNumber": "ACC-1001",
  "accountHolder": "Ada Lovelace",
  "balance": 0.00,
  "createdAt": "2026-09-17T10:00:00"
}
```

| Status | When |
| --- | --- |
| 201 | Account created |
| 400 | Missing/blank fields or validation failure |

---

#### GET `/api/accounts/{id}`

Retrieves an account by id.

**Success response:** `200 OK` (same shape as create response).

| Status | When |
| --- | --- |
| 200 | Account found |
| 404 | Account does not exist |
| 400 | Invalid id format (if applicable) |

---

#### POST `/api/accounts/{id}/deposit`

Deposits money into an account.

**Request body:**

```json
{
  "amount": 100.50
}
```

**Success response:** `200 OK` with updated account (balance increased).

| Status | When |
| --- | --- |
| 200 | Deposit applied |
| 400 | Amount null, ≤ 0, or invalid scale |
| 404 | Account does not exist |

---

### 8.2 Transaction Service — Base URL `http://localhost:8082`

#### POST `/api/transactions`

Records a transaction after validating the account via Account Service.

**Request body:**

```json
{
  "accountId": 1,
  "type": "DEPOSIT",
  "amount": 100.50
}
```

`type` allowed values: `DEPOSIT`, `WITHDRAWAL`.

**Success response:** `201 Created`

```json
{
  "id": 10,
  "accountId": 1,
  "type": "DEPOSIT",
  "amount": 100.50,
  "createdAt": "2026-09-17T10:05:00"
}
```

| Status | When |
| --- | --- |
| 201 | Transaction recorded |
| 400 | Invalid type, amount ≤ 0, or missing fields |
| 404 | Referenced account does not exist (Account Service) |
| 503 | Account Service unreachable (recommended mapping) |

---

#### GET `/api/transactions/account/{accountId}`

Returns transaction history for an account.

**Success response:** `200 OK`

```json
[
  {
    "id": 10,
    "accountId": 1,
    "type": "DEPOSIT",
    "amount": 100.50,
    "createdAt": "2026-09-17T10:05:00"
  }
]
```

| Status | When |
| --- | --- |
| 200 | List returned (may be empty) |
| 400 | Invalid `accountId` format (if applicable) |

**Note:** MVP may return an empty list when no transactions exist, without requiring a prior existence check on every list call. Account existence validation is required on **create** (`POST /api/transactions`).

---

### 8.3 Future scope (not implemented)

Examples only — do not implement in MVP:

- `POST /api/accounts/{id}/withdraw`
- `POST /api/transfers`
- Pagination query params on transaction list

---

## 9. Database Requirements

### 9.1 Deployment model

- One PostgreSQL container/instance.
- Separate schemas for service ownership (recommended names):
  - `account_service`
  - `transaction_service`

### 9.2 Accounts table (Account Service schema)

| Column | Type guidance | Notes |
| --- | --- | --- |
| `id` | BIGSERIAL / BIGINT PK | Surrogate key |
| `account_number` | VARCHAR, unique | Business identifier |
| `account_holder` | VARCHAR | Required |
| `balance` | NUMERIC(19, 2) | `BigDecimal`; non-negative in MVP after deposits from zero |
| `created_at` | TIMESTAMP | Set on insert |

**Index guidance:** unique index on `account_number`; primary key on `id`.

### 9.3 Transactions table (Transaction Service schema)

| Column | Type guidance | Notes |
| --- | --- | --- |
| `id` | BIGSERIAL / BIGINT PK | Surrogate key |
| `account_id` | BIGINT | Logical reference to Account Service account id (no cross-schema FK required) |
| `type` | VARCHAR | `DEPOSIT` or `WITHDRAWAL` |
| `amount` | NUMERIC(19, 2) | `BigDecimal`; must be > 0 |
| `created_at` | TIMESTAMP | Set on insert |

**Index guidance:** index on `account_id` to support history queries.

### 9.4 Ownership rule

Account Service must not write to the transaction schema. Transaction Service must not write to the account schema. Cross-service reads of account state happen only via REST.

---

## 10. Data Validation Rules

| Field | Rules |
| --- | --- |
| `accountNumber` | Required, non-blank |
| `accountHolder` | Required, non-blank |
| Deposit `amount` | Required, > 0, max 2 decimal places recommended |
| Transaction `accountId` | Required, positive |
| Transaction `type` | Required; `DEPOSIT` or `WITHDRAWAL` |
| Transaction `amount` | Required, > 0, max 2 decimal places recommended |

Use `BigDecimal` with an explicit scale (e.g., 2) and a defined rounding mode (e.g., `HALF_UP`) when normalizing monetary input.

---

## 11. Error Handling

| Scenario | Expected behavior |
| --- | --- |
| Bean validation failure | `400` with field-level or message summary |
| Account not found | `404` |
| Invalid monetary amount | `400` |
| Duplicate `account_number` (if unique constraint) | `409` preferred, or `400` if simpler for MVP — document chosen mapping in code |
| Account Service down during transaction create | `503` (or `502`) with clear message |
| Unhandled exception | `500` with generic message (no stack traces to clients) |

Prefer a shared-style error body within each service, for example:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Account not found: 99"
}
```

---

## 12. Inter-Service Communication

| Aspect | Requirement |
| --- | --- |
| Style | Synchronous REST (ADR-004) |
| Caller | Transaction Service |
| Callee | Account Service `GET /api/accounts/{id}` |
| Success | HTTP 200 → proceed to persist transaction |
| Not found | HTTP 404 → reject transaction create |
| Transport failure | Map to `503`/`502`; do not silently succeed |

Account Service does not call Transaction Service in the MVP.

---

## 13. Testing Requirements

| ID | Requirement |
| --- | --- |
| SRS-T-1 | Account Service: tests for create, get, deposit (success and validation/not-found). |
| SRS-T-2 | Do not implement Transaction Service until Account Service builds and basic API tests pass. |
| SRS-T-3 | Transaction Service: tests for create (valid account, missing account, invalid payload) and list by account. |
| SRS-T-4 | Prefer focused unit/slice tests; full multi-service E2E is optional for MVP. |
| SRS-T-5 | Do not claim tests exist before they are written. |

---

## 14. Deployment Requirements

| ID | Requirement |
| --- | --- |
| SRS-D-1 | Provide Docker Compose (or equivalent) for PostgreSQL for local runs. |
| SRS-D-2 | Each service configurable via `application.properties` / `application.yml` (port, datasource, Account Service base URL). |
| SRS-D-3 | MVP deployment target is local developer machine; cloud/K8s is out of scope. |

---

## 15. Security Considerations

MVP security is intentionally minimal:

- No authentication or authorization.
- Bind to localhost for demos when practical.
- Do not log full request bodies containing sensitive personal data in production-like environments (educational caution).
- Do not claim PCI, regulatory, or production banking security compliance.

Future: JWT or OAuth2, HTTPS termination, rate limiting.

---

## 16. Known Limitations

| Limitation | Detail |
| --- | --- |
| Consistency | Balance update and transaction record are separate operations (ADR-005). Partial failure can leave balance and history out of sync. |
| Withdrawals | Transaction Service can record a `WITHDRAWAL`, but Account Service MVP deposit API does not reduce balance on withdrawal. |
| Isolation | Separate schemas, not separate database instances. |
| Auth | Open APIs; unsuitable for real financial use. |
| Scale | Not designed for high throughput or multi-region operation. |

These limitations are accepted for the two-hour educational MVP (ADR-006).

---

## Document Control

Keep this SRS aligned with `PRD.md` acceptance criteria and `CODEX.md` development rules. Record material architecture changes in `DECISIONS.md`.
