# Enterprise Workflow Management System

A Spring Boot backend for an approval workflow: employees submit requests, their assigned manager approves or rejects them, and every state change is recorded and published to Kafka through a transactional outbox.

This is a hands-on learning project built to understand Spring Boot, Spring Security, event-driven design, and Kafka by implementing them end to end.

## Features

- **JWT authentication** — stateless login with BCrypt-hashed passwords and a `Bearer` token filter
- **Role-based access control** — `EMPLOYEE`, `MANAGER`, and `ADMIN` roles enforced at both the URL and method level
- **Request lifecycle** — submit, approve, reject, with a full per-request history trail
- **Search and pagination** — filter by status, title, and manager using JPA Specifications
- **Optimistic locking** — concurrent updates to the same request return `409 Conflict`
- **Transactional outbox** — lifecycle events are stored in the same transaction as the business change, then relayed to Kafka by a scheduler with retry, backoff, and stale-claim recovery
- **API docs and health** — Swagger UI and Spring Boot Actuator
- **Unit tests and CI** — JUnit 5 + Mockito, run on GitHub Actions

## Tech stack

| Area | Technology |
| --- | --- |
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator) |
| Database | PostgreSQL |
| Messaging | Apache Kafka (KRaft mode), Spring Kafka |
| Auth | JJWT 0.12 |
| API docs | springdoc-openapi |
| Build | Maven (wrapper included), Lombok |
| Testing | JUnit 5, Mockito, Spring Security Test |
| Infra | Docker, Docker Compose, GitHub Actions |

## Architecture

```
            ┌──────────────────────────── one DB transaction ───────────────────────────┐
Client ──▶  │ RequestController ─▶ RequestServiceImpl ─▶ requests + request_history     │
            │                                         └▶ outbox_event (status PENDING)  │
            └───────────────────────────────────────────────────────────────────────────┘
                                                              │
                                    OutboxScheduler (every 5s)│  claim batch with
                                                              ▼  FOR UPDATE SKIP LOCKED
                                                   OutboxProcessorService
                                                              │
                                                              ▼
                                              Kafka topic: request-lifecycle
                                                              │
                                                              ▼
                                         downstream consumers (e.g. notifications)
```

Writing the event to the `outbox_event` table inside the same transaction as the request change means an event is never lost if Kafka is down and never published for a change that rolled back.

### Outbox processing

Every 5 seconds the scheduler:

1. **Recovers stale events** — anything stuck in `PROCESSING` for more than 5 minutes is marked `FAILED` and scheduled for retry.
2. **Claims a batch** of up to 10 `PENDING` or due `FAILED` events, locking the rows with `FOR UPDATE SKIP LOCKED` so multiple instances don't pick up the same event.
3. **Publishes** each event to Kafka, keyed by request ID, and waits for the broker acknowledgement before marking it `PROCESSED`.

On failure an event is retried with backoff of 1, 5, then 15 minutes. After 3 retries it becomes `PERMANENTLY_FAILED` and stays there until an admin re-queues it via `POST /admin/outbox/{id}/retry`.

```
PENDING ─▶ PROCESSING ─▶ PROCESSED
               │
               ▼
            FAILED ──(retry with backoff, max 3)──▶ PERMANENTLY_FAILED ──(admin retry)──▶ PENDING
```

### Event payload

Published to the `request-lifecycle` topic as JSON:

| Field | Description |
| --- | --- |
| `requestId` | ID of the request (also the Kafka message key) |
| `title` | Request title |
| `actorEmail` | User who performed the action |
| `action` | `REQUEST_SUBMITTED`, `REQUEST_APPROVED`, or `REQUEST_REJECTED` |
| `employeeEmail` | Employee who owns the request |
| `status` | Request status after the action |
| `occurredAt` | Timestamp of the change |

Delivery is at-least-once, so consumers should be idempotent.

## Getting started

### Prerequisites

- JDK 21
- Docker and Docker Compose

### 1. Configure environment

Create a `.env` file in the project root (it is git-ignored):

```
POSTGRES_DB=workflow_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me

DB_USERNAME=postgres
DB_PASSWORD=change-me
```

`DB_USERNAME` / `DB_PASSWORD` must match `POSTGRES_USER` / `POSTGRES_PASSWORD`.

### 2a. Run everything in Docker

The image copies a pre-built jar, so build it first:

```bash
./mvnw clean package
```

```bash
docker compose up --build
```

This starts PostgreSQL, Kafka, and the application on port `8080`.

### 2b. Run the app locally against Docker infrastructure

Start only the dependencies:

```bash
docker compose up postgres kafka
```

Compose publishes PostgreSQL on host port `5433`, so point the app at it and start it (bash shown; set the same variables in your IDE run configuration or shell of choice):

```bash
DB_URL=jdbc:postgresql://localhost:5433/workflow_db DB_USERNAME=postgres DB_PASSWORD=change-me ./mvnw spring-boot:run
```

The schema is created automatically on startup (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Explore

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/workflow_db` | JDBC URL (default profile only) |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | — | Database password (required) |
| `SPRING_PROFILES_ACTIVE` | — | Set to `docker` to use the Compose service hostnames |

Kafka defaults to `localhost:9092` (`kafka:9092` under the `docker` profile).

## API

All endpoints except register, login, Swagger, and Actuator require an `Authorization: Bearer <token>` header.

### Auth

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | Public | Create a user |
| `POST` | `/auth/login` | Public | Returns a JWT (valid for 24 hours) |

### Requests

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/requests` | Employee, Manager | Submit a request to a manager |
| `GET` | `/requests/my` | Authenticated | Requests you submitted (paged, filterable) |
| `GET` | `/requests/manager` | Manager | Requests assigned to you (paged, filterable) |
| `GET` | `/requests` | Admin | All requests |
| `GET` | `/requests/{id}` | Owner, assigned manager, Admin | Request details |
| `GET` | `/requests/{id}/history` | Owner, assigned manager, Admin | Audit trail for a request |
| `PUT` | `/requests/{id}/approve` | Assigned manager | Approve a pending request |
| `PUT` | `/requests/{id}/reject` | Assigned manager | Reject a pending request |

Query parameters for the paged endpoints: `statuses`, `title`, `managerId` (`/requests/my` only), plus the standard `page`, `size`, and `sort` (default: 20 per page, newest first).

### Admin

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/admin/outbox/{id}/retry` | Admin | Re-queue a permanently failed outbox event |

### Example

Register a manager and an employee:

```bash
curl -X POST http://localhost:8080/auth/register -H "Content-Type: application/json" -d '{"name":"Maya","email":"maya@example.com","password":"password123","role":"ROLE_MANAGER"}'
```

```bash
curl -X POST http://localhost:8080/auth/register -H "Content-Type: application/json" -d '{"name":"Eli","email":"eli@example.com","password":"password123","role":"ROLE_EMPLOYEE"}'
```

Log in as the employee and submit a request (use the manager's `id` from the register response):

```bash
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"eli@example.com","password":"password123"}'
```

```bash
curl -X POST http://localhost:8080/requests -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"title":"Laptop upgrade","description":"Current laptop is 5 years old","managerId":1}'
```

Log in as the manager and approve it:

```bash
curl -X PUT http://localhost:8080/requests/1/approve -H "Authorization: Bearer <manager-token>"
```

### Error responses

Errors return JSON as `{"message": "..."}`; validation failures return `{"errors": {"field": "reason"}}`.

| Status | When |
| --- | --- |
| `400` | Validation failure or business rule violation (e.g. request already processed) |
| `401` | Invalid credentials |
| `403` | Not allowed to access or act on the resource |
| `404` | Resource not found |
| `409` | Duplicate resource, or the request was modified concurrently |

## Testing

```bash
./mvnw test
```

The suite is pure unit tests (JUnit 5 + Mockito) and needs no database or Kafka broker. It covers `JwtService`, `AuthServiceImpl`, `RequestServiceImpl`, `RequestMapper`, and `OutboxProcessorService`.

GitHub Actions runs `mvn -B verify` on every pull request and on pushes to `main` and `develop` (see [.github/workflows/ci.yml](.github/workflows/ci.yml)).

## Project structure

```
src/main/java/com/rishu/workflow
├── config          Security filter chain, beans, scheduling
├── controller      REST endpoints
├── dto             Request / response payloads
├── entity          JPA entities (User, Request, RequestHistory, OutboxEvent)
├── enums           Role, RequestStatus, Action, OutboxEventStatus
├── event           RequestLifecycleEvent published to Kafka
├── exception       Custom exceptions and global handler
├── kafka           Kafka producer
├── mapper          Entity ↔ DTO mapping
├── repository      Spring Data repositories
├── security        JWT service, auth filter, user details
├── service         Business logic, outbox processor and scheduler
├── specification   JPA Specifications for request search
└── util            Shared constants
```

## Known limitations

This is a learning project, and a few things are intentionally simple and not production-ready:

- The JWT signing key is hardcoded in `JwtService`; it should come from configuration or a secret store.
- Registration accepts any role, including `ROLE_ADMIN`.
- All Actuator endpoints are exposed.
- `TestJwtController` (`/test/**`) is a leftover debugging endpoint.
- The schema is managed by `ddl-auto=update` rather than migrations (Flyway / Liquibase).
- The `workflow-service` container does not wait for PostgreSQL or Kafka to be healthy before starting.
