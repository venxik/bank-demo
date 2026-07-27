# bank-demo — Code Walkthrough
For: OCBC Full Stack Developer Technical Interview

Every other doc in this project explains a *concept*. This one is different: it's the actual reading order through `bank-demo`'s real files, so you can talk through *your own code* fluently if asked to share your screen or "walk me through this" — rather than reciting Spring theory disconnected from anything runnable. Read it once, file open beside this doc, in the order below — it follows the same path a real request takes through the app, which is also the order that actually makes sense to learn it in.

**Honesty framing, say this if asked directly**: `bank-demo` is a demo you built specifically for this interview's prep — to make sure you could write and explain real, working Spring/Java code, not something you built professionally. If asked "is this a production system," the honest answer is no — your real professional projects are Laku6 (Go/React Native) and CIMB Niaga (BI-FAST); this is deliberately-built practice, and treating it as anything else is the same overclaiming risk flagged in `Self_Intro_And_Behavioral.md`.

**Three views into the same code**: the **Reading Order** below is indexed *by sequence* — start to finish, the order a request actually flows through the app. The **Annotation Index** (end of this doc) is indexed *by annotation* — look up `@Transactional`, get every place it's used. The **Concept Index** (also at the end) is indexed *by concept* — look up "bean scope," get the explanation. Same codebase, three different ways to navigate it depending on how the question is phrased.

---

## Reading Order

### 1. `pom.xml` — what's actually in this app
Skim, don't memorize. What matters: `spring-boot-starter-web` (REST + embedded Tomcat), `-data-jpa` (Spring Data + Hibernate), `-validation` (Bean Validation), `-actuator` (health endpoints), `h2` (in-memory DB, runtime-only), `springdoc-openapi-starter-webmvc-ui` (Swagger UI, added 22 Jul), `liquibase-core` (schema migrations, added 22 Jul), `spring-boot-starter-test` (JUnit 5 + AssertJ + Mockito, test-scope only). One dependency per starter, not ten hand-picked libraries — if asked "what does Spring Boot actually give you," this file is the answer in miniature.

### 2. `src/main/java/com/ocbc/bankdemo/BankDemoApplication.java` — the entry point
11 lines. `@SpringBootApplication` on the class with `main()` — one annotation, three bundled behind it (`@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`). This is the file that makes every `@Service`/`@Component`/`@RestController` in every package under `com.ocbc.bankdemo` get picked up automatically — component scanning starts here and walks downward.

### 3. `src/main/resources/application.yml` — configuration
```yaml
jpa.hibernate.ddl-auto: validate   # Hibernate checks entities match the schema, never mutates it
liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml
management.endpoints.web.exposure.include: health,info
```
`ddl-auto: validate` + the Liquibase line is the current state as of 22 Jul — it used to be `update` (Hibernate freehanding the schema), which is why the Concept Index below and the README both call this out as a fixed known limitation. If asked "how do you manage schema changes," this file plus the next stop is your answer with a real example, not just the Liquibase primer in the abstract (`Spring_Java_QA.md` Part 19).

### 4. `src/main/resources/db/changelog/` — schema as code
`db.changelog-master.yaml` (the index) includes `changes/0001-initial-schema.yaml` (the actual changeset — two `createTable` blocks, `accounts` and `idempotency_records`, each with a `rollback: dropTable`). Know cold: a changeset is atomic and trackable, Liquibase records which ones have run in a `DATABASECHANGELOG` table, and this is what Hibernate's `validate` mode checks the entities against at every startup. If the changeset and the entities (next stop) ever drift, the app fails loudly at boot — that fail-loud behavior *is* the proof they match, not something separately tested.

### 5. `entity/Account.java` — the core domain object
```java
@Version
private long version;   // line 36-37
```
The single most important field in the whole project. Read the Javadoc directly above it (lines 27-35) — it explains the lost-update race this prevents and why `transfer()` locks accounts in ascending-id order. `credit()`/`debit()` (lines 74-80) are the *only* way `balance` ever changes — that's encapsulation doing real work, not textbook definition, and it's your ready-made answer if asked for a concrete OOP example. `@Table(name = "accounts")`, `@Column(nullable = false, unique = true)` on `accountNumber` — these map directly to the Liquibase changeset in stop 4, column for column.

### 6. `entity/IdempotencyRecord.java` — the dedup store
`idempotencyKey` is the `@Id` (a `String`, not an auto-generated `Long` — deliberate, since the *client* supplies this key, not the database). Holds `accountId`, `responseBody` (the serialized original response), `statusCode`, `createdAt`. This table backing a real unique-constraint-enforced dedup store — not an in-memory map — is exactly the distinction `Spring_Java_QA.md` Part 8 draws between `ConcurrentHashMap` (in-process only, lost on restart) and a real durable idempotency mechanism.

### 7. `repository/AccountRepository.java` + `repository/IdempotencyRecordRepository.java`
```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);
}
```
12 and 7 lines respectively — interfaces, zero implementation written by you. `existsByAccountNumber`/`findByAccountNumber` are derived-query methods — Spring Data JPA parses the method *name* and generates the query. This is your live example for "Repository pattern" *and* for "interface with zero implementation" if asked about abstraction (`Spring_Java_QA.md` Part 2).

### 8. `dto/` — six small files, the entity/API boundary
- `CreateAccountRequest.java`, `AmountRequest.java`, `TransferRequest.java` — Bean Validation annotations (`@NotNull`, `@DecimalMin("0.01")`) on `record` fields. `TransferRequest` is the one to actually reread: `@NotNull Long toAccountId` + `@DecimalMin("0.01") BigDecimal amount`.
- `AccountResponse.java` — a `record` with a static `from(Account)` factory method (lines 15-24) that's the explicit mapping boundary between entity and API shape. Notice what's *missing* from the response compared to the entity: nothing — actually `version` is exposed here (line 13), deliberately, since the frontend displays it (`title="optimistic lock version"` in `App.jsx`). This is a case where the DTO *does* mirror the entity closely — still worth explaining *why* it's still a separate class (schema/contract decoupling, not accidentally leaking a lazy-loaded relation).
- `TransferResponse.java` — 4 lines, just two `AccountResponse` fields (`from`, `to`).
- `ErrorResponse.java` — `record` with two static factory overloads (`of(status, error, message)` and `of(..., details)`), consumed entirely by the next stop.

### 9. `notification/` — Strategy pattern, three files
Read `NotificationService.java` (24 lines) first — the whole SOLID walkthrough is in its Javadoc (lines 5-21): OCP, DIP, ISP, LSP, each explained against this exact interface. Then `ConsoleNotificationService.java` and `SmsNotificationService.java` — both `@Component`, both one-method implementations. Know cold: `AccountService` (next stop) takes `List<NotificationService>` as a constructor parameter, and Spring auto-collects *every* bean implementing the interface into that list — adding a third channel later means adding a third `@Component` class, zero edits to `AccountService`. This is your single best "show me a design pattern in Spring" answer, because it's Strategy + three SOLID principles + Spring's collection-injection behavior, all in ~70 lines total across three small files.

### 10. `service/AccountService.java` — the heart of the app (175 lines, read in full)
The one file worth genuinely knowing line-by-line:
- **Lines 37-45**: constructor injection, four dependencies, no `@Autowired` needed (single constructor). This is your "two types of Autowired" answer with real code.
- **Line 47-49**: `notifyAll` — one line, fans out to every injected `NotificationService`.
- **Lines 51-58**: `createAccount` — checks duplicate account number first, throws before ever touching the DB for the actual insert.
- **Lines 68-91**: `deposit`/`withdraw` — both wrapped in `executeIdempotent` (stop below), `withdraw` throws `InsufficientFundsException` before mutating anything.
- **Lines 93-134**: `transfer` — **read the Javadoc above it in full**, it's the richest paragraph in the codebase: why accounts lock in ascending-id order (deadlock avoidance under a hypothetical pessimistic-lock scheme), why this demo is actually exposed to a lost-update race instead (no explicit lock mode), and why `@Version` is what actually guards against it here. This single comment block is a complete answer to "how do you prevent a race condition on account balances" (`Banking_Wealth_Domain_Playbook.md`, Scenario 6) *and* "what's the difference between optimistic and pessimistic locking."
- **Lines 141-154**: `executeIdempotent` — the generic idempotency helper. A null/blank key skips the mechanism entirely (line 142-144); otherwise, check the dedup store first, and only run+store on a genuine miss (lines 146-153). This is `Banking_Wealth_Domain_Playbook.md` Scenario 2, implemented, not just described.
- **Lines 160-174**: `serialize`/`deserialize` — Jackson `ObjectMapper` turning the stored response back into the right type on replay.

### 11. `antipattern/BrokenAccountLookupService.java` — the contrast case
38 lines, **not wired into any controller** — exists purely to point at. Field injection (`@Autowired private AccountRepository accountRepository`, line 30-31) instead of constructor injection. The Javadoc (lines 9-26) lists all four real reasons this is worse: can't be `final`, `new BrokenAccountLookupService()` compiles with a null repository and only NPEs on first use, testing needs reflection instead of a plain constructor call, and a circular dependency between two field-injected beans fails silently late instead of loud at startup. If the interviewer asks "why constructor injection," you have a real, deliberately-broken file to contrast against, not just an abstract argument.

### 12. `controller/AccountController.java` — the HTTP layer (62 lines)
Six endpoints, each thin — no business logic, just deserialize/delegate/return. Notice: `deposit`/`withdraw`/`transfer` (lines 42-61) all take `@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey` and pass it straight through to the service. This file is your walkthrough answer for "did you use microservices" in miniature — it's the thin controller layer that a real full-stack request actually flows through, HTTP in, HTTP out, everything else delegated.

### 13. `exception/` + `GlobalExceptionHandler.java` — centralized error handling
Five tiny exception classes (`AccountNotFoundException`, `DuplicateAccountNumberException`, `InsufficientFundsException`, `SelfTransferException`, 7 lines each), then `GlobalExceptionHandler.java` (65 lines) — one `@RestControllerAdvice`, six `@ExceptionHandler` methods, each mapping one exception type to one HTTP status: `AccountNotFoundException` → 404, `InsufficientFundsException`/`DuplicateAccountNumberException`/`OptimisticLockingFailureException` → 409, `SelfTransferException`/validation failures → 400, anything else → 500 (line 60-64, the catch-all). The `OptimisticLockingFailureException` handler (lines 38-43) is the other half of stop 5's `@Version` story — this is *where* that lost-update race actually surfaces as an HTTP response, not just a thrown exception nobody catches.

### 14. `src/test/java/.../AccountServiceTransferTest.java` — what's actually verified
`@SpringBootTest` (line 17) — real Spring context, real (in-memory) DB, not mocks. Four tests, know what each one proves: `transferMovesBalanceBetweenAccounts` (happy path, balances move correctly both directions), `transferToSelfIsRejected` (the `SelfTransferException` guard), `transferBeyondBalanceIsRejected` (the `InsufficientFundsException` guard), and `replayingIdempotencyKeyDoesNotDoubleDebit` (lines 51-62) — calls `transfer` twice with the *same* idempotency key and asserts the balance only moved once. That last test is the actual, runnable proof behind every "how do you make a payment idempotent" answer in this project — not a claim, a passing test.

### 15. `frontend/src/` — the React/Redux side
- `App.jsx` (146 lines) — `CreateAccountForm`, `AccountRow`, `App` — plain fetch calls (`apiCall` helper, lines 6-13) for mutations, Redux for the shared account list.
- `accountsSlice.js` — `createSlice` + one `createAsyncThunk` (`fetchAccounts`), `extraReducers` handling `.fulfilled`/`.rejected`.
- `store.js` — `configureStore({ reducer: { accounts: accountsReducer } })`, one slice.
- `main.jsx` — `<Provider store={store}>` wrapping `<App />`.
Know cold: only the shared account list moved into Redux; per-row local state (the amount input, the transfer-target select) deliberately stayed as local `useState` in `AccountRow`. If asked "why not put everything in Redux," this is a real, defensible answer — not every piece of state needs to be global, only what's actually shared.

### 16. `Dockerfile` + `.github/workflows/ci.yml` — the ops side
`Dockerfile` — multi-stage: `maven:3.9-eclipse-temurin-17` builds the jar, `eclipse-temurin:17-jre-jammy` runs it, nothing from the build stage ships in the final image. `ci.yml` — two GitHub Actions jobs, `backend` (`mvn test`) and `frontend` (`npm ci && npm run lint && npm run build`), both triggered on push/PR. Both were actually built and verified end to end (real `docker build`+`docker run`+health-check, real CI commands run locally first) — see `Interview_Countdown_Plan.md` Part 4 for the full verification log if asked for specifics.

---

## Quick Lookup: Question → File

| If asked about... | Go straight to |
|---|---|
| Two types of Autowired / constructor vs. field injection | `service/AccountService.java:37-45` vs. `antipattern/BrokenAccountLookupService.java:30-31` |
| A design pattern in Spring | `notification/NotificationService.java` (Strategy + SOLID Javadoc) |
| `@Transactional` / the self-invocation gotcha | `service/AccountService.java:51,68,79,107` |
| Race conditions / optimistic locking | `entity/Account.java:27-37` + `exception/GlobalExceptionHandler.java:38-43` |
| Idempotency | `service/AccountService.java:141-154` + `AccountServiceTransferTest.java:51-62` |
| Entity vs. DTO | `entity/Account.java` vs. `dto/AccountResponse.java:15-24` |
| Centralized error handling | `exception/GlobalExceptionHandler.java` |
| Bean Validation | `dto/TransferRequest.java:7-14` |
| Schema management / Liquibase | `application.yml:11-22` + `db/changelog/changes/0001-initial-schema.yaml` |
| Testing strategy | `AccountServiceTransferTest.java` (`@SpringBootTest`, all four tests) |
| Redux | `frontend/src/accountsSlice.js` + `store.js` |
| Docker / CI | `Dockerfile` + `.github/workflows/ci.yml` |

If they ask to see any of this live rather than just talk through it: `mvn spring-boot:run` (backend on `:8080`), `cd frontend && npm run dev` (frontend on `:5173`), Swagger UI at `/swagger-ui/index.html` — all in the README.

---

## Annotation Index

Beginner cheat sheet, indexed *by annotation* rather than reading order. Every annotation below actually appears in this codebase — file/line noted so you can go look at real usage.

### App bootstrap

**`@SpringBootApplication`** — `BankDemoApplication.java:6`
Marks the entry point class. Bundles three annotations into one: `@Configuration` (this class can define beans), `@EnableAutoConfiguration` (Spring guesses config from what's on the classpath — e.g. sees JPA + H2 → wires a DataSource), `@ComponentScan` (scans this package and sub-packages for `@Component`/`@Service`/`@RestController` etc. and registers them as beans). One per app, on the class with `main()`.

### Dependency injection / bean registration

Spring keeps a container of objects ("beans") it creates and wires together for you, instead of you calling `new` everywhere.

**`@Component`** — `ConsoleNotificationService.java:15`, `SmsNotificationService.java:14`
Generic "let Spring manage this class" marker. Spring creates one instance (singleton by default) and makes it injectable elsewhere.

**`@Service`** — `AccountService.java:26`
Same mechanics as `@Component` — it's just a more specific name for "this is business-logic layer", so the code documents itself. Spring treats it identically to `@Component` under the hood.

**Constructor injection (no annotation needed)** — `AccountService.java:37-45`, `AccountController.java:22-24`
When a class has exactly one constructor, Spring automatically injects the required beans as arguments — no `@Autowired` needed. This is why `AccountService(...)` just lists `AccountRepository`, `NotificationService`, etc. and Spring supplies real instances at startup. Note in `AccountService`: the constructor takes `List<NotificationService>` — Spring collects **every** bean implementing that interface into the list automatically (`ConsoleNotificationService` + `SmsNotificationService` today).

### Web layer (HTTP in/out)

**`@RestController`** — `AccountController.java:16`
Combines `@Controller` (this class handles HTTP requests) + `@ResponseBody` (return values get serialized straight to JSON in the response body, instead of being treated as a view name to render).

**`@RequestMapping("/accounts")`** — `AccountController.java:17`
Sets a base URL path for every endpoint method in the class. Every method below inherits the `/accounts` prefix.

**`@GetMapping`, `@PostMapping`** — `AccountController.java:26,32,37,42,49,56`
Shorthand for `@RequestMapping(method = GET)` / `(method = POST)`. Maps an HTTP verb + path to a method. `@GetMapping("/{id}")` combined with the class-level mapping means `GET /accounts/{id}`.

**`@PathVariable`** — `AccountController.java:38`
Pulls a value out of the URL path. `@GetMapping("/{id}")` + `@PathVariable Long id` means whatever's in `{id}` in the URL becomes the `id` parameter.

**`@RequestBody`** — `AccountController.java:27`
Deserializes the incoming JSON request body into a Java object (here, `CreateAccountRequest`).

**`@RequestHeader`** — `AccountController.java:45`
Reads an HTTP header value. `@RequestHeader(value = "Idempotency-Key", required = false)` reads the `Idempotency-Key` header, or `null` if the caller didn't send one.

**`@Valid`** — `AccountController.java:27`
Tells Spring to run Bean Validation on the object right after deserializing it (see `@NotNull`/`@DecimalMin` below). If validation fails, Spring throws `MethodArgumentNotValidException` before your method body ever runs — that's what `GlobalExceptionHandler.handleValidation` catches.

**`@RestControllerAdvice`** — `GlobalExceptionHandler.java:17`
Global exception handler for every `@RestController` in the app — `= @ControllerAdvice + @ResponseBody`. One place to turn exceptions into HTTP error responses instead of try/catch in every controller method.

**`@ExceptionHandler(SomeException.class)`** — `GlobalExceptionHandler.java:20,26,32,38,45,51,60`
Inside a `@RestControllerAdvice` (or `@Controller`), marks a method as "run this when `SomeException` (or a subclass) escapes a controller method." Spring matches the most specific exception type.

### Validation (on DTO fields)

**`@NotNull`** — `TransferRequest.java:8,11`
Field must not be `null`, checked when `@Valid` runs.

**`@DecimalMin("0.01")`** — `TransferRequest.java:12`
Numeric field must be `>=` the given value — here, rejects zero/negative transfer amounts.

(Both come from `jakarta.validation.constraints`, the standard Bean Validation API — not Spring-specific, but Spring wires it in via `@Valid`.)

### Persistence (JPA / database)

**`@Entity`** — `Account.java:7`
Marks a class as mapped to a database table — JPA/Hibernate will manage its lifecycle (insert/update/delete rows for it).

**`@Table(name = "accounts")`** — `Account.java:8`
Optional — overrides the table name. Without it, JPA would default to the class name (`Account`/`account`).

**`@Id`** — `Account.java:11`
Marks the primary-key field.

**`@GeneratedValue(strategy = GenerationType.IDENTITY)`** — `Account.java:12`
The database auto-generates the ID (e.g. auto-increment column) — you don't set it yourself before saving.

**`@Column(nullable = false, unique = true)`** — `Account.java:15,18,21,24`
Customizes the mapped database column. `nullable = false` → `NOT NULL` constraint. `unique = true` → unique constraint. `updatable = false` (line 24, `createdAt`) → this column is set once on insert and never touched by later `UPDATE`s.

**`@Version`** — `Account.java:36`
Optimistic locking. JPA auto-increments this field on every `UPDATE` and includes the old value in the `WHERE` clause. If another transaction already bumped it, your update matches zero rows and Hibernate throws `OptimisticLockingFailureException` instead of silently overwriting someone else's change. See the longer comment right above it in `Account.java` for the concurrency reasoning.

**`JpaRepository<Account, Long>`** (interface, not an annotation) — `AccountRepository.java:8`
Not an annotation, but shows up alongside these so worth naming: extend this and Spring Data JPA generates the implementation for you at runtime (`save`, `findById`, `findAll`, etc. all "just work" with zero code). Method names like `findByAccountNumber` get auto-implemented too, by parsing the method name.

### Transactions

**`@Transactional`** — `AccountService.java:51,68,79,107`
Wraps the method in a database transaction: if the method completes normally, the transaction commits; if it throws an unchecked exception, everything the method did gets rolled back. E.g. in `transfer()`, if `to.credit(...)` somehow failed after `from.debit(...)` already ran, the debit would be rolled back too — no half-finished transfer.

---

## Concept Index

The concepts that make the annotations above make sense, indexed *by concept* rather than reading order.

### What Spring / Spring Boot actually is

**Spring** (core) = a container that creates your objects and wires their dependencies together, instead of you writing `new AccountService(new AccountRepository(...), ...)` by hand everywhere. That container is called the **IoC container** (Inversion of Control) — "inversion" because normally your code controls object creation; here the framework controls it and just hands you the finished objects.

```java
// without Spring — you control construction, and you're wiring this by hand everywhere it's needed
AccountService service = new AccountService(new AccountRepository(), new ConsoleNotificationService());

// with Spring — you just declare what AccountService needs; the container builds the whole graph
@Service
public class AccountService {
    public AccountService(AccountRepository repository, List<NotificationService> channels) { ... }
    // Spring calls this constructor at startup with real bean instances — you never call `new` here
}
```

**Spring Boot** = Spring + opinionated auto-setup. Plain Spring needs a lot of manual XML/Java config to wire up a web server, a database connection, JSON serialization, etc. Spring Boot looks at what's on your classpath (`pom.xml` dependencies) and auto-configures all of that for you. This project's `pom.xml` pulls in `spring-boot-starter-web` + `spring-boot-starter-data-jpa` + H2 → Spring Boot sees those and auto-configures an embedded Tomcat server, a JSON converter, and a `DataSource` pointing at H2, with zero manual config beyond `application.yml`.

### Beans and the IoC container

A **bean** = any object Spring's container creates and manages, instead of you calling `new` yourself. `AccountService`, `AccountController`, `ConsoleNotificationService` — all beans.

**How a class becomes a bean**: annotate it `@Component`/`@Service`/`@RestController`/`@Repository` (all are `@Component` under the hood, just semantically named) and it lives inside a package `@SpringBootApplication` scans (`BankDemoApplication.java:6` scans `com.ocbc.bankdemo` and everything under it — which is every package in this project).

**Bean scope**: default is **singleton** — one shared instance for the whole app. `ConsoleNotificationService.java:9-13`'s comment calls this out explicitly: you get the Singleton pattern for free from Spring's default scope, no hand-written `private constructor + static getInstance()` needed.

**Dependency Injection (DI)** = how beans get handed their dependencies. This project uses **constructor injection**: `AccountService`'s constructor (`AccountService.java:37-45`) lists everything it needs as parameters, and because there's exactly one constructor, Spring auto-supplies real bean instances — no `@Autowired` annotation required on the constructor itself.

Contrast: `antipattern/BrokenAccountLookupService.java` exists specifically to show **field injection** (`@Autowired` directly on a field) and why it's worse — can't be `final`, can't be constructed without Spring (breaks plain unit tests / `new SomeService()`), hides dependencies from the constructor signature. Worth reading that file's Javadoc directly.

One field-injection exception you'll see and shouldn't copy into prod code: `AccountServiceTransferTest.java:20` uses `@Autowired` on a field to grab `AccountService` in a test class. That's normal/accepted in test code — tests aren't instantiated with `new` by your own code, Spring's test runner does it, so the usual "breaks plain construction" objection doesn't apply.

### Layered architecture

This project follows the standard three-layer split, one package per layer:

```
controller/  → HTTP in/out only. Parses requests, calls service, returns responses. No business logic.
service/     → Business logic + transaction boundaries (@Transactional). Calls repositories.
repository/  → Database access. Just interfaces — Spring Data JPA writes the implementation.
```

Request flow for, say, `POST /accounts/1/deposit`:
1. `AccountController.deposit()` (`AccountController.java:42-47`) receives the HTTP request, deserializes JSON into `AmountRequest`
2. Delegates to `AccountService.deposit()` (`AccountService.java:68-77`) — this is where the actual "credit the balance" logic + transaction live
3. Service calls `AccountRepository` (`AccountRepository.java`) to load/save the `Account` entity
4. Service maps the `Account` entity to an `AccountResponse` DTO before returning
5. Controller returns that DTO, Spring serializes it to JSON for the HTTP response

### Entity vs DTO — why two classes for "an account"

`entity/Account.java` = mirrors a database row exactly (has `@Version`, JPA annotations, mutable setters via `credit()`/`debit()`). `dto/AccountResponse.java` = what actually goes out over HTTP.

Why not just return the entity directly? Decoupling: the API response shape can differ from the DB schema (add/remove/rename a DB column without breaking the API contract, or vice versa), and you avoid accidentally leaking internal fields or triggering lazy-loading issues by serializing a JPA-managed object straight to JSON. `AccountResponse.from(Account)` (`AccountResponse.java:15-24`) is the explicit mapping boundary between the two.

DTOs in this project are **Java records** (`record AccountResponse(...)`) — a concise way to declare an immutable data-holder class. Java auto-generates the constructor, getters (`id()`, `balance()`, not `getId()`), `equals()`, `hashCode()`, and `toString()` from the field list. No Lombok, no boilerplate.

```java
@Entity                             // entity/Account.java — mirrors the DB row exactly
public class Account {
    @Version private long version;  // JPA bookkeeping, has no business meaning to an API caller
    // ... mutable via credit()/debit(), never serialized straight to JSON
}

public record AccountResponse(Long id, String accountNumber, BigDecimal balance) { // dto/AccountResponse.java
    static AccountResponse from(Account a) {                 // the explicit mapping boundary
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getBalance());
    }
    // no `version` field exposed — the API contract doesn't need to know about optimistic locking
}
```

### Configuration: `application.yml`

`src/main/resources/application.yml` — Spring Boot auto-loads this at startup. Structure mirrors Java package nesting:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:bankdemo   # in-memory DB, wiped on every restart
  jpa:
    hibernate:
      ddl-auto: validate         # Hibernate only CHECKS entities match the schema, never mutates it
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

`ddl-auto` used to be `update` in this project (Hibernate freehanding `CREATE TABLE` from `@Entity` annotations) — as of 22 Jul 2026 it's `validate` instead, and `db/changelog/` (a Liquibase changelog + one changeset, `accounts` + `idempotency_records`) is what actually owns the schema now. If the changeset and the entities ever drift, the app fails loudly at startup instead of silently working with a schema nobody wrote down — see `Spring_Java_QA.md` Part 19 for the full Liquibase concept.

`management.endpoints.web.exposure.include: health,info` turns on **Spring Boot Actuator** endpoints (`/actuator/health`, `/actuator/info`) — built-in ops/monitoring endpoints, added via the `spring-boot-starter-actuator` dependency in `pom.xml`.

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

### Maven / `pom.xml`

Maven = the build tool: declares dependencies, compiles code, runs tests, packages a runnable jar (`mvn clean package` → `target/bank-demo-0.0.1-SNAPSHOT.jar`, per README).

Key parts of this project's `pom.xml`:
- `<parent>spring-boot-starter-parent</parent>` — inherits a curated set of dependency *versions* that are tested to work together, so you don't hand-pick a Spring version + a Jackson version + a Hibernate version and hope they're compatible.
- **Starters** (`spring-boot-starter-web`, `-data-jpa`, `-validation`, `-actuator`) — each is a bundle of related dependencies. `-web` alone pulls in embedded Tomcat, Spring MVC, and Jackson (JSON). You depend on the starter, not the ten individual libraries inside it.
- `spring-boot-starter-test` (`scope: test`) — brings in JUnit 5, AssertJ (`assertThat`), Mockito, and Spring's test support, only on the test classpath, not shipped in the final jar.
- `spring-boot-maven-plugin` — the plugin that makes `mvn spring-boot:run` and the executable "fat jar" (all dependencies bundled inside one jar) possible.

### Embedded server

No separate Tomcat install, no WAR file deployed to an app server. `spring-boot-starter-web` bundles Tomcat *inside* the jar. `mvn spring-boot:run` or `java -jar ...jar` starts the whole app, server included, listening on `:8080` (README) — this is why "just run the jar" works.

### Testing: `@SpringBootTest`

`AccountServiceTransferTest.java:17` — boots the **entire** Spring application context (all beans, real wiring) for the test, then injects `AccountService` and calls it directly, hitting the real (in-memory H2) database. This is an integration test, not a pure unit test — it's testing `AccountService` + `AccountRepository` + JPA + H2 all together, which is why it can catch things like the idempotency-replay behavior (`replayingIdempotencyKeyDoesNotDoubleDebit`, line 51) that a mocked-repository unit test wouldn't.

### Request validation flow, end to end

Ties `@Valid` + Bean Validation + `@RestControllerAdvice` together (see the Annotation Index above for each annotation individually):

1. Client `POST`s a `TransferRequest` JSON body with `amount: -5`
2. `@RequestBody` deserializes it, `@Valid` (`AccountController.java:58`) triggers validation against `@DecimalMin("0.01")` on `TransferRequest.amount` (`TransferRequest.java:12`)
3. Validation fails → Spring throws `MethodArgumentNotValidException` *before* `AccountController.transfer()`'s body ever executes
4. `GlobalExceptionHandler.handleValidation()` (`GlobalExceptionHandler.java:51-58`) catches it, returns `400` with a field-level error list

Same shape for business-rule exceptions (`InsufficientFundsException`, `SelfTransferException`, etc.) — thrown from deep inside `AccountService`, caught centrally, never a try/catch in the controller.
