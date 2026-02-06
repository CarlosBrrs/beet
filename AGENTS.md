# Agent Guidelines (Beet Project)

## How to Use This Guide
- **Start Here**: This file is the global map for the project.
- **Component Specifics**: 
    - For Backend rules: Read `backend/AGENTS.md` (Extends this file).
    - For Frontend rules: Read `frontend/AGENTS.md` (Extends this file).
    - *Note: If those files don't exist yet, follow the Generic Skills below.*

---

## 📚 Available Skills
Use these skills to understand specific technologies or project patterns.

### Generic Skills (Re-usable)
| Skill | Description | URL |
|-------|-------------|-----|
| `java-17` | Record types, switch expressions, modern Java patterns | [SKILL.md](.agent/skills/java-17/SKILL.md) |
| `spring-boot-3` | Observability, native compilation, config | [SKILL.md](.agent/skills/spring-boot-3/SKILL.md) |
| `docker` | Multi-stage builds, non-root users, compose | [SKILL.md](.agent/skills/docker/SKILL.md) |
| `postgres` | JSONB usage, indexing, optimization | [SKILL.md](.agent/skills/postgres/SKILL.md) |
| *`react`* | *(Future) Hooks, functional components* | *[SKILL.md](.agent/skills/react/SKILL.md)* |

### Beet-Specific Skills (Project Domain)
| Skill | Description | URL |
|-------|-------------|-----|
| `beet-auth` | User roles, JWT handling, security config | [SKILL.md](.agent/skills/beet-auth/SKILL.md) |
| `beet-data` | JpaRepository patterns, auditing | [SKILL.md](.agent/skills/beet-data/SKILL.md) |
| `beet-api` | specific API response wrappers/error handling | [SKILL.md](.agent/skills/beet-api/SKILL.md) |

---

## 🚦 Auto-Invoke Skills
**ALWAYS invoke the provided skill FIRST when performing these actions:**

| If you are... | ...Invoke/Read this Skill |
|---------------|---------------------------|
| **Creating a Controller / API Endpoint** | `spring-boot-3` & `beet-api` |
| **Modifying Database Entities** | `beet-data` |
| **Changing Security / Login logic** | `beet-auth` |
| **Updating `Dockerfile` or pipelines** | `docker` |
| **Running the application locally** | Read `.agent/workflows/docker-setup.md` |
| **Adding a Maven Dependency** | Check `backend/pom.xml` version compatibility |
| **Refactoring Service layer** | `java-17` |
| **Implementing a Feature** | CHECK `docs/features/` FIRST. If a spec exists, follow it strictly. |

---

## 🏗 Project Overview
- **Root**: Monorepo orchestrator.
- **`backend/`**: Spring Boot Application.
    - Entry: `com.beet.backend.BeetApplication`
    - Config: `application.yaml`
- **`frontend/`**: *(Coming Soon)*
- **`docker-compose.yml`**: Local Dev Environment (Postgres + Backend).

## 📝 Commit & PR Guidelines
- **Format**: `type(scope): description` (e.g., `feat(auth): add jwt validation`)
- **Types**: `feat`, `fix`, `chore`, `docs`, `refactor`.
