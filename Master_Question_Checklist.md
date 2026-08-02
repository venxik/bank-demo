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
**A:** Two layers — most answers stop at the first one and sound thin. **IoC/DI**: without a container you're wiring every object graph by hand (`new AccountService(new AccountRepository(), new EmailNotifier())`), every dependency hardcoded, swapping an implementation or mocking one for a test means touching every construction site. Spring's container builds and wires that graph for you from annotations instead. **Beyond DI — the part that actually answers "why Spring" and not just "why DI"**: Spring is a coherent, battle-tested answer to nearly everything else a backend needs, so you're not gluing together separate libraries yourself — Spring MVC (REST/web layer), Spring Data (a repository interface becomes a working implementation), declarative transactions via AOP (`@Transactional`), Spring Security, and Spring Boot's auto-configuration + embedded server (`java -jar`, no XML, no separate Tomcat) plus Actuator (health/metrics endpoints for free). `bank-demo` itself is the proof: REST API + JPA persistence + declarative transactions + validation + centralized exception handling + auto-generated OpenAPI docs, wired with a handful of annotations. The real alternative to "use Spring" isn't "no framework" — it's assembling that same list of concerns from separate, less-integrated pieces by hand.

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

## Re-Verification Pass (Aug 2026)

Re-searched for OCBC interview questions to check this checklist against anything new. Result: **nothing in the 22 questions above changed or needs correction** — the design-pattern and two-Autowired-types questions specifically were re-confirmed as real via independent search snippets. Both primary sources (Nodeflair, Glassdoor) are still blocked to direct fetch (403, same as the original research pass — tried `WebFetch` and the Chrome extension again, both failed the same way), so this pass relied on search-result snippets rather than reading either site's full question list directly. That's a real limitation, not a clean re-verification — treat the 22 questions above as still the highest-confidence source, and treat this pass as "nothing contradicted, one useful new thing found" rather than "fully re-audited."

The one genuinely new, useful thing this pass surfaced: current, verifiable "why OCBC specifically" material (Next Frontier strategy, OCBC WoW conversational AI in wealth management) — now in `Self_Intro_And_Behavioral.md` §7, since a "why this bank, not just any bank" question is flagged (by eFinancialCareers' Singapore-bank interview coverage) as likely regardless of role. Worth re-verifying those specific facts are still current if there's a long gap before the actual interview date.

---

## Second Re-Verification Pass (2 Aug 2026, day before the interview)

Re-checked independently, with two results:

**Strengthened confirmation**: questions #11 and #12 are confirmed via direct search snippet, not just inference — both from a **Java Developer** report dated **Jul 1, 2024**: *"What is the difference between String and StringBuffer in Java?"* and *"Can you explain a design pattern and its importance in software development?"* Same source, same day, which suggests one candidate's full interview writeup — good corroborating signal.

**One new, low-confidence data point**: a separate Jun 2, 2024 report (Software Engineer role) mentions *"Did you have to complete a competitive programming and logical test?"* Not previously captured. Weighted low-confidence and most likely describes an earlier online-assessment stage in the pipeline rather than the technical interview round itself — every other confirmed report, including the pattern this checklist is built on, describes the technical round as conversational and concept-level, not a live coding test. Worth being aware it exists as a single data point, not worth re-prioritizing prep around.

**The "why OCBC" material needed an update, not a correction** — it was accurate when written but has since been overtaken by fast-moving news, which is exactly the risk that section flagged. Current state as of today:
- OCBC's **Next Frontier** strategy (unveiled Feb 2026 by new Group CEO Tan Teck Long, in role since Jan 1, 2026) is built around "**ADD**" — AI, Digital, Data. Notably, Data sits at the foundation of the strategy, not AI, despite AI being first in the acronym — a nuance worth having if asked to elaborate rather than name-drop.
- **Wealth management was chosen as the first business** to receive the ADD strategy — directly the division this role sits in, which is a genuinely strong, specific "why this role, why now" angle rather than a generic one.
- **HELIOS** (Holistic wEalth Lifecycle Insights & Ongoing Surveillance) — an agentic AI platform automating KYC/customer due diligence for wealth onboarding — launched **29 July 2026**, four days before this interview. Fresh enough that mentioning it signals real, current research rather than a generic "I looked at your website" answer.
- **OCBC WoW** — an AI-native banking app launched July 2026, described as the region's first with "two avatars" offering real-time, hyper-personalised wealth management service.
- OCBC has committed **over $1 billion per year for three years** to this strategy.

This connects authentically to the "breadth… increasingly building with AI-native tooling (Anthropic API, agentic workflows)" point already in your self-intro — worth drawing that line explicitly if a "why this role" question comes up, since it's a real, non-generic connection rather than a stretch.

---

## What This Means For Your Prep Priority

Given both sources above are real, repeated, confirmed signal: the two-Autowired-types question, the Spring design-pattern question, Redis, SQL vs. NoSQL, "describe a microservice you built," algorithm design techniques, Java basics from scratch, Hashtable vs. HashMap, and designing a HashMap are near-certain to come up in some form — and you now have the full answer for every one of them on this page, not just a pointer. Read this document start to finish as your final pass; use the "full depth" links only for the ones you want more than the cram version on.
