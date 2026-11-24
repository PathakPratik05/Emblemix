# Governed Analytics & KPI Definitions

To prevent organizational discrepancies, **Emblemix enforces a single source of truth** for all site reliability, incident management, and availability metrics.

Whether metrics are viewed in the Next.js Executive Dashboard, team workspaces, or Power BI tabular models, the mathematical formulas and operational criteria remain strictly consistent.

---

## 1. Core Incident & Operational KPIs

### 1.1 Mean Time to Acknowledge (MTTA)

**Definition**: The average duration (in minutes) from when an incident is automatically triggered to when an engineer or responder marks the incident as `ACKNOWLEDGED` or `INVESTIGATING`.

$$\text{MTTA} = \frac{\sum_{i=1}^{N} \max(0, \text{acknowledgedAt}_i - \text{createdAt}_i)}{N}$$

- **Population ($N$)**: All incidents in the measurement window where $\text{acknowledgedAt}$ is not null.
- **Unit**: Minutes (rounded to 1 decimal place).
- **Target SLA**: $< 15$ minutes for `CRITICAL` severity; $< 60$ minutes for `HIGH` severity.

---

### 1.2 Mean Time to Resolve (MTTR)

**Definition**: The average duration (in minutes) from incident creation to when the incident is resolved (`status = RESOLVED` or `CLOSED`).

$$\text{MTTR} = \frac{\sum_{i=1}^{M} \max(0, \text{resolvedAt}_i - \text{createdAt}_i)}{M}$$

- **Population ($M$)**: All incidents in the measurement window where $\text{resolvedAt}$ is not null.
- **Unit**: Minutes (rounded to 1 decimal place).
- **Target SLA**: $< 120$ minutes for `CRITICAL` incidents.

---

### 1.3 SLA Achievement Percentage

**Definition**: The ratio of resolved incidents that met target operational resolution thresholds over total resolved incidents.

$$\text{SLA Compliance} = \left( \frac{\text{Count of incidents where MTTR} \le \text{Threshold}}{\text{Total Resolved Incidents}} \right) \times 100\%$$

- **Threshold**:
  - `CRITICAL`: 120 minutes (2 hours).
  - `HIGH`: 240 minutes (4 hours).
  - `MEDIUM`: 480 minutes (8 hours).
  - `LOW`: 1440 minutes (24 hours).
- **Default Base Target**: $\ge 99.0\%$.

---

### 1.4 Availability Percentage vs. Uptime Percentage

Emblemix differentiates between synthetic endpoint reachability and full service health:

#### Availability Percentage
Measures the percentage of synthetic health checks that returned `SUCCESS`:

$$\text{Availability } \% = \left( \frac{\text{Successful Synthetic Checks}}{\text{Total Synthetic Checks Recorded}} \right) \times 100\%$$

- Calculated over rolling 24-hour, 7-day, or 30-day windows.
- Inclusions: HTTP 2xx/3xx codes for websites, valid token & API limit $< 80\%$ for Salesforce orgs.

#### Uptime Percentage
Measures the scheduled runtime of a service excluding downtime caused by `OPEN`, `ACKNOWLEDGED`, or `INVESTIGATING` incidents:

$$\text{Uptime } \% = \left( 1 - \frac{\sum \text{Downtime Minutes}}{\text{Total Calendar Period Minutes}} \right) \times 100\%$$

---

## 2. Team Performance Metrics

Each engineering team (`DEVOPS`, `SALESFORCE`, `WEB_ENGINEERING`, etc.) is evaluated on team-specific performance cards:

| Metric | Calculation | Notes |
| :--- | :--- | :--- |
| **Active Incidents** | Count of incidents where `assigned_team_id = :teamId` and `status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING')` | Real-time queue depth |
| **Team MTTA** | Average acknowledgement time for incidents owned by the team | Response velocity |
| **Team MTTR** | Average resolution time for incidents resolved by the team | Recovery capability |
| **Team SLA %** | Percentage of team-owned incidents meeting SLA thresholds | Service quality guarantee |

---

## 3. Implementation in Code

All calculations are enforced in [`com.emblemix.analytics.AnalyticsService`](file:///Users/pratikpathak/projects/Emblemix/backend/src/main/java/com/emblemix/analytics/AnalyticsService.java):

```java
// Strict MTTA / MTTR / SLA calculation ensures 1:1 match with Power BI DAX
double mtta = acknowledgedCount > 0 ? (double) totalMttaMinutes / acknowledgedCount : 0.0;
double mttr = resolvedCount > 0 ? (double) totalMttrMinutes / resolvedCount : 0.0;
double slaPct = resolvedCount > 0 ? ((double) withinSlaCount / resolvedCount) * 100.0 : 100.0;
```
