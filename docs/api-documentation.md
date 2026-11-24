# API Documentation: Emblemix Platform

All Emblemix REST APIs are versioned under the `/api/v1` base path. Authentication uses `Bearer <JWT_TOKEN>` passed in the `Authorization` header.

Swagger UI is interactively available at:
```
http://localhost:8080/swagger-ui.html
```
OpenAPI v3 JSON specification is available at:
```
http://localhost:8080/v3/api-docs
```

---

## 1. Authentication (`/api/v1/auth`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/auth/register` | Register new organization and initial admin account | No |
| `POST` | `/auth/login` | Authenticate using email and password | No |
| `POST` | `/auth/refresh` | Rotate refresh token and issue new access token | No |
| `POST` | `/auth/logout` | Revoke active refresh token | No |
| `POST` | `/auth/forgot-password` | Request password reset token | No |
| `POST` | `/auth/reset-password` | Apply new password using reset token | No |

---

## 2. Connected Systems (`/api/v1/systems`)

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| `GET` | `/systems` | List connected systems (paginated) | Any |
| `POST` | `/systems` | Register a new connected system | `ORG_ADMIN`, `ENGINEER` |
| `GET` | `/systems/{id}` | Get connected system details | Any |
| `PUT` | `/systems/{id}` | Update connected system metadata | `ORG_ADMIN`, `ENGINEER` |
| `DELETE` | `/systems/{id}` | Delete connected system | `ORG_ADMIN`, `ENGINEER` |

---

## 3. Website Monitoring (`/api/v1/websites`)

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| `GET` | `/websites` | List website monitoring configurations | Any |
| `POST` | `/websites` | Register new website for automated checks | `ORG_ADMIN`, `ENGINEER` |
| `GET` | `/websites/{id}` | Get website configuration | Any |
| `PUT` | `/websites/{id}` | Update website configuration | `ORG_ADMIN`, `ENGINEER` |
| `POST` | `/websites/{id}/test` | Execute on-demand live HTTP & SSL diagnostic test | Any |
| `GET` | `/websites/{id}/monitoring-history` | Get latency and check result logs | Any |

---

## 4. Salesforce Integration (`/api/v1/integrations/salesforce`)

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| `GET` | `/integrations/salesforce` | List Salesforce configurations | Any |
| `POST` | `/integrations/salesforce` | Register Salesforce org | `ORG_ADMIN`, `ENGINEER` |
| `GET` | `/integrations/salesforce/authorize` | Generate OAuth 2.0 Web Server authorization URL | `ORG_ADMIN`, `ENGINEER` |
| `GET` | `/integrations/salesforce/callback` | OAuth 2.0 callback for token exchange | No |
| `POST` | `/integrations/salesforce/{id}/test` | Probe live API limits and failed Apex jobs | Any |
| `POST` | `/integrations/salesforce/{id}/disconnect` | Revoke credentials and disconnect integration | `ORG_ADMIN`, `ENGINEER` |

---

## 5. Incidents (`/api/v1/incidents`)

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| `GET` | `/incidents` | List incidents with status and severity filters | Any |
| `GET` | `/incidents/{id}` | Get incident details, timeline, and comments | Any |
| `POST` | `/incidents/{id}/acknowledge` | Acknowledge active incident | `ORG_ADMIN`, `ENGINEER` |
| `POST` | `/incidents/{id}/assign` | Assign incident to an engineer | `ORG_ADMIN`, `ENGINEER` |
| `POST` | `/incidents/{id}/resolve` | Mark incident resolved with root cause notes | `ORG_ADMIN`, `ENGINEER` |
| `POST` | `/incidents/{id}/comments` | Add comment or investigation note | Any |

---

## 6. Alert Rules (`/api/v1/alert-rules`)

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| `GET` | `/alert-rules` | List all alert rules | Any |
| `POST` | `/alert-rules` | Create configurable alert rule | `ORG_ADMIN`, `ENGINEER` |
| `GET` | `/alert-rules/{id}` | Get alert rule details | Any |
| `PUT` | `/alert-rules/{id}` | Update alert rule parameters | `ORG_ADMIN`, `ENGINEER` |
| `DELETE` | `/alert-rules/{id}` | Delete alert rule | `ORG_ADMIN`, `ENGINEER` |

---

## 7. Reports & Analytics (`/api/v1/reports`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/reports/overview` | Aggregated dashboard counters and distributions |
| `GET` | `/reports/incidents` | 14-day daily incident trend data |
| `GET` | `/reports/uptime` | System uptime percentages and latency stats |
| `GET` | `/reports/export/incidents.csv` | RFC 4180 CSV export of incidents |
