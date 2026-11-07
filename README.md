# University Management System

[![Build and Test CI](https://github.com/THE-FOOL-T/university-management-system/actions/workflows/ci.yml/badge.svg)](https://github.com/THE-FOOL-T/university-management-system/actions/workflows/ci.yml)

A Java 17 / Spring Boot 3.5.16 monolithic web application implementing role-based university directory management (Admin, Teacher, Student), server-backed session authentication with CSRF protection, Thymeleaf dashboard and list interfaces, PostgreSQL persistence, 8 isolated Mockito unit tests, 4 real PostgreSQL integration tests, and an automated GitHub Actions CI pipeline.

- **GitHub Repository:** [https://github.com/THE-FOOL-T/university-management-system](https://github.com/THE-FOOL-T/university-management-system)
- **Default Branch:** `main`
- **Testing & Viva Defense Guide:** [`docs/TESTING_AND_GITHUB_ACTIONS_VIVA.md`](docs/TESTING_AND_GITHUB_ACTIONS_VIVA.md)
- **Test Execution & CI Verification Report:** [`docs/TEST_AND_CI_REPORT.md`](docs/TEST_AND_CI_REPORT.md)

---

## Automated Testing Suite

The project implements a two-tier testing architecture separating fast, isolated unit tests from comprehensive database integration tests:

| Tier | Test Type | Runner Plugin | Execution Phase | Target | Test Count |
|---|---|---|---|---|---|
| **Tier 1** | **Unit Tests** | Maven Surefire | `test` | `*Test.java` | **8 test methods** |
| **Tier 2** | **PostgreSQL Integration Tests** | Maven Failsafe | `integration-test` / `verify` | `*IT.java` | **4 test methods** |
| **Total** | | | | | **12 test methods** |

### 1. Unit Tests (8 Methods)
Unit tests run in complete isolation from the Spring application context and the database using JUnit 5, Mockito (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`), and AssertJ:
- **`StudentServiceTest`** (4 tests):
  * `createStudent_shouldAssignStudentRoleAndEncodePassword`: Validates `Role.STUDENT` assignment and password delegation.
  * `createStudent_shouldRejectDuplicateEmail`: Validates that duplicate email throws `ConflictException` and prevents profile persistence.
  * `updateStudent_shouldModifyPermittedFieldsAndPreserveRole`: Validates update of permitted profile fields while preserving identity and role.
  * `deleteStudent_shouldInvokeExpectedPersistenceAndAccountDeletion`: Validates foreign-key deletion order (profile deleted and flushed before account).
- **`TeacherServiceTest`** (1 test):
  * `createTeacher_shouldAssignTeacherRoleAndPersistDetails`: Validates `Role.TEACHER` assignment, department, and designation persistence.
- **`AdminServiceTest`** (2 tests):
  * `createAdmin_shouldAssignAdminRoleAndCreateRelatedRecords`: Validates administrative creation and linkage to `UserAccount`.
  * `deleteAdmin_shouldRejectDeletingLastAdministrator`: Validates protection preventing deletion of the last remaining active administrator.
- **`PasswordServiceTest`** (1 test):
  * `encodePassword_shouldProduceDifferentHashAndVerifyWithRawPassword`: Validates BCrypt salting and raw password verification.

### 2. PostgreSQL Integration Tests (4 Methods)
Integration tests boot the complete Spring Boot container (`@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`) and execute real HTTP requests and SQL transactions against an isolated PostgreSQL instance:
- **`AdminCrudIT`**: `adminShouldCompleteFullStudentCrudCycle` — Exercises the complete Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ PostgreSQL CRUD cycle (POST 201 $\rightarrow$ GET 200 $\rightarrow$ PUT 200 $\rightarrow$ DELETE 204 $\rightarrow$ database emptiness check).
- **`TeacherRbacIT`**: `teacherRbac_shouldAllowTeacherEndpointsAndForbidAdminAndStudent` — Proves TEACHER role can manage teachers, but receives HTTP 403 Forbidden on Admin and Student endpoints (tested with valid CSRF tokens).
- **`StudentRbacIT`**: `studentRbac_shouldAllowStudentEndpointsAndForbidAdminAndTeacher` — Proves STUDENT role can access student endpoints, but is forbidden from Teacher and Admin endpoints.
- **`AuthenticationIT`**: `authenticationAndLogout_shouldEstablishSessionAndInvalidateOnLogout` — Exercises database authentication, session establishment, protected `/api/auth/me` access, CSRF logout, and session termination.

---

## Running the Tests

### Execute Unit Tests Only (Surefire)
Runs all 8 unit tests in under 5 seconds without starting Spring or PostgreSQL:
```powershell
# Linux / macOS
./mvnw test

# Windows PowerShell / CMD
.\mvnw.cmd test
```

### Execute All Tests (Surefire + Failsafe)
Runs all 8 unit tests and all 4 PostgreSQL integration tests:
```powershell
# Linux / macOS
./mvnw verify

# Windows PowerShell / CMD
.\mvnw.cmd verify
```

---

## Continuous Integration (GitHub Actions)

Every push or pull request to `main` automatically triggers the GitHub Actions workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml):
1. **Runner:** Spawns an `ubuntu-latest` virtual machine.
2. **PostgreSQL Service Container:** Starts `postgres:16-alpine` on port 5432 with an isolated test database `university_test`, validated via `pg_isready` healthcheck.
3. **Java Setup:** Configures Eclipse Adoptium JDK 17 with automated Maven dependency caching.
4. **Build & Test:** Executes `./mvnw -B clean verify`, running both Surefire unit tests and Failsafe PostgreSQL integration tests.
5. **Artifacts Preserved:**
   * JUnit XML test reports (`target/surefire-reports` and `target/failsafe-reports`) uploaded on every run (`if: always()`).
   * Packaged `university-management.jar` archived on build success.

---

## Running the Application Locally

### Option A — Run Services with Docker Compose
```powershell
# Start both application and PostgreSQL
docker compose up --build -d

# Verify container health
docker compose ps

# Access application
http://localhost:8081/login
```

### Option B — Run Java Locally, PostgreSQL in Docker
```powershell
# 1. Start only the PostgreSQL database container
docker compose up -d db

# 2. Run Spring Boot application locally
.\mvnw.cmd spring-boot:run
```

- **Default Local Web Interface:** [http://localhost:8081/login](http://localhost:8081/login)
- **Local Database Port:** `127.0.0.1:5433`
- **Actuator Health Check:** [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)

---

## Troubleshooting

- **`failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine`:**  
  Docker Desktop is not running or still initializing. Launch Docker Desktop from the Start menu and wait until the status displays "Engine running".
- **Port Conflict on 8081 or 5433:**  
  Change `APP_PORT` or `DB_PORT` in your local `.env` file. Do not commit `.env`.
- **Integration Test Connection Issues:**  
  Ensure PostgreSQL is listening on port 5432 in CI, or port 5434 when using `compose.test.yaml`.
