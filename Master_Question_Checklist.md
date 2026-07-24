# Master Interview Question Checklist
For: OCBC Full Stack Developer Technical Interview

This is the "did I cover everything" doc. Part 0 is the highest-value section — actual questions reported by real OCBC candidates. The rest is organized by category, checked against what's already in your other docs, with genuinely new items (not covered elsewhere) given full answers plus a short code example, and already-covered ones marked with where to find the deep-dive.

---

## Part 0: Confirmed Real OCBC Candidate Questions

Pulled from multiple independently-reported OCBC Software Developer/Engineer interviews (Singapore, Hong Kong, Kuala Lumpur). The pattern is consistent across reports, which is itself useful signal — this is a stable, repeated interview structure, not one-off variation:

1. **Self-introduction and walkthrough of past projects, including tech stack used** → Section 15 of your Consolidated Prep doc.
2. **"What is the design pattern and how do you use it in Spring Boot?"** → Section 5 of the Consolidated Prep doc (Design Patterns) — now has a full worked example (Proxy/AOP + the self-invocation gotcha) plus 8 more patterns each with a Spring class or `bank-demo` file backing it.
3. **"There are two types of Autowired — explain what they are."** → Section 4 of the Consolidated Prep doc / Spring_Java_Interview_QA — field vs. constructor injection, and *why* constructor injection is preferred. This one is confirmed to come up directly, word for word, across multiple reports — know it cold.
4. **"Why do we need to use Spring?"** → Spring_Java_Interview_QA doc.
5. **"What is Redis?"** → Section 6 of the Consolidated Prep doc.
6. **"Difference between SQL and NoSQL database, pros and cons"** → Section 7 of the Banking Playbook.
7. **"Did you use microservices in your past projects? Please share the details."** → Have your Laku6 pricing-service example ready end-to-end (Section 8, Banking Playbook / Consolidated Prep).
8. **"Describe the software development life cycle."** → New, not yet covered — see Part 7 below.
9. **A question on handling a performance issue** (reported from the Hong Kong office specifically) → Real_World_Engineering_Scenarios_QA, Part 3.
10. One Kuala Lumpur report mentions a more junior/digital-assessment format with situational scenario questions — less likely at your level/role, but worth knowing OCBC does use scenario-based formats elsewhere in the org.

**Read across all the reports**: nobody described live coding tests for this stage — every account describes a conversational, concept-level technical discussion. That matches what your original prep doc said. Don't over-index on LeetCode-style prep; index on being able to talk fluently through the concepts above.

**One more thing worth knowing, not a question but a real warning**: a Sep 2025 OCBC Singapore candidate reported going through multiple interview rounds after being transparent about current compensation from the start, then receiving a final offer significantly below that figure anyway. This isn't universal, but it's a real, recent, first-hand account — worth being proactive rather than passive about compensation expectations earlier in the process (e.g., in the follow-up email already sent), rather than assuming early transparency alone protects you from a mismatched final offer.

---

## Part 1: Core Java & Spring — Gap Check

Everything here is already covered in depth in **Spring_Java_Interview_QA.md**. A few additional ones surfaced in this round of research that weren't in that doc yet:

**Q: What does `@SpringBootApplication` actually do under the hood?**
A: It's a convenience annotation that bundles three others: `@Configuration` (marks the class as a source of bean definitions), `@EnableAutoConfiguration` (turns on Spring Boot's auto-configuration mechanism), and `@ComponentScan` (scans the package for other components to register). One annotation, three responsibilities.

```java
// what you write, in bank-demo's BankDemoApplication.java
@SpringBootApplication
public class BankDemoApplication {
    public static void main(String[] args) { SpringApplication.run(BankDemoApplication.class, args); }
}

// what it's shorthand for
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class BankDemoApplication { ... }
```

**Q: What's the difference between a monolith and microservices, in your own words?**
A: A monolith is built and deployed as a single unit — all modules share the same process and typically the same database. Microservices decompose the application into small, independently deployable services, each owning a specific business capability and typically its own data store. The tradeoff: microservices buy you independent scaling and deployment, at the cost of needing to solve problems (service discovery, distributed transactions, network reliability) that simply don't exist inside a single process.

```
Monolith:        [ Web + Orders + Payments + Inventory ]  ← one process, one deploy, one DB
Microservices:   [ Web ] → [ Orders ] → [ Payments ] → [ Inventory ]  ← 4 processes, 4 deploys, own DBs each,
                    talking over the network (REST/gRPC/Kafka) instead of an in-process method call
```

---

## Part 2: Microservices Architecture — New Additions

These are standard microservices interview topics that weren't explicitly covered in your existing docs:

**Q: What's an API Gateway, and why use one?**
A: A reverse proxy sitting between clients and your microservices, centralizing cross-cutting concerns — authentication, SSL termination, rate limiting, request routing, response caching — so individual services don't each have to implement them. (Spring Cloud Gateway is the current standard in the Spring ecosystem; Netflix Zuul is the older, now-legacy alternative worth recognizing by name but not necessarily using.)

```
Client → [ API Gateway: auth, rate limit, routing ] → Orders Service
                                                     → Payments Service
                                                     → Inventory Service
```

**Q: What is service discovery, and why does it matter?**
A: In a system where service instances scale up/down and get rescheduled to different hosts, hardcoded IP addresses don't work. A service registry (e.g., Eureka, Consul) lets services register themselves on startup and lets other services look them up by name instead of a fixed address — the registry stays current as instances come and go.

```java
// without service discovery — brittle, breaks the moment the IP changes
restTemplate.getForObject("http://10.0.1.42:8080/orders", Order.class);

// with it — resolved by name, always current
restTemplate.getForObject("http://orders-service/orders", Order.class);
```

**Q: What's a Config Server, and what problem does it solve?**
A: Centralized, externalized configuration for a fleet of microservices — instead of each service carrying its own config file, they all pull configuration from a central config service at startup (and can refresh it without a redeploy). Same underlying problem as Spring profiles (Consolidated Prep, Section 4), just solved at fleet scale instead of per-service.

**Q: What are the "12-Factor App" principles, at a high level?**
A: A set of conventions for building cloud-native, horizontally-scalable services — the ones most likely to come up: config stored in environment variables (not hardcoded), treating backing services (DB, cache, queue) as attached resources you can swap without code changes, and — critically for the microservices context — services being stateless processes so any instance can handle any request.

```bash
# config in the environment, not hardcoded — Factor III
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-host:5432/bank
java -jar app.jar   # reads DATASOURCE_URL from env, same jar works in every environment
```

**Q: REST vs. GraphQL — when would you choose one over the other?**
A: REST is well-suited to clearly-defined, resource-oriented APIs with predictable access patterns — simple to cache, simple to reason about, and the ecosystem (tooling, familiarity) is mature. GraphQL earns its complexity when clients have highly variable data needs (a mobile app wanting a lean payload vs. a web dashboard wanting a deep one) and you want to avoid either over-fetching or maintaining many bespoke REST endpoints. For a role like this, REST is the safe default answer unless the JD or interviewer specifically signals otherwise — and this one didn't.

```
REST:    GET /accounts/1                → fixed shape, whatever the endpoint returns
GraphQL: query { account(id: 1) { balance } }  → caller picks exactly the fields it needs
```

---

## Part 3: REST API Fundamentals — New Additions

Not explicitly covered elsewhere — worth having crisp, since "full stack" interviews test this directly:

**Q: What makes an API "RESTful"?**
A: Adherence to a small set of architectural constraints: **statelessness** (each request contains everything needed to process it — no server-side session state between requests), a **uniform interface** (consistent use of HTTP methods and resource-based URLs), **cacheability** (responses indicate whether they can be cached), and **resource-based architecture** (URLs represent nouns/resources, not actions — `/orders/123`, not `/getOrder?id=123`).

```
Resource-based, matches bank-demo's own AccountController:
GET    /accounts/1          → fetch account 1
POST   /accounts/1/deposit  → mutate account 1 (the action is the verb+path, not the noun)
```

**Q: Are HTTP methods idempotent? Which ones, and why does it matter?**
A: `GET`, `PUT`, and `DELETE` are idempotent — calling them multiple times with the same input produces the same result/end state. `POST` is not idempotent by default — calling it twice typically creates two resources. This is directly the same idempotency concept from the Banking Playbook, applied at the HTTP-verb level: it's *why* `PUT` is the natural choice for an "update," and why a `POST`-based payment-creation endpoint needs its own explicit idempotency key rather than relying on the HTTP method alone.

```bash
# PUT — idempotent, calling it 5 times leaves the same end state as calling it once
curl -X PUT /accounts/1 -d '{"ownerName":"Kevin"}'

# POST — NOT idempotent by default, calling it twice creates two accounts
curl -X POST /accounts -d '{"accountNumber":"ACC-001", ...}'
# this is exactly why bank-demo's deposit/withdraw/transfer (all POST) need the
# explicit Idempotency-Key header — the HTTP method alone doesn't protect you
```

**Q: What are the HTTP status codes worth knowing cold?**
A: `200` OK, `201` Created, `204` No Content (success, nothing to return — common for DELETE), `400` Bad Request (malformed input), `401` Unauthorized (not authenticated), `403` Forbidden (authenticated, but not allowed), `404` Not Found, `409` Conflict (e.g., a duplicate resource, or an optimistic-locking version mismatch), `422` Unprocessable Entity (well-formed but semantically invalid, e.g., failed validation), `500` Internal Server Error, `503` Service Unavailable (often paired with the circuit-breaker pattern — the service is deliberately failing fast, not crashed).

```java
// bank-demo's GlobalExceptionHandler maps exceptions to exactly these codes
@ExceptionHandler(AccountNotFoundException.class)
ResponseEntity<ErrorResponse> handleNotFound(AccountNotFoundException e) {
    return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
}
@ExceptionHandler(OptimisticLockingFailureException.class)
ResponseEntity<ErrorResponse> handleConflict(OptimisticLockingFailureException e) {
    return ResponseEntity.status(409).body(new ErrorResponse("account was updated concurrently, retry"));
}
```

**Q: How would you approach API versioning?**
A: Most common approaches: URI versioning (`/v1/orders`, `/v2/orders` — simple, visible, but can feel unclean) or header-based versioning (`Accept: application/vnd.company.v2+json` — cleaner URLs, less discoverable). URI versioning is the more common pragmatic default in most real codebases, even though header versioning is sometimes considered more "correct" REST design.

```
URI versioning:    GET /v1/accounts/1   vs.   GET /v2/accounts/1
Header versioning: GET /accounts/1
                   Accept: application/vnd.bank.v2+json
```

---

## Part 4: Database/SQL — Gap Check

Core SQL vs. NoSQL tradeoffs are in the Banking Playbook (Section 7). A couple of foundational ones worth confirming you have crisp, since they're extremely commonly asked and easy to fumble under pressure:

**Q: Explain the types of SQL joins.**
A: `INNER JOIN` — only rows with matches in both tables. `LEFT JOIN` — all rows from the left table, matched rows from the right (unmatched right-side columns are null). `RIGHT JOIN` — mirror of left. `FULL OUTER JOIN` — all rows from both, matched where possible.

```sql
-- accounts: id 1,2,3.  transactions: account_id 1,1,2 (no transactions for account 3)

SELECT * FROM accounts a INNER JOIN transactions t ON a.id = t.account_id;
-- returns 3 rows (accounts 1 and 2 only) — account 3 has no match, so it's dropped entirely

SELECT * FROM accounts a LEFT JOIN transactions t ON a.id = t.account_id;
-- returns 4 rows — account 3 still appears, with every transactions.* column as NULL
```

**Q: What's database normalization, briefly?**
A: Organizing tables to reduce data redundancy and avoid update anomalies, by progressively splitting data into related tables (1NF, 2NF, 3NF being the common levels referenced in interviews) connected by foreign keys. The tradeoff, as covered in Part 1 of this doc's system-design content elsewhere: fully normalized data means more joins, which is exactly why denormalization sometimes gets deliberately reintroduced at scale.

```sql
-- denormalized — owner name repeated on every row, an update means N writes, N chances to drift
CREATE TABLE accounts (id, account_number, owner_name, owner_address, balance);

-- normalized — owner lives in one place, referenced by id
CREATE TABLE owners (id, name, address);
CREATE TABLE accounts (id, account_number, owner_id REFERENCES owners(id), balance);
```

---

## Part 5: ReactJS — Gap Check

Your existing **ReactJS_Interview_QA.md** is already comprehensive against current standard question sets (hooks, Redux, performance, patterns, React Native bridge), and every answer there now has a matching code example. Nothing significant surfaced in this research pass that isn't already there.

---

## Part 6: Open-Ended Full-Stack System Design Prompts

Beyond the banking-specific scenarios you already have, this is the *style* of open-ended, whole-stack prompt that shows up in full-stack loops at product-oriented companies — worth recognizing the shape even if OCBC's technical round (per Part 0) leans more conversational/conceptual than this:

- *"Design a REST API for a multi-tenant SaaS application — how do you handle tenant isolation at the data layer?"* The strong-signal move is addressing tenant isolation (separate schemas vs. a shared table with a `tenant_id` column and row-level security) before diving into endpoint design — interviewers are checking whether you reach for the hard part first.

  ```sql
  -- shared-table approach — every query MUST filter by tenant, or data leaks across tenants
  SELECT * FROM accounts WHERE tenant_id = :currentTenantId AND account_number = :number;
  -- row-level security makes this the DB's job, not every developer's job to remember:
  CREATE POLICY tenant_isolation ON accounts USING (tenant_id = current_setting('app.tenant_id')::uuid);
  ```

- *"Design a real-time collaborative editing feature — walk through every layer of the stack."* The strong-signal move is naming the concurrency problem immediately (two users editing simultaneously creates conflicts a simple REST endpoint can't resolve cleanly) rather than starting with UI details.
- *"How would you test a multi-step API flow where a later call can fail after an earlier one already mutated state?"* This is really the Saga/compensating-transaction question (Banking Playbook, Part 1) wearing a testing hat — the strong answer explicitly raises what happens to already-mutated state on partial failure, not just "I'd write a test for each call."

You don't need full solutions memorized for these — the point is recognizing the shape (find the hard part first, state it explicitly, then build outward) if something like this comes up.

---

## Part 7: Software Development Life Cycle & Behavioral — New Section

Confirmed as an actual OCBC question (Part 0, #8), and behavioral/process questions are common enough in a "closing discussion" that it's worth having ready even though your original prep doc didn't cover this category at all.

**Q: Describe the software development life cycle (SDLC).**
A: The standard phases: **Requirements gathering** → **Design** (architecture, data model) → **Implementation** (coding) → **Testing** (unit, integration, UAT) → **Deployment** → **Maintenance** (monitoring, bug fixes, iteration). Worth adding: most real teams (including yours, presumably) run this iteratively via Agile/Scrum rather than a single linear waterfall pass — short sprints cycling through design → build → test → review, with continuous integration/deployment blurring the line between "testing," "deployment," and "maintenance" rather than treating them as strictly sequential gates.

```
Waterfall: Requirements → Design → Build → Test → Deploy → Maintain   (once, in order)
Agile:     [ Requirements → Design → Build → Test → Review ]  ← repeated every 1-2 week sprint,
           with CI/CD blurring "test," "deploy," and "maintain" into one continuous pipeline
```

**Behavioral questions worth having a real example ready for** (STAR structure — Situation, Task, Action, Result — keeps these tight):
- *"Tell me about a time you had to learn a new technology quickly."* — This one is a gift, given your situation: you can genuinely and honestly use *this interview process itself* (or a real past example of picking up Go, or Python/Django, or AI-native tooling) as the example.
- *"Tell me about a challenging bug you debugged."* — Pull a real one from your Laku6 or CIMB Niaga work; structure it as symptom → investigation → root cause → fix → what you changed afterward (ties directly into the incident-response structure from Real_World_Engineering_Scenarios_QA).
- *"Tell me about a time you disagreed with a teammate or a technical decision."* — Standard collaboration-signal question; pick a real example where you show you pushed back respectfully and it landed somewhere reasonable, not necessarily "and I was right."
- *"Describe your role in a team project."* — Have a one-paragraph answer ready that's honest about scope, not inflated.

Full, real, git-verified answers to all four now live in `Behavioral_Interview_Prep.md` — don't improvise these live, that doc has the actual stories.

---

## What This Means For Your Prep Priority

Given Part 0 is real, repeated, confirmed signal: the two-Autowired-types question, the Spring design-pattern question, Redis, SQL vs. NoSQL, and "describe a microservice you built" are near-certain to come up in some form. Everything else in this doc is genuinely useful breadth, but if your prep time is limited before 3 Aug, weight it toward Part 0 and the sections it points to first.
