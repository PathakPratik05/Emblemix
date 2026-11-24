# Emblemix — Custom Webhook Integration

## 1. Overview

Emblemix provides **Inbound Custom Webhook Integrations** that allow external applications, CI/CD pipelines, third-party monitoring tools (e.g. Datadog, Stripe, GitHub, custom microservices), and scripts to push events directly into the Emblemix incident engine.

When a webhook payload is received:
1. The endpoint validates the **HMAC-SHA256 signature** against the webhook's encrypted shared secret.
2. The payload is parsed and converted into a **Normalized Event**.
3. If the payload indicates a recovery state (`RESOLVED`, `RECOVERED`, `OK`), existing open incidents for that system are **automatically resolved**.
4. If the payload indicates a failure or warning, the **Alert Rule Engine** evaluates thresholds and creates or groups an **Incident**.
5. Every webhook attempt is recorded in immutable **Webhook Delivery Logs**.

---

## 2. Security Architecture

### 2.1 Secret Key Management
- Upon webhook creation or secret rotation, Emblemix generates a cryptographically secure 32-byte (64-character hexadecimal) random key using `java.security.SecureRandom`.
- The secret is immediately encrypted at rest using **AES-256-GCM authenticated encryption** via `AesEncryptionUtil`.
- The plaintext secret is displayed **exactly once** in the response and UI modal. It is never logged or retrievable in plaintext again.

### 2.2 HMAC-SHA256 Signature Verification
External callers must calculate an HMAC-SHA256 hash of the exact raw HTTP request body bytes using their shared webhook secret:

$$\text{Signature} = \text{HMAC-SHA256}(\text{Secret}, \text{RawPayloadBytes})$$

The signature must be transmitted in either of the following HTTP request headers:
- `X-Emblemix-Signature: sha256=<hex_digest>` (or raw hex)
- `X-Hub-Signature-256: sha256=<hex_digest>`

To prevent timing side-channel attacks, Emblemix performs constant-time byte comparisons using `java.security.MessageDigest.isEqual(...)`.

### 2.3 Secret Rotation
Webhooks support zero-downtime secret rotation via:
```http
POST /api/v1/webhooks/{id}/rotate-secret
```
This generates a new secret key, re-encrypts it, and returns the new key to the caller.

---

## 3. Webhook Endpoints & REST API

### 3.1 Ingestion Endpoint (Public / Signature Authenticated)
```http
POST /api/v1/webhooks/receive/{webhookId}
Content-Type: application/json
X-Emblemix-Signature: sha256=<hex_signature>
```

#### Request Payload Specification
```json
{
  "title": "Payment Gateway Timeout",
  "message": "Downstream payment processor returned 504 Gateway Timeout on checkout API.",
  "severity": "HIGH",
  "status": "FAIL",
  "eventType": "WEBHOOK_INCIDENT",
  "details": {
    "gateway": "Stripe",
    "endpoint": "/v1/charges",
    "errorCount": 15
  },
  "timestamp": "2026-11-24T12:00:00Z"
}
```

| Field | Type | Description |
|---|---|---|
| `title` | String | Optional summary title for the incident. |
| `message` | String | Incident description or root cause information. |
| `severity` | String | Severity rating: `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW`. Default: `HIGH`. |
| `status` | String | State indicator: `FAIL`, `WARN`, `DOWN`, `RESOLVED`, `RECOVERED`, or `OK`. |
| `details` | Object | Arbitrary JSON key-value pairs stored in the event record. |
| `timestamp` | String | ISO 8601 timestamp. Defaults to arrival time if omitted. |

#### Auto-Recovery Payload
Sending a status of `RESOLVED`, `RECOVERED`, or `OK` resolves matching incidents:
```json
{
  "title": "Payment Gateway Recovered",
  "message": "Payment gateway latency returned below 150ms. Error rate 0%.",
  "status": "RESOLVED"
}
```

---

## 4. Code Integration Examples

### 4.1 cURL / Bash
```bash
SECRET="your_webhook_secret_here"
WEBHOOK_URL="http://localhost:8080/api/v1/webhooks/receive/<WEBHOOK_UUID>"
PAYLOAD='{"title":"Database Outage","message":"Replica lag exceeded 60s","severity":"CRITICAL","status":"FAIL"}'

# Calculate HMAC-SHA256 signature
SIG=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | cut -d' ' -f2)

# Send request
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -H "X-Emblemix-Signature: sha256=$SIG" \
  -d "$PAYLOAD"
```

### 4.2 Python 3
```python
import hmac
import hashlib
import json
import requests

secret = "your_webhook_secret_here"
webhook_url = "http://localhost:8080/api/v1/webhooks/receive/<WEBHOOK_UUID>"

payload = {
    "title": "Service Memory Spike",
    "message": "Container memory consumption exceeded 92%",
    "severity": "HIGH",
    "status": "FAIL",
    "details": {"pod": "auth-service-7f89b-4x1", "usage": "94.2%"}
}

payload_bytes = json.dumps(payload, separators=(',', ':')).encode('utf-8')
signature = hmac.new(secret.encode('utf-8'), payload_bytes, hashlib.sha256).hexdigest()

headers = {
    "Content-Type": "application/json",
    "X-Emblemix-Signature": f"sha256={signature}"
}

response = requests.post(webhook_url, data=payload_bytes, headers=headers)
print(f"Status: {response.status_code}, Response: {response.json()}")
```

### 4.3 Node.js (TypeScript)
```typescript
import crypto from 'crypto';
import fetch from 'node-fetch';

const secret = 'your_webhook_secret_here';
const webhookUrl = 'http://localhost:8080/api/v1/webhooks/receive/<WEBHOOK_UUID>';

const payload = JSON.stringify({
  title: 'Worker Queue Stalled',
  message: 'Backlog in priority queue exceeds 5,000 messages',
  severity: 'HIGH',
  status: 'FAIL',
});

const signature = crypto
  .createHmac('sha256', secret)
  .update(payload)
  .digest('hex');

await fetch(webhookUrl, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Emblemix-Signature': `sha256=${signature}`,
  },
  body: payload,
});
```

---

## 5. Webhook Delivery Auditing

Every inbound request creates an immutable `WebhookDeliveryLog`:
- **`SUCCESS`** (200): Signature verified, payload parsed, event normalized and routed.
- **`INVALID_SIGNATURE`** (401): Missing or mismatched HMAC-SHA256 signature.
- **`INVALID_PAYLOAD`** (400): Malformed JSON body or unparseable fields.
- **`SYSTEM_DISABLED`** (403): The webhook endpoint has been disabled by an administrator.

Delivery history is visible directly in the **Webhooks Management Console** under `/webhooks`.
