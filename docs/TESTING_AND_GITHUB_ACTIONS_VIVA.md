# CSE 3230 — Testing Architecture & GitHub Actions CI Viva Guide

This guide provides an end-to-end, technically rigorous explanation of the automated testing suite and GitHub Actions Continuous Integration (CI) pipeline implemented for the **University Management System**. It is structured specifically for defending the design and implementation during the lab viva.

---

## Table of Contents
1. [Section A — Unit Testing & Mockito Architecture](#section-a--unit-testing--mockito-architecture)
2. [Section B — PostgreSQL Integration Testing & MockMvc](#section-b--postgresql-integration-testing--mockmvc)
3. [Section C — Git Version Control & Conventional Commits](#section-c--git-version-control--conventional-commits)
4. [Section D — GitHub Actions CI Pipeline Architecture](#section-d--github-actions-ci-pipeline-architecture)
5. [Section E — CI Pipeline vs. Docker Compose Deployment](#section-e--ci-pipeline-vs-docker-compose-deployment)
6. [Section F — 25 Tricky Viva Questions & Detailed Answers](#section-f--25-tricky-viva-questions--detailed-answers)

---

## Section A — Unit Testing & Mockito Architecture

### 1. Fundamentals of Unit Testing
- **What is Unit Testing?**  
  Unit testing evaluates the smallest testable units of software (individual methods and classes) in complete isolation from external collaborators, such as databases, network sockets, or Spring application contexts.
- **Why do we use it?**  
  It provides millisecond-level feedback on business logic correctness, localizes defects instantly to a specific line of code, and ensures safety against regressions during refactoring.
- **Role of JUnit 5 (Jupiter):**  
  JUnit 5 is the modern testing framework for the JVM. It manages the test execution lifecycle, discovers annotated test methods, executes assertions, and reports results to build tools like Maven.
- **Role of Mockito:**  
  Mockito is a mocking framework that dynamically creates mock proxy objects for class dependencies at runtime. It intercepts method invocations, provides stubbed return values, and records interactions for verification.
- **Why are dependencies mocked in Unit Tests?**  
  If a service test interacts with a real database or real Spring container, it is no longer an isolated unit test. Real databases are slow, introduce environmental flakiness (e.g., connection drops, port conflicts, dirty data state), and obscure the root cause of logic errors. Mocking guarantees determinism and speed.

### 2. Key Annotations and Concepts
- `@Test`: Marks a method as an executable test case recognized by the JUnit Jupiter test engine.
- `@ExtendWith(MockitoExtension.class)`: Initializes Mockito annotations before each test and validates framework usage (e.g., detecting unused stubs or uninitialized mocks).
- `@Mock`: Instructs Mockito to instantiate a mock proxy implementation of the specified interface or class (e.g., `StudentRepository`, `AccountService`).
- `@InjectMocks`: Instantiates the real class under test (e.g., `StudentService`) and automatically injects all declared `@Mock` objects into its constructor.
- `@BeforeEach`: Executes before each individual `@Test` method to set up fresh state and fixture data, ensuring test isolation.
- **Arrange–Act–Assert (AAA) Pattern:**  
  * **Arrange:** Set up test inputs, initialize fixtures, and configure mock expectations (`when(...).thenReturn(...)`).
  * **Act:** Invoke the target method under test.
  * **Assert:** Verify the returned result or thrown exception, and verify mock interactions (`verify(...)`).
- **Assertions:** Statements (using AssertJ's `assertThat(...)`) that compare expected vs. actual outcomes. If an assertion fails, an `AssertionFailedError` is thrown and the test is marked FAILED.
- **How to Run Unit Tests via Maven:**
  ```powershell
  ./mvnw test        # Linux / macOS
  .\mvnw.cmd test    # Windows
  ```
  Maven Surefire discovers and executes all classes matching `**/*Test.java`.

---

### 3. Detailed Explanation of the 8 Unit Test Methods

#### UT01 — `createStudent_shouldAssignStudentRoleAndEncodePassword()`
1. **Behavior Tested:** Creating a student correctly validates input, assigns the `STUDENT` role, encodes the password via `AccountService`, and persists the profile.
2. **Class Tested:** `com.cse3230.university.service.StudentService`
3. **Dependencies Mocked:** `StudentRepository`, `AccountService`, `RequestValidator`.
4. **Input Used:** `StudentRequest("Alice Walker", "S101", "CSE", 3, "student@university.edu", "Password123!")`.
5. **Expected Outcome:** Returns a `StudentResponse` with matching profile data and an embedded account possessing `Role.STUDENT`.
6. **Why Assertion Proves Behavior:** It asserts response properties match input, verifies that `accounts.create(...)` was invoked with `Role.STUDENT`, and verifies `studentRepository.saveAndFlush(...)` was called.
7. **Bug Caught:** Prevents privilege escalation bugs where a student account might mistakenly be assigned an elevated role (`TEACHER` or `ADMIN`) or created with unencrypted passwords.

#### UT02 — `createStudent_shouldRejectDuplicateEmail()`
1. **Behavior Tested:** Attempting to register a student with an email address already registered by another account throws a `ConflictException` and aborts profile persistence.
2. **Class Tested:** `com.cse3230.university.service.StudentService`
3. **Dependencies Mocked:** `StudentRepository`, `AccountService`, `RequestValidator`.
4. **Input Used:** `StudentRequest` containing `"taken@university.edu"`.
5. **Expected Outcome:** Throws `ConflictException` with message containing `"Email is already in use"`.
6. **Why Assertion Proves Behavior:** Catches `ConflictException` and verifies via `verify(studentRepository, never()).saveAndFlush(any())` that no profile row is written when the account creation fails.
7. **Bug Caught:** Prevents orphan profile records from being saved when account creation is rejected.

#### UT03 — `updateStudent_shouldModifyPermittedFieldsAndPreserveRole()`
1. **Behavior Tested:** Updating an existing student correctly updates mutable profile fields (`name`, `department`, `semester`) and delegates credentials update to `AccountService`, preserving the immutable `STUDENT` role.
2. **Class Tested:** `com.cse3230.university.service.StudentService`
3. **Dependencies Mocked:** `StudentRepository`, `AccountService`, `RequestValidator`.
4. **Input Used:** ID `1L` and updated `StudentRequest` with name `"Alice Updated"`, department `"EEE"`, semester `4`.
5. **Expected Outcome:** Returned `StudentResponse` reflects updated fields while the account retains `Role.STUDENT`.
6. **Why Assertion Proves Behavior:** Validates updated fields on the entity and verifies `accounts.update(...)` was called with the existing account.
7. **Bug Caught:** Prevents unintended role mutation or field erasure during profile editing.

#### UT04 — `deleteStudent_shouldInvokeExpectedPersistenceAndAccountDeletion()`
1. **Behavior Tested:** Deleting a student properly deletes the referencing profile row and flushes before deleting the underlying `UserAccount` row, respecting database foreign key order.
2. **Class Tested:** `com.cse3230.university.service.StudentService`
3. **Dependencies Mocked:** `StudentRepository`, `AccountService`.
4. **Input Used:** Student ID `1L`.
5. **Expected Outcome:** Profile is deleted and flushed, followed by account deletion.
6. **Why Assertion Proves Behavior:** Uses Mockito's `inOrder` verifier to prove `studentRepository.delete()`, then `studentRepository.flush()`, and finally `accountService.delete()`.
7. **Bug Caught:** Prevents SQL foreign key constraint violation errors (`DataIntegrityViolationException`) where deleting the parent account before the referencing student profile causes a database abort.

#### UT05 — `createTeacher_shouldAssignTeacherRoleAndPersistDetails()`
1. **Behavior Tested:** Creating a teacher assigns `Role.TEACHER`, stores employee designation and department, and delegates account creation.
2. **Class Tested:** `com.cse3230.university.service.TeacherService`
3. **Dependencies Mocked:** `TeacherRepository`, `AccountService`, `RequestValidator`.
4. **Input Used:** `TeacherRequest("Dr. Robert Smith", "EMP-202", "CSE", "Associate Professor", "teacher@university.edu", "Password123!")`.
5. **Expected Outcome:** `TeacherResponse` with `Role.TEACHER` and designation `"Associate Professor"`.
6. **Why Assertion Proves Behavior:** Asserts response DTO correctness and verifies invocations on `accounts.create(..., Role.TEACHER)` and `teacherRepository.saveAndFlush(...)`.
7. **Bug Caught:** Prevents missing employee-specific attributes (employee ID, designation) or misassignment of roles.

#### UT06 — `createAdmin_shouldAssignAdminRoleAndCreateRelatedRecords()`
1. **Behavior Tested:** Creating an administrator assigns `Role.ADMIN` and creates the `Admin` profile linked to the administrative account.
2. **Class Tested:** `com.cse3230.university.service.AdminService`
3. **Dependencies Mocked:** `AdminRepository`, `AccountService`, `RequestValidator`, `UserAccountRepository`.
4. **Input Used:** `AdminRequest("System Admin", "Chief Registrar", "admin@university.edu", "Password123!")`.
5. **Expected Outcome:** `AdminResponse` returned with `Role.ADMIN` and designation `"Chief Registrar"`.
6. **Why Assertion Proves Behavior:** Asserts output and verifies that `accounts.create(..., Role.ADMIN)` was invoked.
7. **Bug Caught:** Catches misconfigured administrative creation flows and unpopulated designation fields.

#### UT07 — `deleteAdmin_shouldRejectDeletingLastAdministrator()`
1. **Behavior Tested:** Attempting to delete the last active administrator account in the system is rejected to prevent administrative lockout.
2. **Class Tested:** `com.cse3230.university.service.AdminService`
3. **Dependencies Mocked:** `AdminRepository`, `AccountService`, `UserAccountRepository`.
4. **Input Used:** Admin ID `3L` when only one active administrator exists in `lockAccountsByRole(Role.ADMIN)`.
5. **Expected Outcome:** Throws `ConflictException` with message containing `"last enabled administrator"`.
6. **Why Assertion Proves Behavior:** Asserts exception type and message, and confirms via `verify(adminRepository, never()).delete(...)` that deletion was aborted.
7. **Bug Caught:** Prevents catastrophic system lockout where all administrator accounts are deleted, leaving the university management system unmanageable.

#### UT08 — `encodePassword_shouldProduceDifferentHashAndVerifyWithRawPassword()`
1. **Behavior Tested:** `BCryptPasswordEncoder` encodes raw passwords into non-plaintext hashes with random salt, and correctly verifies passwords via constant-time comparison.
2. **Class Tested:** `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`
3. **Dependencies Mocked:** None (pure cryptographic algorithm unit test).
4. **Input Used:** Raw password string `"SecurePassword@2026!"`.
5. **Expected Outcome:** Two encodings of the same password produce distinct hashes (due to random salting), both start with `$2a$`, match the raw password, and reject incorrect passwords.
6. **Why Assertion Proves Behavior:** Tests `isNotEqualTo(rawPassword)`, `isNotEqualTo(hash2)`, `matches(rawPassword, hash1) == true`, and `matches("wrong", hash1) == false`.
7. **Bug Caught:** Prevents storing plaintext passwords or using insecure hashing algorithms without cryptographic salt.

---

## Section B — PostgreSQL Integration Testing & MockMvc

### 1. Integration Testing Fundamentals
- **What is Integration Testing?**  
  Integration testing evaluates how multiple application layers—HTTP controllers, Spring MVC filters, Spring Security authentication/authorization, service transactions, Hibernate JPA ORM, and the actual PostgreSQL database—operate together as an integrated system.
- **Difference from Unit Testing:**  
  Unit tests isolate a single class using Mockito. Integration tests boot the Spring context, load real Spring beans, construct real SQL queries via Hibernate, and execute them against a real PostgreSQL instance.
- **Why a Real PostgreSQL Database is Used:**  
  In-memory databases like H2 have different SQL dialects, locking semantics, type systems, and constraint behaviors compared to PostgreSQL. Real PostgreSQL tests guarantee that production SQL queries, foreign key cascades, and unique constraints work without discrepancy.
- **Key Annotations:**  
  * `@SpringBootTest`: Loads the complete Spring application context with all beans, repositories, and configurations.
  * `@AutoConfigureMockMvc`: Configures and injects `MockMvc` for simulating HTTP requests without binding to an actual network port.
  * `@ActiveProfiles("test")`: Activates `application-test.yml`, connecting the application to the isolated test database `university_test`.
- **How MockMvc Works:**  
  `MockMvc` constructs mock HTTP servlet requests and invokes the Spring `DispatcherServlet` pipeline directly in memory. It executes security filters, interceptors, parameter validation, JSON serialization, and controller advice, returning an `MvcResult`.
- **Handling CSRF:**  
  Spring Security enables CSRF protection for modifying requests (POST, PUT, DELETE). In integration tests, `with(csrf())` injects a valid `CsrfToken` into request headers or parameters, verifying that failures are due to genuine business logic or RBAC constraints, not missing CSRF tokens.
- **Database Cleanup & Test Isolation:**  
  Each integration test class executes `@BeforeEach` cleanup:
  ```java
  studentRepository.deleteAllInBatch();
  teacherRepository.deleteAllInBatch();
  adminRepository.deleteAllInBatch();
  accountRepository.deleteAllInBatch();
  ```
  This removes prior test records in batch without violating foreign key constraints, ensuring test independence.
- **How to Run Integration Tests via Maven:**
  ```powershell
  ./mvnw verify        # Runs Surefire (unit) + Failsafe (integration)
  .\mvnw.cmd verify    # Windows
  ```

---

### 2. Detailed Explanation of the 4 Integration Test Methods

#### IT01 — `adminShouldCompleteFullStudentCrudCycle()`
- **Class:** `com.cse3230.university.integration.AdminCrudIT`
- **Methodology:**
  1. Authenticates as an administrator using `@WithMockUser(roles = {"ADMIN"})`.
  2. Sends `POST /api/students` with JSON payload and CSRF:
     * **Request:** `{"name":"David Clark","roll":"STU-1001","department":"CSE","semester":1,"email":"david.clark@university.test","password":"Password123!"}`
     * **Response:** HTTP `201 Created`, `Location: /api/students/{id}`, returns persisted JSON.
  3. Sends `GET /api/students/{id}` to verify read operations against PostgreSQL:
     * **Response:** HTTP `200 OK`, JSON body contains matching fields.
  4. Sends `PUT /api/students/{id}` to update semester and name:
     * **Request:** `{"name":"David Clark Updated","roll":"STU-1001","department":"CSE","semester":2,"email":"david.clark@university.test","password":""}`
     * **Response:** HTTP `200 OK` with updated attributes.
  5. Sends `DELETE /api/students/{id}`:
     * **Response:** HTTP `204 No Content`.
  6. Direct persistence assertion: queries `studentRepository.findById(studentId)` and `accountRepository.findByEmail(...)` to verify complete deletion in PostgreSQL.

#### IT02 — `teacherRbac_shouldAllowTeacherEndpointsAndForbidAdminAndStudent()`
- **Class:** `com.cse3230.university.integration.TeacherRbacIT`
- **Methodology:**
  1. Authenticates as a teacher using `@WithMockUser(roles = {"TEACHER"})`.
  2. Executes `GET /api/teachers` $\rightarrow$ verifies HTTP `200 OK`.
  3. Executes `POST /api/teachers` with CSRF token $\rightarrow$ verifies HTTP `201 Created`.
  4. Executes `GET /api/admins` $\rightarrow$ verifies HTTP `403 Forbidden`.
  5. Executes `GET /api/students` $\rightarrow$ verifies HTTP `403 Forbidden`.
  6. Executes `POST /api/students` with CSRF token $\rightarrow$ verifies HTTP `403 Forbidden`.
  * *Viva point:* Providing `with(csrf())` on the write operation guarantees that the `403 Forbidden` was triggered by RBAC authorization (`hasRole("ADMIN")`), not by CSRF token omission.

#### IT03 — `studentRbac_shouldAllowStudentEndpointsAndForbidAdminAndTeacher()`
- **Class:** `com.cse3230.university.integration.StudentRbacIT`
- **Methodology:**
  1. Authenticates as a student using `@WithMockUser(roles = {"STUDENT"})`.
  2. Executes `GET /api/students` $\rightarrow$ verifies HTTP `200 OK`.
  3. Executes `POST /api/students` with CSRF token $\rightarrow$ verifies HTTP `201 Created` (student peer creation allowed by assignment specifications).
  4. Executes `GET /api/teachers` and `POST /api/teachers` $\rightarrow$ verifies HTTP `403 Forbidden`.
  5. Executes `GET /api/admins` and `POST /api/admins` $\rightarrow$ verifies HTTP `403 Forbidden`.

#### IT04 — `authenticationAndLogout_shouldEstablishSessionAndInvalidateOnLogout()`
- **Class:** `com.cse3230.university.integration.AuthenticationIT`
- **Methodology:**
  1. Seeds an account in PostgreSQL with email `"auth.test@university.test"` and encoded password `"ValidPassword123!"`.
  2. Simulates an incorrect login: `POST /login` with bad password $\rightarrow$ verifies HTTP `302 Redirect` to `/login?error`.
  3. Simulates a valid login: `POST /login` with correct password and CSRF token $\rightarrow$ verifies HTTP `302 Redirect` to `/dashboard`, captures the `MockHttpSession` containing the authenticated `SecurityContext`.
  4. Accesses protected endpoint `GET /api/auth/me` with session $\rightarrow$ verifies HTTP `200 OK` and returned JSON.
  5. Performs logout: `POST /logout` with session and CSRF token $\rightarrow$ verifies HTTP `302 Redirect` to `/login?logout`.
  6. Verifies session invalidation: asserts `session.isInvalid() == true`, re-queries `GET /api/auth/me` with the terminated session and verifies HTTP `401 Unauthorized`.

---

## Section C — Git Version Control & Conventional Commits

### 1. Concepts
- **Git vs. GitHub:**  
  * **Git** is a distributed version control system running locally on your computer that tracks file changes, manages branches, and records commit history.
  * **GitHub** is a cloud-hosted platform providing remote Git hosting, collaboration tools, issue tracking, and CI/CD automation (GitHub Actions).
- **Repository:** A project storage directory managed by Git containing project files and the `.git/` history database.
- **Commit:** An immutable snapshot of staged changes accompanied by an author, timestamp, unique SHA hash, and descriptive message.
- **Branch:** A lightweight pointer to a commit. The default branch is `main`.
- **origin:** The default alias for the primary remote repository URL on GitHub.
- **Push & Pull:**  
  * `git push`: Uploads local branch commits to the remote repository.
  * `git pull`: Fetches and merges remote commits into the local branch.
- **Clone:** Copies an existing remote repository to a local machine.
- **`.gitignore`:** Specifies intentional untracked files (e.g., credentials like `.env`, compiled output `target/`, IDE settings `.idea/`) that Git must never commit.

### 2. Conventional Commits 1.0 Specification
- **Format:** `<type>(<scope>): <short description>`
- **Allowed Types:**
  * `feat`: A new feature for the application.
  * `fix`: A bug fix.
  * `test`: Adding or correcting tests.
  * `ci`: Continuous Integration pipeline modifications.
  * `docs`: Documentation changes.
  * `refactor`: Code change that neither fixes a bug nor adds a feature.
  * `build` / `chore`: Changes to build tool configuration, dependencies, or maintenance.
- **Why Commits Should Be Small & Meaningful:**  
  Small, atomic commits isolate changes, simplify code review, make Git history readable, and allow precise rollbacks using `git revert` without breaking unrelated features.

---

## Section D — GitHub Actions CI Pipeline Architecture

### 1. CI/CD Principles
- **Continuous Integration (CI):**  
  A software engineering practice where developers regularly merge code changes into a shared repository. Every push triggers automated builds, compiling, and testing to detect integration errors immediately.
- **Continuous Deployment (CD):**  
  Automates the release of validated code to production or staging environments following a successful CI build.
- **Workflow:** An automated configurable process defined in `.github/workflows/*.yml`.
- **Job:** A set of steps executed on the same virtual environment runner. Jobs run in parallel unless dependencies (`needs:`) are declared.
- **Step:** An individual task within a job (e.g., running a shell command or invoking an action).
- **Runner:** A virtual machine hosted by GitHub (e.g., `ubuntu-latest`) that executes the workflow job.

---

### 2. Step-by-Step Breakdown of `.github/workflows/ci.yml`

```yaml
name: Java 17 Build, Unit & Integration Tests CI
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
```
- **Explanation:** Names the workflow and specifies triggers: runs automatically whenever commits are pushed to `main` or when a pull request targets `main`.

```yaml
permissions:
  contents: read
```
- **Explanation:** Follows the principle of least privilege, granting the runner read-only access to repository contents.

```yaml
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: university_test
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: test_password
        ports:
          - 5432:5432
        options: >-
          --health-cmd "pg_isready -U postgres -d university_test"
          --health-interval 5s
          --health-timeout 5s
          --health-retries 12
```
- **Explanation:**
  * Allocates a clean Ubuntu virtual machine.
  * Spawns a sidecar **PostgreSQL 16** service container on the runner network.
  * Configures an isolated test database `university_test` with credentials `postgres` / `test_password`.
  * Maps PostgreSQL's internal port `5432` to the runner's `localhost:5432`.
  * Configures a Docker healthcheck using `pg_isready` that polls every 5 seconds up to 12 times. The workflow waits until PostgreSQL is fully accepting connections before executing tests.

```yaml
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
```
- **Explanation:** Fetches the repository code into the runner workspace.

```yaml
      - name: Set up JDK 17 (Eclipse Temurin)
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'
```
- **Explanation:** Installs Eclipse Temurin JDK 17 and enables automatic Maven dependency caching based on `pom.xml` hash to accelerate build times.

```yaml
      - name: Make Maven wrapper executable
        run: chmod +x mvnw
```
- **Explanation:** Grants execute permissions to the Linux Maven Wrapper script.

```yaml
      - name: Build and run Unit & PostgreSQL Integration Tests
        run: ./mvnw -B clean verify
        env:
          TEST_DB_URL: jdbc:postgresql://localhost:5432/university_test
          TEST_DB_USERNAME: postgres
          TEST_DB_PASSWORD: test_password
```
- **Explanation:**
  * Runs Maven in batch non-interactive mode (`-B`).
  * `clean verify` compiles the code, executes **Surefire** (8 unit tests), packages the JAR, and executes **Failsafe** (4 integration tests against PostgreSQL).
  * Injects environment variables pointing Spring Boot datasource configuration directly to the runner's PostgreSQL container (`localhost:5432`).

```yaml
      - name: Archive test reports (Surefire & Failsafe)
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: junit-test-reports
          path: |
            target/surefire-reports/
            target/failsafe-reports/
```
- **Explanation:** The `if: always()` condition ensures that test reports are collected and uploaded as downloadable artifacts even if a test fails, enabling immediate diagnostic review.

```yaml
      - name: Archive packaged application JAR
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: university-management-jar
          path: target/university-management.jar
```
- **Explanation:** Archives the production-ready executable JAR artifact upon build success.

---

## Section E — CI Pipeline vs. Docker Compose Deployment

| Dimension | GitHub Actions CI Pipeline | Docker Compose (`compose.yaml`) |
|---|---|---|
| **Primary Purpose** | Continuous Integration: Automated build, compile, and multi-layer test verification on code events. | Multi-container Orchestration: Running the application and database together in a production-like local environment. |
| **When it runs** | On repository events (`push`, `pull_request`) in GitHub cloud runners. | When invoked manually via `docker compose up -d` on developer or server hosts. |
| **Database Role** | Spawns a disposable `university_test` service container solely for integration testing. | Runs a persistent `postgres_data` volume preserving real university data across restarts. |
| **Application Process** | Maven runs Java directly on the Ubuntu runner operating system; app is tested, not deployed. | Java runs packaged inside a hardened Alpine Docker container as a non-root user. |
| **Network Resolution** | Test code connects to `localhost:5432` because the service container port is mapped to the host runner. | The app container connects to `jdbc:postgresql://db:5432/universitydb` using Docker's internal DNS bridge. |
| **Persistence** | Completely ephemeral; all databases, containers, and runner files are discarded when the job ends. | Persistent; data survives container shutdown unless `-v` is explicitly passed. |

*Viva takeaway:* GitHub Actions verifies code correctness; Docker Compose packages and runs the verified application. They complement each other rather than replace each other.

---

## Section F — 25 Tricky Viva Questions & Detailed Answers

### Q01: What is the exact difference between a Unit Test and an Integration Test in this project?
**Answer:** A unit test tests a single class (e.g., `StudentService`) in isolation using Mockito mocks without loading the Spring container or database. An integration test uses `@SpringBootTest` and `@AutoConfigureMockMvc` to boot the full Spring context, execute HTTP requests through `MockMvc`, and read/write real records in PostgreSQL.

### Q02: Why does `StudentServiceTest` use `@ExtendWith(MockitoExtension.class)` instead of `@SpringBootTest`?
**Answer:** `@SpringBootTest` boots the entire Spring context (component scanning, datasource, security filter chains), taking 5–15 seconds to start. `@ExtendWith(MockitoExtension.class)` initializes lightweight Mockito proxies in memory in a few milliseconds without any container overhead.

### Q03: Why is `@InjectMocks` used on `StudentService`?
**Answer:** It tells Mockito to instantiate `StudentService` and automatically inject the `@Mock` instances (`StudentRepository`, `AccountService`, `RequestValidator`) into its constructor.

### Q04: In UT04 (`deleteStudent`), why is Mockito's `InOrder` verification critical?
**Answer:** In the relational schema, `students` has a foreign key referencing `user_accounts`. If the code attempts to delete the `UserAccount` before deleting the `Student` profile, PostgreSQL throws a foreign key constraint violation. `InOrder` proves that `profiles.delete()` and `profiles.flush()` occur before `accounts.delete()`.

### Q05: In UT07, how does the system prevent the last administrator from being deleted?
**Answer:** `AdminService.delete(id)` calls `accountRepository.lockAccountsByRole(Role.ADMIN)`. If only one active administrator exists in that list, it throws a `ConflictException("The last enabled administrator cannot be deleted")`, preventing administrative lockout.

### Q06: Why do we test BCrypt password encoding in UT08? What does `$2a$` signify?
**Answer:** It proves that plaintext passwords are never stored and that BCrypt's random salting produces distinct outputs for identical inputs. `$2a$` is the BCrypt algorithm identifier indicating the OpenBSD-compatible revision.

### Q07: In integration tests, why do we use `with(csrf())` on POST requests?
**Answer:** Spring Security has CSRF protection enabled. Without `with(csrf())`, every POST/PUT/DELETE request would fail with HTTP 403 Forbidden due to CSRF failure. Providing `with(csrf())` guarantees that a `403 Forbidden` response in RBAC tests is genuinely caused by role authorization failure, not CSRF rejection.

### Q08: How does `@WithMockUser(roles = "TEACHER")` work under the hood?
**Answer:** It populates the Spring Security `SecurityContext` with an `Authentication` token containing a principal with the authority `ROLE_TEACHER` before the request is processed by `MockMvc`.

### Q09: In IT01, how do we confirm that `DELETE /api/students/{id}` actually deleted the database record?
**Answer:** Beyond asserting HTTP 204 No Content from `MockMvc`, the test directly invokes `assertThat(studentRepository.findById(studentId)).isEmpty()` against the live PostgreSQL database.

### Q10: Why do we configure `@ActiveProfiles("test")` in integration tests?
**Answer:** It directs Spring Boot to load `application-test.yml`, which overrides production database settings, disables the bootstrap administrator runner, and configures test-specific logging.

### Q11: What is the role of Maven Surefire vs. Maven Failsafe?
**Answer:** Maven Surefire runs during the `test` phase for unit tests (`*Test.java`). Maven Failsafe runs during the `integration-test` phase for integration tests (`*IT.java`) and verifies pass/fail status during the `verify` phase. Unlike Surefire, Failsafe allows post-integration cleanup goals to execute before failing the build.

### Q12: Why do integration test class names end in `IT` instead of `Test`?
**Answer:** Standard Maven conventions bind files ending in `Test` to Surefire (`mvn test`) and files ending in `IT` to Failsafe (`mvn verify`). This ensures unit and integration test phases can run independently.

### Q13: How does GitHub Actions wait for PostgreSQL to be ready before running tests?
**Answer:** In `.github/workflows/ci.yml`, the PostgreSQL service container defines a healthcheck:
`pg_isready -U postgres -d university_test` with 5-second intervals and 12 retries. The runner waits until this healthcheck passes before advancing to the job steps.

### Q14: Why does the CI workflow connect to `localhost:5432` rather than `postgres:5432`?
**Answer:** In GitHub Actions, when a service container port is mapped (`5432:5432`) and steps run directly on the host runner (not inside another container), the service is accessed via the runner's `localhost` loopback interface.

### Q15: Why is `if: always()` specified on the artifact upload step in CI?
**Answer:** By default, GitHub Actions steps do not run if a previous step fails. `if: always()` forces test report archiving even when tests fail, ensuring failure logs are preserved for debugging.

### Q16: How do we prevent test data pollution between integration tests?
**Answer:** Each integration test class has an `@BeforeEach` method that invokes `deleteAllInBatch()` on `StudentRepository`, `TeacherRepository`, `AdminRepository`, and `UserAccountRepository`, resetting database state before every test method.

### Q17: In IT04, why does `session.isInvalid()` evaluate to true after logout?
**Answer:** `SecurityConfig` specifies `.logout(logout -> logout.invalidateHttpSession(true))`. Calling `POST /logout` causes the session invalidation handler to terminate the servlet session.

### Q18: What is the purpose of `.dockerignore`?
**Answer:** It prevents sensitive files (`.env`), build directories (`target/`), IDE configuration (`.idea/`), and documentation from being copied into the Docker image build context, keeping the build secure and lean.

### Q19: Why does `.gitignore` exclude `.env` but track `.env.example`?
**Answer:** `.env` contains local, sensitive database passwords and admin secrets that must never be committed to Git. `.env.example` serves as a public template documenting the required environment variable names with dummy placeholders.

### Q20: Explain the Conventional Commit format `feat(student): ...` vs `fix(security): ...`.
**Answer:** Conventional Commits structure messages as `<type>(<scope>): <description>`. `feat(student)` indicates a new capability in the student module; `fix(security)` signifies a bug fix in authentication or authorization logic.

### Q21: What happens if `TEST_DB_PASSWORD` is omitted in the integration test environment?
**Answer:** The PostgreSQL JDBC driver fails to establish a connection, throwing a `PSQLException: password authentication failed`, causing Spring's `EntityManagerFactory` bean creation to fail and aborting the test suite.

### Q22: Why doesn't the bootstrap administrator overwrite existing admin accounts on restart?
**Answer:** In `BootstrapAdmin.java`, the runner executes `if (accounts.existsByRoleAndEnabledTrue(Role.ADMIN)) { return; }`. If an administrator account is already present in the database, the bootstrapping logic immediately exits.

### Q23: Why do we test teacher creation from a teacher account in IT02?
**Answer:** The assignment specification defines directory-level permissions: teachers can manage the entire teacher directory (including creating other teachers), while being strictly forbidden from student or admin endpoints.

### Q24: What is the advantage of Spring Security's `AccountStatusFilter`?
**Answer:** It verifies the user's enabled status, email, and password hash on every incoming request. If an administrator disables an account or updates credentials in the database, existing HTTP sessions are invalidated immediately on their next request.

### Q25: How would you debug a test that passes locally on Windows but fails in GitHub Actions?
**Answer:**
1. Download the JUnit XML report artifact from GitHub Actions (`target/surefire-reports` or `target/failsafe-reports`).
2. Check for operating system differences: file path separators (`/` vs `\`), environment variable case-sensitivity, or timezone discrepancies.
3. Verify that the CI PostgreSQL service container database name, user, and password match the `application-test.yml` environment variable overrides.
4. Ensure the test does not depend on Windows-specific default locale or file encoding.
