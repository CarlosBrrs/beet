---
name: scaffold_module
description: Generates a new Hexagonal Architecture Module for the Backend
---

# 🏗 Scaffold New Backend Module

This skill helps you generate a new Feature Module in `com.beet.backend.modules.{moduleName}` following the strict project architecture.

## 1. Prerequisites
- Confirm the **Module Name** (e.g., `inventory`, `catalog`).
- Confirm the **Main Aggregate Name** (e.g., `Ingredient`, `Product`).

## 2. Generation Steps

### Step 1: Create Directory Structure
Run the following command to create the package structure:
```bash
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/domain/model
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/domain/api
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/domain/spi
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/domain/exception
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/domain/usecase
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/domain/constants
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/application/handler
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/application/dto
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/application/mapper
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/infrastructure/input/rest
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/infrastructure/output/adapter
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/infrastructure/output/persistence/jdbc/aggregate
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/infrastructure/output/persistence/jdbc/repository
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/infrastructure/output/persistence/jdbc/mapper
mkdir -p src/main/java/com/beet/backend/modules/{moduleName}/infrastructure/output/persistence/jdbc/adapter
```

### Step 2: Generate Core Files
You must generate the following files using the templates provided in `.agent/skills/scaffold_module/templates`. Provide the content directly in `write_to_file` calls.

**Domain Layer**:
1.  `domain/model/{Aggregate}Domain.java` (POJO)
2.  `domain/api/{Aggregate}ServicePort.java` (Interface)
3.  `domain/spi/{Aggregate}PersistencePort.java` (Interface)
4.  **Exceptions**:
    *   `domain/exception/{Aggregate}NotFoundException.java` (Extends `ResourceNotFoundException`)
    *   `domain/exception/{Aggregate}AlreadyExistsException.java` (Extends `ResourceAlreadyExistsException`)
    *   `domain/exception/{SpecificError}Exception.java` (Extends `RuntimeException` for specific logic)
5.  `domain/usecase/{Aggregate}UseCase.java` (@Service, implements ServicePort)
    *   **Note**: UseCases can inject other `ServicePort`s for cross-domain logic.

**Infrastructure Layer**:
6.  `infrastructure/output/persistence/jdbc/aggregate/{Aggregate}Aggregate.java` (@Table)
7.  `infrastructure/output/persistence/jdbc/repository/{Aggregate}JdbcRepository.java` (CrudRepository)
8.  `infrastructure/output/persistence/jdbc/adapter/{Aggregate}JdbcAdapter.java` (Implements PersistencePort)
9.  `infrastructure/output/persistence/jdbc/mapper/{Aggregate}AggregateMapper.java` (@Component)
10. `infrastructure/input/rest/{Aggregate}Controller.java` (Delegates to Handler)

**Application Layer**:
11. `application/dto/{Aggregate}Request.java` (Record)
12. `application/dto/{Aggregate}Response.java` (Record)
13. `application/mapper/{Aggregate}ServiceMapper.java` (@Component)
14. `application/handler/{Aggregate}Handler.java` (Interface)
15. `application/handler/{Aggregate}HandlerImpl.java` (Orchestrator)
    *   **Note**: `HandlerImpl` can inject multiple `ServicePort`s to orchestrate complex logic.

### Step 3: Domain Exceptions Instructions
When generating exceptions, follow these rules:
1.  **General vs Specific**: 
    *   If it's a standard error (Not Found, Already Exists), extend the Shared Exception.
    *   If it's domain-specific business logic (e.g. `CashRegisterNotOpenException`), extend `RuntimeException`.
2.  **Formatting**:
    *   Use `private` constructors that will build the message using `String.format`.
    *   Use `static` factory methods (e.g. `forId(UUID id)`).
    *   Use `static final String` templates with `%s` for messages.

### Step 4: Database Migration
Verify the current migrations in `src/main/resources/db/migration` to check if there is a table already created for the Domain Model.
If not, create a Flyway migration:
1.  Path: `src/main/resources/db/migration`
2.  Name: `V{next_version}_create_{module_table}.sql`
3.  Content: `CREATE TABLE {table_name} (...)`

## 3. Cross-Module Communication (The Gateway Pattern)

If your module needs data or logic from **another module** (e.g., `Restaurant` needs `User` plan):

1.  **Define a Gateway SPI**:
    *   Location: `domain/spi/{Target}Gateway.java`
    *   Use Template: `.agent/skills/scaffold_module/templates/domain/Gateway.java`
    *   Content: Interface with the specific method you need (e.g., `int getMaxRestaurants(UUID ownerId)`).
    *   **Do NOT** return Domain objects from the other module. Return primitive types or local DTOs if possible to maintain decoupling.

2.  **Implement the Adapter**:
    *   Location: `infrastructure/output/adapter/{Target}GatewayAdapter.java`
    *   Use Template: `.agent/skills/scaffold_module/templates/infrastructure/GatewayAdapter.java`
    *   **Note**: Place in `infrastructure/output/adapter`, **NOT** `infrastructure/output/persistence`.
    *   Dependencies: Inject the **Service Port** or **Persistence Port** of the target module.
    *   Logic: Orchestrate the call and map the result to your primitive/DTO.

**Why?**
*   **Decoupling**: Your Domain doesn't know about the other module's existence.
*   **No Circular Dependencies**: The Adapter handles the glue code in the Infrastructure layer.
*   **Semantic Clarity**: Distinguishes between "Database Access" (Persistence) and "Integration" (Gateway).

## 4. Review
After generation, ask the user to review the generated files, specifically the **Domain Model** fields and the **Database Migration**.
