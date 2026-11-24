# Testing Strategy: Emblemix Platform

Emblemix employs a multi-tiered automated testing pyramid ensuring reliability, regression resistance, and security boundary verification.

---

## 1. Testing Pyramid

```
       / \
      / E2E \       Playwright End-to-End User Journeys
     /-------\
    /  Integ  \     WireMock API Tests, MockMvc, Testcontainers
   /-----------\
  /    Unit     \   JUnit 5, Mockito, Vitest, React Testing Library
 /---------------\
```

---

## 2. Test Execution Commands

### Backend Automated Tests
Execute all unit and WireMock integration tests:
```bash
cd backend
mvn test
```

Generate test coverage report:
```bash
cd backend
mvn clean test jacoco:report
```

### Frontend Automated Tests
Execute unit and component tests via Vitest:
```bash
cd frontend
npm test
```

Execute Playwright browser end-to-end tests:
```bash
cd frontend
npm run test:e2e
```

---

## 3. Test Coverage Areas

1. **SSRF Protections**: Validates rejection of private IP ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.169.254), loopback addresses, metadata hostnames, and invalid URL schemes.
2. **AES-256 GCM Encryption**: Validates roundtrip encryption/decryption, non-deterministic IV generation, and null safety.
3. **Fingerprint Deduplication**: Verifies deterministic SHA-256 hash generation for normalized event grouping.
4. **Authentication & Multi-Tenancy**: Tests login, registration, token refresh, duplicate prevention, and tenant boundary enforcement.
5. **Alert Rule Evaluation**: Asserts threshold triggers for failure counts, latency limits, and API usage percentages.
6. **Incident Lifecycle**: Validates incident creation, duplicate event grouping, auto-resolution on recovery, and state transitions.
7. **WireMock Salesforce Integration**: Realistic simulation of Salesforce OAuth 2.0 token exchange and `/services/data/v61.0/limits` endpoints.
8. **Frontend Components**: Tests rendering, forms, button disabled/loading states, and status badges.
