# Understand your project: simple explanations and the exact code to show

This guide follows your project's source code. Names, passwords, and IDs in examples are imaginary. Read one lesson at a time with IntelliJ open. **Ctrl+G** jumps to the line number in each code link.

For each lesson, learn: **What does it mean? → What happens? → Where is the code? → What should I say?**

**Start with these six sentences:**

1. Authentication checks my email/password and remembers my login in a session.
2. Authorization checks my role before allowing access to a directory.
3. An API controller receives a request and returns data; the service does the business work.
4. Repositories and Hibernate read/write the database; entities describe its tables.
5. Security also checks input, CSRF tokens, safe output, and database constraints.
6. Dockerfile builds the app image; Compose runs that app together with PostgreSQL.

Everything below answers **how** those six sentences happen in your code. You can study one lesson per sitting; you do not need to read the whole file at once.

| Lesson | What you will learn |
|---|---|
| [1. Big picture](#1-the-big-picture) | How the application's parts work together |
| [2. Authentication](#2-authentication-proving-who-you-are) | Saving passwords, checking login, and remembering the user |
| [3. Authorization](#3-authorization-checking-what-you-may-do) | Roles, allowed requests, and blocked requests |
| [4. API and CRUD](#4-api-and-crud-follow-one-request) | Following an HTTP request all the way through Java |
| [5. Database](#5-database-where-and-how-records-are-stored) | Tables, relationships, repositories, and transactions |
| [6. Security](#6-security-which-problem-is-stopped-where) | CSRF, validation, password protection, and safe responses |
| [7. Docker basics](#7-docker-understand-five-words-first) | Image, container, build, network, volume |
| [8. Dockerfile](#8-dockerfile-every-line) | Every line and its purpose |
| [9. compose.yaml](#9-composeyaml-every-line) | Every line of the application/database setup |
| [10. compose.test.yaml](#10-composetestyaml-every-line) | Every line of the separate test-database setup |
| [11. Connecting Docker pieces](#11-connect-the-docker-pieces) | Ports, localhost, variables, startup, storage |
| [12. Teacher practice](#12-practice-showing-the-code-to-your-teacher) | Short answers, code map, demonstration order |

## 1. The big picture

Your application stores **university profiles** and **login accounts**.

- Profile: name, roll, department, semester, or designation.
- Account: email, password hash, role, enabled status.

Students, teachers, and admins have profiles linked to accounts. Courses, attendance, marks, and fees are not implemented.

```text
Request from browser/API client
    ↓
Security: Is this request allowed?
    ↓
Controller: Which method handles this URL and HTTP method?
    ↓
Service: Apply rules and coordinate changes
    ↓
Repository: Find/save/delete records
    ↓
Hibernate + JDBC driver: Carry out the database work
    ↓
PostgreSQL: Read/store the rows
```

The result travels back as HTML or JSON. **Two front doors, same office:** `/students` uses StudentController for HTML; `/api/students` uses StudentRestController for JSON. Both call StudentService. The website's Save button submits an HTML form, not a JavaScript call to the JSON API.

**Say:** “My Java 17 Spring Boot application manages admin, teacher, and student profiles with linked login accounts. HTML pages and JSON APIs share services and PostgreSQL. Spring Security handles login and permissions. Docker Compose runs the application and database.”

Show [pom.xml](<D:/University management system/pom.xml:5>) for libraries and [UniversityManagementApplication.java](<D:/University management system/src/main/java/com/cse3230/university/UniversityManagementApplication.java:6>) for startup. `SpringApplication.run(...)` creates the Spring application and starts the embedded web server. Spring supplies dependencies through constructors; this is **dependency injection**.

## 2. Authentication: proving who you are

### Easy idea

At an exam hall, you first prove your identity. In this application, you submit **email + password**.

**Authentication asks: “Is this really the owner of this account?”** Understand two separate moments: saving a password and checking a login.

### 2.1 Before login: where the saved password comes from

Open [AccountService.java:22](<D:/University management system/src/main/java/com/cse3230/university/service/AccountService.java:22>):

```java
String normalized = email.trim().toLowerCase(Locale.ROOT);
if (accounts.existsByEmail(normalized)) { throw new ConflictException("Email is already in use"); }
return accounts.save(new UserAccount(normalized, passwords.encode(password), role));
```

| Expression | Meaning |
|---|---|
| `trim().toLowerCase(...)` | Remove surrounding spaces and lowercase the email. |
| `existsByEmail(...)` | Check whether an account already uses that email. |
| `passwords.encode(password)` | Turn the raw password into a hash. **This is where hashing happens.** |
| `accounts.save(...)` | Save the email, password hash, and role as a login account. |

Which algorithm? [SecurityConfig.java:18](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:18>) defines:

```java
public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
```

Spring gives that BCrypt encoder to AccountService. BCrypt uses a random salt and a work factor. The database stores a hash in `UserAccount.passwordHash`. **It does not store the raw password, and login does not decrypt it.** There is no separate salt column managed by this application.

### 2.2 Follow Ayesha pressing Sign in

Assume Ayesha already has a student account.

| Step | What happens | Where it is handled |
|---|---|---|
| 1 | She opens the page and enters email/password. | [login.html:14](<D:/University management system/src/main/resources/templates/login.html:14>) |
| 2 | The form submits POST `/login`, including its CSRF token. | The form; [SecurityConfig:35](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:35>) |
| 3 | Spring Security handles that submission. The username parameter is named `email`. | [SecurityConfig:36](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:36>) |
| 4 | Spring asks our lookup service to find the account by email. | [DatabaseUserDetailsService:16](<D:/University management system/src/main/java/com/cse3230/university/security/DatabaseUserDetailsService.java:16>) |
| 5 | Our service returns AccountPrincipal, containing the identity details Spring needs. | [AccountPrincipal:19](<D:/University management system/src/main/java/com/cse3230/university/security/AccountPrincipal.java:19>) |
| 6 | Spring's authentication provider checks account status and verifies the submitted password against the saved hash using BCrypt. | Framework code uses our UserDetailsService and PasswordEncoder. |
| 7 | Success: associate authentication with the server session, change the session ID, redirect to `/dashboard`. | Framework session handling; [SecurityConfig:37](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:37>) |
| 8 | The browser sends JSESSIONID on later requests so the server can find the session. | [Session settings](<D:/University management system/src/main/resources/application.yml:19>) |

**Where is the POST login controller?** There is no custom `@PostMapping("/login")`. Spring Security's `UsernamePasswordAuthenticationFilter` processes it. Your configuration tells Spring how to process it. GET `/login` displays the page through [PageController.java:9](<D:/University management system/src/main/java/com/cse3230/university/controller/PageController.java:9>). [Spring form-login reference](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/form.html).

### 2.3 Read the lookup code in English

From [DatabaseUserDetailsService.java:16](<D:/University management system/src/main/java/com/cse3230/university/security/DatabaseUserDetailsService.java:16>):

```java
return accounts.findByEmail(email.trim().toLowerCase(Locale.ROOT))
        .map(AccountPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
```

1. `findByEmail`: look up the normalized email in user_accounts through the repository.
2. `map(AccountPrincipal::new)`: if found, create the identity object Spring understands.
3. `orElseThrow`: otherwise fail the lookup with a generic message.

This method does **not** compare passwords. AccountPrincipal's `getUsername()` supplies email; `getPassword()` supplies the hash; `isEnabled()` supplies status; `getAuthorities()` supplies role permissions. Spring performs verification using those values and the encoder.

### 2.4 Session and cookie: a simple picture

Think of a session as the server's guest register and the cookie as the visitor's reference number:

```text
Browser: JSESSIONID = some-session-number
                          ↓
Server session: authenticated account 12, role STUDENT
```

This is an illustration, not a database table. The project does not configure a shared database/Redis session store. `user_accounts` stores accounts, not these login sessions.

| Word | Meaning in this project |
|---|---|
| Session | Server-side state remembering authentication. |
| Cookie | Browser-carried session identifier. It does not contain the raw password. |
| JWT | A different token format. This application does not implement JWT/bearer login. |
| Session timeout | 30 minutes of inactivity, not a fixed 30 minutes after login. |

Changing the session ID after successful login helps prevent session fixation. [Spring session reference](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html).

### 2.5 Failed login, logout, and old sessions

| Event | Result | Code |
|---|---|---|
| Wrong credentials | Redirect to `/login?error`; show a generic failure message. | [SecurityConfig:37](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:37>) |
| POST `/logout` with CSRF | Invalidate session, clear authentication, delete JSESSIONID, redirect to login. | [SecurityConfig:38](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:38>) |
| Saved email/password changes or account is deleted | Revoke the old session on its next request. | [AccountStatusFilter:18](<D:/University management system/src/main/java/com/cse3230/university/security/AccountStatusFilter.java:18>) |
| GET `/api/auth/me` | Return safe data about the current account using principal ID. | [AuthRestController:17](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/AuthRestController.java:17>) |

**What the extra filter does:** get the current authentication → load its account by ID → compare enabled/email/hash/role → on mismatch, call `session.invalidate()` and `SecurityContextHolder.clearContext()`. SecurityContextHolder exposes the current request's authentication. A name/department change does not invalidate it. The filter can detect externally changed role/enabled status, although the public API offers no role-change/disable operation.

**Say:** “DatabaseUserDetailsService loads the account, AccountPrincipal provides its hash and role, and Spring verifies the password with BCrypt. Successful login uses a server session and JSESSIONID. AccountService is where passwords are hashed before saving.”

## 3. Authorization: checking what you may do

### Easy idea

Your ID lets you enter campus; it does not give you the keys to every office.

**Authentication: Who are you? Authorization: What may you do?**

| Role | Admin records | Teacher records | Student records |
|---|---|---|---|
| ADMIN | All CRUD | All CRUD | All CRUD |
| TEACHER | Blocked | All CRUD | Blocked |
| STUDENT | Blocked | Blocked | All CRUD |

### 3.1 Where the permission is enforced

Open [SecurityConfig.java:24](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:24>). Actual student rule:

```java
.requestMatchers("/api/students", "/api/students/**", "/students", "/students/**")
    .hasAnyRole("ADMIN", "STUDENT")
```

| Part | Read it as |
|---|---|
| `requestMatchers(...)` | When the request path matches one of these... |
| `/api/students` | The student JSON collection route. |
| `/api/students/**` | Student API paths below it, such as an ID. |
| `/students`, `/students/**` | Student browser pages/forms. |
| `hasAnyRole("ADMIN", "STUDENT")` | Allow an ADMIN or a STUDENT. |

Rules are not split by HTTP method, so a permitted role gets all implemented CRUD operations in that directory. The final `denyAll()` rules block unmatched requests. Public routes use `permitAll()`, which permits anonymous access but does not disable other checks such as CSRF for a POST.

### 3.2 How a database role becomes a permission

```text
user_accounts.role = STUDENT
    ↓ AccountPrincipal.getAuthorities()
Spring authority = ROLE_STUDENT
    ↓ hasAnyRole("ADMIN", "STUDENT")
Student-directory request allowed
```

Show [AccountPrincipal.java:30](<D:/University management system/src/main/java/com/cse3230/university/security/AccountPrincipal.java:30>): it constructs `"ROLE_" + role`. Spring's hasRole/hasAnyRole convention adds that prefix when checking the configured names.

### 3.3 Follow one forbidden request

Ayesha is logged in as STUDENT and requests GET `/api/admins`:

1. Spring retrieves her session authentication.
2. AccountStatusFilter checks that her identity is still current.
3. The admin URL rule requires ADMIN.
4. Her authority is ROLE_STUDENT, so access is denied.
5. The API returns **403 Forbidden**. AdminRestController does not perform its CRUD operation.

Show [SecurityConfig.java:27](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:27>) and [ApiSecurityErrors.java:23](<D:/University management system/src/main/java/com/cse3230/university/security/ApiSecurityErrors.java:23>).

**401 vs 403:** anonymous protected API request → 401, login required. Authenticated forbidden request → 403. Missing/invalid CSRF can also cause 403 for an authenticated write. Anonymous browser-page requests normally redirect to login.

**Project fact to remember:** a STUDENT can edit other students, including account details. Authorization is by directory, not ownership. Restricting “only my profile” would require comparing target profile account ID with logged-in account ID, with an admin exception.

The menu hides irrelevant links with `sec:authorize` in [fragments.html:12](<D:/University management system/src/main/resources/templates/fragments.html:12>). The backend rules still block forbidden URLs typed manually. Hiding a button alone would not secure an operation.

**Say:** “SecurityConfig enforces URL permissions before controllers. AccountPrincipal converts the database role into a Spring authority. Access is role-based across a whole directory; ownership checks are not implemented.”

## 4. API and CRUD: follow one request

### Easy idea

An API lets a program request data or operations. This API uses HTTP and returns **JSON**, text data with named fields. An endpoint is an HTTP method plus a URL.

REST means Representational State Transfer. Describe this project as a **REST-style JSON CRUD API**. It uses resource URLs and HTTP methods, but its session authentication means the whole application is not strictly stateless REST.

### 4.1 Every student operation and its code

| CRUD | Request | Controller line | Service line | Success |
|---|---|---|---|---|
| Read all | GET `/api/students` | [17](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/StudentRestController.java:17>) | [25](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:25>) | 200 + list |
| Read one | GET `/api/students/{id}` | [20](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/StudentRestController.java:20>) | [29](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:29>) | 200 + object |
| Create | POST `/api/students` | [23](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/StudentRestController.java:23>) | [31](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:31>) | 201 + object + Location |
| Update | PUT `/api/students/{id}` | [29](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/StudentRestController.java:29>) | [43](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:43>) | 200 + updated object |
| Delete | DELETE `/api/students/{id}` | [34](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/StudentRestController.java:34>) | [58](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:58>) | 204, empty body |

CRUD = Create, Read, Update, Delete. Teachers/admins have the same five operations under `/api/teachers` and `/api/admins`. PATCH is not implemented.

### 4.2 Example: admin creates a student

```http
POST /api/students
Content-Type: application/json
Cookie: JSESSIONID=<admin-session>
X-CSRF-TOKEN: <fresh-token-for-that-session>

{
  "name": "Ayesha Rahman",
  "roll": "CSE-101",
  "department": "CSE",
  "semester": 3,
  "email": "ayesha@example.test",
  "password": "DemoPassword123!"
}
```

POST is the **method**, `/api/students` is the **path**, Content-Type/Cookie/X-CSRF-TOKEN are **headers**, and JSON is the **body**. The placeholder cookie/token must be replaced in a real request.

### 4.3 Where does JSON enter Java?

Open [StudentRestController.java:11](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/StudentRestController.java:11>). Its class declares `@RestController` and `@RequestMapping("/api/students")`. Actual create method:

```java
@PostMapping
public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
    var created = service.create(request);
    return ResponseEntity.created(URI.create("/api/students/" + created.id())).body(created);
}
```

| Code | Meaning |
|---|---|
| `@RestController` | Return response data, normally JSON here. |
| `@RequestMapping` | Give this class a common URL prefix. |
| `@PostMapping` | Handle POST at that prefix. |
| `@RequestBody StudentRequest` | Jackson converts JSON into this Java input object. |
| `@Valid` | Check default validation rules. |
| `service.create(request)` | Call the business rules. |
| `ResponseEntity.created(...)` | Set 201 Created and Location pointing at the new resource. |
| `.body(created)` | Return the response data. |

A **DTO** (Data Transfer Object) defines input/output fields. StudentRequest is accepted input; StudentResponse is output. An **entity** instead maps stored database data. `@PathVariable Long id` on other methods reads an ID from the URL.

### 4.4 Where does the actual creation happen?

Open [StudentService.java:31](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:31>):

| Actual line | What happens | Why |
|---|---|---|
| 31 `@Transactional` | Run the operation in one transaction. | Linked writes must succeed together. |
| 33 `validator.validate(request, true)` | Validate creation requirements. | Require valid profile fields and password. |
| 34–36 `existsByRoll(...)` | Reject duplicate roll. | Keep roll unique. |
| 37 `accounts.create(..., Role.STUDENT)` | Create the login account with fixed role. | Client cannot choose ADMIN. |
| 38 `new Student(...)` | Build profile linked to that account. | Connect university data and identity. |
| 40 `profiles.saveAndFlush(profile)` | Save and send pending changes to the database. | Persist profile and detect database conflicts. |
| 40 `StudentResponse.from(...)` | Build safe output. | Exclude credentials/internal fields. |

Line 37 enters AccountService: normalize email → check duplicate → hash password → save account. Successful service completion commits the transaction. The controller returns 201. **One request saves two linked rows.**

### 4.5 Read, update, delete in ordinary English

**Read:** `findAll(Sort.by("id"))` loads profiles ordered by ID; `StudentResponse::from` converts each to output. `requireProfile(id)` finds one or throws NotFoundException. [StudentService:25](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:25>).

**Update:** validate → load existing profile → reject a roll belonging to another student → update account → update profile → flush. `existsByRollAndIdNot` means “Does a different profile use this roll?” Null/empty password keeps the hash; a supplied replacement is validated/hashed. PUT requires profile fields; it is not a general partial-update operation. [StudentService:43](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:43>), [AccountService:28](<D:/University management system/src/main/java/com/cse3230/university/service/AccountService.java:28>).

**Delete:** load profile → remember account → delete profile → flush → delete account. The order respects the foreign key explained in section 5. [StudentService:58](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:58>).

### 4.6 How errors are handled

| Situation | HTTP result | Handling |
|---|---|---|
| Protected API without login | 401 | SecurityConfig + ApiSecurityErrors |
| Wrong role / authenticated write with bad CSRF | 403 | Security filters + ApiSecurityErrors |
| Invalid fields, malformed JSON, unknown JSON field | 400 | Parsing/validation + RestExceptionHandler |
| Missing profile | 404 | Service throws NotFoundException; exception handler maps it |
| Duplicate email/roll or restricted admin deletion | 409 | Service/database exception; handler maps it |
| Unsupported method on resource route | 405 | Framework/error handling |
| Unsupported body content type | 415 | Framework/error handling |

Show [RestExceptionHandler.java:14](<D:/University management system/src/main/java/com/cse3230/university/exception/RestExceptionHandler.java:14>), [ApiSecurityErrors.java:19](<D:/University management system/src/main/java/com/cse3230/university/security/ApiSecurityErrors.java:19>), and [ApplicationErrorController.java:16](<D:/University management system/src/main/java/com/cse3230/university/controller/ApplicationErrorController.java:16>) for fallback errors. [ApiError.java:7](<D:/University management system/src/main/java/com/cse3230/university/exception/ApiError.java:7>) defines timestamp, status, error, message, path, fieldErrors. Security may reject a request before input validation runs.

**Say:** “The controller handles HTTP input/output. The service applies business rules and transactions. Repositories access stored records. Response DTOs return selected fields as JSON with the correct status.”

## 5. Database: where and how records are stored

### 5.1 Four tables with different jobs

| Table | Stores | Entity |
|---|---|---|
| `user_accounts` | Email, hash, role, enabled, creation time | [UserAccount:9](<D:/University management system/src/main/java/com/cse3230/university/entity/UserAccount.java:9>) |
| `students` | Name, roll, department, semester, account link | [Student:5](<D:/University management system/src/main/java/com/cse3230/university/entity/Student.java:5>) |
| `teachers` | Name, employee ID, department, designation, account link | [Teacher:5](<D:/University management system/src/main/java/com/cse3230/university/entity/Teacher.java:5>) |
| `admins` | Name, designation, account link | [Admin:5](<D:/University management system/src/main/java/com/cse3230/university/entity/Admin.java:5>) |

An **entity** is a Java class mapped to a table. A **row** is one stored record; a **column** is one stored field.

### 5.2 Understand the relationship with example IDs

```text
students: id=7, name=Ayesha, roll=CSE-101, account_id=12
                                                    │
                         points to account with id=12
                                                    ↓
user_accounts: id=12, email=ayesha@example.test, hash=<BCrypt>, role=STUDENT
```

| Term | Meaning |
|---|---|
| Primary key | Identifies a row in its own table, such as students.id. |
| Foreign key | Points to another table's row, such as students.account_id. |
| Unique constraint | Prevent duplicate values in the constrained column(s). |
| NOT NULL | Requires a value; it does not by itself reject an empty string. |

Profile ID 7 and account ID 12 need not match. `/api/students/7` uses the profile ID.

### 5.3 Show the actual mapping

From [Student.java:18](<D:/University management system/src/main/java/com/cse3230/university/entity/Student.java:18>):

```java
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "account_id", nullable = false, unique = true)
private UserAccount account;
```

Read it as: “Each student must reference an account using account_id, and the same account_id cannot repeat in students.” LAZY requests loading the relationship on demand; DTO conversion accesses it inside the service transaction.

| Annotation | Meaning |
|---|---|
| `@Entity` | Hibernate manages this persistent class. |
| `@Table(name = "students")` | Use the students table. |
| `@Id @GeneratedValue(IDENTITY)` | Database-generated primary key. |
| `@Column(... unique = true)` | Database uniqueness constraint. |
| `@Enumerated(EnumType.STRING)` | Store role names as text. |
| `@PrePersist @PreUpdate` | Check profile/account role before JPA inserts/updates. |

Accounts are separate because all profile types share login fields/logic. Profiles keep their own university attributes. Role matching is enforced by services and Java lifecycle checks. Do not claim a global database constraint proves every account has exactly one matching profile across all three tables; raw SQL can bypass Java checks. `updatable = false` on role is an ORM instruction, not a database access permission.

### 5.4 “Where is the SQL? The repository is only an interface!”

Open [StudentRepository.java:6](<D:/University management system/src/main/java/com/cse3230/university/repository/StudentRepository.java:6>):

```java
public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByRoll(String value);
    boolean existsByRollAndIdNot(String value, Long id);
}
```

- `Student, Long`: entity type and ID type.
- Spring Data supplies the implementation of inherited findAll/findById/save/delete.
- Spring derives queries from names such as existsByRollAndIdNot.
- Hibernate translates persistence operations to SQL; JDBC/the PostgreSQL driver communicates with PostgreSQL.

**JPA** is the standard API for managing mapped Java objects. **Hibernate** implements it. **Spring Data JPA** simplifies repository writing. The explicit admin-lock query in [UserAccountRepository.java:17](<D:/University management system/src/main/java/com/cse3230/university/repository/UserAccountRepository.java:17>) is JPQL: it uses Java entity/property names.

### 5.5 Why one transaction matters

Imagine saving the account succeeds but saving its profile fails. Both must roll back, otherwise an unwanted standalone login account could remain.

```text
One transaction:
    save account
    save student linked to account
    both succeed → COMMIT both
    failure      → ROLLBACK the changes
```

Show [StudentService.java:31](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:31>): `@Transactional`. Read methods use the class's read-only transaction default; write methods override it.

[AccountService.java:12](<D:/University management system/src/main/java/com/cse3230/university/service/AccountService.java:12>) uses `Propagation.MANDATORY`: “An existing transaction is required.” Account writes join the profile operation. These operations' unchecked exceptions trigger normal rollback behavior.

**Flush ≠ commit:** flush sends pending changes to the database; commit completes the transaction. Flushed changes can still roll back. `saveAndFlush` helps detect database conflicts during the operation.

**Delete order:** delete/flush profile, then delete account. Removing the account first would conflict with the referencing profile's foreign key. This is explicit service code, not an automatic cascade.

### 5.6 Duplicates and simultaneous changes

Service duplicate checks give friendly errors. Two requests could check at the same time, so database uniqueness is the final protection. Show [Student.java:12](<D:/University management system/src/main/java/com/cse3230/university/entity/Student.java:12>) and [UserAccount.java:14](<D:/University management system/src/main/java/com/cse3230/university/entity/UserAccount.java:14>).

Admin deletion also locks admin account rows and prevents deleting the last enabled admin or the logged-in admin's own account. The lock prevents competing deletions from both passing the last-admin check. Show [AdminService.java:54](<D:/University management system/src/main/java/com/cse3230/university/service/AdminService.java:54>).

### 5.7 Database connection settings

Open [application.yml:4](<D:/University management system/src/main/resources/application.yml:4>):

```yaml
datasource:
  url: ${DB_URL:jdbc:postgresql://localhost:5433/universitydb}
  username: ${DB_USERNAME:university}
  password: ${DB_PASSWORD:}
```

URL locates PostgreSQL; username/password let the app connect. **Database credentials are separate from a person's application login email/password.**

`ddl-auto: update` asks Hibernate to adjust schema from entity mappings at startup; no Flyway/Liquibase migrations are configured. `open-in-view: false` disables holding the persistence context open for the whole view-rendering phase; services build DTOs inside transactions.

**Say:** “Entities map Java objects to PostgreSQL tables. Each profile has an account_id foreign key. Repositories use Spring Data JPA/Hibernate to access rows. Transactions keep account/profile changes all-or-nothing; database constraints protect uniqueness and relationships.”

If asked: ACID = Atomicity, Consistency, Isolation, Durability. Atomicity means all-or-nothing. A transaction does not replace the need for specific constraints/locks. Current list APIs load all records without pagination; larger datasets would benefit from pagination and deliberate relationship fetching.

## 6. Security: which problem is stopped where?

### 6.1 Protect saved passwords

**Problem:** database exposure should not reveal raw passwords. **Handling:** BCrypt in [AccountService.java:25](<D:/University management system/src/main/java/com/cse3230/university/service/AccountService.java:25>) and password-update [line 34](<D:/University management system/src/main/java/com/cse3230/university/service/AccountService.java:34>).

[PasswordValidator.java:9](<D:/University management system/src/main/java/com/cse3230/university/dto/PasswordValidator.java:9>) requires at least 10 Unicode code points and at most 72 UTF-8 bytes. A supplied password cannot be whitespace-only. Creation requires a password; update may omit it/send empty to keep the hash. There is no uppercase/digit/symbol composition rule.

### 6.2 CSRF: why the cookie is not enough

**Problem:** browsers automatically send applicable cookies. A malicious site could try to make a logged-in browser submit an unwanted change.

**Handling:** the legitimate client obtains a CSRF token and sends it with state-changing requests. Spring checks it. Show [SecurityConfig.java:35](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:35>):

```java
.csrf(Customizer.withDefaults())
```

| Item | Its job |
|---|---|
| Session cookie | Identify the login session. |
| Role authority | Decide which directory the user may access. |
| CSRF token | Add anti-forgery protection to state-changing requests. |

A CSRF token is not a password, JWT, or permission to access admin data. [Spring CSRF reference](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html).

**Actual API-client steps:**

1. GET `/api/auth/csrf`; keep its token and session cookie.
2. POST `/login` using **form-encoded** email/password, the token in X-CSRF-TOKEN, and the cookie.
3. Preserve the logged-in session cookie.
4. GET `/api/auth/csrf` again; successful login clears the previous token, so get a fresh one.
5. Send fresh token + cookie with POST/PUT/DELETE and POST logout.

Show [AuthRestController.java:23](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/AuthRestController.java:23>): it returns headerName, parameterName, token. Thymeleaf POST forms receive a hidden CSRF field through Spring integration at rendering time. GET reads normally need no CSRF header.

### 6.3 Reject invalid input on the server

Clients can bypass HTML restrictions, so [StudentRequest.java:5](<D:/University management system/src/main/java/com/cse3230/university/dto/StudentRequest.java:5>) declares server rules:

| Rule | Meaning |
|---|---|
| `@NotBlank` | Reject missing/blank required text. |
| `@Size(max = ...)` | Limit text length. |
| `@Email` | Check email format, not mailbox ownership. |
| `@NotNull @Min(1) @Max(12)` | Semester is required and between 1 and 12. |
| `@NotBlank(groups = Create.class)` | Password required for creation. |
| `@ValidPassword` | Custom password validation. |

Controller `@Valid` checks default constraints. Service calls [RequestValidator.java:13](<D:/University management system/src/main/java/com/cse3230/university/service/RequestValidator.java:13>), adding the Create group when creating. Service validation also protects callers outside the HTTP controller.

### 6.4 Stop the client choosing a privileged role

Suppose a client adds `"role": "ADMIN"` to student JSON:

1. StudentRequest has no role/account-ID field.
2. [application.yml:14](<D:/University management system/src/main/resources/application.yml:14>) rejects unknown JSON properties.
3. [StudentService.java:37](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:37>) fixes the new role as STUDENT.

This prevents **mass assignment**, meaning a client setting fields it should not control. It does not add ownership restrictions to the allowed directory.

### 6.5 Keep credentials out of output

[StudentResponse.java:5](<D:/University management system/src/main/java/com/cse3230/university/dto/StudentResponse.java:5>) includes [AccountResponse.java:7](<D:/University management system/src/main/java/com/cse3230/university/dto/AccountResponse.java:7>): ID, email, role, enabled, creation time. No password/hash field. UserAccount additionally marks its hash `@JsonIgnore`.

Request DTO `toString()` methods redact data; request-detail logging is disabled. These reduce accidental disclosure, rather than guaranteeing every possible log statement is safe.

### 6.6 Cookie/error settings and other checks

Show [application.yml:21](<D:/University management system/src/main/resources/application.yml:21>):

| Setting/protection | Easy meaning |
|---|---|
| `timeout: 30m` | Expire idle sessions after 30 minutes. |
| `http-only: true` | Ordinary browser JavaScript cannot directly read the cookie. |
| `same-site: lax` | Restrict cookie sending in many cross-site situations; CSRF checks still matter. |
| `secure: ${COOKIE_SECURE:false}` | Secure-cookie behavior is configurable; false supports local HTTP by default. |
| `include-stacktrace: never` | Do not expose stack traces through normal errors. |
| `include-message: never` | Do not automatically expose internal exception messages; custom handlers choose safe messages. |
| SQL injection protection | Current repository/JPQL paths bind values, rather than concatenate request text into SQL. |
| Script/HTML injection protection | Current templates display text using escaped th:text. |
| Old session revocation | AccountStatusFilter detects changed/deleted accounts on their next request. |

For HTTPS, configure Secure through a setting actually passed into the app. Current Compose does not pass COOKIE_SECURE just because it is written in .env. No explicit cross-origin API/CORS policy is configured; the website uses the same origin.

**Honest limitations:** no ownership restriction inside student/teacher directories, application login rate limiting, MFA, password-reset workflow, configured HTTPS termination, shared session store, or versioned database migrations. A .env file is configuration, not encryption or a secret manager.

**Say:** “Security happens at several points: BCrypt for passwords, backend URL role rules, CSRF for cookie-authenticated writes, server input validation, safe response DTOs, and database constraints plus transactions.”

## 7. Docker: understand five words first

### Easy idea

You want the same application/runtime/database setup each time you run the project. Docker packages the app; Compose connects it to PostgreSQL.

| Word | Easy meaning | Here |
|---|---|---|
| Image | A packaged template used to create containers. | App image and PostgreSQL image. |
| Container | A running instance of an image. | app container and db container. |
| Build | Turn source plus Dockerfile instructions into an image. | Compile/test/package the Java app. |
| Network | How containers communicate. | app reaches db using hostname db. |
| Volume | Storage with a lifecycle separate from a container. | postgres_data stores database files. |

**Dockerfile = recipe to build the app image. Compose = instructions to run cooperating services.**

These are Linux container images. On Windows, Docker Desktop supplies the Linux container-host environment. A container is not a complete separate virtual machine for each service.

The actual filenames are **Dockerfile**, **compose.yaml**, and **compose.test.yaml**. The leading dot belongs to **.dockerignore**, another file explained later.

## 8. Dockerfile: every line

**Follow two stages:** first create the JAR using build tools; then put the JAR into a Java runtime image.

Open [Dockerfile](<D:/University management system/Dockerfile:1>). This is the actual file:

```dockerfile
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp verify

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/university-management.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

The table covers **every source line**, including blank lines/comments. Indentation is shown in the file above; spaces show which block a YAML line belongs to.

| Line | Actual text | Meaning and purpose |
|---|---|---|
| 1 | `FROM maven:3.9.11-eclipse-temurin-17 AS build` | Start a build stage containing Maven 3.9.11 and Java 17 development tools. AS build gives this stage a name so another stage can copy from it. |
| 2 | `WORKDIR /workspace` | Work inside /workspace in the build container. Relative COPY/RUN paths now use this directory. This is not your Windows project path. |
| 3 | `COPY pom.xml ./` | Copy only pom.xml into /workspace first. pom.xml describes dependencies and the build. |
| 4 | `RUN mvn -B -ntp dependency:go-offline` | At image-build time, prepare Maven dependencies/plugins; downloads can happen here. -B means noninteractive batch mode. -ntp hides download-progress noise. dependency:go-offline prepares dependencies; it is not the same as Maven -o offline mode. Copying pom.xml first lets Docker reuse this layer when only source changes. |
| 5 | `COPY src ./src` | Copy your src folder into /workspace/src, including main Java/resources and test source/resources. |
| 6 | `RUN mvn -B -ntp verify` | At image-build time, compile, run normal tests, package the executable JAR, and complete Maven verification. A failing Maven invocation fails this build step. Normal tests use H2; this command does not enable the optional PostgreSQL integration profile. |
| 7 | *blank* | Blank line for readability. It performs no operation. |
| 8 | `FROM eclipse-temurin:17-jre-alpine` | Start a separate runtime stage with a Java 17 JRE on Alpine Linux. JRE runs Java; the builder JDK also supplies development tools. The final image is based on this stage rather than the full Maven build stage. |
| 9 | `RUN addgroup -S app && adduser -S app -G app` | Create a system group app and a system user app belonging to it. -S means system account/group; -G selects the group. The shell executes the second command only if the first succeeds because of &&. |
| 10 | `WORKDIR /app` | Use /app as the runtime working directory. |
| 11 | `COPY --from=build --chown=app:app /workspace/target/university-management.jar app.jar` | Copy the JAR from the named build stage, not from your PC. --chown gives ownership to user/group app. The build output is renamed app.jar under /app. pom.xml finalName produces university-management.jar. |
| 12 | `USER app` | Use the app user for the container process instead of root. This reduces the privileges of the Java process. |
| 13 | `EXPOSE 8080` | Document that the application expects port 8080. EXPOSE does not open/publish a port on your PC; Compose ports does that. |
| 14 | `ENTRYPOINT ["java", "-jar", "app.jar"]` | When a container starts, run java -jar app.jar. This JSON/exec form launches Java directly without a shell wrapper. ENTRYPOINT is runtime behavior; the earlier RUN instructions executed while building the image. |

### What to say after showing the Dockerfile

> “The first FROM stage uses Maven and Java 17 tools to compile, test, and package. The second stage copies only the JAR into a Java runtime image. USER app runs it without root, and ENTRYPOINT launches Java when the container starts.”

**Why two stages?** The final image does not need Maven, source, or the builder dependency cache. **Why pom.xml first?** Reuse the dependency layer when only source changes. **JAR name?** [pom.xml:33](<D:/University management system/pom.xml:33>) sets finalName to university-management.

RUN is build time; ENTRYPOINT is container startup. CMD, if asked, commonly provides a default command/arguments, but this Dockerfile does not use CMD. [Docker multi-stage builds](https://docs.docker.com/get-started/docker-concepts/building-images/multi-stage-builds/) and [Dockerfile instruction reference](https://docs.docker.com/reference/dockerfile/).

## 9. compose.yaml: every line

**Follow two services:** db runs PostgreSQL; app runs your Java application. The final volumes block declares persistent database storage.

Open [compose.yaml](<D:/University management system/compose.yaml:1>). This is the actual file:

```yaml
name: university-management
services:
  db:
    image: postgres:16.15-alpine
    environment:
      POSTGRES_DB: universitydb
      POSTGRES_USER: ${POSTGRES_USER:-university}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD in .env}
    ports:
      - "127.0.0.1:${DB_PORT:-5433}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 5s
      timeout: 5s
      retries: 12
      start_period: 10s
    restart: unless-stopped
  app:
    build: .
    ports:
      - "127.0.0.1:${APP_PORT:-8080}:8080"
    environment:
      DB_URL: jdbc:postgresql://db:5432/universitydb
      DB_USERNAME: ${POSTGRES_USER:-university}
      DB_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD in .env}
      BOOTSTRAP_ADMIN_NAME: ${BOOTSTRAP_ADMIN_NAME:-}
      BOOTSTRAP_ADMIN_EMAIL: ${BOOTSTRAP_ADMIN_EMAIL:-}
      BOOTSTRAP_ADMIN_PASSWORD: ${BOOTSTRAP_ADMIN_PASSWORD:-}
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://127.0.0.1:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 45s
    restart: unless-stopped
volumes:
  postgres_data:
```

The table covers **every source line**, including blank lines/comments. Indentation is shown in the file above; spaces show which block a YAML line belongs to.

| Line | Actual text | Meaning and purpose |
|---|---|---|
| 1 | `name: university-management` | Give the Compose project the name university-management. Docker normally uses this name to prefix resources such as the default network and named volume. |
| 2 | `services:` | Begin the collection of cooperating services. A service describes how to create/run its container. |
| 3 | `db:` | Define the PostgreSQL service as db. Other services on this Compose network can use db as its hostname. |
| 4 | `image: postgres:16.15-alpine` | Use the tagged PostgreSQL 16.15 Alpine image. Docker obtains the image if needed; this project does not build PostgreSQL itself. |
| 5 | `environment:` | Begin variables passed into the database container. These POSTGRES variables are understood by the PostgreSQL image. |
| 6 | `POSTGRES_DB: universitydb` | Choose universitydb as the initial database name when PostgreSQL initializes an empty data directory. |
| 7 | `POSTGRES_USER: ${POSTGRES_USER:-university}` | Take POSTGRES_USER from Compose interpolation, or use university if it is unset/empty. This is the database login user, not an application student/admin. |
| 8 | `POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD in .env}` | Require a nonempty database password from Compose configuration. :? stops configuration with the written message when the value is missing/empty. No usable password is hardcoded here. |
| 9 | `ports:` | Begin port forwarding from your PC to the database container. |
| 10 | `- "127.0.0.1:${DB_PORT:-5433}:5432"` | Read left to right: host address 127.0.0.1 : host port DB_PORT (default 5433) : container port 5432. A database client on your PC uses localhost:5433 by default. Loopback binding limits the published port to local host access. |
| 11 | `volumes:` | Begin the database container storage mounts. |
| 12 | `- postgres_data:/var/lib/postgresql/data` | Attach named volume postgres_data at /var/lib/postgresql/data inside the database container. This is where PostgreSQL keeps its database files. Container replacement does not normally remove this named volume. |
| 13 | `healthcheck:` | Begin the instructions Docker uses to report whether the database container is healthy. |
| 14 | `test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]` | Run pg_isready through the container shell. -U selects the database user; -d selects the database. $$ tells Compose to leave a literal $ so the container shell expands its own POSTGRES_USER and POSTGRES_DB. This checks server acceptance of connections, not full application login/CRUD correctness. |
| 15 | `interval: 5s` | Use a regular 5-second interval between health checks. |
| 16 | `timeout: 5s` | Give an individual health check up to 5 seconds to finish. |
| 17 | `retries: 12` | Mark unhealthy after 12 consecutive counted failures. |
| 18 | `start_period: 10s` | Allow a 10-second startup grace period before failures count toward unhealthy status. It is not a mandatory 10-second sleep; an early successful check can establish health. |
| 19 | `restart: unless-stopped` | Apply Docker restart policy unless-stopped. It restarts eligible stopped/exited containers unless deliberately stopped. Being marked unhealthy alone does not cause an automatic restart. |
| 20 | `app:` | Define the Java application service as app. It is separate from db. |
| 21 | `build: .` | Build the app image using the current project directory as the build context and its default Dockerfile. |
| 22 | `ports:` | Begin forwarding a host port to the application container. |
| 23 | `- "127.0.0.1:${APP_PORT:-8080}:8080"` | Forward host loopback APP_PORT (default 8080) to internal app port 8080. If APP_PORT is 8081, the browser uses 8081 while Java inside Docker still uses 8080. |
| 24 | `environment:` | Begin variables passed into the Java application container. |
| 25 | `DB_URL: jdbc:postgresql://db:5432/universitydb` | Give Spring this JDBC database URL: PostgreSQL at hostname db, internal port 5432, database universitydb. db is the other container service; localhost here would mean the app container itself. |
| 26 | `DB_USERNAME: ${POSTGRES_USER:-university}` | Give the application the same database username chosen for db. Spring reads this through DB_USERNAME in application.yml. |
| 27 | `DB_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD in .env}` | Give the application the same required database password. Spring uses DB_PASSWORD to connect; this is separate from an application admin password. |
| 28 | `BOOTSTRAP_ADMIN_NAME: ${BOOTSTRAP_ADMIN_NAME:-}` | Pass the initial application-admin name. ${NAME:-} uses the provided nonempty value or an empty string. Java requires a valid name if it needs to bootstrap the first admin. |
| 29 | `BOOTSTRAP_ADMIN_EMAIL: ${BOOTSTRAP_ADMIN_EMAIL:-}` | Pass the initial application-admin email, with an empty fallback. This is an application login, not the PostgreSQL database user. |
| 30 | `BOOTSTRAP_ADMIN_PASSWORD: ${BOOTSTRAP_ADMIN_PASSWORD:-}` | Pass the initial application-admin password, with an empty fallback. BootstrapAdmin uses normal validated, hashed account creation only if no enabled admin exists. Changing this later does not reset an existing admin. |
| 31 | `depends_on:` | Begin startup dependency rules for app. |
| 32 | `db:` | Declare db as the dependency app needs. |
| 33 | `condition: service_healthy` | Wait for the db health check to report healthy before starting the dependent app service. This coordinates startup; it does not provide ongoing database failover. |
| 34 | `healthcheck:` | Begin the application container health check. |
| 35 | `test: ["CMD", "wget", "--spider", "-q", "http://127.0.0.1:8080/actuator/health"]` | Execute wget directly inside app; CMD means no shell expansion. --spider checks availability; -q keeps output quiet. The URL points to this container itself, internal port 8080, public Spring Actuator health endpoint. |
| 36 | `interval: 10s` | Use a regular 10-second interval for application health checks. |
| 37 | `timeout: 5s` | Allow an individual application health check up to 5 seconds. |
| 38 | `retries: 12` | Mark app unhealthy after 12 consecutive counted failures. |
| 39 | `start_period: 45s` | Allow a 45-second startup grace period before failures count. This allows time for Java, database/schema work, and bootstrap initialization; it is not a guaranteed fixed delay. |
| 40 | `restart: unless-stopped` | Apply unless-stopped restart policy to app. Health status and process restart policy are separate mechanisms. |
| 41 | `volumes:` | At top level, declare named storage volumes available to this project. This is outside services because of its indentation. |
| 42 | `postgres_data:` | Declare the name postgres_data used by db on line 12. Docker manages the storage; it is not the Java target directory. The actual volume name normally includes the Compose project prefix. |

### What to say after showing Compose

> “Compose starts PostgreSQL and the app as separate services, puts them on a network, passes database settings, publishes local ports, waits for database health, and mounts persistent PostgreSQL storage.”

The app health endpoint is allowed publicly in [SecurityConfig:26](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:26>). [application.yml:30](<D:/University management system/src/main/resources/application.yml:30>) exposes only minimal Actuator health without details.

Bootstrap is handled by [BootstrapAdmin.java:33](<D:/University management system/src/main/java/com/cse3230/university/config/BootstrapAdmin.java:33>): if an enabled admin exists, leave it unchanged; otherwise require name/email/password and call normal AdminService creation. Empty bootstrap defaults in Compose do not mean the first startup can succeed without credentials.

[Compose service reference](https://docs.docker.com/reference/compose-file/services/), [dependency startup order](https://docs.docker.com/compose/how-tos/startup-order/), and [variable syntax/dollar escaping](https://docs.docker.com/reference/compose-file/interpolation/).

## 10. compose.test.yaml: every line

**This is a separate test database setup.** It keeps destructive test setup/cleanup away from the normal universitydb database.

Open [compose.test.yaml](<D:/University management system/compose.test.yaml:1>). This is the actual file:

```yaml
name: university-management-tests
services:
  test-db:
    image: postgres:16.15-alpine
    environment:
      POSTGRES_DB: university_test
      POSTGRES_USER: university_test
      POSTGRES_PASSWORD: ${TEST_DB_PASSWORD:?Set TEST_DB_PASSWORD for the isolated test database}
    ports:
      - "127.0.0.1:5434:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U university_test -d university_test"]
      interval: 3s
      timeout: 3s
      retries: 20
    # Test data is disposable and never uses the development volume.
    tmpfs:
      - /var/lib/postgresql/data
```

The table covers **every source line**, including blank lines/comments. Indentation is shown in the file above; spaces show which block a YAML line belongs to.

| Line | Actual text | Meaning and purpose |
|---|---|---|
| 1 | `name: university-management-tests` | Use a different Compose project name, university-management-tests, so test resources are separate from the normal stack. |
| 2 | `services:` | Begin the services for the test stack. |
| 3 | `test-db:` | Define only the isolated PostgreSQL service test-db. This file does not define an app service or run Java tests itself. |
| 4 | `image: postgres:16.15-alpine` | Use the same PostgreSQL 16.15 Alpine image family as the application setup. |
| 5 | `environment:` | Begin database-container initialization variables. |
| 6 | `POSTGRES_DB: university_test` | Initialize a database named university_test. Tests must use their own database, not universitydb. |
| 7 | `POSTGRES_USER: university_test` | Use database user university_test. |
| 8 | `POSTGRES_PASSWORD: ${TEST_DB_PASSWORD:?Set TEST_DB_PASSWORD for the isolated test database}` | Require nonempty TEST_DB_PASSWORD. Configuration fails with this message if missing/empty. Use a disposable test credential, separate from the development database. |
| 9 | `ports:` | Begin host-to-container port publishing. |
| 10 | `- "127.0.0.1:5434:5432"` | Publish host loopback port 5434 to PostgreSQL internal port 5432. This differs from the normal database host port, usually 5433. |
| 11 | `healthcheck:` | Begin the readiness check for test PostgreSQL. |
| 12 | `test: ["CMD-SHELL", "pg_isready -U university_test -d university_test"]` | Use the container shell to run pg_isready with the fixed test user/database. No $$ expansion is needed because the names are written literally. |
| 13 | `interval: 3s` | Use a regular 3-second check interval. |
| 14 | `timeout: 3s` | Allow 3 seconds for each check. |
| 15 | `retries: 20` | Mark unhealthy after 20 consecutive counted failures. No custom start_period is specified in this file. |
| 16 | `# Test data is disposable and never uses the development volume.` | Comment for humans: this data is disposable and must not use the development named volume. Comments do not execute. |
| 17 | `tmpfs:` | Begin an in-memory tmpfs mount rather than a persistent named-volume mount. |
| 18 | `- /var/lib/postgresql/data` | Mount temporary storage at the PostgreSQL data directory. Its contents are ephemeral and lost when that tmpfs is unmounted, including container stop; this makes the isolated test database disposable. |

### How this file connects to Java tests

Select the test stack explicitly with `docker compose -f compose.test.yaml ...`. It starts the database, not Maven. Java's optional **postgres-it** Maven profile runs PostgresIT against that database.

[PostgresIT.java:11](<D:/University management system/src/test/java/com/cse3230/university/integration/PostgresIT.java:11>) reads TEST_DB_URL/USERNAME/PASSWORD. Its URL guard requires the isolated university_test database before allowing schema creation/drop and cleanup. The default host URL is localhost:5434/university_test.

Normal tests use H2, a temporary in-memory database, with bootstrap disabled and schema create-drop. See [application-test.yml](<D:/University management system/src/test/resources/application-test.yml:1>). H2's PostgreSQL mode is useful but is not actual PostgreSQL. [pom.xml:38](<D:/University management system/pom.xml:38>) defines the optional profile; [ci.yml:28](<D:/University management system/.github/workflows/ci.yml:28>) runs it in CI.

**Say:** “compose.yaml runs the application stack with persistent data. compose.test.yaml runs an isolated disposable PostgreSQL database. Normal tests use H2; actual PostgreSQL integration testing is a separate Maven profile.”

## 11. Connect the Docker pieces

### 11.1 Why db:5432 and localhost:5433 are both correct

**localhost means “this machine/container itself,” depending on where the code runs.**

```text
Your PC browser
  localhost:<APP_PORT, default 8080>
                │ port forwarding
                ↓
        app container:8080
                │ Compose network, hostname db
                ↓
        db container:5432
                ↑
                │ port forwarding
Your PC database client
  localhost:<DB_PORT, default 5433>
```

| Where the caller runs | Destination | Address to use |
|---|---|---|
| Java inside app container | Database in db container | `jdbc:postgresql://db:5432/universitydb` |
| Java directly in IntelliJ on PC | Database in db container | `jdbc:postgresql://localhost:5433/universitydb` by default |
| Browser on PC | App container | `http://localhost:<APP_PORT>/login` |
| App health check inside app | Its own web server | `http://127.0.0.1:8080/actuator/health` |
| Java integration tests on PC | Isolated test database | `jdbc:postgresql://localhost:5434/university_test` by default |

If app used localhost for PostgreSQL **inside Docker**, it would look for PostgreSQL in the app container. It needs db instead. Compose automatically creates the default network and service-name discovery; an explicit networks block is not required here.

APP_PORT controls the **host published port**. SERVER_PORT controls **Spring's listening port**. Current Compose leaves the internal Spring port at 8080. The local helper sets SERVER_PORT from the configured publishing port when running Java directly on the PC. README describes this PC using host port 8081; Compose's fallback remains 8080.

127.0.0.1 binds published ports locally. A teacher on another laptop cannot normally use your PC's LAN address to access this setup as currently configured.

### 11.2 Read the dollar-sign expressions without guessing

| Expression | Who interprets it? | Meaning |
|---|---|---|
| `${POSTGRES_USER:-university}` | Compose | Use a provided nonempty user, otherwise university. |
| `${POSTGRES_PASSWORD:?Set ...}` | Compose | Require a provided nonempty password, otherwise stop with the message. |
| `${BOOTSTRAP_ADMIN_NAME:-}` | Compose | Use the value if nonempty, otherwise empty text. |
| `$$POSTGRES_USER` | Compose, then container shell | Compose leaves a literal dollar; the shell expands the container's variable. |
| `${DB_USERNAME:university}` | Spring in application.yml | Use the configured property/environment value or Spring's written fallback. This is Spring syntax, not the Compose `:-` expression. |

The same word can appear in two configurations because Compose supplies a value and Spring consumes it. For example:

```text
.env POSTGRES_PASSWORD
    ↓ Compose interpolation
app environment DB_PASSWORD
    ↓ application.yml placeholder
spring.datasource.password
    ↓
Java can authenticate its connection to PostgreSQL
```

This is database connection authentication, separate from Ayesha's application login.

### 11.3 What .env does and does not automatically do

`.env` provides local values for Compose interpolation. Only values configured for a container, such as the environment entries in compose.yaml, get passed into it. Simply adding a new variable to .env does not inject it into app.

Spring does not automatically load arbitrary .env files either. The named IntelliJ configuration explicitly imports it. [scripts/run-local.ps1:7](<D:/University management system/scripts/run-local.ps1:7>) asks Compose to parse configuration and sets the Java process variables. [.env.example](<D:/University management system/.env.example:1>) lists the keys with empty secrets. [Docker interpolation reference](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/).

PostgreSQL's POSTGRES_DB/USER/PASSWORD variables initialize an **empty** data directory. Changing .env after a database already exists does not automatically change its saved user password. Likewise, changing bootstrap variables does not reset an existing application-admin password.

### 11.4 Tell the complete startup story

1. Compose reads the file and substitutes variables.
2. Docker builds the application image as required by the build/cache state.
3. PostgreSQL starts with its data volume.
4. PostgreSQL's pg_isready check reports healthy.
5. Compose starts app with its environment settings.
6. Java starts Spring; Spring connects to db; Hibernate applies configured schema updates.
7. BootstrapAdmin creates the first enabled admin if needed, using validated/hashed normal creation.
8. The app's own health check reports its health; use completed startup/health when preparing the demonstration.

If db later fails, depends_on is not a continuous repair system. If app is unhealthy but its process keeps running, restart policy alone does not automatically restart it.

### 11.5 Storage and commands

The named volume is normally called university-management_postgres_data. It is managed by Docker outside the Java target directory. Rebuilding app does not erase PostgreSQL data. A persistent volume is not a backup.

| Command | Meaning |
|---|---|
| `docker compose up --build -d` | Build as required and start the stack in the background. |
| `docker compose ps` | Show service state, health, published ports. |
| `docker compose logs app` | Read Java application output/startup errors. |
| `docker compose logs db` | Read PostgreSQL output. |
| `docker compose up -d --wait db` | Start/wait for only the database, useful before running Java in IntelliJ. |
| `docker compose stop` | Stop services; keep containers and volumes. |
| `docker compose down` | Remove stack containers/network; keep named volume by default. |
| `docker compose down -v` | Also remove named volumes, including stored application database data. Know this before choosing it. |
| `docker compose restart app` | Restart existing app; does not rebuild changed source. |
| `docker compose config --quiet` | Validate configuration without printing fully resolved credentials. |

These are learning/reference commands, not a record of a Docker stack run during this guide edit. A full `docker compose config` output can contain substituted secrets.

### 11.6 The supporting .dockerignore file

This file controls what is excluded from the Docker build context. It is separate from .gitignore, which controls Git tracking. Open [.dockerignore](<D:/University management system/.dockerignore:1>):

| Line | Text | Meaning |
|---|---|---|
| 1 | `.git` | Exclude Git metadata. |
| 2 | `.github` | Exclude GitHub workflow/support files. |
| 3 | `.tools` | Exclude this PC's local tools/caches. |
| 4 | `target` | Exclude host-generated build output; the Docker build creates its own JAR. |
| 5 | `.env` | Exclude the local configuration/secrets file. |
| 6 | `.env.*` | Exclude other environment-file variants. |
| 7 | `!.env.example` | Exception: allow the empty-secret example template back in. |
| 8 | `.idea` | Exclude IntelliJ settings. |
| 9 | `.vscode` | Exclude VS Code settings. |
| 10 | `*.log` | Exclude matching log files. |
| 11 | `scripts` | Exclude local helper scripts. |

The Dockerfile actually copies only pom.xml and src. Ignore rules reduce context and exclude local files; they do not encrypt secrets or undo an earlier leak.

## 12. Practice showing the code to your teacher

### 12.1 The “where is it handled?” map

| Teacher asks | Open this code | Point to this exact job |
|---|---|---|
| Where is login configured? | [SecurityConfig:36](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:36>) | formLogin, email parameter, redirects. |
| Where is the login account found? | [DatabaseUserDetailsService:16](<D:/University management system/src/main/java/com/cse3230/university/security/DatabaseUserDetailsService.java:16>) | Normalize/find email; return AccountPrincipal. |
| Where is a password hashed? | [AccountService:25](<D:/University management system/src/main/java/com/cse3230/university/service/AccountService.java:25>) | passwords.encode before saving. |
| Where is the algorithm selected? | [SecurityConfig:18](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:18>) | BCryptPasswordEncoder bean. |
| Where is the password compared? | Framework, using those configured objects | Spring authentication provider verifies the raw password against the hash. Do not invent an application matches method. |
| Where are role permissions checked? | [SecurityConfig:24](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:24>) | requestMatchers + hasRole/hasAnyRole. |
| Where does ROLE_STUDENT come from? | [AccountPrincipal:30](<D:/University management system/src/main/java/com/cse3230/university/security/AccountPrincipal.java:30>) | getAuthorities constructs ROLE_ plus stored role. |
| Where are old sessions revoked? | [AccountStatusFilter:18](<D:/University management system/src/main/java/com/cse3230/university/security/AccountStatusFilter.java:18>) | Compare current account; invalidate/clear on mismatch. |
| Where is CSRF handled? | [SecurityConfig:35](<D:/University management system/src/main/java/com/cse3230/university/config/SecurityConfig.java:35>), [AuthRestController:23](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/AuthRestController.java:23>) | Enable default checks; expose token-fetch endpoint. |
| Where does API creation start? | [StudentRestController:23](<D:/University management system/src/main/java/com/cse3230/university/controller/rest/StudentRestController.java:23>) | POST mapping, body validation, service call, 201/Location. |
| Where are profile/account created? | [StudentService:31](<D:/University management system/src/main/java/com/cse3230/university/service/StudentService.java:31>) | Shared transaction; account create; profile save. |
| Where are database tables/links defined? | [Student:5](<D:/University management system/src/main/java/com/cse3230/university/entity/Student.java:5>), [UserAccount:9](<D:/University management system/src/main/java/com/cse3230/university/entity/UserAccount.java:9>) | Entity/table/ID/column/relationship annotations. |
| Where is database access? | [StudentRepository:6](<D:/University management system/src/main/java/com/cse3230/university/repository/StudentRepository.java:6>) | JpaRepository and derived query names. |
| Where is invalid input rejected? | [StudentRequest:5](<D:/University management system/src/main/java/com/cse3230/university/dto/StudentRequest.java:5>), [RequestValidator:13](<D:/University management system/src/main/java/com/cse3230/university/service/RequestValidator.java:13>) | DTO constraints and service validation. |
| Where are safe JSON fields chosen? | [StudentResponse:5](<D:/University management system/src/main/java/com/cse3230/university/dto/StudentResponse.java:5>), [AccountResponse:7](<D:/University management system/src/main/java/com/cse3230/university/dto/AccountResponse.java:7>) | Response records omit password/hash. |
| Where does the HTML Save button go? | [students/form.html:10](<D:/University management system/src/main/resources/templates/students/form.html:10>), [StudentController:43](<D:/University management system/src/main/java/com/cse3230/university/controller/StudentController.java:43>) | HTML form POST → controller → shared StudentService. |
| Where is deployment described? | [Dockerfile](<D:/University management system/Dockerfile:1>), [compose.yaml](<D:/University management system/compose.yaml:1>), [compose.test.yaml](<D:/University management system/compose.test.yaml:1>) | Build recipe, normal stack, isolated test stack. |

### 12.2 Say these answers aloud

| Question | Simple answer |
|---|---|
| Authentication vs authorization? | Prove identity, then check what that identity is allowed to do. |
| Why use BCrypt? | Store a one-way password hash and verify passwords without storing raw credentials. |
| Are you using JWT? | No. We use server sessions and a JSESSIONID cookie. |
| Why CSRF for a JSON API? | This API authenticates with browser cookies, so writes need anti-forgery protection. |
| Can a student edit another student? | Yes, under the current directory-level role policy. |
| Can student JSON choose ADMIN? | No. The input excludes role, unknown JSON fields are rejected, and the service fixes STUDENT. |
| What is an endpoint? | An HTTP method and URL handled by the server. |
| Why controller and service separately? | Controller handles HTTP; service shares business rules between HTML and JSON callers. |
| Why two database rows for one student? | Login identity and university profile have separate jobs, linked by account_id. |
| Why @Transactional? | The linked account/profile changes must commit or roll back together. |
| Why delete profile first? | It contains the foreign key pointing to the account. |
| Why DTOs? | Accept only expected input and expose only intended output fields. |
| Why database constraints after service checks? | Concurrent requests can race; the database is the final uniqueness protection. |
| Dockerfile vs Compose? | Build the app image versus configure/run cooperating containers. |
| Image vs container? | Packaged template versus a running instance. |
| Why multi-stage? | Build with Maven/JDK, run with JRE/JAR without build tools. |
| Does EXPOSE publish a port? | No. Compose ports publishes it. |
| Why db instead of localhost? | PostgreSQL is in another container; db is its network hostname. |
| Does rebuilding app erase students? | No. PostgreSQL data is in the named volume. |
| Is the volume a backup? | No. Persistence and backup are different. |
| Does unhealthy automatically restart a service? | No. Health reporting and restart policy are separate. |
| What would you improve? | Ownership checks, login abuse controls, HTTPS/Secure cookies, managed secrets, migrations, pagination, backups. |

### 12.3 A simple live demonstration

1. Explain the opening project summary. Open SecurityConfig and StudentRestController beside the browser.
2. Log in using your configured account. Explain lookup, BCrypt, session cookie. Avoid displaying private .env credentials on a projector.
3. Create a disposable student. Trace StudentController → StudentService → AccountService/repositories → two linked rows.
4. Open `/api/students` in the logged-in browser to show JSON for the same data. GET uses the session cookie; no CSRF header is normally needed.
5. Edit the disposable profile. Show update and explain that an empty password keeps the hash.
6. With a separate student login/session, request `/api/admins`: explain 403 and the role rule. A request to another student's profile is allowed under current policy.
7. Demonstrate invalid semester or duplicate roll on disposable data. Show DTO validation and the unique database annotation.
8. Delete only the disposable record; show profile deletion/flush before account deletion.
9. Log out, then request a protected API again to show authentication is required.
10. Open Dockerfile/Compose; explain build vs runtime, both services, db hostname, ports, health, and volume. If already running, docker compose ps shows actual state/ports.

For a Postman write demonstration: fetch CSRF → form-encoded POST login with cookie/token → preserve cookie → fetch fresh CSRF → JSON POST/PUT/DELETE with cookie/token. GET `/api/auth/me` confirms who is logged in. Use an admin and disposable student so the demonstration does not modify/delete the session owner's account. A client following redirects may show the dashboard instead of the raw successful 302 login response.

### 12.4 Show the relationship directly in a database client

This read-only example joins profiles to accounts without selecting password hashes:

```sql
SELECT s.id AS student_id, s.name, s.roll, s.department, s.semester,
       a.id AS account_id, a.email, a.role, a.enabled
FROM students s
JOIN user_accounts a ON s.account_id = a.id
ORDER BY s.id;
```

JOIN combines rows through the key relationship. This is teaching SQL for a database client; it is not a literal query written inside StudentRestController.

### 12.5 Tests: evidence and limits

| Evidence | Test source |
|---|---|
| CRUD, status codes, safe JSON, invalid input | [RestCrudTest.java](<D:/University management system/src/test/java/com/cse3230/university/web/RestCrudTest.java:18>) |
| Role × directory × operation permissions and CSRF rejection | [SecurityMatrixTest.java](<D:/University management system/src/test/java/com/cse3230/university/web/SecurityMatrixTest.java:32>) |
| Real HTTP login, session change, logout, old-session revocation | [HttpSessionTest.java](<D:/University management system/src/test/java/com/cse3230/university/web/HttpSessionTest.java:41>) |
| Database uniqueness, foreign keys, rollback, admin deletion concurrency | [DatabaseContract.java](<D:/University management system/src/test/java/com/cse3230/university/integration/DatabaseContract.java:42>) |
| Actual PostgreSQL integration | [PostgresIT.java](<D:/University management system/src/test/java/com/cse3230/university/integration/PostgresIT.java:8>) |

The earlier guide-review test attempt on 1 November 2026 ran 178 cases: **172 passed, 0 assertion failures, 6 errors**. The real-HTTP test context encountered `Permission denied: connect` / `Unable to establish loopback connection` in this tool environment. Maven reported BUILD FAILURE; this was not a full successful verification. An initial Mockito attachment issue was avoided by preloading the cached Byte Buddy agent. The service, H2 database, MVC, REST CRUD, and role-matrix groups passed. The HTTP-session group needs a localhost-permitted environment to verify it.

The log is [target/viva-study-verification.log](<D:/University management system/target/viva-study-verification.log>). No fresh application test run, Docker deployment, or PostgreSQL integration run is claimed by this documentation rewrite. This edit checks explanations, source references, and Docker-line coverage. For normal startup/build instructions, use [README.md](<D:/University management system/README.md>).

**When stuck, follow this chain:** Which request? Which security check? Which controller? Which service rule? Which table? Which response?


