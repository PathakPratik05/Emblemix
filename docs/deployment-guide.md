# Deployment & Operations Guide

Emblemix is container-native, provider-independent, and configured for single-command local development as well as cloud deployment (AWS, Azure, GCP).

---

## 1. Single-Command Local Startup (Docker Compose)

The fastest way to run Emblemix locally:
```bash
docker compose up --build
```

Services initialized:
- **Frontend App (Next.js)**: `http://localhost:3000`
- **Backend API (Spring Boot)**: `http://localhost:8080`
- **Swagger OpenAPI Documentation**: `http://localhost:8080/swagger-ui.html`
- **PostgreSQL 16**: `localhost:5432`
- **Mailpit Web UI (Local SMTP)**: `http://localhost:8025`

### Pre-Seeded Development Credentials
- **Admin Account**: `admin@emblemix.com` / `Password123!` (Role: `ORG_ADMIN`)
- **Engineer Account**: `engineer@emblemix.com` / `Password123!` (Role: `ENGINEER`)
- **Viewer Account**: `viewer@emblemix.com` / `Password123!` (Role: `VIEWER`)

---

## 2. Cloud Deployment Strategy

The application is stateless and containerized, making it suitable for AWS ECS / Fargate, Azure App Service / Container Apps, or Google Cloud Run.

### Health and Readiness Endpoints
- **Liveness probe**: `GET /actuator/health`
- **Metrics endpoint**: `GET /actuator/metrics`

### Database Scaling & High Availability
- Use a managed PostgreSQL service (e.g. AWS RDS PostgreSQL or Azure Database for PostgreSQL).
- Enable automated backups and multi-AZ failover.
- Flyway migrations execute automatically on application container startup.

### Future Scale Paths
- **Worker pool isolation**: Decouple the monitoring scheduler into dedicated worker processes.
- **Message queue ingestion**: Migrate internal Spring events to RabbitMQ or Apache Kafka for high-throughput distributed ingestion.
- **Distributed coordination**: Utilize Redis for distributed locks (`Redlock`) to support multi-replica scheduling.
