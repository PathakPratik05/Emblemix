# Architecture Documentation: Emblemix Platform

Emblemix is built as a high-performance **modular monolith** adhering to clean architecture principles, strict multi-tenancy boundaries, and an event-driven incident pipeline.

---

## 1. High-Level System Architecture

```mermaid
graph TD
    Client["Frontend App (Next.js 14 App Router / TypeScript)"] -->|REST / JWT| Gateway["Spring Boot Backend (Port 8080)"]
    
    subgraph "Backend Modular Monolith (com.emblemix)"
        Gateway --> Auth["Authentication & Security Filter"]
        Auth --> Tenant["Tenant Context Enforcer"]
        
        Tenant --> SystemMgr["Connected Systems Management"]
        Tenant --> IncidentMgr["Incident Management Lifecycle"]
        Tenant --> RulesEngine["Alert Rule Engine"]
        Tenant --> ReportingMgr["Reporting & Analytics"]
        Tenant --> NotificationMgr["Notification Service"]
        Tenant --> WebhookMgr["Custom Webhook Ingestion & HMAC"]
        
        subgraph "Monitoring & Ingestion Engines"
            WebMon["Website Monitoring (SSRF Protected)"]
            SFMon["Salesforce Monitoring (REST / Limits)"]
            HookMon["Custom Webhook Receiver (HMAC-SHA256)"]
        end
        
        subgraph "Common Ingestion Pipeline"
            EventIngest["Normalized Event Ingestion"]
            EventNorm["Fingerprint & Deduplication"]
        end
        
        WebMon --> EventIngest
        SFMon --> EventIngest
        HookMon --> EventIngest
        EventIngest --> EventNorm
        EventNorm --> RulesEngine
        RulesEngine --> IncidentMgr
        IncidentMgr --> NotificationMgr
    end
    
    Tenant --> DB[("PostgreSQL 16 (Flyway Migrations)")]
    NotificationMgr -.-> Mailpit["SMTP Mailer (Mailpit / SES)"]
    SFMon -.-> SF["Salesforce External Client App (OAuth 2.0)"]
    WebMon -.-> Web["Target Public Websites"]
```

---

## 2. Event-Driven Incident Lifecycle

```mermaid
sequenceDiagram
    autonumber
    participant Engine as Monitoring Engine (Website / Salesforce)
    participant Ingestion as Event Ingestion Service
    participant Rules as Alert Rule Engine
    participant IncService as Incident Service
    participant Notify as Notification Service
    participant DB as PostgreSQL Database

    Engine->>Ingestion: Emit Normalized Event (Failure or Recovery)
    Ingestion->>Ingestion: Calculate Deterministic SHA-256 Fingerprint
    
    alt Event is Failure
        Ingestion->>Rules: Evaluate Active Rules & Consecutive Counts
        Rules-->>Ingestion: Target Severity (CRITICAL / HIGH / MEDIUM / LOW)
        Ingestion->>IncService: createOrGroupIncident(Event, Severity)
        IncService->>DB: Check for Active Incident with Matching Fingerprint
        alt Matching Active Incident Found
            IncService->>DB: Link Event & Escalate Severity if Higher
        else No Matching Incident
            IncService->>DB: Insert New Incident & Initial Timeline History
            IncService->>Notify: Asynchronously Dispatch Alerts (Email / Retries)
        end
    else Event is Recovery
        Ingestion->>IncService: autoResolveByFingerprint(Fingerprint, Notes)
        IncService->>DB: Transition Active Incidents to RESOLVED
        IncService->>Notify: Dispatch Resolution Notifications
    end
```

---

## 3. Package Structure

The backend source code strictly enforces domain boundaries under `com.emblemix`:
- `auth`: JWT token authentication, refresh rotation, password recovery.
- `organization`: Tenant organization domain and workspace isolation.
- `user`: User identities, profiles, and role-based permissions (`ORG_ADMIN`, `ENGINEER`, `VIEWER`).
- `system`: Connected system catalogue and multi-environment tracking.
- `integration`: Encrypted credential persistence and connector abstractions.
- `monitoring.website`: SSRF-protected HTTP/HTTPS scheduler and SSL certificate inspector.
- `monitoring.salesforce`: OAuth 2.0 Web Server code exchange, API limits, and Async Apex Job tracking.
- `event`: Normalized event model, fingerprint generation, and ingestion pipeline.
- `alert`: Configurable threshold evaluation and rule engine.
- `incident`: Complete incident lifecycle (OPEN, ACKNOWLEDGED, RESOLVED), grouping, timeline, and comments.
- `notification`: Multi-channel asynchronous notification dispatch with exponential retries.
- `reporting`: Aggregation engine, uptime metrics, and RFC 4180 CSV exports.
- `audit`: Immutable security audit trails.
- `common`: Cross-cutting security, encryption, global exception handling, and response models.
