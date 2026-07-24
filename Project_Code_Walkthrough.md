# bank-demo — Code Walkthrough
For: OCBC Full Stack Developer Technical Interview

Every other doc in this project explains a *concept*. This one is different: it's the actual reading order through `bank-demo`'s real files, so you can talk through *your own code* fluently if asked to share your screen or "walk me through this" — rather than reciting Spring theory disconnected from anything runnable. Read it once, file open beside this doc, in the order below — it follows the same path a real request takes through the app, which is also the order that actually makes sense to learn it in.

**Honesty framing, say this if asked directly**: `bank-demo` is a demo you built specifically for this interview's prep — to make sure you could write and explain real, working Spring/Java code, not something you built professionally. If asked "is this a production system," the honest answer is no — your real professional projects are Laku6 (Go/React Native) and CIMB Niaga (BI-FAST); this is deliberately-built practice, and treating it as anything else is the same overclaiming risk flagged in `Behavioral_Interview_Prep.md`.

**How this relates to the other Spring docs**: `SPRING_ANNOTATIONS.md` is indexed *by annotation* (look up `@Transactional`, get every place it's used). `SPRING_FUNDAMENTALS.md` is indexed *by concept* (look up "bean scope," get the explanation). This doc is indexed *by reading order* — start to finish, in the sequence a request actually flows through the app.

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
`ddl-auto: validate` + the Liquibase line is the current state as of 22 Jul — it used to be `update` (Hibernate freehanding the schema), which is why `SPRING_FUNDAMENTALS.md` and the README both call this out as a fixed known limitation. If asked "how do you manage schema changes," this file plus the next stop is your answer with a real example, not just the Liquibase primer in the abstract.

### 4. `src/main/resources/db/changelog/` — schema as code
`db.changelog-master.yaml` (the index) includes `changes/0001-initial-schema.yaml` (the actual changeset — two `createTable` blocks, `accounts` and `idempotency_records`, each with a `rollback: dropTable`). Know cold: a changeset is atomic and trackable, Liquibase records which ones have run in a `DATABASECHANGELOG` table, and this is what Hibernate's `validate` mode checks the entities against at every startup. If the changeset and the entities (next stop) ever drift, the app fails loudly at boot — that fail-loud behavior *is* the proof they match, not something separately tested.

### 5. `entity/Account.java` — the core domain object
```java
@Version
private long version;   // line 36-37
```
The single most important field in the whole project. Read the Javadoc directly above it (lines 27-35) — it explains the lost-update race this prevents and why `transfer()` locks accounts in ascending-id order. `credit()`/`debit()` (lines 74-80) are the *only* way `balance` ever changes — that's encapsulation doing real work, not textbook definition, and it's your ready-made answer if asked for a concrete OOP example. `@Table(name = "accounts")`, `@Column(nullable = false, unique = true)` on `accountNumber` — these map directly to the Liquibase changeset in stop 4, column for column.

### 6. `entity/IdempotencyRecord.java` — the dedup store
`idempotencyKey` is the `@Id` (a `String`, not an auto-generated `Long` — deliberate, since the *client* supplies this key, not the database). Holds `accountId`, `responseBody` (the serialized original response), `statusCode`, `createdAt`. This table backing a real unique-constraint-enforced dedup store — not an in-memory map — is exactly the distinction `Technical_Fundamentals_Gap_Fill.md` Part 8 draws between `ConcurrentHashMap` (in-process only, lost on restart) and a real durable idempotency mechanism.

### 7. `repository/AccountRepository.java` + `repository/IdempotencyRecordRepository.java`
```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);
}
```
12 and 7 lines respectively — interfaces, zero implementation written by you. `existsByAccountNumber`/`findByAccountNumber` are derived-query methods — Spring Data JPA parses the method *name* and generates the query. This is your live example for "Repository pattern" *and* for "interface with zero implementation" if asked about abstraction (Technical_Fundamentals_Gap_Fill Part 2).

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
- **Lines 93-134**: `transfer` — **read the Javadoc above it in full**, it's the richest paragraph in the codebase: why accounts lock in ascending-id order (deadlock avoidance under a hypothetical pessimistic-lock scheme), why this demo is actually exposed to a lost-update race instead (no explicit lock mode), and why `@Version` is what actually guards against it here. This single comment block is a complete answer to "how do you prevent a race condition on account balances" (Banking Playbook, Scenario 6) *and* "what's the difference between optimistic and pessimistic locking."
- **Lines 141-154**: `executeIdempotent` — the generic idempotency helper. A null/blank key skips the mechanism entirely (line 142-144); otherwise, check the dedup store first, and only run+store on a genuine miss (lines 146-153). This is Banking Playbook Scenario 2, implemented, not just described.
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
