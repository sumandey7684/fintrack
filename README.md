# FinTrack

FinTrack is an educational personal banking backend MVP built with Java 17, Spring Boot, PostgreSQL, and REST APIs.

The project demonstrates account management, balance updates, transaction recording, inter-service communication, validation, and automated testing through two independently runnable microservices.

> **Note:** FinTrack is a portfolio project, not a production banking system. It does not implement authentication, payment-network integration, distributed transactions, or financial regulatory compliance.

## Features

* Create and retrieve accounts
* Maintain account balances using `BigDecimal`
* Deposit money into accounts
* Record deposit and withdrawal transactions
* Retrieve transaction history by account
* Validate account existence through REST communication
* Validate request payloads and return structured HTTP errors
* Use separate PostgreSQL schemas for service data ownership
* Run automated unit and controller tests

## Architecture

FinTrack consists of two Spring Boot services connected to one PostgreSQL database.

```text
Client
  │
  ├── REST ──▶ Account Service (:8081)
  │              └── account_service schema
  │
  └── REST ──▶ Transaction Service (:8082)
                 ├── REST validation through Account Service
                 └── transaction_service schema
```

| Component           |   Port | Responsibility                        |
| ------------------- | -----: | ------------------------------------- |
| Account Service     | `8081` | Accounts, balances, and deposits      |
| Transaction Service | `8082` | Transaction recording and history     |
| PostgreSQL          | `5433` | Shared database with separate schemas |

The PostgreSQL host port is `5433` to avoid conflicts with local PostgreSQL installations using port `5432`.

## Technology Stack

* Java 17
* Spring Boot 3.4
* Spring Web
* Spring Data JPA
* Bean Validation
* Maven
* PostgreSQL 16
* Docker Compose
* JUnit 5
* MockMvc
* Mockito
* H2 for tests

## Repository Structure

```text
FinTrack/
├── account-service/
├── transaction-service/
├── docker/
│   └── init-db.sql
├── docs/
│   └── mvp-consistency.md
├── docker-compose.yml
├── PRD.md
├── SRS.md
├── CODEX.md
├── DECISIONS.md
└── README.md
```

## API Overview

### Account Service

Base URL:

```text
http://localhost:8081
```

| Method | Endpoint                     | Description                          |
| ------ | ---------------------------- | ------------------------------------ |
| `POST` | `/api/accounts`              | Create an account                    |
| `GET`  | `/api/accounts/{id}`         | Retrieve an account                  |
| `POST` | `/api/accounts/{id}/deposit` | Deposit money and update the balance |

### Transaction Service

Base URL:

```text
http://localhost:8082
```

| Method | Endpoint                                | Description                    |
| ------ | --------------------------------------- | ------------------------------ |
| `POST` | `/api/transactions`                     | Record a deposit or withdrawal |
| `GET`  | `/api/transactions/account/{accountId}` | Retrieve transaction history   |

Supported transaction types:

```text
DEPOSIT
WITHDRAWAL
```

Common response codes:

| Status | Meaning                        |
| -----: | ------------------------------ |
|  `200` | Request completed successfully |
|  `201` | Resource created               |
|  `400` | Invalid request                |
|  `404` | Resource or account not found  |
|  `409` | Duplicate account number       |
|  `503` | Account Service unavailable    |

## Local Setup

### Prerequisites

* Java 17 or later
* Maven 3.9 or later
* Docker and Docker Compose

### 1. Start PostgreSQL

From the project root:

```bash
docker compose up -d
```

Verify the container:

```bash
docker compose ps
```

### 2. Start Account Service

Open a terminal:

```bash
cd account-service
mvn spring-boot:run
```

The service runs on:

```text
http://localhost:8081
```

### 3. Start Transaction Service

Open a second terminal:

```bash
cd transaction-service
mvn spring-boot:run
```

The service runs on:

```text
http://localhost:8082
```

## Example Requests

Create an account:

```bash
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"ACC-1001","accountHolder":"Ada Lovelace"}'
```

Deposit money:

```bash
curl -X POST http://localhost:8081/api/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount":1000.00}'
```

Record a transaction:

```bash
curl -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId":1,"type":"DEPOSIT","amount":1000.00}'
```

Retrieve transaction history:

```bash
curl http://localhost:8082/api/transactions/account/1
```

## Testing

Automated tests and package builds were completed for both services:

```bash
cd account-service
mvn test
mvn package

cd ../transaction-service
mvn test
mvn package
```

The MVP verification covered:

* Account creation, retrieval, and deposits
* Account validation and error responses
* Duplicate account handling
* Transaction recording and history retrieval
* Missing-account handling
* Account Service failure mapping to HTTP `503`
* PostgreSQL startup through Docker Compose

## Data Ownership

The services use separate schemas within the same PostgreSQL database:

| Schema                | Service             | Table          |
| --------------------- | ------------------- | -------------- |
| `account_service`     | Account Service     | `accounts`     |
| `transaction_service` | Transaction Service | `transactions` |

The Transaction Service stores the logical `accountId` reference but does not directly access the Account Service schema. Account validation is performed through REST communication.

## Known Limitations

* No authentication or authorization
* No HTTPS enforcement
* No distributed transaction management
* Balance updates and transaction records are separate operations
* Withdrawal records do not automatically reduce the account balance
* Default database credentials are intended for local development only
* Not designed for production banking workloads

## Future Improvements

* Implement coordinated deposit and withdrawal workflows
* Add stronger consistency using an outbox or saga pattern
* Add authentication and authorization
* Introduce account-to-account transfers
* Add pagination and filtering for transaction history
* Add database migrations using Flyway
* Improve monitoring and observability

## Related Documentation

* [Product Requirements](PRD.md)
* [Software Requirements](SRS.md)
* [Architecture Decisions](DECISIONS.md)
* [MVP Consistency Notes](docs/mvp-consistency.md)
