# Emblemix — Next.js App Router Frontend Architecture

## 1. Overview & Architectural Principles

The Emblemix frontend has been migrated to **Next.js 14 App Router** with TypeScript strict mode, providing server-first rendering, modern layout nesting, route grouping, and robust client-side state management.

### Key Decisions
- **Next.js App Router**: Route groups `(auth)` and `(dashboard)` enforce visual and logical boundary separation.
- **Server vs Client Components**: Server components are utilized for structural layout shells and static containers; interactive dashboards, charts, real-time tables, and modal forms use Client Components (`'use client'`).
- **Single Source of Truth**: The Java 21 + Spring Boot 3.3.5 backend remains the single source of truth for business logic, multi-tenancy, authentication, and persistence. Next.js does not run standalone persistence routes.
- **API Proxying**: In local development and Docker Compose, Next.js rewrites `/api/:path*` to the Spring Boot backend (`http://localhost:8080`), eliminating browser CORS issues while maintaining secure authorization headers.

---

## 2. Directory Structure

```
frontend/
├── Dockerfile                   # Multi-stage standalone Next.js container
├── next.config.mjs              # API rewrites & standalone output configuration
├── package.json                 # Next.js 14, TanStack Query, Recharts, Lucide React
├── tailwind.config.js           # Theme colors, CSS variables, and layout utility tokens
├── tsconfig.json                # TypeScript strict mode and path alias @/*
└── src/
    ├── app/
    │   ├── layout.tsx           # Global HTML wrapper, metadata, Inter font, Providers
    │   ├── globals.css          # Tailwind base & component CSS variable definitions
    │   ├── providers.tsx        # TanStack QueryClientProvider & AuthProvider
    │   ├── page.tsx             # Root redirector (/dashboard or /login)
    │   ├── (auth)/
    │   │   ├── layout.tsx       # Centered card layout with Emblemix branding
    │   │   ├── login/page.tsx   # Login with demo accounts quick-fill
    │   │   ├── register/page.tsx # Organization & admin tenant registration
    │   │   ├── forgot-password/page.tsx # Password reset token generator
    │   │   └── reset-password/page.tsx  # New password submission
    │   └── (dashboard)/
    │       ├── layout.tsx       # Authenticated shell (Sidebar, Navbar, Tenant badge)
    │       ├── dashboard/page.tsx # KPIs, incident trend area charts, health bars
    │       ├── systems/page.tsx # Connected systems catalog & registration modal
    │       ├── integrations/page.tsx # Unified integration directory (Websites, SF, Webhooks)
    │       ├── websites/page.tsx # Probing interval, SSL expiry, live test & history
    │       ├── salesforce/page.tsx # OAuth2 flow, API usage %, Apex job diagnostics
    │       ├── webhooks/page.tsx # Inbound webhook endpoints, HMAC signing, test simulator
    │       ├── incidents/
    │       │   ├── page.tsx     # Incident filtering by status & severity
    │       │   └── [id]/page.tsx # Incident detail, timeline audit, engineer notes
    │       ├── alert-rules/page.tsx # Threshold rules for failures, latency, API limits
    │       ├── notifications/page.tsx # Email/SMTP notification channel management
    │       ├── reports/page.tsx # 30-day uptime SLA percentages & CSV export
    │       └── settings/page.tsx # Tenant slug, organization members, user profile
    ├── components/
    │   ├── layout/
    │   │   ├── Navbar.tsx       # Top bar with active ingestion engine indicator & user avatar
    │   │   └── Sidebar.tsx      # Vertical nav with Lucide icons and active route highlighting
    │   └── ui/
    │       ├── Badge.tsx        # Severity and status badges (success, warning, destructive)
    │       ├── Button.tsx       # Standard buttons with loading states & variants
    │       ├── Card.tsx         # Card container primitives
    │       ├── Input.tsx        # Form input fields with validation and error states
    │       └── Modal.tsx        # Accessible dialog wrapper with backdrop
    ├── context/
    │   └── AuthContext.tsx      # Token persistence, user profile, and login/logout handlers
    ├── lib/
    │   ├── api.ts               # Centralized typed HTTP client with SSR safe guards
    │   └── utils.ts             # Tailwind class merging utility (cn)
    └── test/
        ├── setup.ts             # Jest DOM matchers
        ├── Button.test.tsx      # UI component tests
        └── Login.test.tsx       # Authentication form render tests
```

---

## 3. Route Groups & Routing Model

| Path | Route Group | Description |
|---|---|---|
| `/` | Root | Automatically redirects to `/dashboard` if authenticated, or `/login`. |
| `/login` | `(auth)` | Organization email/password authentication. Includes dev seed account shortcuts. |
| `/register` | `(auth)` | Self-service organization onboarding. Creates tenant and initial `ORG_ADMIN`. |
| `/forgot-password`| `(auth)` | Initiates password reset flow and issues reset token. |
| `/reset-password` | `(auth)` | Accepts password reset token and saves new hashed password. |
| `/dashboard` | `(dashboard)` | Operational overview: Open Incidents, Uptime %, MTTR, Trends, System Health. |
| `/systems` | `(dashboard)` | Connected Systems inventory (`WEBSITE`, `SALESFORCE`, `WEBHOOK`). |
| `/integrations` | `(dashboard)` | Unified integrations catalog with status counters and future roadmap cards. |
| `/websites` | `(dashboard)` | Website health checks, SSL certificate expiry checks, latency diagnostics. |
| `/salesforce` | `(dashboard)` | Salesforce OAuth2 connect, daily REST API request limits, Apex error tracking. |
| `/webhooks` | `(dashboard)` | Inbound webhook endpoints, HMAC-SHA256 signature documentation, test simulator. |
| `/incidents` | `(dashboard)` | Incident table with multi-criteria status/severity filtering and acknowledge actions. |
| `/incidents/[id]` | `(dashboard)` | Incident details, audit timeline, engineer assignment, resolution notes, discussion. |
| `/alert-rules` | `(dashboard)` | Alert rules engine configuring failure count, latency, and API thresholds. |
| `/notifications`| `(dashboard)` | Notification channels (Email/SMTP) with trigger conditions. |
| `/reports` | `(dashboard)` | 30-day uptime SLA calculations and CSV incident compliance export. |
| `/settings` | `(dashboard)` | Tenant organization metadata, UUID partition display, and team member list. |

---

## 4. State Management & Server State

- **TanStack Query (v5)**:
  - Automatic background polling (`refetchInterval: 15000` for incidents and dashboard metrics).
  - Optimistic query cache invalidation on create, update, or resolve actions.
  - Query keys follow structured patterns (`['incidents-list', status, severity]`, `['dashboard-overview']`).
- **AuthContext**:
  - Encapsulates JWT access tokens, refresh tokens, and user profile.
  - Safe for server-side rendering with client-side hydration guards.

---

## 5. Testing & Quality Assurance

- **Vitest & React Testing Library**:
  - Unit tests verifying component rendering, form states, and button interactions.
  - Run via `npm test` from the `frontend/` directory.
- **Playwright End-to-End**:
  - Browser tests for login workflows, dashboard rendering, and route traversal.
  - Run via `npm run test:e2e`.
- **Production Build Verification**:
  - Strict type checking via `next build` ensures no unhandled props or invalid types.
