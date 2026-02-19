# Backend Agent Guidelines (Hexagonal Architecture)

This file extends the root `AGENTS.md` with strict architectural rules for the `backend/` directory.

## 🏗 Package Structure (Package by Module)
We follow a **Hexagonal Architecture** organized by **Feature Module**.

### 1. Module Layout
```text
com.beet.backend.modules.{featureName}
├── domain                          # CORE: Pure Java, No Frameworks
│   ├── model                       # POJOs (The Domain Objects) -> {Aggregate}Domain
│   ├── api                         # DRIVING PORTS (Service Interfaces)
│   ├── spi                         # DRIVEN PORTS (Persistence Interfaces)
│   ├── exception                   # Domain-specific exceptions
│   └── constants                   # Domain Logic Constants
│
├── application                     # ORCHESTRATION: DTOs & Mappers
│   ├── handler                     # INTERFACE + IMPL (UseCase orchestration)
│   ├── dto                         # Input/Output DTOs (Records)
│   └── mapper                      # ServiceMapper (DTO <-> Domain)
│
└── infrastructure                  # ADAPTERS: Framework code
    ├── input
    │   └── rest                    # REST Controllers
    └── output
        └── persistence             # PERSISTENCE ADAPTERS
            ├── jdbc                # STRATEGY: Spring Data JDBC
            │   ├── aggregate       # JDBC Aggregates (@Table) -> {Aggregate}Aggregate
            │   ├── repository      # CrudRepository -> {Aggregate}JdbcRepository
            │   ├── mapper          # Aggregate <-> Domain Mapper -> {Aggregate}AggregateMapper
            │   └── adapter         # Adapter Implementation (@Component) -> {Aggregate}JdbcAdapter
├── shared                          # SHARED KERNEL
│   ├── domain                      # Shared Value Objects / Exceptions
│   ├── application                 # Shared Utils / Response Wrappers
│   └── infrastructure              # Global Config / Generic Adapters
```

### 2. Constants Strategy
*   **Validation Constants**: For DTO annotations (e.g., regex, max lengths).
*   **Exception Constants**: Error messages for `BeetBusinessException`.
*   **General Constants**: Other logic constants needed for the module.

## 🔄 Architectural Flow & Responsibilities
To avoid confusion, here is the strict flow of data and responsibility:

1.  **Infrastructure (Input)**: `{Aggregate}Controller`
    *   Receives HTTP Request.
    *   Validates DTO constraints (`@Valid`).
    *   **Delegates** purely to `{Aggregate}Handler` (Interface).
    *   Returns `ApiGenericResponse`.

2.  **Application**: `{Aggregate}HandlerImpl`
    *   **Orchestrates** the flow.
    *   Uses `{Aggregate}ServiceMapper` to convert `Request` (DTO) -> `{Aggregate}Domain` (Domain Object).
    *   Calls appropriate `{Aggregate}ServicePort` (UseCase).
    *   Uses `{Aggregate}ServiceMapper` to convert `{Aggregate}Domain` (Result) -> `{Aggregate}Response` (DTO).

3.  **Domain (Core)**: `{Aggregate}UseCase` (implements `{Aggregate}ServicePort`)
    *   Contains **ALL** business logic.
    *   Can inject multiple ServicePorts (other UseCases) for cross-domain logic.
    *   Calls `{Aggregate}PersistencePort` to save/retrieve data.

4.  **Infrastructure (Output)**: `{Aggregate}JdbcAdapter` (implements `{Aggregate}PersistencePort`)
    *   **Delegates** DB operations.
    *   Uses `{Aggregate}AggregateMapper` to convert `{Aggregate}Domain` -> `{Aggregate}Aggregate` (DB Entity).
    *   Calls `{Aggregate}JdbcRepository` to perform SQL operations.
    *   Uses `{Aggregate}AggregateMapper` to convert `{Aggregate}Aggregate` -> `{Aggregate}Domain`.
    *   Returns Domain Object to UseCase.

---

## 🗄️ Database Strategy (`Spring Data JDBC` + `Flyway`)

### 1. The Philosophy: Aggregates over Entities
We use **Spring Data JDBC**. This is fundamentally different from JPA.
*   **No Magic**: No Dirty Checking, No Lazy Loading, No "Open Session in View".
*   **Aggregates**: An Aggregate is loaded and saved as a whole.
*   **References**: Aggregates reference other Aggregates **BY ID ONLY**.
    *   *Bad*: `Order` has a `User user` field.
    *   *Good*: `Order` has a `UUID userId` field.

### 2. The Workflow
1.  **Code**: Create `{Aggregate}Aggregate.java` (mapped to `@Table`).
2.  **Script**: Write `V1__create_{table}.sql` in `src/main/resources/db/migration`.
3.  **Run**: App starts -> Flyway creates table.
4.  **Test**: **CRITICAL**. Since JDBC has no startup validation, you **MUST** write `@DataJdbcTest` to verify your Aggregate maps correctly to the Table.

### 3. Date & Time & Audit Strategy
*   **Java Type**: `java.time.Instant` (**ALWAYS**).
*   **DB Type**: `TIMESTAMPTZ`.
*   **Inheritance**: All Aggregates extend `BaseAuditableAggregate` (in `shared`).
*   **Automation Rule**: `createdAt`, `createdBy`, `updatedAt`, `updatedBy` are **Managed by Infrastructure** (via `AuditorAware`).
    *   **Constraint**: Do **NOT** manually set these fields in Business Logic.
    *   **Exception**: `deletedBy` is handled by `markDeleted()` for now.

### 4. Soft Deletes
*   **Mechanism**: **Logical Deletion**.
*   **Fields**: `deleted_at` (timestamp), `deleted_by` (user ID).
*   **Rule**: NEVER use `repository.delete()`. 
    *   *Correct*: `aggregate.delete(userId)`; `repository.save(aggregate)`;
    *   *Read*: Repositories must filter `where deleted_at is null`.

---

## 💻 Code Samples & Rules

### Domain Model (POJO)
**Rule**: Pure Java. Independent of Persistence. **Use UUID for ALL IDs**.
```java
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class UserDomain {
    private final UUID id;
    private String email;
}
```

### Driving Port (API)
```java
public interface UserServicePort {
    UserDomain create(UserDomain user);
}
```

### Infrastructure: Jdbc Base Aggregate (Shared)
```java
@Data
public abstract class BaseAuditableAggregate {
    @CreatedDate
    private Instant createdAt;
    @CreatedBy
    private UUID createdBy;
    
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedBy
    private UUID updatedBy;

    // Soft Delete
    private Instant deletedAt;
    private UUID deletedBy;
    
    public void markDeleted(UUID actorId) {
        this.deletedAt = Instant.now();
        this.deletedBy = actorId;
    }
}
```

### Infrastructure: JDBC Aggregate
**Rule**: Use `@Table`, `@Id`. Extend Base.
```java
@Table("users")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class UserAggregate extends BaseAuditableAggregate {
    @Id
    private UUID id;
    private String email;
    // References to other aggregates are simple IDs (UUID)
    private UUID departmentId; 
}
```

### Infrastructure: Persistence Adapter
**Rule**: Explicit Mapping.
```java
@Component
@RequiredArgsConstructor
public class UserJdbcAdapter implements UserPersistencePort {
    private final UserJdbcRepository repository; // extends CrudRepository
    private final UserAggregateMapper mapper;

    @Override
    public UserDomain save(UserDomain user) {
        UserAggregate aggregate = mapper.toAggregate(user);
        return mapper.toDomain(repository.save(aggregate));
    }
}
```

---

## 🏆 Quality Standards

### 1. Transaction Management (`@Transactional`)
*   **Location**: **UseCase Layer Only** (Implementation of Service Port).
*   **Why**: The UseCase defines the atomic unit of business work.
*   **Rule**: `readOnly = true` by default, `false` for mutating operations.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserUseCase implements UserServicePort {
    private final UserPersistencePort userPersistencePort;

    @Transactional
    public UserDomain createUser(UserDomain user) {
        // Business Logic...
        if (userPersistencePort.existsByEmail(user.getEmail())) {
             throw new UserAlreadyExistsException(user.getEmail());
        }
        return userPersistencePort.save(user);
    }
}
```

### 2. Validation Strategy
*   **Basic Validation**: Use JAX-RS `@Valid` / `@NotNull` in **Controller** (DTOs).
    *   *Goal*: Fail fast on bad HTTP requests (400 Bad Request).
*   **Business Validation**: Use explicit logic in **UseCase**.
    *   *Goal*: Enforce domain invariants. Throw `BeetBusinessException`.
    *   *Example*: "User cannot withdraw more than balance."

### 3. Mapping Strategy
*   **Type**: **Manual Mapping** (No MapStruct/ModelMapper).
*   **Why**: compile-time safety, explicit transformations, no magic.
*   **Location**: 
    *   `application/mapper`: DTO <-> Domain
    *   `infrastructure/persistence/jdbc/mapper`: Aggregate <-> Domain

### 4. Observability (Wide Events)
We follow the **"Wide Events"** philosophy.
*   **Goal**: One structured log event per request, containing ALL context.
*   **Mechanism**:
    1.  **Start**: `RequestInterceptor` initializes a `RequestScope` event bean.
    2.  **Enrich**: Controllers/UseCases inject the bean and add fields (`user_id`, `cart_total`).
    3.  **End**: Filter logs the bean as a single JSON blob.
*   **Rule**: **Avoid** ad-hoc `log.info("step 1")`. **Prefer** `event.put("step", 1)`.

### 5. Cross-Module Communication (Gateway Pattern)
When a module needs data/logic from another module, do **NOT** use direct Repository/Service checks from the foreign module in your UseCase.

**The Pattern**:
1.  **Define SPI**: `domain/spi/{Target}Gateway.java` (e.g., `RestaurantSubscriptionGateway`).
2.  **Implement Adapter**: `infrastructure/output/adapter/{Target}GatewayAdapter.java`.
3.  **Inject**: The Adapter injects the external module's **Service Port** or **Persistence Port**.
4.  **Return**: Primitives or local DTOs. **Never** return external Domain objects.

**Example**:
```java
// Domain
public interface RestaurantSubscriptionGateway {
    int getMaxRestaurantsAllowed(UUID ownerId);
}

// Infrastructure
@Component
@RequiredArgsConstructor
public class RestaurantSubscriptionGatewayAdapter implements RestaurantSubscriptionGateway {
    private final UserPersistencePort userPort;
    private final SubscriptionServicePort subPort;

    @Override
    public int getMaxRestaurantsAllowed(UUID ownerId) {
        // orchestrated logic...
        return limit;
    }
}
```
