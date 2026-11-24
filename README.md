# Emblemix

## Universal Incident Management & Monitoring Platform

**Emblemix** is a centralized monitoring and automated incident management platform designed to connect heterogeneous systems and monitor their health from a single dashboard.

It provides automated failure detection, event normalization, deduplication, alert rule evaluation, incident lifecycle tracking, and asynchronous multi-channel notifications.

---

## 1. Key Capabilities

- **Multi-Tenant Architecture**: Organization-level isolation enforced at the database and application levels.
- **SSRF-Protected Website Monitoring**: Automated HTTP availability, latency, SSL certificate expiration checks, and response content validation, with strict protections against Server-Side Request Forgery.
- **Real Salesforce Integration**: Salesforce OAuth 2.0 Web Server Flow (Authorization Code Grant), AES-256 GCM encrypted token storage at rest, REST API limits monitoring, and Async Apex Job failure tracking.
- **Common Event Pipeline & Deduplication**: Deterministic SHA-256 fingerprinting that prevents duplicate open incidents from repeated failures.
- **Automated Incident Lifecycle**: `OPEN` &rarr; `ACKNOWLEDGED` &rarr; `INVESTIGATING` &rarr; `RESOLVED` &rarr; `CLOSED`, with automatic recovery resolution when healthy signals are detected.
- **Audit Trails & Collaboration**: Chronological timeline audit trail, engineer assignment, and incident discussion notes.
- **Asynchronous Notification Engine**: Email dispatch with retry handling and fallback isolation.
- **Analytics & Reporting**: SLA uptime calculations, response time trends, and one-click RFC 4180 CSV incident exports.

---

## 2. Technology Stack

### Backend
- **Language & Runtime**: Java 21, Spring Boot 3.3.5
- **Build Tool**: Maven 3.9
- **Security**: Spring Security 6 with stateless JWT (`jjwt 0.12.6`) and BCrypt password hashing
- **Persistence**: Spring Data JPA, Hibernate, PostgreSQL 16
- **Database Migrations**: Flyway (18 normalized tables with indexes)
- **Encryption**: AES-256 GCM (`AesEncryptionUtil`)
- **Documentation**: Springdoc OpenAPI v3 / Swagger UI (`springdoc 2.6.0`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, WireMock Standalone

### Frontend
- **Framework & Language**: React 18, TypeScript, Vite
- **Styling**: Tailwind CSS, shadcn/ui design primitives
- **Routing**: React Router v6
- **Server State**: TanStack React Query v5
- **Charts**: Recharts (Trends, health distributions)
- **Icons**: Lucide React
- **Testing**: Vitest, React Testing Library, Playwright E2E

### Infrastructure
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
│   │       ├── event/        # Ingestion & normalization pipeline
│   │       ├── alert/        # Alert rule evaluation engine
│   │       ├── incident/     # Complete incident lifecycle & timeline
│   │       ├── notification/ # Asynchronous email dispatch & logs
│   │       ├── reporting/    # Overview metrics, uptime, CSV export
│   │       ├── audit/        # Security and operational audit logs
│   │       └── common/       # Security filters, SSRF validator, AES encryption
│   ├── src/main/resources/   # application.yml and Flyway migrations V1 & V2
│   ├── src/test/java/        # Unit and WireMock integration tests
│   ├── pom.xml               # Maven dependencies and build plugins
│   └── Dockerfile            # Multi-stage container build
├── frontend/                 # React 18 + TypeScript + Vite Application
│   ├── src/
│   │   ├── components/       # Layout shell, Sidebar, Navbar, and UI primitives
│   │   ├── context/          # AuthContext session management
│   │   ├── lib/              # Typed API client and utilities
│   │   ├── pages/            # Dashboard, Websites, Salesforce, Incidents, etc.
│   │   └── test/             # Vitest component test suites
│   ├── package.json          # Frontend dependencies
│   ├── vite.config.ts        # Vite configuration and reverse proxy
│   ├── tailwind.config.js    # Tailwind styling configuration
│   └── Dockerfile            # Multi-stage Nginx container build
├── docs/                     # Comprehensive architecture and operations docs
│   ├── architecture.md       # Architecture overview with Mermaid diagrams
│   ├── database-design.md    # Normalized schema and ER diagram
│   ├── api-documentation.md  # Full REST API endpoint reference
│   ├── website-monitoring.md # SSRF protection and scheduled probe engine
│   ├── salesforce-integration.md # External Client App and OAuth 2.0 flow
│   ├── security.md           # Multi-tenancy, token rotation, and encryption
│   ├── testing-strategy.md   # Testing pyramid and automated execution
│   ├── deployment-guide.md   # Docker and cloud deployment guidelines
│   └── development-phases.md # Phase execution log
├── .github/workflows/ci.yml  # GitHub Actions automated CI workflow
├── docker-compose.yml        # Orchestration (Postgres, Mailpit, Backend, Frontend)
├── .env.example              # Environment variables template
└── README.md                 # Primary platform documentation
```

---

## 4. Quick Start (Single-Command Docker Startup)

### Prerequisites
- Docker & Docker Compose installed.

### Run with Docker Compose
```bash
# Clone the repository and enter the directory
git clone <repo-url>
cd Emblemix

# Copy example environment configuration
cp .env.example .env

# Build and start all services
docker compose up --build
```

### Accessing the Services
- **Web Application**: [http://localhost:5173](http://localhost:5173)
- **Backend REST API**: [http://localhost:8080](http://localhost:8080)
- **Interactive Swagger Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Mailpit Web UI (SMTP Inbox)**: [http://localhost:8025](http://localhost:8025)

---

## 5. Development Seed Accounts

Emblemix automatically seeds a default demo organization (`Acme Global Enterprises`) and accounts on startup:

| Email | Password | Role | Permissions |
|-------|----------|------|-------------|
| `admin@emblemix.com` | `Password123!` | `ORG_ADMIN` | Full organization administration, integration, and incident management |
| `engineer@emblemix.com` | `Password123!` | `ENGINEER` | Manage systems, alert rules, incidents, and monitoring configurations |
| `viewer@emblemix.com` | `Password123!` | `VIEWER` | Read-only access to dashboards, reports, and monitors |

---

## 6. Local Manual Development Setup

### 1. Database & Mailer Setup
Start PostgreSQL and Mailpit:
```bash
docker compose up -d postgres mailpit
```

### 2. Backend Setup
```bash
cd backend
mvn clean test
mvn spring-boot:run
```

### 3. Frontend Setup
```bash
cd frontend
npm install
npm test
npm run dev
```

---

## 7. Automated Test Execution

### Backend Tests
```bash
cd backend
mvn test
```
*Executes all unit tests and WireMock mocks (SSRF validation, AES encryption, fingerprinting, alert rule engine, incident lifecycle, Salesforce API limits).*

### Frontend Tests
```bash
cd frontend
npm test
```
*Executes all component tests using Vitest and React Testing Library.*

---

## 8. Salesforce External Client App Setup

To configure a real Salesforce Production or Sandbox organization:
1. In Salesforce Setup, go to **App Manager** &rarr; **New Connected App** (or External Client App).
2. Enable OAuth Settings and set Callback URL to:
   ```
   http://localhost:5173/integrations/salesforce/callback
   ```
3. Assign scopes:
   - `Manage user data via APIs (api)`
   - `Perform requests at any time (refresh_token, offline_access)`
4. Set the credentials in your `.env` file:
   ```bash
   SALESFORCE_CLIENT_ID=<your-consumer-key>
   SALESFORCE_CLIENT_SECRET=<your-consumer-secret>
   SALESFORCE_REDIRECT_URI=http://localhost:5173/integrations/salesforce/callback
   ```
5. In Emblemix, navigate to **Salesforce Orgs** &rarr; **Connect OAuth**.

---

## 9. Security & SSRF Protection

Website monitoring enforces comprehensive SSRF protections in `IpValidationUtil`:
- Only `http` and `https` schemes are allowed.
- Loopback addresses (`127.0.0.0/8`, `::1`) and `localhost` are blocked.
- RFC 1918 private subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) are blocked.
- Cloud metadata IP ranges (`169.254.169.254`, `metadata.google.internal`) are blocked.
- DNS pre-resolution verifies all resolved addresses before connecting.
- Redirects are validated individually against SSRF rules up to a maximum depth of 3.

---

## 10. Documentation Index

Detailed engineering documentation is located in the `docs/` folder:
- [Architecture & Mermaid Diagrams](docs/architecture.md)
- [Database Schema & ER Diagrams](docs/database-design.md)
- [REST API Reference](docs/api-documentation.md)
- [Website Monitoring & SSRF](docs/website-monitoring.md)
- [Salesforce Integration Guide](docs/salesforce-integration.md)
- [Security & Multi-Tenancy](docs/security.md)
- [Testing Strategy](docs/testing-strategy.md)
- [Deployment Guide](docs/deployment-guide.md)
- [Development Phases Execution Log](docs/development-phases.md)
