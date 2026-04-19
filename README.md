# 🛡️ PayShield — Intelligent Payment Protection Platform

A production-grade, microservices-based payment processing and fraud detection platform built with **Java 21**, **Spring Boot 3**, **Kafka**, **Redis**, and **PostgreSQL**.

## Architecture

```
Browser → PayShield Gateway (8080)
            ├── payshield-auth         (8081) — JWT Auth
            ├── payshield-payment      (8082) — Transactions
            ├── payshield-fraud        (8083) — Fraud Detection + AI Scoring
            ├── payshield-reconciliation (8084) — Settlement & Reconciliation
            ├── payshield-notification  (8085) — Email/SMS/Webhook
            └── payshield-reporting    (8086) — Dashboard & Analytics

         payshield-ai-scorer (9000) — Python FastAPI ML Fraud Model
```

## Services

| Service | Port | Responsibility |
|---------|------|----------------|
| `payshield-gateway` | 8080 | API Gateway — JWT validation, routing |
| `payshield-auth` | 8081 | Register, login, JWT + refresh tokens |
| `payshield-payment` | 8082 | Initiate, process, track transactions |
| `payshield-fraud` | 8083 | Real-time fraud scoring via AI + rules |
| `payshield-reconciliation` | 8084 | Settlement reconciliation + Spring Batch |
| `payshield-notification` | 8085 | Email/SMS notifications with retry |
| `payshield-reporting` | 8086 | Dashboard, daily summaries, analytics |
| `payshield-ai-scorer` | 9000 | Python FastAPI XGBoost fraud model |

## Tech Stack

- **Java 21** — Virtual threads (`spring.threads.virtual.enabled=true`)
- **Spring Boot 3.2** — All microservices
- **Spring Cloud Gateway** — API routing + JWT filter
- **Apache Kafka** — Event streaming between services
- **PostgreSQL 15** — Per-service databases
- **Redis 7** — Caching + fraud velocity tracking
- **Spring Batch** — Reconciliation jobs
- **FastAPI + NumPy** — AI fraud scoring (Python 3.11)
- **Docker Compose** — Full stack orchestration

## Quick Start

```bash
# Clone and start everything
docker-compose up --build

# Open the dashboard
open http://localhost:3000
```

## API Endpoints

### Auth (`/api/auth`)
```
POST /api/auth/register    — Register new merchant
POST /api/auth/login       — Login → access + refresh tokens
POST /api/auth/refresh     — Refresh access token
```

### Payments (`/api/payments`)
```
POST /api/payments/initiate          — Initiate transaction
POST /api/payments/{id}/process      — Process payment
GET  /api/payments/{id}              — Get transaction
GET  /api/payments                   — List transactions
```

### Fraud (`/api/fraud`)
```
POST /api/fraud/score                — Score a transaction
GET  /api/fraud/scores/{txnId}       — Get fraud score
GET  /api/fraud/alerts               — Get fraud alerts
GET  /api/fraud/risky                — High-risk transactions
```

### Reconciliation (`/api/reconciliation`)
```
POST /api/reconciliation/run               — Run reconciliation job
GET  /api/reconciliation/runs/{runId}      — Get run status
GET  /api/reconciliation/runs/{runId}/mismatches — Get mismatches
POST /api/reconciliation/records/{id}/resolve  — Resolve mismatch
```

### Reporting (`/api/reports`)
```
GET /api/reports/dashboard?merchantId=&days=   — Dashboard summary
GET /api/reports/payment-methods?merchantId=   — Payment breakdown
```

## Kafka Topics

| Topic | Producer | Consumer(s) |
|-------|----------|-------------|
| `payment.initiated` | payment-service | fraud-service, reporting-service, notification-service |
| `payment.completed` | payment-service | reporting-service, notification-service |
| `payment.failed` | payment-service | reporting-service, notification-service |
| `fraud.check.request` | payment-service | fraud-service |
| `fraud.check.response` | fraud-service | payment-service |
| `fraud.alert` | fraud-service | notification-service |

## Demo Credentials
```
Email:    demo@merchant.com
API Key:  mk_test_demo123456789
```

## Databases
Each service has its own PostgreSQL database:
- `auth_db` — Users, refresh tokens
- `payment_db` — Transactions, merchants, refunds
- `fraud_db` — Fraud scores, alerts
- `recon_db` — Settlements, reconciliation records
- `notification_db` — Notification log
- `reporting_db` — Daily summaries, payment method stats
