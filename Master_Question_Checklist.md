# Master Interview Question Checklist
For: OCBC Full Stack Developer Technical Interview

The highest-signal document in this whole project: actual questions reported by real OCBC candidates, from two independent sources — and, unlike every other doc here, the **full answer sits inline**, not behind a pointer. The point of this doc specifically is to be the one place you can cram from without bouncing across files. A "full depth" link is given per question for anyone who wants the deeper treatment (more patterns, more code, more edge cases) — but you don't need to click it to answer the question competently.

---

## Confirmed Real OCBC Candidate Questions

Pulled from multiple independently-reported OCBC Software Developer/Engineer interviews (Singapore, Hong Kong, Kuala Lumpur). The pattern is consistent across reports, which is itself useful signal — this is a stable, repeated interview structure, not one-off variation.

### 1. Self-introduction and walkthrough of past projects, including tech stack used
This one's yours to deliver, not something to memorize verbatim — full script in `Self_Intro_And_Behavioral.md` §1. The shape: current role (Laku6/Carousell, Go + React Native) → one notable project (the pricing-service story, §5) → banking background (CIMB Niaga, BI-FAST) → breadth (Python/Next.js/AI tooling) → why this role.

### 2. "What is the design pattern and how do you use it in Spring Boot?"
**A:** Lead with **Proxy (AOP)** — Spring's answer to "add behavior around a method without touching its code." `@Transactional`, `@Cacheable`, `@Async` all work by wrapping your bean in a dynamic proxy at startup (JDK proxy if your bean implements an interface, CGLIB subclassing if it doesn't). Call a `@Transactional` method → hits the proxy first → proxy opens a transaction → delegates to the real method → commits or rolls back.

**Classic gotcha, know cold**: self-invocation bypasses the proxy. Call a `@Transactional` method from another method in the *same class* and you're calling `this.method()` directly — never through the proxy — so the transaction silently doesn't apply. `bank-demo`'s `AccountService.createAccount()/deposit()/withdraw()/transfer()` all rely on being called from `AccountController`, outside the class, through the real proxy.

Then name 2–3 more by pattern name so it doesn't sound like you only know one: **Singleton** (Spring's default bean scope — you get this for free, no hand-written `getInstance()`), **Strategy** (`bank-demo`'s `NotificationService` interface with `ConsoleNotificationService`/`SmsNotificationService` swappable implementations), **Repository** (`AccountRepository extends JpaRepository` — an abstraction over data access Spring Data implements for you).

Full depth (11 patterns total, Spring usage + from-scratch code for each): `Spring_Java_QA.md` Part 5.

### 3. "There are two types of Autowired — explain what they are."
*Confirmed to come up word-for-word across multiple reports — know this cold.*

**A:** **Constructor injection** — dependencies listed as constructor parameters, and because there's exactly one constructor, Spring supplies real bean instances with no `@Autowired` annotation needed at all. **Field injection** — `@Autowired` directly on a field. Constructor injection is preferred: dependencies are explicit in the signature, fields can be `final` (immutable, can't be reassigned after construction), and testing is trivial (pass mocks straight into the constructor, no Spring context or reflection needed). Field injection hides dependencies, can't be `final`, and makes plain unit testing awkward. `bank-demo`'s `AccountService` uses constructor injection; `antipattern/BrokenAccountLookupService.java` exists specifically to show field injection and why it's worse.

Full depth: `Spring_Java_QA.md` Part 10.

### 4. "Why do we need to use Spring?"
**A:** Inversion of Control — normally your code controls creating its own dependencies (`new SomeService(new SomeRepo())`); Spring inverts that, with a container (`ApplicationContext`) creating and wiring objects ("beans") for you based on annotations, and handing you the finished graph. The payoff: your code depends on interfaces/abstractions instead of concrete `new` calls scattered everywhere, which is what makes testing (swap in a mock) and swapping implementations (a different `NotificationService`) possible without touching the classes that use them.

Full depth: `Spring_Java_QA.md` Part 10.

### 5. "What is Redis?"
**A:** An in-memory key-value store — used as a cache, session store, pub/sub broker, or lightweight queue. Core structures: strings, hashes, lists, sets, sorted sets. Volatile unless persistence is explicitly configured (RDB/AOF) — it's not a substitute for a relational DB when strong consistency or complex queries matter, which is why it pairs with SQL rather than replacing it. Wealth-domain angle if this is a follow-up: real-time price/quote caching with a short TTL is a classic Redis use case in trading platforms, since staleness has a real cost there.

Full depth (cache-aside/write-through/write-behind, eviction policies): `General_Backend_Engineering_QA.md` Part 6; domain framing: `Banking_Wealth_Domain_Playbook.md` Part 1.

### 6. "Difference between SQL and NoSQL database, pros and cons"
**A:**

| | SQL (relational) | NoSQL |
|---|---|---|
| Structure | Fixed schema, tables, rows | Flexible schema |
| Consistency | Strong, ACID | Often eventual, BASE |
| Best for | Complex relationships, transactions | High write throughput, flexible/evolving data |
| Examples | PostgreSQL, MySQL | MongoDB, Cassandra, DynamoDB, Redis |

For this role specifically: order books, trade blotters, and position-keeping demand ACID guarantees — justify SQL as the default for anything touching an order's lifecycle or account/position state. NoSQL fits supporting data: audit logs, market-data snapshots, high-volume event streams. Good moment to mention BI-FAST at CIMB Niaga — same financial-consistency discipline, different product surface (payments vs. wealth/trading).

Full depth: `Banking_Wealth_Domain_Playbook.md` Part 1.

### 7. "Did you use microservices in your past projects? Please share the details."
**A:** Have the Laku6 pricing-service story ready end-to-end (`Self_Intro_And_Behavioral.md` §1/§5) — real commits, a real bug caught (462 mispriced SKUs), real observability built. If pushed on microservices concepts specifically rather than your own story: a monolith is one deployable unit sharing a process/DB; microservices decompose into small, independently-deployable services each owning its own data — buys independent scaling/deployment at the cost of needing service discovery, distributed transactions, and network reliability that don't exist inside a single process.

Full depth: `General_Backend_Engineering_QA.md` Part 16.

### 8. "Describe the software development life cycle."
**A:** Requirements gathering → Design (architecture, data model) → Implementation (coding) → Testing (unit, integration, UAT) → Deployment → Maintenance (monitoring, bug fixes, iteration). Worth adding: most real teams run this iteratively via Agile/Scrum rather than one linear waterfall pass — short sprints cycling through design → build → test → review, with CI/CD blurring the line between "testing," "deployment," and "maintenance" into one continuous pipeline rather than strictly sequential gates.

Full depth: `General_Backend_Engineering_QA.md` Part 19.

### 9. A question on handling a performance issue (reported from the Hong Kong office)
**A:** Methodology matters more than tool names here. **Establish a baseline first** — you can't call something "slow" without knowing what "normal" looks like (p50/p95/p99 latency, not just averages, since averages hide the worst experiences). **Isolate where in the stack the time is actually going** before diagnosing — frontend, network/API, or database? Distributed tracing tells you which service in a multi-hop call chain is actually slow; don't assume it's the one throwing the error. Then drill into whichever layer the evidence points to (slow query → `EXPLAIN`; slow downstream call → parallelize with `CompletableFuture` or add caching; GC pressure → heap dump).

Full depth: `General_Backend_Engineering_QA.md` Part 17.

### 10. Kuala Lumpur note
One KL report mentions a more junior/digital-assessment format with situational scenario questions — less likely at your level/role, but worth knowing OCBC uses scenario-based formats elsewhere in the org.

---

**Read across all the reports**: nobody described live coding tests for this stage — every account describes a conversational, concept-level technical discussion (the "Design a HashMap" question below is the one confirmed exception). Don't over-index on LeetCode-style prep; index on being able to talk fluently through the concepts above.

**One more thing worth knowing, not a question but a real warning**: a Sep 2025 OCBC Singapore candidate reported going through multiple interview rounds after being transparent about current compensation from the start, then receiving a final offer significantly below that figure anyway. This isn't universal, but it's a real, recent, first-hand account — worth being proactive rather than passive about compensation expectations earlier in the process, rather than assuming early transparency alone protects you from a mismatched final offer.

---

## Second Confirmed Source — Nodeflair Aggregation (107 reported OCBC Bank interview questions, pulled Jul 2026)

A separate, independent aggregation of real candidate-reported questions. Cross-checked against everything else in this project; the ones that weren't already covered got a full new answer written specifically because this source surfaced the gap.

### 11. "What is the difference between String and StringBuffer in Java?"
**A:** `String` is immutable — every apparent modification (`concat`, `+`) creates a *new* object; the JVM also pools string literals for reuse. `StringBuilder`/`StringBuffer` are mutable — they modify an internal buffer in place, which is far cheaper when building a string in a loop (avoids creating N throwaway intermediate `String` objects). The difference between the two mutable ones: `StringBuffer` is the older, synchronized (thread-safe) version; `StringBuilder` is the modern, unsynchronized, faster-in-single-threaded-use default — reach for `StringBuffer` only if the same builder is genuinely shared across threads, which is rare.

```java
String s = "a";
s += "b"; // creates a NEW String "ab" — the original "a" is unchanged, just discarded

StringBuilder sb = new StringBuilder();
sb.append("a").append("b"); // mutates the same internal buffer — no throwaway objects
```

Full depth: `Spring_Java_QA.md` Part 7.

### 12. "Can you explain a design pattern and its importance in software development?"
Same as #2 above.

### 13. "Explain algorithm design techniques such as divide and conquer, dynamic programming, greedy algorithms and their complexity analysis."
**A:** **Divide and conquer** — split into same-shaped subproblems, solve recursively, combine (merge sort: `T(n) = 2T(n/2) + O(n)` → `O(n log n)`). **Dynamic programming** — subproblems *overlap* (unlike D&C), so cache each result once instead of recomputing it (naive Fibonacci is `O(2^n)`, memoized is `O(n)`). **Greedy** — make the locally-optimal choice at each step, never reconsidering; fast (`O(n log n)` or better) but only correct when the problem has "optimal substructure" — coin change with canonical denominations works greedily, general knapsack doesn't and needs DP instead. One-breath framing: check for overlapping subproblems first (DP candidate); if none, check whether a greedy choice is provably safe; divide and conquer fits when the problem naturally splits into independent same-shaped halves.

Full depth (Big-O refresher, worked code for each): `General_Backend_Engineering_QA.md` Part 12.

### 14. "What are the core concepts of java programming including basic syntax, data types, control structures, memory management and exception handling?"
**A:** **Data types** — 8 primitives (`int`, `long`, `double`, `boolean`, etc., stored by value) vs. reference types (`String`, objects — a variable holds a pointer to the heap). **Control structures** — `if/else`, `switch` (including the modern arrow form), `for`/`while`/`do-while`, `for-each`. **Memory management** — stack (per-thread, local variables/call frames) vs. heap (shared, every `new` object, GC-managed); no manual `free()`, the tradeoff being GC pause behavior you have to understand under real load. **Exception handling** — `try`/`catch`/`finally`/`throw`/`throws`, plus checked vs. unchecked exceptions (checked must be declared or caught, e.g. `IOException`; unchecked don't, e.g. `NullPointerException`).

Full depth: `Spring_Java_QA.md` Part 0 (basics) and Part 1 (checked/unchecked, JVM/GC).

### 15. "What is oauth2?"
**A:** An **authorization** framework, not authentication — it's about a user granting a third-party app limited access to their resources on another service, not verifying who they are. Flow: app redirects to the resource owner's login → user approves a scope of access → app gets an authorization code → exchanges it server-side for an access token → uses that token on API calls. (OIDC layers actual authentication on top via a JWT `id_token` — that's the "why" behind "Sign in with Google" working as real login.)

Full depth: `General_Backend_Engineering_QA.md` Part 0 and Part 11.

### 16. "What do you know about jwt?"
**A:** Three base64url parts joined by dots: `header.payload.signature`. Header names the signing algorithm; payload holds claims (`sub`, `exp`, `iat`, custom claims); signature is a cryptographic hash of header+payload that lets any server verify it wasn't tampered with, without a DB lookup — this statelessness is the entire reason JWTs scale across multiple servers. Not encrypted by default, only signed — anyone can base64-decode and read the payload, so the guarantee is integrity, not confidentiality. Never put a secret in a JWT payload thinking it's hidden.

Full depth: `General_Backend_Engineering_QA.md` Part 0.

### 17. "What is the difference between a Singleton and a Prototype bean?"
**A:** `singleton` (the default) — the container creates exactly one instance for the whole application context; every injection point gets the same shared instance. `prototype` — a brand-new instance every time one is requested, and Spring stops managing its lifecycle after handing it off (no `@PreDestroy` callback). Reach for `prototype` when a bean holds per-use mutable state that must never be shared across callers; every bean in `bank-demo` (`AccountService`, the notification implementations) is a singleton, which is the right default the overwhelming majority of the time.

Full depth: `Spring_Java_QA.md` Part 10.

### 18. "Tell me about the java collections you are familiar with."
**A:** **Lists**: `ArrayList` (resizable array, O(1) random access, O(n) middle insert/delete) vs. `LinkedList` (doubly-linked, O(1) insert/delete at a known position, O(n) random access) — default to `ArrayList` unless frequent arbitrary-position insertion is the actual pattern. **Maps**: `HashMap` (no ordering, O(1) average) vs. `LinkedHashMap` (preserves insertion order) vs. `TreeMap` (sorted by key, O(log n), red-black tree) vs. legacy `Hashtable` (synchronized, no nulls allowed — `ConcurrentHashMap` is the modern thread-safe answer instead). **Sets**: `HashSet`/`TreeSet`/`LinkedHashSet`, same ordering tradeoffs as their Map counterparts. All backed by the `Collection`/`Map` interfaces, so code depends on the interface type, not the concrete implementation.

Full depth: `Spring_Java_QA.md` Part 1 and Part 8.

### 19. "Tell me about the design patterns you have used in java programming?"
Same as #2 above.

### 20. "Explain the concept of microservice architecture."
**A:** Decomposing an application into small, independently deployable services, each owning a specific business capability and typically its own data store — as opposed to a monolith (one process, one deploy, one shared DB). Buys independent scaling and deployment, at the cost of needing to solve problems that don't exist inside a single process: service discovery (how does one service find another when instances scale up/down?), distributed transactions (no single DB transaction spans services — Saga/eventual consistency instead), and network reliability (a call that used to be a method call can now fail, time out, or arrive twice).

Full depth: `General_Backend_Engineering_QA.md` Part 16.

### 21. "What are the differences between a hashtable and a hashmap?"
**A:** `Hashtable` — legacy (pre-Java-2), every method synchronized (locks the whole table), doesn't allow `null` keys or values (throws `NullPointerException`). `HashMap` — modern, unsynchronized (faster single-threaded), allows one `null` key and multiple `null` values, the correct default today. If thread-safety is actually needed, name `ConcurrentHashMap`, not `Hashtable` — that's the answer that signals current knowledge rather than a decade-old textbook.

Full depth: `Spring_Java_QA.md` Part 8.

### 22. "Design a hashmap."
*A real "implement this" ask — the one confirmed exception to "nobody reported live coding."*

**A:** Core structure: an array of buckets, each holding entries that hashed to the same index (a linked list is enough for interview level — production `HashMap` upgrades a long bucket to a tree, an optimization detail, not the core idea). Three operations to get right: hash-and-index, collision handling within a bucket, and resizing once load factor is exceeded.

```java
class SimpleHashMap<K, V> {
    private static final int INITIAL_CAPACITY = 16;
    private Entry<K, V>[] buckets = new Entry[INITIAL_CAPACITY];
    private int size = 0;

    static class Entry<K, V> {
        K key; V value; Entry<K, V> next; // collision chain within this bucket
        Entry(K key, V value) { this.key = key; this.value = value; }
    }

    private int indexFor(K key) {
        return (key == null ? 0 : Math.abs(key.hashCode())) % buckets.length;
    }

    void put(K key, V value) {
        int index = indexFor(key);
        for (Entry<K, V> e = buckets[index]; e != null; e = e.next) {
            if (Objects.equals(e.key, key)) { e.value = value; return; } // key already present, overwrite
        }
        Entry<K, V> newEntry = new Entry<>(key, value);
        newEntry.next = buckets[index]; // insert at head of this bucket's chain
        buckets[index] = newEntry;
        if (++size > buckets.length * 0.75) resize(); // load factor 0.75 — same default real HashMap uses
    }

    V get(K key) {
        for (Entry<K, V> e = buckets[indexFor(key)]; e != null; e = e.next) {
            if (Objects.equals(e.key, key)) return e.value;
        }
        return null;
    }

    private void resize() {
        Entry<K, V>[] old = buckets;
        buckets = new Entry[old.length * 2];
        size = 0;
        for (Entry<K, V> head : old) {
            for (Entry<K, V> e = head; e != null; e = e.next) put(e.key, e.value); // rehash every entry
        }
    }
}
```
Talking points while walking through it: average-case `O(1)` for `put`/`get` assuming a decent hash spreads keys evenly; worst case `O(n)` if every key collides into one bucket (broken `hashCode()`); resizing amortizes to `O(1)` per operation even though any single resize is `O(n)`.

Full depth: `Spring_Java_QA.md` Part 8.

---

## What This Means For Your Prep Priority

Given both sources above are real, repeated, confirmed signal: the two-Autowired-types question, the Spring design-pattern question, Redis, SQL vs. NoSQL, "describe a microservice you built," algorithm design techniques, Java basics from scratch, Hashtable vs. HashMap, and designing a HashMap are near-certain to come up in some form — and you now have the full answer for every one of them on this page, not just a pointer. Read this document start to finish as your final pass; use the "full depth" links only for the ones you want more than the cram version on.
