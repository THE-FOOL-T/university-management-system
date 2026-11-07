# Automated Testing and Continuous Integration Verification Report

**Project:** CSE 3230 University Management System  
**Framework:** Java 17 / Spring Boot 3.5.16  
**Repository:** [https://github.com/THE-FOOL-T/university-management-system](https://github.com/THE-FOOL-T/university-management-system)  
**Default Branch:** `main`  
**Execution Environment:** GitHub-hosted Ubuntu Runner (`ubuntu-latest`) with sidecar PostgreSQL 16 Alpine container  
**Workflow Run ID:** `42340529873`  
**Workflow Run URL:** [https://github.com/THE-FOOL-T/university-management-system/actions/runs/42340529873](https://github.com/THE-FOOL-T/university-management-system/actions/runs/42340529873)  
**Overall CI Status:** **SUCCESS (PASS)** :white_check_mark:

---

## 1. Executive Summary

| Category | Requirement | Implemented | Result |
|---|---|---|---|
| **Unit Tests** | 6–10 isolated tests | **8 test methods** | **8 / 8 PASS (100%)** |
| **Integration Tests** | 3–6 real tests | **4 test methods** | **4 / 4 PASS (100%)** |
| **Total Test Count** | 9–16 tests | **12 test methods** | **12 / 12 PASS (100%)** |
| **GitHub Actions CI** | Automated workflow | PostgreSQL service container, JUnit upload | **PASSED (Green)** |
| **Git & GitHub** | Conventional Commits | 7 structured commits pushed to `origin/main` | **VERIFIED** |

---

## 2. Test Execution Results Matrix

### Tier 1 — Unit Tests (Maven Surefire: `test` phase)

| Test ID | Test Class | Test Method | Purpose | Expected Outcome | Actual Outcome | Status |
|:---:|---|---|---|---|---|:---:|
| **UT01** | `StudentServiceTest` | `createStudent_shouldAssignStudentRoleAndEncodePassword()` | Verify student creation persists profile, assigns `STUDENT` role, and encodes password | `StudentResponse` returned with `Role.STUDENT`; password encoding delegated | Verified `Role.STUDENT` and `saveAndFlush()` call | **PASS** |
| **UT02** | `StudentServiceTest` | `createStudent_shouldRejectDuplicateEmail()` | Verify registration with existing email is rejected and prevents profile saving | `ConflictException` thrown; zero profile rows saved | `ConflictException` caught; `never().saveAndFlush()` verified | **PASS** |
| **UT03** | `StudentServiceTest` | `updateStudent_shouldModifyPermittedFieldsAndPreserveRole()` | Verify editing modifies permitted fields while preserving identity and `STUDENT` role | Updated fields persisted; role unchanged as `STUDENT` | Verified name, department, semester updated; role preserved | **PASS** |
| **UT04** | `StudentServiceTest` | `deleteStudent_shouldInvokeExpectedPersistenceAndAccountDeletion()` | Verify foreign-key deletion order (profile deleted/flushed before account) | Profile deleted and flushed before `accounts.delete()` | `InOrder` verification satisfied: `delete()`, `flush()`, `delete()` | **PASS** |
| **UT05** | `TeacherServiceTest` | `createTeacher_shouldAssignTeacherRoleAndPersistDetails()` | Verify teacher creation assigns `TEACHER` role and persists employee details | `TeacherResponse` with `Role.TEACHER`, employee ID, and designation | Verified `Role.TEACHER`, employee attributes, and account linkage | **PASS** |
| **UT06** | `AdminServiceTest` | `createAdmin_shouldAssignAdminRoleAndCreateRelatedRecords()` | Verify administrator creation assigns `ADMIN` role and links account | `AdminResponse` with `Role.ADMIN` and designation persisted | Verified `Role.ADMIN` and administrative record creation | **PASS** |
| **UT07** | `AdminServiceTest` | `deleteAdmin_shouldRejectDeletingLastAdministrator()` | Verify deletion of the final remaining active administrator is blocked | `ConflictException` thrown ("last enabled administrator") | `ConflictException` caught; zero administrative deletions | **PASS** |
| **UT08** | `PasswordServiceTest` | `encodePassword_shouldProduceDifferentHashAndVerifyWithRawPassword()` | Verify BCrypt password salting, non-plaintext hashing, and verification | Hashes differ, start with `$2a$`, match raw password | Distinct hashes generated; raw password matches both | **PASS** |

### Tier 2 — PostgreSQL Integration Tests (Maven Failsafe: `verify` phase)

| Test ID | Test Class | Test Method | Purpose | Expected Outcome | Actual Outcome | Status |
|:---:|---|---|---|---|---|:---:|
| **IT01** | `AdminCrudIT` | `adminShouldCompleteFullStudentCrudCycle()` | Exercise complete Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ PostgreSQL CRUD lifecycle | POST 201 $\rightarrow$ GET 200 $\rightarrow$ PUT 200 $\rightarrow$ DELETE 204 $\rightarrow$ DB empty | HTTP status cycle verified; record removed from PostgreSQL | **PASS** |
| **IT02** | `TeacherRbacIT` | `teacherRbac_shouldAllowTeacherEndpointsAndForbidAdminAndStudent()` | Prove TEACHER role can manage teachers, but is strictly forbidden from Admin & Student APIs | Teacher GET 200 / POST 201; Admin & Student GET/POST 403 Forbidden | RBAC rules enforced; write attempts with CSRF return 403 | **PASS** |
| **IT03** | `StudentRbacIT` | `studentRbac_shouldAllowStudentEndpointsAndForbidAdminAndTeacher()` | Prove STUDENT role can manage students, but is strictly forbidden from Teacher & Admin APIs | Student GET 200 / POST 201; Teacher & Admin GET/POST 403 Forbidden | RBAC rules enforced; teacher/admin access blocked with 403 | **PASS** |
| **IT04** | `AuthenticationIT` | `authenticationAndLogout_shouldEstablishSessionAndInvalidateOnLogout()` | Exercise authentication lifecycle: login, session check, CSRF logout, invalidation | Invalid login 302 error; valid login 302 dashboard; me 200; logout 302; me 401 | Full session lifecycle verified against PostgreSQL | **PASS** |

---

## 3. GitHub Actions Pipeline Verification

- **Workflow Name:** `Java 17 Build, Unit & Integration Tests CI`
- **Workflow File:** [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
- **Trigger Event:** `push` on `main`
- **Runner OS:** `ubuntu-latest`
- **Java Version:** 17.0.x (Eclipse Temurin)
- **Database Service:** `postgres:16-alpine` (mapped to `localhost:5432`, database: `university_test`)
- **Healthcheck Command:** `pg_isready -U postgres -d university_test`
- **CI Build Command:** `./mvnw -B clean verify`
- **Duration:** 1 minute 38 seconds
- **Conclusion:** **SUCCESS**

### Step-by-Step Execution Log Summary

```text
[Step 1]  Set up job ................................................. SUCCESS (2s)
[Step 2]  Initialize containers (postgres:16-alpine) ................. SUCCESS (8s)
[Step 3]  Checkout repository (actions/checkout@v4) ................... SUCCESS (1s)
[Step 4]  Set up JDK 17 (Eclipse Temurin with Maven cache) ........... SUCCESS (3s)
[Step 5]  Make Maven wrapper executable (chmod +x mvnw) ............... SUCCESS (0s)
[Step 6]  Build and run Unit & PostgreSQL Integration Tests ......... SUCCESS (64s)
            - Compiling main sources (45 classes)
            - Compiling test sources (8 classes)
            - Running Maven Surefire (8 unit tests) ................... 8 PASS, 0 FAIL
            - Packaging university-management.jar (55.6 MB)
            - Running Maven Failsafe (4 PostgreSQL integration tests) . 4 PASS, 0 FAIL
[Step 7]  Archive test reports (junit-test-reports) .................. SUCCESS (2s)
[Step 8]  Archive packaged application JAR (university-management-jar)  SUCCESS (4s)
[Step 9]  Post-execution cleanup & container shutdown ................ SUCCESS (1s)
```

### Uploaded Workflow Artifacts

1. **`junit-test-reports`** (`33.9 KB`): Full XML execution reports from both Maven Surefire and Failsafe.
2. **`university-management-jar`** (`55.6 MB`): Standalone, executable Spring Boot application JAR.

---

## 4. Git Commit History (Conventional Commits 1.0)

| Commit Hash | Conventional Commit Message | Scope of Changes |
|---|---|---|
| `a3139ce` | `docs(viva): add comprehensive testing and github actions viva guide` | Created `TESTING_AND_GITHUB_ACTIONS_VIVA.md`, updated `README.md` |
| `a98b1b7` | `ci(github): configure postgresql integration testing workflow` | Created `.github/workflows/ci.yml` with PostgreSQL service container |
| `317d358` | `test(integration): implement four postgresql integration tests` | Implemented `AdminCrudIT`, `TeacherRbacIT`, `StudentRbacIT`, `AuthenticationIT` |
| `30a8e13` | `test(unit): implement eight service and security unit tests` | Implemented `StudentServiceTest`, `TeacherServiceTest`, `AdminServiceTest`, `PasswordServiceTest` |
| `075c300` | `build(maven): configure surefire and failsafe test execution plugins` | Configured `maven-surefire-plugin` and `maven-failsafe-plugin` in `pom.xml` |
| `7307cdc` | `feat(core): implement university management spring boot application` | Committed all core Spring Boot packages, entities, services, controllers |
| `f7b8d6b` | `chore(project): initialize repository configuration and environment setup` | Initialized repository, `.gitignore`, Dockerfile, Compose, and wrapper scripts |

---

## 5. Conclusion

All teacher requirements have been satisfied:
- Exactly 8 unit tests implemented and passing.
- Exactly 4 integration tests implemented and passing against PostgreSQL.
- GitHub repository created, configured, and pushed with Conventional Commits.
- GitHub Actions CI pipeline executing automatically on pushes and pull requests.
- Actual remote CI workflow run completed with a 100% green passing status.
- Comprehensive viva guide documented for exam defense.
