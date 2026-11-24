# Emblemix

## Universal Incident Management & Monitoring Platform

> **One Platform. Every System. Every Incident.**

**Emblemix** is a centralized monitoring and automated incident management platform designed to connect heterogeneous systems and monitor their operational health from a single, intuitive dashboard.

It provides automated failure detection, event normalization, deterministic deduplication, an alert rule evaluation engine, full incident lifecycle tracking, asynchronous multi-channel notifications, and compliance audit reporting.

---

## 1. Primary Integrations

1. **Website Monitoring**: Automated HTTP/HTTPS availability, latency, SSL certificate expiry alerts, and strict SSRF defenses against private network probing.
2. **Salesforce Organization Monitoring**: Production and Sandbox OAuth 2.0 Web Server integration, AES-256-GCM encrypted tokens at rest, daily REST API request limit tracking, and Async Apex Job failure alerts.
3. **Custom Inbound Webhooks**: Push alerts from any external microservice, CI/CD pipeline, Datadog, Stripe, or GitHub. Authenticated via HMAC-SHA256 signature verification (`X-Emblemix-Signature` / `X-Hub-Signature-256`) with secret rotation and delivery logs.
4. **Pluggable Architecture**: Easily extendable to AWS CloudWatch, Azure Monitor, Kubernetes, and Prometheus without redesigning the incident engine.

---

## 2. Technology Stack

### Backend
- **Language & Runtime**: Java 21, Spring Boot 3.3.5
- **Build Tool**: Maven 3.9
- **Security**: Spring Security 6 with stateless JWT, BCrypt password hashing, and HMAC-SHA256 webhook validation
- **Persistence**: Spring Data JPA, Hibernate, PostgreSQL 16
- **Database Migrations**: Flyway (`V1__init_schema.sql`, `V2__seed_data.sql`, `V3__webhook_tables.sql`)
- **Encryption**: AES-256 GCM authenticated encryption (`AesEncryptionUtil`)
- **Documentation**: Springdoc OpenAPI v3 / Swagger UI (`springdoc 2.6.0`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, WireMock Standalone (31 unit & integration tests)

### Frontend
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript with strict mode
- **Architecture**: Route groups `(auth)` and `(dashboard)`, Server Components by default, Client Components for interactive UI
- **Styling**: Tailwind CSS with shadcn/ui design primitives
- **Server State**: TanStack React Query v5
- **Forms & Validation**: React Hook Form, Zod
- **Data Visualization**: Recharts (Trends area charts, system health distributions)
- **Icons**: Lucide React
- **Testing**: Vitest, React Testing Library, Playwright E2E

### Infrastructure & DevOps
- **Containers**: Docker, Docker Compose
- **Database**: PostgreSQL 16
- **Local Mailer**: Mailpit (SMTP on port 1025, Web UI on port 8025)
- **CI/CD**: GitHub Actions

---

## 3. Directory Layout

```
Emblemix/
├── backend/                  # Java 21 Spring Boot Application
│   ├── src/main/java/        # Clean modular monolith packages
│   │   └── com/emblemix/
│   │       ├── auth/         # Login, registration, token rotation, password reset
│   │       ├── user/         # User management, roles, profiles
│   │       ├── organization/ # Tenant organization scoping
│   │       ├── system/       # Connected systems catalogue
│   │       ├── integration/  # Encrypted credentials at rest
│   │       ├── monitoring/   # Website & Salesforce monitoring engines
│   │       ├── webhook/      # Custom Webhook ingestion, HMAC verification, secret rotation
│   │       ├── event/        # Ingestion & normalization pipeline
│   │       ├── alert/        # Alert rule evaluation engine
│   │       ├── incident/     # Complete incident lifecycle & timeline
│   │       ├── notification/ # Asynchronous email dispatch & logs
│   │       ├── reporting/    # Overview metrics, uptime, CSV export
│   │       ├── audit/        # Security and operational audit logs
│   │       └── common/       # Security filters, SSRF validator, AES encryption
│   ├── src/main/resources/   # application.yml and Flyway migrations (V1, V2, V3)
│   ├── src/test/java/        # Unit and WireMock integration tests (31 tests)
│   ├── pom.xml               # Maven dependencies and build plugins
│   └── Dockerfile            # Multi-stage container build
├── frontend/                 # Next.js 14 App Router Application
│   ├── src/
│   │   ├── app/              # Next.js App Router (Route groups: (auth) & (dashboard))
│   │   ├── components/       # Layout shell, Sidebar, Navbar, and UI primitives
│   │   ├── context/          # AuthContext session management
│   │   ├── lib/              # Typed API client with SSR protection
│   │   └── test/             # Vitest component test suites
│   ├── package.json          # Next.js 14 dependencies
│   ├── next.config.mjs       # Backend API rewrites & standalone output
│   ├── tailwind.config.js    # Tailwind styling configuration
│   └── Dockerfile            # Multi-stage standalone Node.js runner
├── docs/                     # Comprehensive architecture and operations docs
│   ├── migration-assessment.md   # Baseline assessment & frontend migration plan
│   ├── frontend-nextjs.md        # Next.js App Router architecture and routing model
│   ├── webhook-integration.md    # Custom Webhook HMAC-SHA256 specification and examples
│   ├── integration-architecture.md # Pluggable adapter pattern & extension guide
│   ├── architecture.md           # Architecture overview with Mermaid diagrams
│   ├── database-design.md        # Normalized schema and Flyway migration reference
│   ├── api-documentation.md      # Full REST API endpoint reference
│   ├── website-monitoring.md     # SSRF protection and scheduled probe engine
│   ├── salesforce-integration.md # External Client App and OAuth 2.0 flow
│   ├── security.md               # Multi-tenancy, token rotation, and encryption
│   ├── testing-strategy.md       # Testing pyramid and automated execution
│   └── deployment-guide.md       # Docker Compose and cloud deployment guidelines
├── .github/workflows/ci.yml  # GitHub Actions automated CI workflow
├── docker-compose.yml        # Orchestration (Postgres, Mailpit, Backend, Frontend)
└── .env.example              # Environment variables template
```

---

## 4. Quick Start: Single-Command Docker Compose

Launch the entire platform including PostgreSQL, Mailpit, Spring Boot backend, and Next.js frontend:

```bash
docker compose up --build
```

### Initial Service Endpoints
- **Next.js Frontend**: [http://localhost:3000](http://localhost:3000)
- **Spring Boot API**: [http://localhost:8080](http://localhost:8080)
- **OpenAPI Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Mailpit Web UI (Local SMTP)**: [http://localhost:8025](http://localhost:8025)
- **PostgreSQL Database**: `localhost:5432` (db: `emblemix`, user/pass: `postgres`/`postgres`)

### Pre-Seeded Development Credentials
- **Admin**: `admin@emblemix.com` / `Password123!` (Role: `ORG_ADMIN`)
- **Engineer**: `engineer@emblemix.com` / `Password123!` (Role: `ENGINEER`)
- **Viewer**: `viewer@emblemix.com` / `Password123!` (Role: `VIEWER`)

---

## 5. Local Development (Without Docker)

### 1. Prerequisites
- Java 21 JDK (`openjdk 21` or `temurin 21`)
- Node.js 18+ (tested on Node 20 / 23)
- Maven 3.9+
- Running PostgreSQL 16 on `localhost:5432` with database `emblemix`

### 2. Start Backend
```bash
cd backend
mvn clean spring-boot:run
```

### 3. Start Next.js Frontend
```bash
cd frontend
npm install
npm run dev
```
Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## 6. Automated Testing

### Backend Unit & Integration Tests (JUnit 5, Mockito, WireMock)
```bash
cd backend
mvn test
```
*Executes all 31 tests covering authentication, SSRF IP blocking, AES encryption, SHA-256 deduplication, alert threshold rules, auto-recovery incident grouping, WireMock Salesforce client, and Webhook HMAC validation.*

### Frontend Tests (Vitest, React Testing Library)
```bash
cd frontend
npm test
```

### Frontend Production Build (Type Check & Route Prerendering)
```bash
cd frontend
npm run build
```
