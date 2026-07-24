# OCBC Technical Interview — Consolidated Prep Guide
**Role:** JR00009212, Full Stack Developer (Java, ReactJS) — **OCBC Group Wealth**
**Date:** 3 Aug 2026 (Monday), 3:00pm SGT / 2:00pm WIB
**Interviewers:** Nilaksha Perera & Saminda Pathirana
**Format:** Microsoft Teams, technical round

---

## At a Glance

| | |
|---|---|
| **Team** | OCBC Group Wealth — front-office & order-management platforms for wealth/trading products |
| **Domain** | Wealth management, capital markets — OTC, Bonds, Cash Equities, Funds, IPO, Loans, Deposits |
| **Your edge** | Go/Spring conceptual translation + CIMB Niaga BI-FAST as your regulated-fintech credibility |
| **Your gap** | No wealth/trading domain exposure, limited hands-on Java, no Docker/K8s/Liquibase in original prep |
| **Open item** | Kevin asked Lam Yan Kay 4 logistics questions (permanent/contract, WFH, location, team) on 15 Jul. Looped to Lee Kin Kit on 16 Jul — **no reply yet as of prep time.** Worth a polite nudge before 3 Aug. |

---

## Table of Contents
1. What To Expect
2. Your Positioning: Go → Java Translation
3. Honest Framing: Your Java Experience
4. Spring & Spring Boot Fundamentals
5. Design Patterns
6. Redis
7. SQL vs NoSQL
8. Microservices
9. Docker & Kubernetes Primer
10. Liquibase Primer
11. Wealth & Trading Domain Primer
12. Java Language Refresher
13. ReactJS Refresher
14. System Design — Wealth Platform Edition
15. Self-Introduction (Updated)
16. Questions to Ask Them
17. Day Of Checklist

---

## 1. What To Expect

Based on real candidate reports for OCBC engineering interviews:

1. Self introduction and walkthrough of past projects, tech stack
2. Deep dive into one or two projects from your CV
3. Core Java and Spring/Spring Boot concept questions
4. Database and caching questions (SQL vs NoSQL, Redis)
5. Microservices experience and design questions
6. Closing discussion of team structure and role expectations

Concept-level, not a live coding test at this stage — but be ready to talk through code or whiteboard if asked. Given the actual JD, also be ready for questions that probe your **hands-on Java depth** and any **wealth/trading domain familiarity**, since both are explicitly listed as must-have / preferred.

---

## 2. Your Positioning: Go → Java Translation

You're translating vocabulary, not learning concepts from scratch.

| Concept | Go (what you know) | Java / Spring (how to say it) |
|---|---|---|
| Dependency management | Manual wiring, constructor params, Wire/fx | Dependency Injection via Spring's IoC container |
| Interfaces | Implicit satisfaction | Explicit `implements`, paired with `@Service`/`@Repository` |
| Concurrency | Goroutines and channels | Threads, `ExecutorService`, or Project Reactor |
| Error handling | Explicit `error` returns | Checked/unchecked exceptions, try/catch |
| HTTP routing | gin, echo, net/http | Spring MVC, `@RestController` |
| JSON tags | `json:"field_name"` | Jackson `@JsonProperty` |
| Config | Viper, env vars | `application.yml`, `@Value`, `@ConfigurationProperties` |
| ORM | GORM, sqlx | JPA / Hibernate, Spring Data JPA |
| DB migrations | golang-migrate / goose (if you've used them) | Liquibase — see Section 10 |
| Containerizing services | Docker (if you already containerize your Go services) | Same Docker, same concepts — see Section 9 |

When asked "tell me about your experience": primarily Golang and React Native, with the same architectural instincts (separation of concerns, DI-style patterns, microservices) applying directly to Java and Spring Boot.

```go
// Go — dependencies passed explicitly into a constructor function
func NewAccountService(repo AccountRepository, notifier Notifier) *AccountService {
    return &AccountService{repo: repo, notifier: notifier}
}
```
```java
// Java/Spring — the same instinct, just handed to you by a container instead of called by hand
@Service
public class AccountService {
    public AccountService(AccountRepository repo, NotificationService notifier) { ... }
    // Spring calls this constructor for you at startup — you never call `new AccountService(...)`
}
```

---

## 3. Honest Framing: Your Java Experience

The JD lists *"10+ years of hands-on software engineering experience in enterprise Java/J2EE applications"* as a **must-have**. You've told me your actual background is coursework, side projects, or brief professional use — not primary daily-driver experience. Here's how to frame that honestly and still land well, rather than getting caught improvising in the room.

**Don't do this:** imply years of production Java you don't have, or dodge the question when asked directly. If they ask "how many years have you written Java professionally," a vague answer reads worse than a direct one.

**Do this — a three-part structure:**

1. **Name it plainly, without over-apologizing.** *"My hands-on Java has been through [coursework / personal projects / a brief professional stint — fill in your actual specifics here], so I won't pretend it's my primary language. My production depth is in Go."*
2. **Immediately pivot to why that's lower-risk than it sounds.** *"The reason I'm comfortable with this role despite that is Java and Go solve the same problems — dependency injection, interfaces, concurrency, REST — with different syntax. The learning curve for me is vocabulary and ecosystem conventions, not the underlying engineering judgment."*
3. **Give one concrete anchor.** Fill in a real, specific example: a personal project where you used Spring Boot, a course where you built something in Java, or a task at a previous job where you touched a Java codebase even briefly. Specificity here matters far more than the three-part structure — one real detail beats a well-rehearsed generality.

**Fill in before the interview** (don't leave this blank — write your actual answer now, not live in the room):
- What was the Java thing? _______________________
- Roughly when / how long? _______________________
- What did you build or touch? _______________________

Rehearse saying this out loud once. The goal isn't to sound like you have 10 years of Java — it's to sound like someone who knows exactly what they have and isn't rattled by the gap.

---

## 4. Spring & Spring Boot Fundamentals

### What Spring solves
Inversion of Control (IoC) and Dependency Injection (DI). Instead of a class creating its own dependencies, Spring's container creates and injects them — the same instinct as passing dependencies into a Go constructor function instead of hardcoding them, just formalized with a container and annotations.

### Spring vs Spring Boot
Spring = underlying framework (IoC container, MVC, data access, security). Spring Boot = opinionated layer on top: auto-configuration, embedded servlet container (Tomcat), minimal XML/manual config. Expected interview answer: Spring Boot gets a production-ready Spring app running fast, via starters and auto-configuration.

### Autowiring — two types (has come up directly in past OCBC interviews)
1. **Field injection**: `@Autowired` on a class field. Quick, but hides dependencies, prevents `final` fields, harder to test. Considered poor practice.
2. **Constructor injection**: `@Autowired` on the constructor (or inferred automatically for a single constructor in modern Spring). **Recommended** — explicit dependencies, supports `final` immutability, trivial to unit test with mocks.

Good framing: *"I'd default to constructor injection since it makes dependencies explicit and testable — similar to how I always pass dependencies explicitly into constructors in Go rather than relying on globals."*

```java
// constructor injection — matches bank-demo's real AccountService.java
@Service
public class AccountService {
    private final AccountRepository repository; // final: can't be reassigned, thread-safe by construction
    public AccountService(AccountRepository repository) { this.repository = repository; }
}
```

### Spring Batch (new — not in your original doc, and it's a named requirement)
Spring's framework for **batch processing**: reading large volumes of data, processing/transforming it, writing it out — with built-in chunking, retry, skip logic, and job restart. Think: end-of-day reconciliation jobs, overnight settlement processing, bulk pricing updates — exactly the kind of job a wealth/trading backend runs regularly. If you've written any Go batch/cron job that processes records in chunks with retry logic, that's the same shape of problem.

---

## 5. Design Patterns to Have Ready

This is a **confirmed real OCBC question** — "What is the design pattern and how do you use it in Spring Boot?" (Master Checklist, Part 0, #2). Lead with the Proxy/AOP answer below since it's concrete and you can point at real code for it; then name 2–3 more by pattern name plus a one-line Spring example, to show breadth without rambling.

### Go-to answer: Proxy pattern (AOP)

Spring's answer to "add behavior around a method without touching its code." `@Transactional`, `@Cacheable`, `@Async`, `@PreAuthorize` — all the same mechanism: Spring wraps your bean in a dynamic proxy at startup. Two flavors:
- **JDK dynamic proxy** — bean implements an interface → proxy implements the same interface, delegates to the real object.
- **CGLIB proxy** — no interface → proxy subclasses your concrete class.

Call a `@Transactional` method → hits the proxy first → proxy opens a transaction → delegates to the real method → commits (or rolls back on an unchecked exception) → returns.

**Classic gotcha, worth knowing cold**: self-invocation bypasses the proxy. Call a `@Transactional` method from another method *in the same class* and you're calling `this.method()` directly, never through the proxy — the transaction silently doesn't apply. In `bank-demo`, `AccountService.java` has `@Transactional` on `deposit()`, `withdraw()`, `transfer()`, and the idempotency helper (lines 51, 68, 79, 107) — each only works because they're invoked from `AccountController`, *outside* the class, through the real proxy.

### Rest, by name

- **Singleton** — Spring bean default scope. One instance per container (not JVM-wide, per-`ApplicationContext`). `bank-demo`'s `ConsoleNotificationService`/`SmsNotificationService` get this free from `@Component` — no hand-written `private constructor + static getInstance()`.
- **Factory** — three places: `@Bean` methods in `@Configuration` classes (each one's a Factory Method); `ApplicationContext`/`BeanFactory` itself (you ask for a bean, don't know or care how it's built); `FactoryBean<T>` (a bean whose job is producing *other* beans).
- **Template Method** — `JdbcTemplate`, `RestTemplate`, `TransactionTemplate`. Fixed skeleton (open connection, handle exceptions, close connection) lives in the template; you supply only the variable part (a `RowMapper`, a callback). "Template" in the class name isn't decoration — it's literally the pattern.
- **Strategy** — interface, swappable implementations, picked/injected at runtime. `bank-demo`'s `NotificationService` — `AccountService`'s constructor takes `List<NotificationService>`, Spring auto-collects every implementing bean (`ConsoleNotificationService` + `SmsNotificationService`), and deposit/withdraw/transfer fan out to all of them without knowing which channels exist. Mention if you've swapped implementations behind an interface in Go, too.
- **Repository** — Spring Data JPA. `AccountRepository extends JpaRepository<Account, Long>` — you write an interface, zero implementation, Spring Data generates it at runtime. (Also secretly another Proxy pattern instance — the generated repository implementation *is* a dynamic proxy.) You already do the rough equivalent with GORM or sqlx.
- **Observer** — `ApplicationEventPublisher` + `@EventListener`/`ApplicationListener`. Publish an event, any number of decoupled listeners react — the publisher never knows who's listening.
- **Builder** — fluent object construction. `HttpSecurity`'s chained `.authorizeHttpRequests(...).csrf(...)`, `WebClient.builder()`, `ResponseEntity.ok().body(x)`. Comparable to the functional options pattern in Go.
- **Front Controller** — `DispatcherServlet`. Single entry point for every HTTP request, routes to the right handler. This is Spring MVC's architecture-level pattern, one level up from any single annotation.
- **Adapter** — `HandlerAdapter`. Lets `DispatcherServlet` invoke wildly different handler types (`@Controller` methods, the old-style `Controller` interface) through one uniform interface.

---

## 6. Redis

- **What it is**: in-memory key-value store — cache, session store, pub/sub broker, lightweight queue.
- **Core structures**: strings, hashes, lists, sets, sorted sets.
- **When to use**: reducing load on the primary DB for frequently-read/rarely-changed data, session storage in stateless services, rate limiting, leaderboard-style sorted-set use cases.
- **Your anchor**: cite concrete Redis use cases from your Laku6 or fintech work if applicable (caching, session management).
- **Tradeoffs**: volatile unless persistence configured (RDB/AOF); not a substitute for a relational DB when you need strong consistency or complex queries.
- **Wealth-domain angle**: real-time or near-real-time pricing/quote caching is a classic Redis use case in trading platforms — worth mentioning if asked how you'd cache frequently-changing market data.

```java
// cache-aside — the pattern you'd actually reach for over the raw Redis structures
BigDecimal getQuote(String symbol) {
    BigDecimal cached = redis.opsForValue().get("quote:" + symbol);
    if (cached != null) return cached;                     // fast path — Redis hit
    BigDecimal fresh = marketDataClient.fetchQuote(symbol); // slow path — DB/upstream miss
    redis.opsForValue().set("quote:" + symbol, fresh, Duration.ofSeconds(2)); // short TTL — prices move fast
    return fresh;
}
```

---

## 7. SQL vs NoSQL

| | SQL (relational) | NoSQL |
|---|---|---|
| Structure | Fixed schema, tables, rows | Flexible schema |
| Consistency | Strong, ACID | Often eventual, BASE |
| Best for | Complex relationships, transactions | High write throughput, flexible/evolving data |
| Examples | PostgreSQL, MySQL | MongoDB, Cassandra, DynamoDB, Redis |

```sql
-- SQL: an order's lifecycle demands ACID — a transfer either fully happens or fully doesn't
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```
```json
// NoSQL: a market-data snapshot — schema can vary document to document, no migration needed to add a field
{ "symbol": "AAPL", "price": 189.32, "timestamp": "2026-07-22T14:00:00Z", "source": "NYSE" }
```

**For this role specifically**: order books, trade blotters, and position-keeping (who owns what, at what cost basis) are exactly the kind of state that demands ACID guarantees — justify SQL as default for anything touching an order's lifecycle or account/position state. NoSQL fits supporting data: audit logs, market data snapshots, high-volume event streams. This is a good moment to mention your BI-FAST integration at CIMB Niaga — same underlying discipline (financial consistency requirements), different product surface (payments vs. wealth/trading).

---

## 8. Microservices

Be ready to walk through one real example end to end: what the service did, why it was split out, how it communicated (REST, gRPC, Kafka), how you handled failure cases.

**Talking points from your background:**
- Migrating pricing logic from Python/Jupyter to a standalone Go service at Laku6 — a concrete example of turning a fragile manual process into a proper engineering solution.
  - **Verified alternative (git-checked 22 Jul 2026 — pick one, don't blend them)**: real commit history in `lk6-sales-services` shows a teammate built the original Go pricing service (Oct 2025); your own verified contribution (Jun 2026) was building a `PricingSyncRun` observability layer on top of it and catching a real margin-config bug (cap/floor logic backwards) that had silently mispriced 462 non-smartphone SKUs, while validating the service's output against the legacy Jupyter baseline it's meant to replace. Full STAR version in `Behavioral_Interview_Prep.md`, "Taking initiative" section. More specific and fully defensible under follow-up questions; the tradeoff is it's "hardened and fixed" rather than "migrated."
- Kafka experience — useful for event-driven communication between services (also a named requirement in the JD: "messaging/event-driven systems (MQ, Kafka)").
- BigQuery/analytics pipeline work — useful if asked about data flow between services and a warehouse.

**Common angles**: service discovery, API gateway, partial-failure handling (circuit breaker), cross-service consistency (saga pattern, eventual consistency), API versioning.

---

## 9. Docker & Kubernetes Primer

*Not in your original prep doc — but named directly in the JD ("containerisation/orchestration technologies (Docker, Kubernetes)").* If you already containerize your Go/Node services for deployment, most of this will feel familiar; the vocabulary below is what to have ready either way.

### Docker — the basics
- **Image**: a read-only, layered snapshot of an application + its dependencies (like a static Go binary, but it bundles the whole OS-level environment, not just the binary — which is *why* Java needs it more than Go does. A Go binary is already self-contained; a JVM app needs its runtime bundled too).
- **Container**: a running instance of an image — isolated process, own filesystem view, own network namespace.
- **Dockerfile**: instructions to build an image (base image → copy code → install deps → set entrypoint).
- **Layers**: each Dockerfile instruction adds a cached layer — this is why Docker builds are fast on unchanged layers.

```dockerfile
# bank-demo's real Dockerfile — multi-stage, so the JDK/Maven build tooling
# never ships in the final image, only the JRE + the built jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/bank-demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes — the basics
- **Pod**: the smallest deployable unit — one or more tightly-coupled containers sharing network/storage.
- **Deployment**: manages a set of replica Pods, handles rolling updates and rollbacks.
- **Service**: stable network endpoint that load-balances across a Deployment's Pods (Pods are ephemeral; Services aren't).
- **Ingress**: routes external HTTP(S) traffic into Services.
- **Liveness vs. readiness probes**: liveness = "is this container alive, restart it if not"; readiness = "is this container ready to receive traffic, remove it from the Service pool if not." A common interview question — know the distinction cold.
- **Horizontal Pod Autoscaler**: scales replica count based on CPU/memory or custom metrics.

**Why it matters for this role**: the JD explicitly describes a microservices architecture — Docker/K8s is almost certainly how those services are packaged and run in production, so expect at least a conceptual question ("how would you deploy this service," "what happens if a Pod crashes").

**A likely interview question and a solid answer shape**: *"How would you roll out a new version of a Spring Boot service with zero downtime?"* → Deployment's rolling update strategy: spin up new Pods with the new image, wait for readiness probes to pass, gradually shift traffic via the Service, terminate old Pods only once new ones are healthy. Same rolling-update instinct applies whether the container runs a JVM or a Go binary.

```yaml
# the readiness probe that makes the rolling-update answer above actually true —
# a new Pod gets zero traffic from the Service until this passes
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

---

## 10. Liquibase Primer

*Also not in your original doc — named explicitly in the JD's DevOps toolchain list.*

**What it is**: a database schema **change management** tool. Instead of manually running ALTER TABLE statements, you write versioned **changesets** (in XML, YAML, JSON, or SQL) describing each schema change. Liquibase tracks which changesets have already run (in a `DATABASECHANGELOG` table) and applies only the new ones, in order, on deploy.

**Core concepts:**
- **Changelog**: the master file listing all changesets in order.
- **Changeset**: one atomic, trackable change (e.g., "add column X to table Y").
- **Rollback**: each changeset can define how to undo itself.

**Your analogy, if it fits**: if you've used `golang-migrate` or `goose` in your Go stack, Liquibase solves the exact same problem — versioned, incremental, revertible schema changes tracked in code rather than applied by hand. The core idea (schema-as-code, migrations run in a known order, tracked in the DB itself) is identical; Liquibase is just the Java-ecosystem-standard tool for it, with a bit more built-in tooling (rollback generation, diff-based changelog generation, multiple DB support).

**Why it matters here**: order management and account-state schemas in a wealth platform change carefully and get audited — a formal, trackable migration tool (vs. ad hoc scripts) is exactly what you'd expect in a regulated banking context.

```yaml
# bank-demo's real changeset — db/changelog/changes/0001-initial-schema.yaml
databaseChangeLog:
  - changeSet:
      id: 0001-1-create-accounts
      author: bank-demo
      changes:
        - createTable:
            tableName: accounts
            columns:
              - column: { name: id, type: BIGINT, autoIncrement: true, constraints: { primaryKey: true } }
              - column: { name: balance, type: DECIMAL(19,2), constraints: { nullable: false } }
      rollback:
        - dropTable: { tableName: accounts }
```
```yaml
# application.yml — Hibernate only VALIDATES against this schema now, never mutates it
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

---

## 11. Wealth & Trading Domain Primer

*This is the domain gap flagged from the actual JD. You don't need deep expertise — you need enough to sound like you did your homework and can ask an intelligent question, not enough to bluff a trader.*

| Term | What it means |
|---|---|
| **OTC (Over-The-Counter)** | Trades negotiated directly between two parties, not through a centralized exchange. Common for bonds, derivatives, some FX. Contrast with exchange-traded instruments like listed equities. |
| **Bonds** | Debt instruments: the issuer borrows money from the investor, pays periodic interest (a coupon), and returns the principal at maturity. Platform-side, this means handling coupon schedules, accrued interest, and settlement dates. |
| **Cash Equities** | Straightforward buying/selling of shares (as opposed to derivatives). Typically settles T+1 or T+2 (one or two business days after the trade). |
| **Funds** | Pooled investment vehicles (mutual funds, unit trusts). Priced by **NAV** (Net Asset Value), usually once a day — not continuously traded. Workflow is subscription/redemption, not buy/sell order matching. |
| **IPO (Initial Public Offering)** | A private company issues shares to the public for the first time. Platform-side: handling subscription periods, allocation logic (often oversubscribed), and conversion from "application" to "holding." |
| **Loans & Deposits** | Not "trading" instruments, but wealth products offered alongside investments (e.g., structured deposits) — relevant because a wealth platform often needs to show a client's *full* position across trading and banking products. |
| **Order Management System (OMS)** | The system managing an order's lifecycle: creation → routing → execution → settlement. This is core to "order-management workflows" in the JD. Order states typically look like: New → Partially Filled → Filled / Cancelled / Rejected — a state machine, which is a very translatable concept from any backend work you've done with status-driven entities. |
| **Front-to-back integration** | Front office (client-facing dealing/advisory) → middle office (risk, compliance checks) → back office (settlement, accounting). A trade flows through this pipeline; the JD's "front-office and order-management workflows" phrase is pointing at the front-to-middle part of this chain. |

**If asked "what's your experience with capital markets/wealth products"** — be honest, then pivot: *"I haven't worked directly on trading or wealth products, but I have worked on systems with the same underlying demands — BI-FAST integration at CIMB Niaga required the same rigor around consistency and auditability that I'd expect an order management workflow to need. I'd expect the domain vocabulary to be the fastest part to pick up; the harder part — building for correctness under regulatory and financial-consistency constraints — is exactly what I've already done."*

---

## 12. Java Language Refresher

- **OOP fundamentals**: classes, interfaces, inheritance, polymorphism. Go uses composition and implicit interfaces; Java uses explicit inheritance and interfaces — be ready to explain both models.
- **Collections**: `List`, `Set`, `Map` and common implementations (`ArrayList`, `HashSet`, `HashMap`). Go's slices/maps are the rough equivalents.
- **Concurrency**: threads and `ExecutorService`, vs. Go's goroutines/channels. Java concurrency is heavier-weight per unit (OS threads) than goroutines; Java's newer virtual threads (Project Loom) close that gap — worth knowing this exists even if you don't go deep.
- **Exceptions**: checked (must declare/catch) vs. unchecked (`RuntimeException`). Go has no exceptions, only explicit error returns — be ready to explain why you'd choose one Java exception style over another (generally: unchecked for programming errors, checked sparingly for recoverable conditions).
- **Generics**: more verbose than Go's, conceptually similar — type safety without duplicating code.

```java
// interface + explicit implementation (Java) vs. implicit satisfaction (Go)
interface NotificationService { void notify(Account account, String message); }
class SmsNotificationService implements NotificationService {
    public void notify(Account account, String message) { /* ... */ }
}

// generics — same goal as Go 1.18+, more verbose syntax
<T extends Comparable<T>> T max(T a, T b) { return a.compareTo(b) >= 0 ? a : b; }

// checked vs. unchecked exception, vs. Go's explicit error return
void readFile(String path) throws IOException { ... }      // checked — caller must handle
void withdraw(BigDecimal amt) { if (amt.signum() < 0) throw new IllegalArgumentException(); } // unchecked
```

---

## 13. ReactJS Refresher

- **Hooks**: `useState`, `useEffect`, `useMemo`, `useCallback`, and when each applies. Know the dependency-array footgun in `useEffect`.
- **State management**: local state vs. lifting state up vs. Context API vs. external libraries (Redux, Zustand) — know when you'd reach for each. (Redux specifically is named in the JD.)
- **Component lifecycle**: hooks replaced class lifecycle methods (`componentDidMount`/`componentDidUpdate`/`componentWillUnmount` → `useEffect` variants).
- **Performance**: memoization (`React.memo`, `useMemo`), avoiding unnecessary re-renders, virtualization for long lists.
- **Your bridge**: React Native and ReactJS share the same core mental model (components, hooks, JSX) — the differences are rendering target and platform APIs. Easy, honest talking point.

```jsx
// bank-demo's real accountsSlice.js — Redux Toolkit, one slice, one async thunk
export const fetchAccounts = createAsyncThunk('accounts/fetch', async () => {
  const res = await fetch('/accounts')
  return res.json()
})
const accountsSlice = createSlice({
  name: 'accounts',
  initialState: { items: [], error: null },
  reducers: {},
  extraReducers: (builder) => {
    builder.addCase(fetchAccounts.fulfilled, (state, action) => { state.items = action.payload })
  },
})

// consumed in App.jsx via hooks — no class lifecycle methods anywhere
const accounts = useSelector((state) => state.accounts.items)
const dispatch = useDispatch()
useEffect(() => { dispatch(fetchAccounts()) }, [])
```

---

## 14. System Design — Wealth Platform Edition

Original banking considerations still apply, sharpened for this specific domain:

- **Consistency over availability** for anything touching money movement, balances, or order state (favor CP over AP in CAP terms).
- **Auditability**: every state change traceable — append-only ledgers or event sourcing are common patterns, and doubly important where regulators can ask "show me the history of this order."
- **Idempotency**: critical for payment and order-submission APIs — retries must not double-process a transaction or double-submit an order. Directly relevant to your BI-FAST integration experience.
- **Order state machine**: if asked to design part of an OMS — New → Partially Filled → Filled/Cancelled/Rejected, with clear rules about which transitions are valid. Same shape of problem as any status-driven workflow you've built.
- **Security**: encryption at rest/in transit, strict access control, PCI DSS-style compliance awareness if relevant.
- **Scalability**: read-heavy (price/quote lookups) vs. write-heavy (order submission) paths often need different scaling strategies — read replicas, caching (Redis) for the former, queue-based load leveling (Kafka) for the latter.

```java
// order state machine — New -> Partially Filled -> Filled/Cancelled/Rejected,
// every transition auditable (full version with the transitions map in Banking Playbook, Part 3)
enum OrderStatus { NEW, ROUTED, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED, EXPIRED }

void transition(Order order, OrderStatus next) {
    if (!isAllowed(order.status(), next)) throw new IllegalStateException("invalid transition");
    order.setStatus(next);
    eventPublisher.publish(new OrderStatusChanged(order.id(), order.status(), next)); // the audit trail
}
```

---

## 15. Self-Introduction (Updated — ~2 minutes)

1. **Current role**: Senior Software Engineer at Laku6/Carousell Group, primary stack Golang backend + React Native mobile.
2. **Notable project**: migrating pricing logic from Python/Jupyter to a Go service — turning something fragile and manual into a proper engineering solution.
   - **Verified alternative (git-checked 22 Jul 2026 — pick one)**: "I built the observability layer and validated a teammate's Go pricing service against its Jupyter baseline, catching a margin-config bug that had silently mispriced 462 SKUs." More specific and defensible, but a different claim than "I migrated it" — see §8 above and `Behavioral_Interview_Prep.md` for the full real story before deciding.
3. **Banking background, framed honestly**: prior experience at Bank CIMB Niaga, including BI-FAST integration. This is your strongest differentiator for a bank interview — but be precise that it's payments infrastructure, not wealth/trading. Frame it as: *"That gave me direct experience with the consistency, auditability, and idempotency demands of regulated financial systems — which I'd expect map closely to what an order-management platform needs, even though the specific product domain here is new to me."*
4. **Breadth**: comfortable across Python/Django, Next.js, TypeScript, increasingly building with AI-native tooling (Anthropic API, agentic workflows) — shows you stay current.
5. **Why this role**: full-stack scope (Java + ReactJS) matches your full-stack background even though Java specifically is new; the regulated-finance domain plays to your CIMB Niaga experience even though the product surface (wealth vs. payments) is a genuine gap you're upfront about closing.

---

## 16. Questions to Ask Them

**Technical/team questions:**
1. What does the current architecture look like — monolith, microservices, or a mix, and how far along is the modernization the JD mentions?
2. What's the split between building new wealth-product features vs. maintaining/supporting production trading workflows?
3. How does the team handle real-time or near-real-time market data / pricing feeds in the architecture?
4. What's the biggest technical challenge the team is working through right now?
5. What would success look like in the first six months in this role?

**Logistics — only if not answered before the interview:**
You already asked these on 15 Jul (permanent vs. contract, work arrangement, location/relocation, team structure/reporting line) and they were routed to Lee Kin Kit with no reply yet. If they're still unanswered by 3 Aug, it's fair to raise once, briefly, near the end: *"I'd sent a few questions about the role setup ahead of this call — happy to follow up separately if now isn't the moment, but wanted to flag it in case it's useful context for our conversation."* Don't lead with this; it's a wrap-up item, not an opener.

---

## 17. Day Of Checklist

- Join link: https://teams.microsoft.com/meet/48940049444283?p=g0a50RPQ0EOaAixlRJ
- Meeting ID: 489 400 494 442 83
- Passcode: F9YY9ha6
- Dial-in backup: +65 6263 9022,,670425137#
- Double-check Teams app/browser access before 2:00pm WIB
- Have your CV (Backend Golang or Full Stack variant) open for the "walk through your projects" portion
- Have your filled-in answer from Section 3 (Java experience) rehearsed, not improvised
- If anything changes, they've asked for one working day's notice
