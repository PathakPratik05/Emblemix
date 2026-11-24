# Emblemix Multi-Disciplinary Engineering Collaboration

## 1. Overview & Objective

Modern IT organizations operate with diverse engineering disciplines: DevOps engineers, Salesforce developers, website reliability engineers, BI and data analysts, full-stack application engineers, and incident commanders. 

Historically, each team operated in isolated silos—DevOps in Grafana/Datadog, Salesforce in Salesforce Setup/Event Monitoring, Web engineers in Pingdom/Cloudflare, and Analysts in disconnected BI spreadsheets.

**Emblemix** consolidates these specialties into **One Platform. Every System. Every Incident.**

---

## 2. Shared Foundations

All engineering disciplines share:
1. **One Organization & Identity Model**: Multi-tenant isolation with granular RBAC (`SUPER_ADMIN`, `ADMIN`, `MEMBER`, `VIEWER`).
2. **One Connected Systems Registry**: Every website, Salesforce org, custom webhook, API, and infrastructure node is registered as a first-class `ConnectedSystem`.
3. **One Normalized Ingestion Engine**: All health checks, alerts, and metrics map to standard `NormalizedEvent` records.
4. **One Incident Orchestrator**: Automated deduplication, fingerprinting, alerting, team routing, timeline tracking, and resolution workflows.
5. **One Governed Analytics Layer**: Single source of truth for MTTA, MTTR, SLA achievement, uptime, and availability.

---

## 3. Teams & Ownership Model

### 3.1 Organization-Level Teams

Emblemix supports the following initial team disciplines:
- `DEVOPS`: Cloud infrastructure, Kubernetes, microservices, container health, deployment events.
- `SALESFORCE`: Production and sandbox orgs, API governor limits, Apex job failures, login exceptions.
- `WEB_ENGINEERING`: Public web portals, customer-facing sites, SSL certificates, latency spikes.
- `DATA_ANALYTICS`: Data warehouses, ETL pipelines, Power BI tabular datasets, ingestion latency.
- `APPLICATION_ENGINEERING`: Core business services, custom API webhooks, downstream payment/auth dependencies.
- `SECURITY`: WAF anomalies, unauthorized access, credential abuse, security compliance.
- `SUPPORT`: Tier-1/Tier-2 customer support escalations, external user-reported outages.

### 3.2 Team Roles & RBAC

A user may belong to multiple teams:
- **LEAD**: Oversees the team, assigns system ownership, configures escalation policies, and triages critical incidents.
- **MEMBER**: Resolves assigned incidents, monitors workspace telemetry, and posts investigation comments.

Team membership is distinct from organization RBAC roles (`ADMIN`, `MEMBER`, `VIEWER`).

---

## 4. System Ownership & Metadata

Every connected system maintains explicit ownership attributes:
- **Owning Team (`owning_team_id`)**: The primary engineering team responsible for the system's operational SLA.
- **Primary Owner (`primary_owner_id`)**: Lead engineer directly alerted on critical alerts.
- **Backup Owner (`backup_owner_id`)**: Secondary responder alerted during escalation or out-of-hours coverage.
- **Business Criticality (`criticality`)**: `TIER_1` (Mission Critical), `TIER_2` (Business Important), `TIER_3` (Internal/Non-critical).
- **Escalation Policy (`escalation_policy`)**: Rule matrix defining escalation wait times before alerting Tier 2/3.

---

## 5. Intelligent Incident Routing

When a monitoring failure or webhook triggers an incident:
1. **System Identification**: The system determines the affected `ConnectedSystem`.
2. **Team Auto-Assignment**: If the system has an `owningTeamId`, the incident is automatically assigned to that team.
3. **Owner Routing**: If the system has a `primaryOwnerId`, the incident is pre-assigned to that engineer.
4. **Timeline Auditing**: An audit entry `ASSIGNED_TEAM` or `ASSIGNED_USER` is automatically appended to the incident's immutable timeline.
5. **Workspace Scoping**: The incident appears in the team's dedicated workspace and team incident queue.
6. **Escalation Handling**: Responders can escalate unresolved incidents (`POST /api/v1/incidents/{id}/escalate`), bumping the escalation tier and recording audit logs.

---

## 6. Engineering Team Workspaces

Emblemix provides 5 specialized workspaces accessible via `/workspaces/*`:

| Workspace | URL | Target Audience | Primary Telemetry |
| :--- | :--- | :--- | :--- |
| **DevOps** | `/workspaces/devops` | SREs, Platform Engineers | Infrastructure nodes, APIs, deployment logs, system health |
| **Salesforce** | `/workspaces/salesforce` | Salesforce Architects/Devs | Prod & Sandbox orgs, API governor limits, Apex jobs, OAuth health |
| **Web Engineering** | `/workspaces/web` | Frontend & Web Engineers | Synthetic ping latency, HTTP status codes, SSL certificate expiry |
| **Data & Analytics** | `/workspaces/analytics` | BI Engineers, Data Analysts | Governed KPIs (MTTA, MTTR, SLA), Power BI Embedded reports, OData endpoints |
| **Application Eng** | `/workspaces/application` | Full-Stack & Backend Devs | Custom Webhook integrations, API endpoints, payload validation logs |
