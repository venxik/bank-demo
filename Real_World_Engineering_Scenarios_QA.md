# Real-World Engineering Scenarios & System Design Q&A
For: OCBC Full Stack Developer Technical Interview

This complements the Banking System Design Playbook — that doc is banking/wealth-specific scenarios; this one is the general "how would you actually build/fix/scale this" questions that come up in almost any full-stack technical round, researched from how these questions actually get asked in practice. Code/pseudocode added throughout so each pattern is something you can picture, not just describe.

---

## Table of Contents
- Part 1: Building a Scalable System
- Part 2: "The System/Page Breaks Suddenly" — Incident Response
- Part 3: Debugging a Performance Bottleneck (Step by Step)
- Part 4: Quick-Fire Additional Scenarios

---

## Part 1: Building a Scalable System

### "How would you design a system to handle 10x the current traffic?"
The structure interviewers want to see, in order:
1. **Clarify first**: what's the current bottleneck — reads, writes, or both? What's the actual growth timeline (10x over a year vs. 10x tomorrow changes the answer)? This alone signals seniority — jumping straight to a solution without scoping the problem is a common junior tell.
2. **Stateless services + load balancer**: if your application servers don't hold session state locally, you can add more of them behind a load balancer and scale horizontally almost for free. This is why externalizing session state (e.g., to Redis) matters — it's what makes horizontal scaling possible in the first place.
3. **Scale the database** (usually the real bottleneck): indexing and query optimization first — cheapest fix, do this before anything structural. Then read replicas if you're read-heavy. Then caching (Redis) in front of the DB for hot, frequently-read data. Sharding/partitioning is the last resort — it solves scale but adds real complexity (cross-shard queries, rebalancing), so justify it, don't default to it.
4. **CDN for static assets** — offload anything that doesn't change per-request (images, JS bundles, CSS) to the edge.
5. **Move non-critical-path work off the request thread**: anything that doesn't need to happen synchronously for the user to get their response (sending a notification, updating an analytics count) goes onto a queue and gets processed asynchronously.
6. **Monitor and iterate**: scalability isn't a one-time design decision, it's an ongoing practice — instrument the system so you can see the *next* bottleneck coming before it becomes an outage.

```java
// #2 — stateless: session lives in Redis, not in this instance's memory,
// so any instance behind the load balancer can serve any request
@Service
class SessionService {
    private final RedisTemplate<String, Session> redis;
    Session get(String sessionId) { return redis.opsForValue().get(sessionId); }
}

// #5 — move work off the request thread onto a queue
@PostMapping("/orders")
ResponseEntity<Order> createOrder(@RequestBody OrderRequest req) {
    Order order = orderService.create(req);
    eventPublisher.publish(new OrderCreated(order.id())); // notification/analytics happen async
    return ResponseEntity.ok(order); // response returns immediately, doesn't wait on either
}
```

### "How do you scale a database that's become a bottleneck?"
In rough order of "do this first, it's cheap" to "do this last, it's expensive":
1. **Indexing** — the single highest-leverage fix for slow queries; missing indexes on frequently filtered/joined columns are the most common real-world cause of a "slow database."
2. **Query optimization** — check execution plans (`EXPLAIN`), eliminate unnecessary joins, avoid `SELECT *`.
3. **Connection pooling** — make sure you're not exhausting DB connections under load; a misconfigured pool is a very common outage cause.
4. **Read replicas** — for read-heavy workloads, route reads to replicas and keep writes on the primary.
5. **Caching** (Redis/similar) in front of the DB for hot data — trades a bit of staleness for a lot of load reduction.
6. **Denormalization** — duplicate some data to avoid expensive joins at read time, when normalized structure is causing real performance pain.
7. **Sharding/partitioning** — split data across multiple database instances by some key (e.g., customer ID) — solves scale, but adds meaningful complexity (cross-shard queries, rebalancing when a shard gets too big). Bring this up as an option, but be clear it's not step one.

```sql
-- #1 — the fix that's usually the actual answer
EXPLAIN SELECT * FROM accounts WHERE account_number = 'ACC-001';
-- "Seq Scan on accounts" in the output means: no index, full table scan every call
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
-- re-run EXPLAIN: "Index Scan using idx_accounts_account_number" — O(log n) instead of O(n)
```

### "How would you design a notification system?"
- Event-driven: something happens (a trade fills, a large transfer completes) → an event is published (Kafka or similar) → a notification service consumes it and decides who to notify and how.
- Support multiple channels (push, SMS, email) — usually via a fan-out step, each channel handled by its own worker/service.
- **Idempotency matters here too** — if the consumer processes the same event twice, the user shouldn't get the same notification twice.
- Retry with backoff for delivery failures, but with a cap — don't retry a push notification forever if the device is offline.
- Respect user preferences and rate limits — notification systems that don't throttle themselves train users to ignore (or disable) them entirely.

```java
// same Strategy-pattern fan-out bank-demo already implements —
// NotificationService.java + ConsoleNotificationService/SmsNotificationService
for (NotificationService channel : notificationChannels) {
    channel.notify(account, message); // every registered channel fires, caller doesn't know which exist
}
```

### "How would you design a rate limiter?"
- **Token bucket**: a bucket refills with tokens at a fixed rate; each request consumes a token; if the bucket's empty, the request is rejected/delayed. Allows short bursts up to the bucket size.
- **Leaky bucket**: requests queue and are processed at a fixed output rate — smooths bursts rather than allowing them.
- **Sliding window**: count requests in a rolling time window rather than a fixed one, avoiding the edge-case where a fixed window resets and allows a burst right at the boundary.
- Where to implement: at the API gateway/edge for coarse per-client limits, or in-app for finer-grained business-logic limits (e.g., "max 3 large transfers per hour").
- For a distributed system, the rate-limit counter itself needs to live somewhere shared — Redis is the standard choice, since it's fast and supports atomic increment operations.

```java
// token bucket, simplified — Redis makes the increment atomic across instances
boolean allowRequest(String clientId, int maxTokens, Duration refillPeriod) {
    String key = "rate:" + clientId;
    Long count = redis.opsForValue().increment(key);
    if (count == 1) {
        redis.expire(key, refillPeriod); // first request in this window starts the clock
    }
    return count <= maxTokens; // over the limit once count exceeds maxTokens for this window
}
```

### Core building blocks worth having crisp one-line definitions for:
- **Vertical scaling**: bigger machine (more CPU/RAM). Simple, but has a ceiling and a single point of failure.
- **Horizontal scaling**: more machines. No hard ceiling, but requires your application to be designed for it (stateless, or state externalized).
- **Load balancing strategies**: round robin (simple, even distribution), least connections (routes to the least-busy server), consistent hashing (routes the same key to the same server consistently — useful when you want cache locality or session affinity).
- **Cache invalidation strategies**: cache-aside (app checks cache, falls back to DB on miss and populates cache), write-through (writes go to cache and DB together, staying in sync), write-behind (writes go to cache first, DB is updated asynchronously — faster writes, some risk if the cache fails before the DB write happens).

```java
// cache-aside — the most common pattern in practice
Account getAccount(Long id) {
    Account cached = redis.opsForValue().get("account:" + id);
    if (cached != null) return cached;                 // hit
    Account fromDb = accountRepository.findById(id).orElseThrow(); // miss — fall back to DB
    redis.opsForValue().set("account:" + id, fromDb, Duration.ofMinutes(5));
    return fromDb;
}
```

---

## Part 2: "The System/Page Breaks Suddenly" — Incident Response

### General incident response shape (backend or full-stack)
The structure that reads as "this person has actually been on call," in order:
1. **Acknowledge and declare**: confirm the alert is real, and if it's significant, formally declare an incident (severity level, a dedicated channel/war room) so effort doesn't get duplicated and stakeholders know who owns it.
2. **Assess impact fast, using dashboards** — how many users/requests affected, is it growing or stable, is it one region/service or everything.
3. **Look for what changed recently** — a recent deploy or config change is the most common root cause; correlate the incident start time against your deploy history first.
4. **Mitigate before you fully understand root cause** — a rollback that restores service in minutes is almost always the right first move, even if you don't yet know exactly *why* the deploy broke things. Understanding "why" can happen after service is restored.
5. **Communicate on a fixed cadence** — regular short updates (e.g., every 10–15 minutes) to stakeholders, even if the update is just "still investigating, next update at X." Silence is worse than an unfinished update.
6. **Blameless postmortem afterward** — what happened, what you did, what you'll change (better pre-deploy testing, an added monitor, a safer deployment pattern) so the same failure mode is caught earlier next time.

```bash
# step 4 in real terms — rollback first, root-cause after service is restored
kubectl rollout undo deployment/bank-demo   # or: git revert + redeploy the last known-good image
```

### "What if a page suddenly shows a blank screen or crashes for users?" (frontend-specific)
1. **Contain the blast radius first**: React **error boundaries** (`componentDidCatch` / `static getDerivedStateFromError` — still one of the few remaining reasons to write a class component) catch JavaScript errors in a component subtree and render a fallback UI instead of letting one broken component take down the entire page. Wrapping major page sections in their own boundaries means one widget failing doesn't blank-screen the whole app.
2. **Check the browser console and Network tab first** — most "blank screen" bugs surface as a clear JS error in the console (a common one: trying to render before an async value has loaded, or a malformed API response the component didn't expect) or a failed critical asset/API request in the Network tab.
3. **Log to an error-tracking service in production** (Sentry or similar) with source maps enabled, so a minified production stack trace still points to readable source lines — without this, debugging a report of "it broke for a user" from just a minified stack trace is painful.
4. **Reproduce deliberately**: was there a recent deploy? Is it browser-specific? Is it tied to a particular data shape (e.g., a null field the UI didn't expect) rather than every user? Narrowing this down fast is most of the actual debugging work.
5. **Design for graceful degradation on the data-fetching path too**, not just render errors: loading skeletons instead of blank space, retries with backoff for transient failures, and a clear inline error state with a recovery action (e.g., "retry") rather than a dead end.

```jsx
// #1 — an error boundary around one section, not the whole app
class AccountsSectionBoundary extends React.Component {
  state = { hasError: false }
  static getDerivedStateFromError() { return { hasError: true } }
  componentDidCatch(error, info) { logToSentry(error, info) }
  render() {
    if (this.state.hasError) return <p>Accounts unavailable right now. <button onClick={() => this.setState({ hasError: false })}>Retry</button></p>
    return this.props.children
  }
}
// usage: <AccountsSectionBoundary><AccountList /></AccountsSectionBoundary>
// AccountList crashing no longer blanks the whole page — just that section
```

---

## Part 3: Debugging a Performance Bottleneck (Step by Step)

This is a methodology question as much as a tools question — interviewers are checking whether you have a repeatable process, not just a list of tool names.

1. **Establish a baseline first.** You can't say something is "slow" without knowing what "normal" looks like — response time percentiles (p50/p95/p99, not just averages, since averages hide the worst experiences), throughput, error rate.
2. **Isolate where in the stack the time is actually going** before diagnosing anything: frontend rendering, network/API call, or database? Distributed tracing (or, more simply, timing each hop) tells you *which* service in a multi-service call chain is actually slow — don't assume it's the one that happens to be throwing an error.
3. **Backend-specific diagnosis**:
   - **Profilers** (JVisualVM, YourKit, JProfiler for Java) to see where CPU time is actually spent inside the application.
   - **Heap dumps** for memory-related slowness (excessive GC pauses from memory pressure).
   - **Thread dumps** for concurrency issues — deadlocks, threads stuck waiting on a lock or a slow downstream call.
   - **Slow query logs / `EXPLAIN` plans** for database-side bottlenecks — almost always the first place to look for a "the API got slow" report.
4. **Frontend-specific diagnosis**:
   - **Core Web Vitals** — LCP (how fast the main content appears), INP (responsiveness to interaction), CLS (visual stability) — the current standard metrics for "does this feel fast."
   - **Chrome DevTools Performance tab and Network waterfall** — shows exactly what's blocking the page: a large blocking script, a slow API call, an unoptimized image.
   - **Bundle size analysis** — an oversized JS bundle delays interactivity even if the server responded instantly.
   - **React Profiler** — for "the page loaded fine but feels laggy while using it," this usually points to unnecessary re-renders rather than a network issue.
5. **Reproduce the issue in a controlled environment** if possible, rather than debugging blind against production — confirms your hypothesis and lets you test a fix safely.
6. **Fix the highest-leverage cause first**, not every possible inefficiency — the common real-world culprits, roughly in order of how often they're the actual answer:
   - N+1 queries (see the Spring Q&A doc)
   - Missing database indexes
   - Unbounded result sets (fetching far more rows than needed, without pagination)
   - Synchronous blocking calls on a request-handling thread that should have been async
   - Chatty APIs — many small round trips where one batched call would do
   - Render-blocking JS/CSS or oversized images on the frontend
7. **Verify the fix against the baseline from step 1, and keep monitoring** — a fix that isn't measured against a before/after baseline is a guess, not a verified improvement.

```bash
# step 3 — thread dump, the go-to for "requests are hanging, CPU isn't even high"
jstack <pid> > threads.txt
grep -A 5 "BLOCKED" threads.txt   # threads stuck waiting on a lock or a slow call
```

```java
// step 6 — pagination instead of an unbounded result set
@GetMapping("/accounts")
Page<AccountResponse> listAccounts(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
    return accountRepository.findAll(PageRequest.of(page, size)).map(AccountResponse::from);
}
```

---

## Part 4: Quick-Fire Additional Scenarios

**"How would you roll out a risky change safely?"**
Feature flags to decouple deploy from release (ship the code dark, turn it on separately), canary or blue-green deployment to expose the change to a small slice of traffic first, a gradual rollout percentage, and a fast kill switch to turn it off without a redeploy if something goes wrong.

```java
@ConditionalOnProperty("feature.new-pricing-engine.enabled") // code shipped dark, flipped on separately
@Service
class NewPricingEngine implements PricingEngine { ... }
```

**"How do you handle a memory leak?"**
Start with a heap dump comparison over time to see what's growing unbounded. Common frontend causes: event listeners or intervals that are never cleaned up (exactly the `useEffect` cleanup-function footgun from the React doc), or closures holding references to large objects longer than intended. Common backend/Java causes: static collections that grow without bound, unclosed resources (connections, streams), or listener registrations that are never deregistered.

```java
// classic Java leak — a static collection nothing ever removes from
static final Map<String, Session> sessions = new HashMap<>(); // grows forever, never evicted

// fixed — bounded, or backed by something with expiry (Caffeine, Redis with TTL)
static final Cache<String, Session> sessions = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(30)).maximumSize(10_000).build();
```

**"How do you achieve zero-downtime deployment?"**
Rolling updates with readiness probes gating when new instances receive traffic (see the Docker/Kubernetes primer in the main prep doc), and — often the harder part — making database migrations backward-compatible during the rollout window, so old and new application code can both run correctly against the schema at the same time (commonly called the expand/contract pattern: add the new column/table first, deploy code that can use it, then remove the old one in a later, separate step).

```yaml
# readiness probe — new Pods don't receive traffic until this passes
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8080 }
  initialDelaySeconds: 5
  periodSeconds: 5
```

**"How do you prevent a cache stampede?"** (many clients hitting a cold cache simultaneously and all missing at once, hammering the DB)
A lock or "single-flight" pattern so only one request actually goes to the DB on a cache miss while others wait for that result; staggering TTLs with a bit of random jitter so cached items don't all expire at exactly the same moment; and refreshing hot cache entries in the background just before they expire, rather than waiting for a hard expiry.

```java
// single-flight — only the first request on a miss hits the DB, others wait on the same future
private final Map<Long, CompletableFuture<Account>> inFlight = new ConcurrentHashMap<>();

CompletableFuture<Account> getAccount(Long id) {
    return inFlight.computeIfAbsent(id, key ->
        CompletableFuture.supplyAsync(() -> accountRepository.findById(key).orElseThrow())
            .whenComplete((r, e) -> inFlight.remove(key)));
}
```
