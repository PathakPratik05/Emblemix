# Emblemix Development Phases & Implementation Log

This document records the phase-by-phase implementation of **Emblemix** (Universal Incident Management & Monitoring Platform).

---

## Phase Execution Summary

| Phase | Description | Status | Completion Date |
|-------|-------------|--------|-----------------|
| **Phase 1** | Project Structure, Scaffolding, and Baseline Configuration | Completed | 2026-11-24 |
| **Phase 2** | Backend Foundation, Database Schema, Flyway Migrations, Auth, Multi-Tenancy | Completed | 2026-11-24 |
| **Phase 3** | Frontend Foundation, Navigation, Auth (Login/Register), Dashboard UI | Completed | 2026-11-24 |
| **Phase 4** | Connected Systems Management (CRUD, Environments, Status, Linking) | Completed | 2026-11-24 |
| **Phase 5** | Website Monitoring Engine (Availability, SSL, Response Time, SSRF Protection, Auto-scheduler) | Completed | 2026-11-24 |
| **Phase 6** | Salesforce Integration (OAuth 2.0 Web Server Flow, Encrypted Storage, API Limits, Async Apex Jobs, Logins) | Completed | 2026-11-24 |
| **Phase 7** | Common Event Engine, Alert Rules, Incident Lifecycle, and Notification Engine | Completed | 2026-11-24 |
| **Phase 8** | Reporting, Analytics, System & Severity Breakdown, CSV Exports | Completed | 2026-11-24 |
| **Phase 9** | Automated Testing Suite (Unit, Integration, Component, E2E) | Completed | 2026-11-24 |
| **Phase 10** | Dockerization, CI/CD Pipeline, Production Readiness & Full Documentation | Completed | 2026-11-24 |

---

## Phase-by-Phase Details

### Phase 1: Project Structure & Scaffolding
- Established isolated `backend/` and `frontend/` directories alongside root configuration.
- Initialized Maven Spring Boot 3.3.5 / Java 21 project and Vite / React 18 / TypeScript frontend.

### Phase 2: Database Schema & Authentication
- Created Flyway migrations `V1__init_schema.sql` (18 normalized tables with foreign keys and indexes) and `V2__seed_data.sql` (Acme demo organization, seed accounts, default website, and alert rules).
- Built BCrypt password hashing, stateless JWT authentication, refresh token rotation, and password reset handling.
- Enforced multi-tenancy with `TenantContext` and `SecurityUtils`.

### Phase 3: Frontend Foundation & Dashboard
- Established Tailwind CSS design system with shadcn/ui components (`Button`, `Input`, `Card`, `Badge`, `Modal`).
- Implemented `AuthContext`, React Router v6 navigation, and responsive `Layout` with `Sidebar` and `Navbar`.
- Built `Dashboard` with real-time counter widgets and Recharts visualizations.

### Phase 4: Connected Systems Management
- Implemented CRUD APIs and frontend interface for registering and managing target systems.
- Supported multi-environment tagging (`PRODUCTION`, `STAGING`, `DEVELOPMENT`) and status tracking (`HEALTHY`, `DEGRADED`, `DOWN`, `UNKNOWN`).

### Phase 5: Website Monitoring Engine
- Built `WebsiteCheckExecutor` using Java 21 HTTP Client and TLS SSLSocket certificate expiry inspector.
- Implemented SSRF defenses: blocked loopback, RFC 1918 private subnets, link-local addresses, and cloud metadata (169.254.169.254), with DNS pre-validation and safe redirect handling.
- Implemented automated `@Scheduled` background worker and instant live test check.

### Phase 6: Salesforce Integration
- Implemented real Salesforce OAuth 2.0 Web Server Flow (Authorization Code Grant) with cryptographically secure state parameters.
- Implemented AES-256 GCM authenticated encryption at rest for OAuth access and refresh tokens.
- Built Salesforce REST API client monitoring `DailyApiRequests` limits and Tooling API queries for failed `AsyncApexJob` executions.
- Supported automatic token refresh and credential revocation upon disconnect.

### Phase 7: Event Pipeline, Rules & Incident Lifecycle
- Built `EventIngestionService` and `EventNormalizationService` with deterministic SHA-256 fingerprinting.
- Implemented `AlertRuleEngine` supporting failure count, response time, and API usage thresholds.
- Implemented complete incident lifecycle (`OPEN` -> `ACKNOWLEDGED` -> `INVESTIGATING` -> `RESOLVED` -> `CLOSED`), duplicate event grouping, audit history, engineer assignment, and engineering comments thread.
- Implemented automatic incident resolution upon detection of system recovery events.
- Built `NotificationService` for asynchronous multi-channel email dispatch with exponential retry handling.

### Phase 8: Reporting & Analytics
- Implemented overview counters, 14-day daily incident trend aggregation, and 30-day uptime percentage calculations.
- Built RFC 4180 CSV export for all incident records (`/api/v1/reports/export/incidents.csv`).

### Phase 9: Automated Testing
- Backend unit and WireMock integration tests (SSRF validation, AES encryption, fingerprinting, auth service, alert rule engine, incident grouping, Salesforce API limits mock).
- Frontend component tests using Vitest and React Testing Library (`Login.test.tsx`, `Button.test.tsx`).
- Playwright E2E browser test specification (`e2e/workflow.spec.ts`).

### Phase 10: Docker & CI/CD
- Created multi-stage `Dockerfile` for backend and frontend.
- Created `docker-compose.yml` orchestrating PostgreSQL 16, Mailpit, Backend, and Frontend.
- Created `.github/workflows/ci.yml` pipeline.
- Authored comprehensive documentation in `docs/` and root `README.md`.
