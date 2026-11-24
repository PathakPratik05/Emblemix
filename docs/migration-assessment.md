# Emblemix — Migration Assessment & Strategy

## 1. Executive Summary

This document establishes the assessment baseline for migrating and continuing development of **Emblemix** — Universal Incident Management & Monitoring Platform.

The core architectural direction:
1. **Preserve the Spring Boot 3.3.5 / Java 21 modular monolith** as the single source of truth for business logic, authentication, event ingestion, incident management, monitoring, and persistence.
2. **Migrate the frontend from React + Vite to Next.js App Router (TypeScript strict mode)**, utilizing Server Components by default, Client Components for reactive dashboard UI, Route Groups `(auth)` and `(dashboard)`, shadcn/ui patterns, React Hook Form, Zod, TanStack Query, Recharts, and Lucide React.
3. **Expand integrations**: In addition to Website Monitoring and Salesforce Monitoring, introduce a **Custom Webhook Integration** with HMAC-SHA256 signature verification, secret management/rotation, and payload normalization into the common incident engine.

---

## 2. Existing Architecture Assessment

### 2.1 Backend Architecture
- **Language & Framework**: Java 21, Spring Boot 3.3.5, Maven, Spring Security (JWT + BCrypt), Spring Data JPA, Flyway, PostgreSQL, Spring Mail, Springdoc OpenAPI.
- **Module Structure**: Clean package boundary monolith under `com.emblemix.*`:
  - `auth`: Registration, JWT access/refresh token rotation, security context.
  - `user`: User management, tenant role-based access (`SUPER_ADMIN`, `ORG_ADMIN`, `ENGINEER`, `VIEWER`).
  - `organization`: Tenant entity and tenant context isolation.
  - `system`: Monitored systems management (`WEBSITE`, `SALESFORCE`, `API`, `WEBHOOK`).
  - `monitoring.website`: Website check scheduling, SSRF protection (`IpValidationUtil`), HTTP probe execution, SSL check, latency calculation.
  - `monitoring.salesforce`: OAuth2 code exchange, encrypted token storage (AES-256 GCM), REST API / limits polling, Apex failure ingestion.
  - `event`: Common event pipeline (`EventIngestionService`, `RawEvent`, `NormalizedEvent`).
  - `alert`: Rule engine evaluating failure count, response time, threshold operators, auto-resolve triggers.
  - `incident`: Universal incident engine with SHA-256 fingerprint deduplication, auto-recovery resolution, timeline entries.
  - `notification`: Multi-channel notification dispatchers (Email via Spring Mail, Webhook dispatch, In-App).
  - `reporting`: Mean Time to Detect (MTTD), Mean Time to Resolve (MTTR), SLA uptime percentages, incident volume trends.
  - `audit`: Immutable audit logging for security-sensitive actions.

### 2.2 Database Structure (Flyway)
- **`V1__init_schema.sql`**: 18 relational tables including `organizations`, `users`, `refresh_tokens`, `connected_systems`, `website_monitors`, `website_check_results`, `salesforce_integrations`, `salesforce_org_limits`, `raw_events`, `normalized_events`, `alert_rules`, `incidents`, `incident_timeline`, `notification_channels`, `notifications`, `audit_logs`, `reports`.
- **`V2__seed_data.sql`**: Seeds default demo organization (`Acme Corp`), administrator (`admin@emblemix.com`), default system, website monitor, and alert rules.

### 2.3 Existing Frontend (React + Vite)
- The initial baseline frontend was implemented using React 18 with Vite, React Router DOM, TanStack Query, and Tailwind CSS.
- **Key UI flows completed**:
  - Authentication: Login, Register, Forgot Password, Reset Password.
  - Dashboard: Metrics summary cards (Open Incidents, Uptime, Monitored Systems, MTTR), recent incidents list, system status grid.
  - Systems & Integrations: System creation, website configuration, Salesforce OAuth connect flow.
  - Incidents: Table view, status filtering, incident detail view with timeline and manual acknowledge/resolve actions.
  - Alert Rules & Notifications: Rule definitions, channel configurations.
  - Reports: Metric visualizations and trend charts.

---

## 3. Completed vs. Incomplete Functionality

| Capability | Status | Assessment / Action Required |
|---|---|---|
| **Auth & Multi-Tenancy** | Completed | Backend JWT + BCrypt + tenant filter fully functional. Retain and adapt to Next.js cookie/session pattern. |
| **Website Monitoring** | Completed | Periodic probe, SSRF prevention, SSL validation, latency tracking operational. |
| **Salesforce Integration** | Completed | OAuth exchange, encrypted token storage, API limits & error polling complete. WireMock tests pass. |
| **Common Incident Engine** | Completed | Event normalization, fingerprint deduplication, threshold rules, auto-recovery working. |
| **Custom Webhook Integration** | Incomplete | Needs Flyway migration `V3`, entity `WebhookConfiguration`, HMAC-SHA256 signature verification, and ingestion endpoint. |
| **Frontend Framework** | Needs Migration | Current React 18 + Vite must be migrated to Next.js App Router (TypeScript strict, Route Groups). |
| **Docker Configuration** | Needs Update | Update Docker Compose and Dockerfiles for Next.js standalone container on port 3000. |

---

## 4. Migration Risks & Mitigations

1. **Risk: Breaking Authentication & Cookie Handling**
   - *Detail*: React SPA used local token memory/storage. Next.js App Router works seamlessly with backend API proxying and secure cookies.
   - *Mitigation*: Configure Next.js rewrites in `next.config.js` to route `/api/*` directly to Spring Boot backend (`http://localhost:8080`), preserving authorization headers or forwarding credentials cleanly.

2. **Risk: Duplicating Backend Logic in Next.js Server Actions / API Routes**
   - *Detail*: Temptation to implement business logic in Next.js server routes.
   - *Mitigation*: Strictly enforce Spring Boot as the single source of truth. Next.js App Router serves as the presentation layer and uses TanStack Query or server fetchers purely as API clients to Spring Boot.

3. **Risk: Regression of Working Features (SSRF Protection, AES Encryption, Deduplication)**
   - *Detail*: Modifying backend contracts could disrupt tested logic.
   - *Mitigation*: Do not touch working modules (`IpValidationUtil`, `AesEncryptionUtil`, `IncidentService`). All existing unit and integration tests must pass continuously (`mvn test`).

4. **Risk: Next.js Client vs Server Component Mismatch**
   - *Detail*: Interactive hooks (`useState`, `useQuery`, forms) failing in Server Components.
   - *Mitigation*: Use Server Components for page shells and layouts; isolate interactive controls, tables, and forms into dedicated Client Components marked with `'use client'`.

---

## 5. Frontend Migration Plan

### 5.1 Route Hierarchy (Next.js App Router)
```
frontend/src/app/
├── layout.tsx                    # Root HTML layout with fonts and metadata
├── globals.css                   # Tailwind CSS base styles
├── providers.tsx                 # TanStack Query & Auth context provider ('use client')
├── (auth)/
│   ├── layout.tsx                # Centered auth card layout
│   ├── login/page.tsx
│   ├── register/page.tsx
│   ├── forgot-password/page.tsx
│   └── reset-password/page.tsx
└── (dashboard)/
    ├── layout.tsx                # App shell layout (Sidebar, Navbar, Tenant badge)
    ├── dashboard/page.tsx        # Overview & KPI metrics
    ├── systems/page.tsx          # Connected systems
    ├── integrations/page.tsx     # Unified integration directory
    ├── websites/page.tsx         # Website monitoring list & status
    ├── salesforce/page.tsx       # Salesforce org monitoring & limits
    ├── webhooks/page.tsx         # Custom webhook endpoints & simulator
    ├── incidents/
    │   ├── page.tsx              # Incident list & filters
    │   └── [id]/page.tsx         # Incident detail & timeline
    ├── alert-rules/page.tsx      # Threshold and notification rules
    ├── notifications/page.tsx    # Notification channels & dispatch logs
    ├── reports/page.tsx          # MTTD, MTTR, SLA analytics
    └── settings/page.tsx         # Organization & profile management
```

### 5.2 Component & Feature Organization
- `src/components/ui`: Primitive shadcn-style UI components (Button, Input, Card, Badge, Dialog, Table, Tabs, Dropdown).
- `src/components/layout`: Sidebar, Navbar, Mobile Nav, Breadcrumbs.
- `src/lib/api`: Centralized HTTP client configured with base URL, token interceptors, and error handling.
- `src/features/*`: Feature-specific query hooks, mutations, and domain components.

---

## 6. Features That Must Be Preserved

1. **Security & SSRF Mitigation**: `IpValidationUtil` blocking loopback, private networks, and link-local ranges for website monitors.
2. **Encrypted Credentials**: `AesEncryptionUtil` handling AES-256-GCM encryption for Salesforce refresh tokens and webhook signing secrets.
3. **Deterministic Deduplication**: Fingerprint algorithm hashing system ID, title, and rule key to prevent incident floods.
4. **Multi-Tenancy Enforcers**: Organization IDs resolved from authenticated JWT security context, preventing tenant data leakage.
5. **Auto-Recovery**: Automatic incident resolution when healthy check results or event recovery signals arrive.
