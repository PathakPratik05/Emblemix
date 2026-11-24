# Salesforce Integration Guide

Emblemix integrates with Salesforce using the **OAuth 2.0 Web Server Flow (Authorization Code Grant)** via Salesforce External Client Apps / Connected Apps.

---

## 1. Prerequisites & Salesforce Setup

1. In your Salesforce Production or Sandbox Org, navigate to **Setup > App Manager** (or **External Client App Manager**).
2. Create a new Connected App or External Client App:
   - **Connected App Name**: `Emblemix Monitoring`
   - **Contact Email**: `dev@emblemix.com`
   - **Enable OAuth Settings**: Checked
   - **Callback URL**: `http://localhost:5173/integrations/salesforce/callback` (or your production domain callback)
   - **Selected OAuth Scopes**:
     - `Manage user data via APIs (api)`
     - `Perform requests at any time (refresh_token, offline_access)`
3. Save the application and note:
   - **Consumer Key** (`SALESFORCE_CLIENT_ID`)
   - **Consumer Secret** (`SALESFORCE_CLIENT_SECRET`)

---

## 2. Configuration Environment Variables

Set the following in `.env`:
```bash
SALESFORCE_CLIENT_ID=your-consumer-key
SALESFORCE_CLIENT_SECRET=your-consumer-secret
SALESFORCE_REDIRECT_URI=http://localhost:5173/integrations/salesforce/callback
SALESFORCE_PRODUCTION_LOGIN_URL=https://login.salesforce.com
SALESFORCE_SANDBOX_LOGIN_URL=https://test.salesforce.com
SALESFORCE_API_VERSION=v61.0
```

---

## 3. Monitored Capabilities

### A. Daily REST API Request Limits
- **Endpoint**: `/services/data/v61.0/limits`
- Extracts `DailyApiRequests.Max` and `DailyApiRequests.Remaining`.
- Calculates percentage consumed.
- Emits `SALESFORCE_API_LIMIT_WARNING` if usage exceeds warning threshold (default 80%).
- Emits `SALESFORCE_API_LIMIT_CRITICAL` if usage exceeds critical threshold (default 95%).

### B. Async Apex Job Failures
- **Endpoint**: `/services/data/v61.0/tooling/query?q=...`
- Queries `AsyncApexJob` for jobs in `Failed` or `Aborted` state.
- Emits `APEX_JOB_FAILED` event with job type and extended status error message.

### C. Automated Token Refresh
- If an access token expires during monitoring, `SalesforceMonitoringService` automatically invokes the token refresh endpoint with the decrypted refresh token, updates the encrypted credentials in the database, and resumes monitoring transparently.
