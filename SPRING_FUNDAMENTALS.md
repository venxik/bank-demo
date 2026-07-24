# Spring Boot Fundamentals

Companion to `SPRING_ANNOTATIONS.md` (that file = what each annotation does; this file = the concepts that make the annotations make sense). Grounded in this project's actual files.

## What Spring / Spring Boot actually is

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

## Beans and the IoC container

A **bean** = any object Spring's container creates and manages, instead of you calling `new` yourself. `AccountService`, `AccountController`, `ConsoleNotificationService` — all beans.

**How a class becomes a bean**: annotate it `@Component`/`@Service`/`@RestController`/`@Repository` (all are `@Component` under the hood, just semantically named) and it lives inside a package `@SpringBootApplication` scans (`BankDemoApplication.java:6` scans `com.ocbc.bankdemo` and everything under it — which is every package in this project).

**Bean scope**: default is **singleton** — one shared instance for the whole app. `ConsoleNotificationService.java:9-13`'s comment calls this out explicitly: you get the Singleton pattern for free from Spring's default scope, no hand-written `private constructor + static getInstance()` needed.

**Dependency Injection (DI)** = how beans get handed their dependencies. This project uses **constructor injection**: `AccountService`'s constructor (`AccountService.java:37-45`) lists everything it needs as parameters, and because there's exactly one constructor, Spring auto-supplies real bean instances — no `@Autowired` annotation required on the constructor itself.

Contrast: `antipattern/BrokenAccountLookupService.java` exists specifically to show **field injection** (`@Autowired` directly on a field) and why it's worse — can't be `final`, can't be constructed without Spring (breaks plain unit tests / `new SomeService()`), hides dependencies from the constructor signature. Worth reading that file's Javadoc directly.

One field-injection exception you'll see and shouldn't copy into prod code: `AccountServiceTransferTest.java:20` uses `@Autowired` on a field to grab `AccountService` in a test class. That's normal/accepted in test code — tests aren't instantiated with `new` by your own code, Spring's test runner does it, so the usual "breaks plain construction" objection doesn't apply.

## Layered architecture

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

## Entity vs DTO — why two classes for "an account"

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

## Configuration: `application.yml`

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

`ddl-auto` used to be `update` in this project (Hibernate freehanding `CREATE TABLE` from `@Entity` annotations) — as of 22 Jul 2026 it's `validate` instead, and `db/changelog/` (a Liquibase changelog + one changeset, `accounts` + `idempotency_records`) is what actually owns the schema now. If the changeset and the entities ever drift, the app fails loudly at startup instead of silently working with a schema nobody wrote down — see the Liquibase Primer in `OCBC_Interview_Consolidated_Prep.md` §10 for the full concept.

`management.endpoints.web.exposure.include: health,info` turns on **Spring Boot Actuator** endpoints (`/actuator/health`, `/actuator/info`) — built-in ops/monitoring endpoints, added via the `spring-boot-starter-actuator` dependency in `pom.xml`.

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

## Maven / `pom.xml`

Maven = the build tool: declares dependencies, compiles code, runs tests, packages a runnable jar (`mvn clean package` → `target/bank-demo-0.0.1-SNAPSHOT.jar`, per README).

Key parts of this project's `pom.xml`:
- `<parent>spring-boot-starter-parent</parent>` — inherits a curated set of dependency *versions* that are tested to work together, so you don't hand-pick a Spring version + a Jackson version + a Hibernate version and hope they're compatible.
- **Starters** (`spring-boot-starter-web`, `-data-jpa`, `-validation`, `-actuator`) — each is a bundle of related dependencies. `-web` alone pulls in embedded Tomcat, Spring MVC, and Jackson (JSON). You depend on the starter, not the ten individual libraries inside it.
- `spring-boot-starter-test` (`scope: test`) — brings in JUnit 5, AssertJ (`assertThat`), Mockito, and Spring's test support, only on the test classpath, not shipped in the final jar.
- `spring-boot-maven-plugin` — the plugin that makes `mvn spring-boot:run` and the executable "fat jar" (all dependencies bundled inside one jar) possible.

## Embedded server

No separate Tomcat install, no WAR file deployed to an app server. `spring-boot-starter-web` bundles Tomcat *inside* the jar. `mvn spring-boot:run` or `java -jar ...jar` starts the whole app, server included, listening on `:8080` (README) — this is why "just run the jar" works.

## Testing: `@SpringBootTest`

`AccountServiceTransferTest.java:17` — boots the **entire** Spring application context (all beans, real wiring) for the test, then injects `AccountService` and calls it directly, hitting the real (in-memory H2) database. This is an integration test, not a pure unit test — it's testing `AccountService` + `AccountRepository` + JPA + H2 all together, which is why it can catch things like the idempotency-replay behavior (`replayingIdempotencyKeyDoesNotDoubleDebit`, line 51) that a mocked-repository unit test wouldn't.

## Request validation flow, end to end

Ties `@Valid` + Bean Validation + `@RestControllerAdvice` together (see `SPRING_ANNOTATIONS.md` for each annotation individually):

1. Client `POST`s a `TransferRequest` JSON body with `amount: -5`
2. `@RequestBody` deserializes it, `@Valid` (`AccountController.java:58`) triggers validation against `@DecimalMin("0.01")` on `TransferRequest.amount` (`TransferRequest.java:12`)
3. Validation fails → Spring throws `MethodArgumentNotValidException` *before* `AccountController.transfer()`'s body ever executes
4. `GlobalExceptionHandler.handleValidation()` (`GlobalExceptionHandler.java:51-58`) catches it, returns `400` with a field-level error list

Same shape for business-rule exceptions (`InsufficientFundsException`, `SelfTransferException`, etc.) — thrown from deep inside `AccountService`, caught centrally, never a try/catch in the controller.
