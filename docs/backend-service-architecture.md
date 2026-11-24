# Emblemix — Backend Service Interface and Implementation Architecture

## 1. Architectural Overview & Design Principles

Emblemix enforces a strict **Service Interface + Service Implementation** separation across its Spring Boot 3.x / Java 21 backend.

This architectural pattern adheres to core software engineering principles:

- **Dependency Inversion Principle (DIP)**: High-level modules (Controllers, cross-module services) depend on abstractions (interfaces), not on concrete implementations.
- **Interface Segregation Principle (ISP)**: Clients only know the method contracts they need, decoupling controller API contracts from underlying persistence and processing logic.
- **Testability & Mocking**: Tests can readily inject mock implementations or test doubles without requiring Spring application context startup or concrete class instantiation.
- **Maintainability & Extensibility**: Implementation logic, third-party libraries, caching layers, and database queries can be refactored or replaced without altering controller definitions or client contracts.

---

## 2. Package Structure Conventions

For every domain module in `com.emblemix.*`, services follow this strict hierarchy:

```text
com.emblemix.<module>/
├── controller/
│   └── <Module>Controller.java
├── dto/
│   ├── <Module>Request.java
│   └── <Module>Response.java
├── entity/
│   └── <Module>Entity.java
├── repository/
│   └── <Module>Repository.java
└── service/
    ├── <Name>Service.java           <-- Public interface defining business contracts
    └── impl/
        └── <Name>ServiceImpl.java   <-- Concrete implementation with @Service and @Transactional
```

### Key Rules:
1. **Interface Placement**: `<module>.service.<Name>Service`
   - Pure Java interface.
   - Declares public business methods, parameter contracts, and return DTOs or entities.
   - Contains **no** business logic, SQL queries, or repository dependencies.
   - No `@Service` annotation on interfaces.
2. **Implementation Placement**: `<module>.service.impl.<Name>ServiceImpl`
   - Annotated with `@Service`.
   - Annotated with `@Transactional` / `@Transactional(readOnly = true)`.
   - Implements the corresponding `<Name>Service` interface.
   - Contains private dependencies (`Repository`, other `Service` interfaces, `PasswordEncoder`, etc.).
   - Utilizes constructor-based dependency injection.
   - Encapsulates execution logs (`LoggerFactory.getLogger(...)`).

---

## 3. Dependency Injection Rules

### Rule 1: Controllers Inject Interfaces Only
Controllers must never reference concrete `*ServiceImpl` classes. They must strictly declare dependencies on the service interface:

```java
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    // Correct: injects the interface
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }
}
```

### Rule 2: Inter-Service Dependencies Inject Interfaces Only
When a service implementation depends on another domain service, it must inject the target service interface, not the concrete implementation:

```java
@Service
@Transactional
public class IncidentServiceImpl implements IncidentService {

    // Correct: injects the NotificationService interface
    private final NotificationService notificationService;
    private final IncidentRepository incidentRepository;

    public IncidentServiceImpl(
            IncidentRepository incidentRepository,
            NotificationService notificationService) {
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
    }
}
```

---

## 4. Transaction Management Conventions

Transaction boundaries are managed at the service implementation layer using Spring's `@Transactional`:

- **Default Read-Only**: Read-heavy query services or class-level defaults use `@Transactional(readOnly = true)` to optimize database connection and Hibernate snapshot management.
- **Write Operations**: Mutating operations (create, update, delete, status transitions) override with `@Transactional` on the method level, ensuring atomic commits and proper rollbacks on unchecked exceptions (`RuntimeException`).

```java
@Service
@Transactional(readOnly = true)
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;

    @Override
    public List<TeamResponse> getTeams(UUID orgId) {
        // Read-only execution
        return teamRepository.findByOrganizationId(orgId)...;
    }

    @Override
    @Transactional
    public TeamResponse createTeam(UUID orgId, CreateTeamRequest request) {
        // Mutating transaction
        ...
    }
}
```

---

## 5. Comprehensive Module & Service Mapping

Emblemix implements **18 services across 15 core modules**, all fully conforming to the interface-implementation separation:

| Module | Interface | Implementation | Responsibility |
| :--- | :--- | :--- | :--- |
| `com.emblemix.auth` | `AuthService` | `AuthServiceImpl` | Registration, login, JWT token issuance, refresh tokens |
| `com.emblemix.user` | `UserService` | `UserServiceImpl` | User profile retrieval, password management |
| `com.emblemix.organization` | `OrganizationService` | `OrganizationServiceImpl` | Organization tenant creation, updates, membership |
| `com.emblemix.system` | `ConnectedSystemService` | `ConnectedSystemServiceImpl` | System registry, health status, team ownership, SLA config |
| `com.emblemix.monitoring.website` | `WebsiteMonitoringService` | `WebsiteMonitoringServiceImpl` | HTTP/HTTPS checks, SSL monitoring, latency metrics, cron scheduler |
| `com.emblemix.monitoring.salesforce` | `SalesforceOAuthService` | `SalesforceOAuthServiceImpl` | Salesforce OAuth2 authorization, token exchange, refresh |
| `com.emblemix.monitoring.salesforce` | `SalesforceMonitoringService` | `SalesforceMonitoringServiceImpl` | API usage limits, Apex error tracking, scheduled pollers |
| `com.emblemix.webhook` | `WebhookService` | `WebhookServiceImpl` | Inbound webhook configuration, signature HMAC verification, replay protection |
| `com.emblemix.event` | `EventNormalizationService` | `EventNormalizationServiceImpl` | Canonical event schema mapping from diverse sources |
| `com.emblemix.event` | `EventIngestionService` | `EventIngestionServiceImpl` | Normalized event ingestion, alert rule evaluation trigger |
| `com.emblemix.alert` | `AlertRuleService` | `AlertRuleServiceImpl` | Alert rule CRUD, threshold condition matching |
| `com.emblemix.incident` | `IncidentService` | `IncidentServiceImpl` | Incident lifecycle, severity assignment, team routing, timeline tracking |
| `com.emblemix.notification` | `NotificationService` | `NotificationServiceImpl` | Incident alert dispatch (email, webhook channels) |
| `com.emblemix.notification` | `NotificationChannelService` | `NotificationChannelServiceImpl` | Notification channel configuration & delivery logs |
| `com.emblemix.reporting` | `ReportingService` | `ReportingServiceImpl` | System availability, incident metrics, MTTA/MTTR calculation |
| `com.emblemix.analytics` | `AnalyticsService` | `AnalyticsServiceImpl` | Governed cross-disciplinary analytics & Power BI export endpoints |
| `com.emblemix.audit` | `AuditService` | `AuditServiceImpl` | Security and administrative audit trail logging |
| `com.emblemix.team` | `TeamService` | `TeamServiceImpl` | Multi-disciplinary team hierarchy, membership, role management |

---

## 6. Testing Conventions

Unit tests target the concrete implementation class (`*ServiceImpl`) directly with isolated Mockito mocks, bypassing Spring context overhead. Controllers are tested using `MockMvc` injecting the mock service interface.

### Example Unit Test (`IncidentServiceTest`):
```java
@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private ConnectedSystemRepository systemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private AuditService auditService;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentServiceImpl(
            incidentRepository,
            systemRepository,
            userRepository,
            notificationService,
            teamRepository,
            auditService
        );
    }

    @Test
    void testAcknowledgeIncident_Success() {
        // Test assertions using mock contracts
    }
}
```

---

## 7. Concrete Implementation Examples

### Example 1: `com.emblemix.auth`

#### Interface (`AuthService.java`)
```java
package com.emblemix.auth.service;

import com.emblemix.auth.dto.AuthResponse;
import com.emblemix.auth.dto.LoginRequest;
import com.emblemix.auth.dto.RefreshTokenRequest;
import com.emblemix.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
}
```

#### Implementation (`AuthServiceImpl.java`)
```java
package com.emblemix.auth.service.impl;

import com.emblemix.auth.dto.*;
import com.emblemix.auth.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Business logic, validations, user persistence, JWT creation
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Authentication check, password verification, token issuance
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Token validation and refresh logic
    }
}
```

---

### Example 2: `com.emblemix.incident`

#### Interface (`IncidentService.java`)
```java
package com.emblemix.incident.service;

import com.emblemix.incident.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface IncidentService {
    IncidentResponse createIncident(UUID orgId, CreateIncidentRequest request);
    Page<IncidentResponse> getIncidents(UUID orgId, UUID teamId, String status, String severity, Pageable pageable);
    IncidentResponse getIncident(UUID orgId, UUID incidentId);
    IncidentResponse updateIncident(UUID orgId, UUID incidentId, UpdateIncidentRequest request);
    IncidentResponse acknowledgeIncident(UUID orgId, UUID incidentId, UUID userId);
    IncidentResponse resolveIncident(UUID orgId, UUID incidentId, UUID userId, String resolutionNotes);
    IncidentResponse reassignIncident(UUID orgId, UUID incidentId, UUID assignedUserId, UUID assignedTeamId, String reason, UUID actorId);
}
```

#### Implementation (`IncidentServiceImpl.java`)
```java
package com.emblemix.incident.service.impl;

import com.emblemix.incident.service.IncidentService;
import com.emblemix.notification.service.NotificationService;
import com.emblemix.team.service.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final ConnectedSystemRepository systemRepository;
    private final NotificationService notificationService;
    private final TeamRepository teamRepository;
    private final AuditService auditService;

    public IncidentServiceImpl(
            IncidentRepository incidentRepository,
            ConnectedSystemRepository systemRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            TeamRepository teamRepository,
            AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.systemRepository = systemRepository;
        this.notificationService = notificationService;
        this.teamRepository = teamRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public IncidentResponse createIncident(UUID orgId, CreateIncidentRequest request) {
        // System lookup, intelligent routing to owning team, SLA determination, notification dispatch
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID orgId, UUID incidentId) {
        // Single incident retrieval with permission check
    }

    @Override
    @Transactional
    public IncidentResponse acknowledgeIncident(UUID orgId, UUID incidentId, UUID userId) {
        // State transition OPEN -> ACKNOWLEDGED, MTTA computation, audit trail
    }

    @Override
    @Transactional
    public IncidentResponse resolveIncident(UUID orgId, UUID incidentId, UUID userId, String notes) {
        // State transition -> RESOLVED, MTTR computation, audit trail
    }
}
```

---

## 8. Verification & Build Integrity

All interface and implementation refactoring is verified via automated Maven compilation and test suite runs:

```bash
# Clean compilation of all modules
mvn clean compile

# Execution of all unit and slice tests
mvn test

# Packaging and verification
mvn clean verify
```

**Verification Results:**
- `mvn clean compile`: **194 source files compiled successfully** with 0 errors.
- `mvn test`: **40 of 40 tests passed** with 0 failures, 0 errors, 0 skipped.
- `mvn clean verify`: **BUILD SUCCESS** producing executable Spring Boot artifact `emblemix-backend-1.0.0-SNAPSHOT.jar`.
