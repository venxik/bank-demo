# Spring & Java Interview Q&A
For: OCBC Full Stack Developer Technical Interview

Format: question, then a tight, interview-ready answer, then a short code example so the concept isn't just words. Where useful, a Go analogy is included since that's your daily-driver language — use these to bridge naturally rather than reciting them verbatim.

---

## Core Java

**Q: What's the difference between `==` and `.equals()`?**
A: `==` compares references (memory addresses) for objects, or raw values for primitives. `.equals()` is a method that can be overridden to compare *logical* equality — two different `String` objects with identical characters are `==`-unequal but `.equals()`-equal. Analogy: comparing two Go struct pointers with `==` compares the pointers, not the dereferenced values field-by-field.

```java
String a = new String("hi");
String b = new String("hi");
a == b;          // false — different objects on the heap
a.equals(b);      // true  — same characters

String c = "hi";  // string-pool literal
String d = "hi";
c == d;           // true — both point at the same pooled instance
```

**Q: Checked vs. unchecked exceptions?**
A: Checked exceptions (extend `Exception`, not `RuntimeException`) must be declared in a `throws` clause or caught — the compiler enforces handling. Unchecked (`RuntimeException` and subclasses) don't need to be declared or caught. Modern convention: unchecked for programmer errors (null pointer, illegal argument), checked sparingly for conditions a caller can reasonably recover from. Go has no exceptions at all — the equivalent is explicit `error` returns, which arguably push the "handle every failure path" discipline even further than Java's checked exceptions do.

```java
// checked — caller MUST catch or declare, compiler enforces it
void readFile(String path) throws IOException { ... }

// unchecked — caller can ignore it, compiler says nothing
void withdraw(BigDecimal amount) {
    if (amount.signum() < 0) throw new IllegalArgumentException("amount must be positive");
}
```

**Q: What is the JVM, and what does garbage collection do?**
A: The JVM executes compiled Java bytecode, giving platform independence ("write once, run anywhere"). The garbage collector automatically reclaims memory for objects no longer reachable from any live reference. Go has its own GC with the same goal (automatic memory management) but different tuning tradeoffs (concurrent, low-latency-focused) — same problem, different implementation.

```java
Account account = new Account(...); // object allocated on the heap
account = null;                     // no live reference left to the old object
// GC is now free to reclaim it — you never called free() or delete
```

**Q: What are generics, and why do they matter?**
A: They let you write classes/methods that work with any type while keeping compile-time type safety (`List<String>` vs. a raw `List` that could silently hold anything) — catching type mismatches at compile time instead of a runtime `ClassCastException`. Conceptually the same as Go generics (Go 1.18+) — same goal, Java's syntax is more verbose (bounded wildcards, `<T extends X>`).

```java
List<String> names = new ArrayList<>();
names.add("Kevin");
// names.add(42);           // compile error — caught before it ever runs
String first = names.get(0); // no cast needed

<T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
}
```

**Q: `ArrayList` vs. `LinkedList`?**
A: `ArrayList` — backed by a resizable array: O(1) random access, O(n) insert/delete in the middle (has to shift elements). `LinkedList` — doubly-linked list: O(1) insert/delete at a known position, O(n) random access. Default to `ArrayList` unless you specifically need frequent insertion/removal at arbitrary positions.

```java
List<Account> accounts = new ArrayList<>();
accounts.get(0);           // O(1) — direct index into the backing array

List<Account> queue = new LinkedList<>();
((LinkedList<Account>) queue).addFirst(account); // O(1) — just relinks pointers
```

**Q: `HashMap` vs. `TreeMap` vs. `LinkedHashMap`?**
A: `HashMap` — no ordering guarantee, O(1) average lookup. `LinkedHashMap` — preserves insertion order, same performance plus a small linked-list overhead. `TreeMap` — sorted by key, O(log n) operations (red-black tree backed).

```java
Map<String, Account> byNumber = new HashMap<>();        // fastest, no order
Map<String, Account> insertionOrdered = new LinkedHashMap<>(); // remembers put() order
Map<String, Account> sorted = new TreeMap<>();           // always iterates key-sorted
```

---

## Concurrency

**Q: What is `ExecutorService`, and how does it relate to what you already know from Go?**
A: Java's abstraction over a thread pool — you submit tasks (`Runnable`/`Callable`) and it manages the underlying threads, instead of you manually creating `Thread` objects. It's Java's answer to "don't spawn unbounded threads" — the same instinct behind a worker-pool pattern in Go. The key difference: goroutines are far lighter-weight (a few KB of stack each) than OS threads, so Go code spawns concurrency much more liberally than pre-virtual-threads Java does.

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
Future<BigDecimal> result = pool.submit(() -> computeInterest(account));
BigDecimal interest = result.get(); // blocks until done
pool.shutdown();
```

```go
// the Go instinct this maps to — a bounded worker pool
jobs := make(chan Account, 100)
for i := 0; i < 4; i++ {
    go worker(jobs)
}
```

**Q: What are Java virtual threads (Project Loom), and why do they matter?**
A: Lightweight threads managed by the JVM rather than mapped 1:1 to OS threads — you can have millions of them, similar in spirit to goroutines. They exist specifically to close the gap between Java's traditional heavyweight-thread model and the lightweight-concurrency model Go already offers. Good to mention you're aware this exists, even without hands-on use yet.

```java
// Java 21+, one line swaps the whole threading model
ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
pool.submit(() -> callSlowDownstreamService()); // cheap enough to do this per-request
```

**Q: What's a race condition, and how do you prevent one in Java?**
A: When the outcome of concurrent operations depends on unpredictable timing/interleaving, producing incorrect results. Prevention: the `synchronized` keyword/blocks, `java.util.concurrent` locks (`ReentrantLock`), atomic classes (`AtomicInteger`, etc.), or — best of all — avoiding shared mutable state entirely via immutable objects or message-passing. Same philosophy as Go's "don't communicate by sharing memory, share memory by communicating."

```java
// unsafe — two threads can both read balance=100 before either writes
balance = balance.add(amount);

// fixed — only one thread executes this block at a time
synchronized (this) {
    balance = balance.add(amount);
}

// or, lock-free for simple counters
AtomicInteger requestCount = new AtomicInteger(0);
requestCount.incrementAndGet();
```

---

## Spring Core

**Q: What is Inversion of Control, and how does Spring implement it?**
A: Normally your code controls creating its own dependencies (`new SomeService()`). IoC inverts that — a container creates and manages objects ("beans") and hands them to whatever needs them. Spring implements this via its `ApplicationContext`, which reads configuration (annotations, in modern Spring) to know what beans to create and how to wire them together.

```java
// without IoC — you control construction, and you're stuck with this one implementation
AccountService service = new AccountService(new AccountRepository(), new EmailNotifier());

// with IoC — Spring builds the graph, you just declare what you need
@Service
public class AccountService {
    public AccountService(AccountRepository repo, NotificationService notifier) {
        // Spring supplies real instances at startup — you never call `new` here
    }
}
```

**Q: What are the Spring bean scopes?**
A: Most common: `singleton` (default — one instance per container, shared everywhere) and `prototype` (a new instance every time it's requested). Web-specific scopes also exist: `request` (one per HTTP request) and `session` (one per HTTP session).

```java
@Service                       // singleton by default — one instance, shared
public class AccountService { }

@Scope("prototype")            // a fresh instance every time it's injected/requested
@Component
public class ReportBuilder { }
```

**Q: `@Component` vs. `@Service` vs. `@Repository` vs. `@Controller` — what's the actual difference?**
A: All four are specializations of `@Component` and get picked up identically by component scanning — at the container level they're nearly the same. The difference is mostly semantic (signals intent), plus a couple of targeted behaviors: `@Repository` adds automatic translation of persistence exceptions into Spring's unified `DataAccessException` hierarchy; `@Controller` (with `@RestController` combining it with `@ResponseBody`) is what Spring MVC scans specifically for request-mapping methods.

```java
@Repository  // persistence layer — SQLException gets translated to DataAccessException
public interface AccountRepository extends JpaRepository<Account, Long> {}

@Service     // business logic layer
public class AccountService { }

@RestController // web layer — methods return JSON, not a view name
public class AccountController { }
```

**Q: What's the Spring bean lifecycle, briefly?**
A: Instantiate → populate properties (dependency injection) → call `@PostConstruct` (or `InitializingBean.afterPropertiesSet()`) → bean is ready for use → on container shutdown, call `@PreDestroy` (or `DisposableBean.destroy()`).

```java
@Service
public class CacheWarmer {
    @PostConstruct
    void warmUp() { /* runs once, right after DI finishes, before first real use */ }

    @PreDestroy
    void flush() { /* runs once, right before the container shuts this bean down */ }
}
```

**Q: Field injection vs. constructor injection — which do you prefer, and why?**
A: Constructor injection — dependencies are explicit in the constructor signature, supports `final` fields for immutability, and makes unit testing trivial (pass mocks straight into the constructor, no Spring context or reflection needed in tests). Field injection (`@Autowired` directly on a field) is more concise but hides dependencies and makes testing awkward. This mirrors how you'd always pass dependencies explicitly into a Go constructor function rather than relying on globals.

```java
// preferred — matches AccountService.java in bank-demo
@Service
public class AccountService {
    private final AccountRepository repository; // final — can't be reassigned
    public AccountService(AccountRepository repository) { // plain constructor, no @Autowired needed
        this.repository = repository;
    }
}

// avoid — antipattern/BrokenAccountLookupService.java in bank-demo exists to show why
@Service
public class BrokenAccountLookupService {
    @Autowired
    private AccountRepository repository; // can't be final, can't `new` this class in a plain unit test
}
```

---

## Spring Boot

**Q: What does Spring Boot actually add on top of Spring?**
A: Auto-configuration (sensible defaults inferred from what's on the classpath — add `spring-boot-starter-web` and it configures an embedded Tomcat + Spring MVC automatically), curated starter dependencies, and an embedded servlet container — no external server deployment needed, `java -jar` and it's running.

```xml
<!-- one starter in pom.xml... -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
```bash
# ...and this just works, no external Tomcat install, no WAR deploy
mvn spring-boot:run
```

**Q: What's Spring Boot Actuator?**
A: A set of production-ready endpoints exposed out of the box for monitoring and management — health checks, metrics, environment info, thread dumps. `/actuator/health` is exactly the endpoint you'd hit first when checking "is the system down" (see the System Design Playbook, Scenario 1).

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

**Q: What are Spring profiles, and why use them?**
A: A way to segment configuration by environment (`application-dev.yml`, `application-prod.yml`), activated via `spring.profiles.active`. Same problem Go solves with environment-specific config files or env-var-driven config (Viper, etc.) — different mechanism, same goal.

```yaml
# application-prod.yml — only loaded when the "prod" profile is active
spring:
  datasource:
    url: jdbc:postgresql://prod-host:5432/bank
```
```bash
java -jar app.jar --spring.profiles.active=prod
```

---

## Spring Data JPA

**Q: What's the N+1 query problem, and how do you avoid it?**
A: When you fetch a list of entities and then lazily load a related entity for each one inside a loop, you end up issuing 1 query for the list plus N more (one per related entity) instead of a single join. Avoid it with eager fetching where it makes sense, `JOIN FETCH` in a JPQL query, or `@EntityGraph` to specify what to fetch upfront.

```java
// 1 query for accounts, then N more — one per account.getOwner() call
List<Account> accounts = accountRepository.findAll();
for (Account a : accounts) {
    System.out.println(a.getOwner().getName()); // lazy load fires here, N times
}

// fixed — one query, joined upfront
@Query("SELECT a FROM Account a JOIN FETCH a.owner")
List<Account> findAllWithOwner();
```

**Q: Lazy vs. eager loading?**
A: Lazy — related entities are only fetched when accessed (default for `@OneToMany`/`@ManyToMany`). Eager — fetched immediately with the parent (default for `@ManyToOne`/`@OneToOne`). Lazy avoids over-fetching but risks a `LazyInitializationException` if accessed outside an active persistence context (e.g., after the transaction has closed) — a very common real-world Spring bug worth knowing by name.

```java
@OneToMany(fetch = FetchType.LAZY)   // default for collections — fetched on first access
private List<Transaction> transactions;

@ManyToOne(fetch = FetchType.EAGER)  // default for single references — fetched immediately
private Account owner;
```

---

## Transactions

**Q: What does `@Transactional` actually do?**
A: Wraps the annotated method in a database transaction. Spring achieves this with an AOP proxy around your bean that starts a transaction before the method runs and commits (or rolls back, by default on an unchecked exception) after. **Classic gotcha**: it only works when the method is invoked from *outside* the class through the Spring proxy — calling a `@Transactional` method from another method in the same class (self-invocation) bypasses the proxy entirely, and the transaction silently won't apply. This is a favorite Spring interview trap.

```java
@Transactional
public TransferResponse transfer(Long fromId, TransferRequest request) {
    from.debit(request.amount());
    to.credit(request.amount());
    // if anything below throws an unchecked exception, both lines above roll back
    return buildResponse(from, to);
}

// the self-invocation trap:
public void outer() {
    this.transfer(...); // "this." — bypasses the proxy, @Transactional silently ignored
}
```

**Q: What's transaction propagation? Name one or two common values.**
A: Controls how a transactional method behaves when called from within an existing transaction. `REQUIRED` (default) — join the existing transaction, or start a new one if none exists. `REQUIRES_NEW` — always start a new, independent transaction, suspending the current one (useful when a sub-operation, like writing an audit log entry, must commit or roll back independently of the caller).

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void writeAuditLog(String event) {
    // commits independently — survives even if the calling transaction later rolls back
}
```

**Q: What are transaction isolation levels, at a high level?**
A: Control what one transaction can see of another's uncommitted or concurrent changes, from weakest to strongest: `READ_UNCOMMITTED` (allows dirty reads) → `READ_COMMITTED` → `REPEATABLE_READ` → `SERIALIZABLE` (fully isolated, most expensive). Most databases default to `READ_COMMITTED`. Directly relevant to the race-condition scenario in the System Design Playbook.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void closeOfDayReconciliation() {
    // strongest isolation — nobody else's concurrent change can be seen mid-transaction
}
```

---

## REST / Spring MVC

**Q: How do you handle exceptions globally in a Spring REST API?**
A: `@ControllerAdvice` (or `@RestControllerAdvice`) combined with `@ExceptionHandler` methods — a centralized place to catch exceptions thrown anywhere in your controllers and translate them into consistent error responses, instead of scattering try/catch across every controller method.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(AccountNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }
}
```

**Q: How would you validate incoming request data?**
A: Bean Validation annotations (`@NotNull`, `@Size`, `@Email`, etc.) on DTO fields, combined with `@Valid` on the controller method parameter. Spring validates automatically and throws `MethodArgumentNotValidException` on failure — typically caught in a `@ControllerAdvice` to return a clean, structured 400 response.

```java
public record TransferRequest(
    @NotNull Long toAccountId,
    @DecimalMin("0.01") BigDecimal amount
) {}

@PostMapping("/{id}/transfer")
public TransferResponse transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
    // invalid amount never reaches this line — Spring throws before the method body runs
}
```

---

## Testing

**Q: Unit test vs. integration test in a Spring context — what's the practical difference?**
A: A unit test isolates a single class, mocking its dependencies (`@Mock`/`@InjectMocks` with Mockito) — fast, no Spring context loaded. An integration test (`@SpringBootTest`) loads some or all of the Spring context and tests how components work together, often against a real or test-container database — slower, but catches wiring/configuration issues a unit test can't.

```java
// unit test — no Spring context, milliseconds
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock AccountRepository repository;
    @InjectMocks AccountService service;
}

// integration test — real Spring context + real (in-memory) DB, matches
// AccountServiceTransferTest.java in bank-demo
@SpringBootTest
class AccountServiceTransferTest {
    @Autowired AccountService service;
}
```

---

## Microservices / Resilience

**Q: How would you implement a circuit breaker in a Spring Boot service?**
A: Resilience4j — the modern standard (replacing the now-deprecated Netflix Hystrix). Annotate a method with `@CircuitBreaker(name = "...")`, configure failure-rate thresholds and open-state wait duration, optionally supply a fallback method. Conceptually the same pattern you'd hand-roll or pull a library for in Go.

```java
@CircuitBreaker(name = "pricingService", fallbackMethod = "fallbackPrice")
public BigDecimal getPrice(String sku) {
    return pricingClient.fetchPrice(sku); // fails fast once the breaker trips
}

private BigDecimal fallbackPrice(String sku, Throwable t) {
    return lastKnownPrice(sku); // served instead, while the breaker is open
}
```

**Q: How does Spring Boot expose metrics for something like Prometheus/Grafana?**
A: Spring Boot Actuator + Micrometer. Micrometer is a vendor-neutral metrics facade (think SLF4J, but for metrics), and Actuator exposes a `/actuator/prometheus` endpoint a Prometheus server can scrape. This is exactly the toolchain that answers "how do you check if the system is down" from the System Design Playbook.

```bash
curl http://localhost:8080/actuator/prometheus | grep http_server_requests
# http_server_requests_seconds_count{method="GET",uri="/accounts",status="200"} 42.0
```

---

## Security Basics

**Q: Authentication vs. authorization — and where does Spring Security fit?**
A: Authentication = confirming *who you are* (login). Authorization = confirming *what you're allowed to do* once authenticated (permissions/roles). Spring Security handles both via a filter chain intercepting requests before they reach your controllers — commonly configured with JWT-based authentication for stateless REST APIs (no server-side session), validating the token on each request and populating a `SecurityContext` used downstream for authorization checks like `@PreAuthorize`.

```java
@PreAuthorize("hasRole('ADMIN')")     // authorization — checked after the token is already validated
@DeleteMapping("/accounts/{id}")
public void deleteAccount(@PathVariable Long id) { ... }

http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/accounts/**").authenticated()  // authentication — must present a valid token
    .anyRequest().permitAll());
```

---

## Quick-Fire Round
Short, direct answers you should be able to give in under 15 seconds each:

- **What's the default HTTP method Spring Boot Actuator's `/health` responds to?** GET.
- **What annotation turns a class into a REST controller?** `@RestController` (combines `@Controller` + `@ResponseBody`).
- **What's `@Value` used for?** Injecting a single configuration property from `application.yml`/`.properties` into a field — e.g. `@Value("${server.port}") private int port;`.
- **What's the difference between `@RequestParam` and `@PathVariable`?** `@RequestParam` reads a query string parameter (`?id=5` → `@RequestParam Long id`); `@PathVariable` reads a segment of the URL path (`/users/{id}` → `@PathVariable Long id`).
- **What does `@ConditionalOnProperty` do?** Only registers a bean/config if a specified property is set to a given value — e.g. `@ConditionalOnProperty("feature.new-pricing.enabled")` — core to how Spring Boot auto-configuration decides what to wire up.
- **What's the purpose of a DTO vs. an Entity?** Entities map to DB tables and carry persistence concerns (`@Entity class Account`); DTOs shape data specifically for API request/response payloads (`record AccountResponse(...)`), decoupling your API contract from your DB schema.
