# FinTrack — Product Requirements Document (PRD)

| Field | Value |
| --- | --- |
| Product | FinTrack |
| Document | Product Requirements Document |
| Version | 1.0.0 |
| Status | Draft — documentation foundation |
| Audience | Portfolio reviewers; Cognizant Jr. Software Engineer (Job ID 00068949144) |
| Related documents | `SRS.md`, `CODEX.md`, `DECISIONS.md` |
| Last updated | 2026-09-17 |

---

## 1. Product Overview

FinTrack is an **educational personal banking backend** built as a small Java Spring Boot microservices MVP. It demonstrates account creation, deposits, transaction recording, balance retrieval, and transaction history through REST APIs backed by PostgreSQL.

**Important:** FinTrack is **not** a real banking product. It does not move real money, connect to payment networks, or provide production-grade financial guarantees. It exists to demonstrate software engineering skills relevant to junior backend roles.

---

## 2. Problem Statement

Junior backend candidates need a compact, credible project that shows:

- Microservice boundaries and REST communication.
- Relational data modeling with PostgreSQL.
- Correct handling of monetary values.
- Maintainable Spring Boot structure (DTOs, services, repositories, tests).

Many sample projects are either too large to finish or too trivial to discuss in interviews. FinTrack fills that gap with a deliberately small, completable MVP.

---

## 3. Product Goals

| ID | Goal |
| --- | --- |
| PG-1 | Demonstrate a working two-service Spring Boot backend within ~two hours of implementation. |
| PG-2 | Show clear service ownership: accounts/balances vs. transaction history. |
| PG-3 | Practice REST API design, validation, and basic error handling. |
| PG-4 | Use PostgreSQL with relational modeling suitable for SQL discussion (joins, indexes). |
| PG-5 | Produce documentation and tests that support portfolio and interview discussion. |

---

## 4. Target Users

| User | Description |
| --- | --- |
| Developer / learner | Primary user of the APIs during local development and demos. |
| Portfolio reviewer / interviewer | Evaluates design decisions, code quality, and trade-offs. |

There are no end-customer banking users in scope for the MVP.

---

## 5. User Stories

| ID | As a… | I want to… | So that… |
| --- | --- | --- | --- |
| US-1 | Developer | create an account with a holder name and account number | I can start tracking a balance |
| US-2 | Developer | deposit money into an account | the account balance increases |
| US-3 | Developer | record a deposit or withdrawal transaction | I have an audit trail of money movement |
| US-4 | Developer | retrieve an account by id | I can view holder details and current balance |
| US-5 | Developer | retrieve transactions for an account | I can review transaction history |

---

## 6. MVP Scope

### In scope

- Account Service (port **8081**): create account, get account, deposit.
- Transaction Service (port **8082**): record deposit/withdrawal, list transactions by account.
- One PostgreSQL instance with **separate schemas** for service data ownership.
- Synchronous REST call from Transaction Service to Account Service to validate account existence.
- Automated tests for core API behavior.
- Documentation in `PRD.md`, `SRS.md`, `CODEX.md`, and `DECISIONS.md`.

### Primary user workflow

```
Create account → Deposit money → Record transaction → View balance and transaction history
```

---

## 7. Explicitly Out of Scope

The following are **not** part of the MVP:

- Frontend / UI.
- JWT or other authentication and authorization.
- Kafka, message queues, or event-driven flows.
- Kubernetes or cloud orchestration.
- Transfers between accounts.
- Interest, fees, overdraft products, or loan features.
- Distributed transactions / saga / two-phase commit.
- Production-grade audit, compliance, or PCI controls.
- Real bank integrations, payment rails, or KYC.

---

## 8. User Workflows

### WF-1: Create account and view balance

1. Client sends `POST /api/accounts` to Account Service with holder name and account number.
2. System creates the account with an initial balance of `0.00`.
3. Client retrieves the account via `GET /api/accounts/{id}` and observes the balance.

### WF-2: Deposit money

1. Client sends `POST /api/accounts/{id}/deposit` with a positive amount.
2. Account Service increases the account balance.
3. Client may optionally record a matching deposit via Transaction Service (see WF-3).

### WF-3: Record transaction and view history

1. Client ensures the account exists (Account Service).
2. Client sends `POST /api/transactions` with `accountId`, `type` (`DEPOSIT` or `WITHDRAWAL`), and `amount`.
3. Transaction Service validates account existence via Account Service REST call.
4. On success, the transaction is stored.
5. Client retrieves history via `GET /api/transactions/account/{accountId}`.

**Consistency note (MVP):** Balance updates (Account Service) and transaction records (Transaction Service) are **separate operations**. The MVP does not guarantee atomic consistency between them (see ADR-005 in `DECISIONS.md`).

---

## 9. Functional Requirements

| ID | Requirement |
| --- | --- |
| FR-1 | The system shall allow creation of an account with `account_number`, `account_holder`, and an initial balance of zero. |
| FR-2 | The system shall allow retrieval of an account by its identifier, including current balance. |
| FR-3 | The system shall allow depositing a positive monetary amount into an existing account. |
| FR-4 | The system shall allow recording a transaction of type `DEPOSIT` or `WITHDRAWAL` for an existing account. |
| FR-5 | Before recording a transaction, Transaction Service shall verify that the referenced account exists via Account Service. |
| FR-6 | The system shall allow retrieval of all transactions for a given account id. |
| FR-7 | Monetary amounts shall be represented using decimal precision suitable for currency (implemented with `BigDecimal`). |

---

## 10. Non-Functional Product Requirements

| ID | Requirement |
| --- | --- |
| NFR-1 | Services shall be independently runnable Spring Boot applications. |
| NFR-2 | Local development shall use Docker for PostgreSQL where practical. |
| NFR-3 | Core happy-path and validation failure paths shall be covered by automated tests. |
| NFR-4 | APIs shall return clear HTTP status codes and error messages for validation and not-found cases. |
| NFR-5 | Documentation shall remain consistent with implemented ports, endpoints, and data model. |
| NFR-6 | The MVP implementation effort shall remain achievable in approximately two hours. |

---

## 11. Success Criteria

- A reviewer can start PostgreSQL, run both services, and exercise the primary workflow with HTTP clients (e.g., curl or Postman).
- Account and transaction data persist in PostgreSQL under separate schemas.
- Automated tests for Account Service basic APIs pass before Transaction Service work begins.
- Documentation accurately describes what was built and what was deferred.

---

## 12. MVP Acceptance Criteria

| ID | Criterion |
| --- | --- |
| AC-1 | `POST /api/accounts` creates an account and returns `201` with account details. |
| AC-2 | `GET /api/accounts/{id}` returns `200` for an existing account and `404` when missing. |
| AC-3 | `POST /api/accounts/{id}/deposit` increases balance for valid deposits and rejects invalid amounts. |
| AC-4 | `POST /api/transactions` records a transaction when the account exists and rejects when it does not. |
| AC-5 | `GET /api/transactions/account/{accountId}` returns the list of transactions for that account. |
| AC-6 | Account Service runs on port **8081**; Transaction Service runs on port **8082**. |
| AC-7 | Monetary fields use `BigDecimal` (or equivalent precise decimal mapping) end to end. |
| AC-8 | No out-of-scope infrastructure (Kafka, Kubernetes, frontend, JWT) is required to demo the MVP. |

---

## 13. Future Enhancements

Items below are **not** committed for the MVP. They may appear in later iterations:

- Account-to-account transfers.
- Stronger consistency between balance updates and transaction ledger (sagas or transactional outbox).
- Authentication and authorization.
- Pagination, filtering, and sorting for transaction history.
- Withdrawal that also decreases account balance in a coordinated flow.
- API gateway and centralized configuration.
- Observability (structured logging, metrics, tracing).

---

## Document Control

Changes to product scope must be reflected in `SRS.md`, `CODEX.md`, and `DECISIONS.md` in the same change set whenever practical.
