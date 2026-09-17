# FinTrack

Educational personal banking backend MVP built with **Java 17** and **Spring Boot** microservices.

FinTrack demonstrates account management, deposits, transaction recording, and history retrieval over REST APIs backed by PostgreSQL. It is designed as a compact portfolio project for junior backend / software engineering roles—not a real bank.

**Status:** MVP complete (Account Service + Transaction Service verified)

> This is an **educational** system. It does **not** provide production-grade banking security, atomic distributed transactions, payment-network integration, or financial/regulatory compliance.

---

## Key Features

- Create and retrieve bank-style accounts with decimal balances
- Deposit money and update account balance (`BigDecimal`)
- Record `DEPOSIT` / `WITHDRAWAL` transactions as a separate ledger
- Retrieve transaction history by account
- Synchronous REST validation of account existence between services
- Bean Validation, structured HTTP errors, and automated tests
- One PostgreSQL instance with **separate schemas** for service data ownership

---

## Architecture Overview

Two independently runnable Spring Boot services share one PostgreSQL database (separate schemas):

```
Client
  │
  ├── REST ──▶ Account Service (:8081)
  │                 │
  │                 └── schema: account_service
  │
  └── REST ──▶ Transaction Service (:8082)
                    │
                    ├── REST GET /api/accounts/{id} ──▶ Account Service
                    └── schema: transaction_service
```

| Service | Port | Responsibility |
| --- | --- | --- |
| **Account Service** | `8081` | Accounts, balances, deposits |
| **Transaction Service** | `8082` | Transaction history; validates accounts via Account Service |
| **PostgreSQL** | host `5433` → container `5432` | Shared DB, separate schemas |

Host port **5433** is used because **5432** is often already occupied by a local PostgreSQL installation.

Architecture decisions are recorded in [`DECISIONS.md`](DECISIONS.md) (ADR-001 … ADR-006). Product and software requirements: [`PRD.md`](PRD.md), [`SRS.md`](SRS.md). Development contract: [`CODEX.md`](CODEX.md).

---

## Technology Stack

| Layer | Choice |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.4.x (Web, Data JPA, Validation) |
| Build | Maven |
| Database | PostgreSQL 16 (Docker Compose) |
| Money | `java.math.BigDecimal` / `NUMERIC(19,2)` |
| Timestamps | `java.time.Instant` |
| HTTP (service-to-service) | Spring `RestClient` |
| Tests | JUnit 5, MockMvc, Mockito, H2 (test profile) |

---

## Repository Structure

```
FinTrack/
├── PRD.md / SRS.md / CODEX.md / DECISIONS.md
├── README.md
├── docker-compose.yml
├── docker/init-db.sql
├── docs/mvp-consistency.md
├── account-service/
└── transaction-service/
```

---

## Service Responsibilities

### Account Service (`account-service`)

- Create account (initial balance `0.00`)
- Get account by id
- Deposit positive amounts and update balance
- **Does not** store transaction history

### Transaction Service (`transaction-service`)

- Record `DEPOSIT` or `WITHDRAWAL` after confirming the account exists
- List transactions for an account
- **Does not** update account balances (deposit API remains the balance owner)

**Consistency note (ADR-005):** balance updates and transaction records are separate operations. See [`docs/mvp-consistency.md`](docs/mvp-consistency.md).

---

## API Documentation

### Account Service — `http://localhost:8081`

#### `POST /api/accounts` → `201 Created`

```bash
curl -s -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d "{\"accountNumber\":\"ACC-1001\",\"accountHolder\":\"Ada Lovelace\"}"
```

```json
{
  "id": 1,
  "accountNumber": "ACC-1001",
  "accountHolder": "Ada Lovelace",
  "balance": 0.00,
  "createdAt": "2026-09-17T13:53:21.826540200Z"
}
```

#### `GET /api/accounts/{id}` → `200 OK` / `404`

#### `POST /api/accounts/{id}/deposit` → `200 OK`

```bash
curl -s -X POST http://localhost:8081/api/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -d "{\"amount\":100.50}"
```

| Status | Meaning |
| --- | --- |
| 201 | Account created |
| 200 | Get / deposit success |
| 400 | Validation failure (blank fields, non-positive amount) |
| 404 | Account not found |
| 409 | Duplicate account number |

### Transaction Service — `http://localhost:8082`

#### `POST /api/transactions` → `201 Created`

```bash
curl -s -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":1,\"type\":\"DEPOSIT\",\"amount\":1000.00}"
```

```json
{
  "id": 1,
  "accountId": 1,
  "type": "DEPOSIT",
  "amount": 1000.00,
  "createdAt": "2026-09-17T14:07:01.193402500Z"
}
```

`type` must be `DEPOSIT` or `WITHDRAWAL`.

#### `GET /api/transactions/account/{accountId}` → `200 OK`

Returns a list (may be empty). Account existence is validated on **create**, not required on list.

| Status | Meaning |
| --- | --- |
| 201 | Transaction recorded |
| 200 | History retrieved |
| 400 | Invalid payload (amount ≤ 0, invalid type, non-positive accountId) |
| 404 | Referenced account does not exist |
| 503 | Account Service unreachable or returned a server/transport failure |

---

## Database Schema and Data Ownership

One database: `fintrack`. Schemas:

| Schema | Owner service | Table |
| --- | --- | --- |
| `account_service` | Account Service | `accounts` |
| `transaction_service` | Transaction Service | `transactions` |

**Accounts:** `id`, `account_number` (unique), `account_holder`, `balance NUMERIC(19,2)`, `created_at`  
**Transactions:** `id`, `account_id` (logical reference, no cross-schema FK), `type`, `amount NUMERIC(19,2)`, `created_at` (indexed on `account_id`)

Services must not write to each other’s schemas. Cross-service account checks use REST only.

---

## Local Setup

### Prerequisites

- Java 17+
- Maven 3.9+ (or use `account-service/mvnw` where present)
- Docker + Docker Compose

### 1. Start PostgreSQL

```bash
docker compose up -d
```

Confirm health and schemas:

```bash
docker compose ps
docker exec fintrack-postgres psql -U fintrack -d fintrack -c "\dn"
```

### 2. Configuration

Default local credentials (educational only):

| Setting | Value |
| --- | --- |
| JDBC URL | `jdbc:postgresql://localhost:5433/fintrack` |
| Username | `fintrack` |
| Password | `fintrack` |
| Account schema | `account_service` |
| Transaction schema | `transaction_service` |

### Environment variables / properties

| Variable / property | Default | Used by |
| --- | --- | --- |
| `FINTRACK_ACCOUNT_SERVICE_BASE_URL` | `http://localhost:8081` | Transaction Service |
| `fintrack.account-service.base-url` | same | Transaction Service (`application.properties`) |

Optional overrides example:

```bash
# Windows PowerShell
$env:FINTRACK_ACCOUNT_SERVICE_BASE_URL="http://localhost:8081"
```

---

## How to Run Both Services

From two terminals (PostgreSQL must be up):

```bash
# Terminal 1 — Account Service
cd account-service
mvn spring-boot:run

# Terminal 2 — Transaction Service
cd transaction-service
mvn spring-boot:run
```

- Account Service: http://localhost:8081  
- Transaction Service: http://localhost:8082  

---

## How to Run Tests

```bash
cd account-service
mvn test
mvn package

cd ../transaction-service
mvn test
mvn package
```

Unit/API tests use an in-memory H2 profile under `src/test/resources`.

---

## Integration Workflow

Primary demo path:

1. Create account (Account Service)
2. Deposit money (Account Service) — updates **balance**
3. Record matching deposit transaction (Transaction Service) — updates **history**
4. View balance (`GET /api/accounts/{id}`) and history (`GET /api/transactions/account/{accountId}`)
5. Optionally record a `WITHDRAWAL` in Transaction Service (history only; MVP does not auto-decrease balance)

---

## Sample curl Commands

```bash
# Create account
curl -s -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d "{\"accountNumber\":\"ACC-2001\",\"accountHolder\":\"Grace Hopper\"}"

# Deposit
curl -s -X POST http://localhost:8081/api/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -d "{\"amount\":500.00}"

# Record deposit transaction
curl -s -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":1,\"type\":\"DEPOSIT\",\"amount\":500.00}"

# Record withdrawal transaction (history only)
curl -s -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":1,\"type\":\"WITHDRAWAL\",\"amount\":50.00}"

# History
curl -s http://localhost:8082/api/transactions/account/1

# Missing account → 404
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d "{\"accountId\":999999,\"type\":\"DEPOSIT\",\"amount\":10.00}"
```

---

## Testing and Verification Results

Verified during MVP implementation:

| Check | Result |
| --- | --- |
| Account Service `mvn test` / `mvn package` | Pass |
| Transaction Service `mvn test` / `mvn package` | Pass |
| Docker Compose PostgreSQL on host `5433` | Healthy; schemas present |
| Account API smoke (create / get / deposit / 409 / 404 / 400) | Pass |
| Transaction API smoke (deposit tx / history / withdrawal / missing account → 404) | Pass |
| Account Service unreachable → **503** | Covered by unit/controller tests |

---

## Known Limitations

- Educational MVP only — no authentication, authorization, or HTTPS enforcement
- No distributed transactions; balance and history can diverge (ADR-005)
- Withdrawal transactions do not reduce Account Service balance in the MVP
- Separate schemas, not separate database instances
- Local default DB credentials are for demos only
- Not designed for high throughput, multi-region, or production banking controls

---

## Future Improvements

- Coordinated deposit/withdraw flows (or outbox/saga) for stronger consistency
- Account-to-account transfers
- Authentication (e.g., JWT) and API gateway
- Pagination/filtering for transaction history
- Flyway migrations and richer observability

---

## Learning Outcomes

This project practices skills relevant to Java backend and software engineering roles:

- Spring Boot microservice structure (controller → service → repository + DTOs)
- REST API design, validation, and HTTP status mapping
- PostgreSQL relational modeling, schemas, and indexing
- Precise monetary handling with `BigDecimal`
- Synchronous inter-service REST and failure mapping (`404` / `503`)
- Automated testing with JUnit 5 and Spring Boot Test
- Explicit architecture trade-offs documented as ADRs

---

## Related Documents

| Document | Purpose |
| --- | --- |
| [PRD.md](PRD.md) | Product requirements |
| [SRS.md](SRS.md) | Software requirements & API contracts |
| [CODEX.md](CODEX.md) | Development instructions |
| [DECISIONS.md](DECISIONS.md) | Architecture decision records |
| [docs/mvp-consistency.md](docs/mvp-consistency.md) | Balance vs history limitation |
