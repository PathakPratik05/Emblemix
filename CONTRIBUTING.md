# Contributing to Emblemix

Thank you for contributing to Emblemix — the Universal Incident Management & Monitoring Platform.

Emblemix is built as a collaborative platform bridging DevOps, Salesforce developers, Web engineers, Data/BI analysts, and Security responders into a unified operational ecosystem.

---

## 1. Multi-Disciplinary Architecture Principles

1. **Single Source of Truth**:
   - The Java 21 / Spring Boot 3 modular monolith backend manages all business logic, incident orchestration, alert rules, and persistence.
   - Frontend is built with Next.js 14 App Router, TypeScript (strict mode), and Tailwind CSS.
2. **Normalized Ingestion Pipeline**:
   - Every monitoring event, whether from Website synthetic checks, Salesforce REST polls, or custom webhooks, flows through the unified normalization engine.
3. **Governed Analytics**:
   - Never compute custom MTTA, MTTR, or SLA definitions outside the backend analytics service. Power BI tabular models and Web dashboards share identical governed contracts.
4. **Tenant Isolation**:
   - All queries, mutations, and analytics endpoints must resolve through `SecurityUtils.getRequiredCurrentOrgId()`.

---

## 2. Development Setup

### Prerequisites
- **Java**: 21 (Temurin / OpenJDK)
- **Maven**: 3.9+
- **Node.js**: 20+
- **Docker & Docker Compose**: latest

### Running Locally

```bash
# 1. Start PostgreSQL (and optional Redis)
docker-compose up -d postgres

# 2. Run Backend
cd backend
mvn clean spring-boot:run

# 3. Run Frontend
cd ../frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:3000` and the backend API at `http://localhost:8080`.

---

## 3. Pull Request Guidelines

1. Create a feature branch from `main`: `git checkout -b feat/my-new-feature` or `fix/my-bug-fix`.
2. Ensure all tests pass:
   ```bash
   # Backend
   cd backend && mvn clean test

   # Frontend
   cd frontend && npm test && npm run build
   ```
3. Submit a Pull Request targeting `main`, using the provided PR template.
4. Obtain review from the relevant domain code owners specified in `.github/CODEOWNERS`.
