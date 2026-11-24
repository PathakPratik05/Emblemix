# Security Architecture & Multi-Tenancy

Emblemix is built with security-first architecture across all layers.

---

## 1. Authentication & Session Security

- **Password Hashing**: BCrypt with salted rounds.
- **Stateless JWTs**: Access tokens signed using HMAC-SHA256 (`jjwt-api`).
- **Refresh Token Rotation**: Each token refresh revokes the existing token and issues a newly generated token, limiting window of vulnerability.
- **Secure Password Reset**: One-time UUID tokens expiring in 1 hour. All active refresh tokens are revoked upon password change.

---

## 2. Multi-Tenancy & Tenant Isolation

- **ThreadLocal Tenant Context**: `TenantContext` tracks the authenticated organization partition.
- **Authorized Scoping**: `SecurityUtils.getRequiredCurrentOrgId()` guarantees all database queries are scoped by `organization_id`.
- **Role-Based Access Control**:
  - `SUPER_ADMIN`: Cross-tenant administration.
  - `ORG_ADMIN`: Organization settings and team member management.
  - `ENGINEER`: System configuration, incident operations, alert rules.
  - `VIEWER`: Read-only access to dashboards, reports, and monitors.

---

## 3. Credential Encryption at Rest

- All external OAuth tokens (`access_token`, `refresh_token`) are encrypted before database insertion using **AES-256 GCM** (`AesEncryptionUtil`).
- Unique 96-bit Initialization Vectors (IV) are generated per encryption operation to ensure identical tokens produce distinct ciphertexts.
- Tokens and secrets are never logged in application logs.

---

## 4. SSRF Defense-in-Depth

- Restricts monitoring target protocols strictly to `http` and `https`.
- Resolves all DNS records before connecting and verifies IP ranges against loopback, private networks (RFC 1918), link-local addresses, and cloud metadata targets (`169.254.169.254`).
- Sanitizes redirect targets with identical SSRF validation rules.

---

## 5. Webhook HMAC-SHA256 & Timing Attack Defense

- Inbound webhooks are authenticated via HMAC-SHA256 using an organization-isolated shared secret.
- Constant-time message comparison via `MessageDigest.isEqual(...)` prevents timing side-channel attacks.
- Webhook secrets are stored encrypted at rest with AES-256-GCM.
- Secrets support on-demand rotation via authenticated management APIs.
