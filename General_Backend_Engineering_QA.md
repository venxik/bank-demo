# General Backend Engineering Interview Q&A
For: OCBC Full Stack Developer Technical Interview

Everything else in this project is Java/Spring-specific, React-specific, or banking-domain-specific. This doc is the layer underneath all of that — the backend engineering fundamentals that are true regardless of language or framework, and that a "full stack" or "backend depth" question can reach for even when the interviewer isn't naming Spring at all. Same format as the rest: question, tight answer, code/pseudocode example. Examples are deliberately framework-neutral (pseudocode, bash, SQL, or plain diagrams) except where tying back to `bank-demo` genuinely helps ground the concept.

---

## Table of Contents
- Part 0: API & Auth Fundamentals — Plain English First
- Part 1: Networking & Protocol Fundamentals
- Part 2: Concurrency Models
- Part 3: Distributed Systems Fundamentals
- Part 4: Database Internals
- Part 5: API Design Principles
- Part 6: Caching Strategies
- Part 7: Message Queues & Event-Driven Architecture
- Part 8: Backend Architecture Patterns
- Part 9: Observability
- Part 10: Testing Strategy
- Part 11: Authentication & Authorization Protocols
- Part 12: Algorithm Design Techniques & Complexity Analysis
- Part 13: Quick-Fire Round
- Part 14: REST API & HTTP Fundamentals
- Part 15: Building Scalable Systems
- Part 16: Microservices Fundamentals
- Part 17: Incident Response & Debugging
- Part 18: Open-Ended System Design Prompts
- Part 19: Software Development Life Cycle
- Part 20: Security Fundamentals (Bank-Relevant)
- Part 21: CI/CD & Git Basics

---

## Part 0: API & Auth Fundamentals — Plain English First

Everywhere else in this doc assumes you already know what a REST API or a token is and goes straight to the nuance. This section is the layer *underneath* that — the plain-English definitions, for the version of the question that comes before the nuanced one ("what's a REST API," not "what makes an API RESTful"). If the deeper version of a topic here is wanted, it's cross-referenced.

**Q: What's an API, in the simplest possible terms?**
A: A contract that lets one piece of software ask another to do something or hand over data, without needing to know how the other side is built internally — you call a defined set of functions/endpoints, and get a defined response back. A REST API is just one common *style* of that contract, built on HTTP.

**Q: What's a REST API?**
A: An API that uses standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`) to operate on "resources" identified by URLs (`/accounts/1`), where the HTTP method says *what* to do and the URL says *what to do it to* — `GET /accounts/1` reads account 1, `DELETE /accounts/1` deletes it. The deeper version of this question — the actual architectural constraints that make an API "RESTful" (statelessness, uniform interface, cacheability) — is in Part 14 below; this is the one-sentence version to lead with if asked cold.

```
GET    /accounts/1          -> read account 1
POST   /accounts            -> create a new account
PUT    /accounts/1          -> replace/update account 1
DELETE /accounts/1          -> delete account 1
-- the URL names the resource, the HTTP method names the action
-- bank-demo's own AccountController deviates from this a bit: it exposes create/deposit/withdraw/transfer
-- as POST-only actions (e.g. POST /accounts/1/deposit), not PUT/DELETE — worth naming as a real-world
-- example of REST conventions bent for clarity, not a literal match to the diagram above
```

**Q: What's JSON, and why is it the default over XML today?**
A: A lightweight, human-readable text format for structured data — objects as `{key: value}` pairs, arrays as `[...]`. It won out over XML as the default API payload format for being less verbose (no closing tags), mapping directly onto native data structures in most languages (a JS object, a Python dict), and being trivially parseable. XML still shows up in enterprise/legacy systems (and is explicitly named in this JD's toolchain) — mainly where a formal schema (XSD) or namespacing matters more than payload size.

```json
{ "id": 1, "accountNumber": "ACC-001", "balance": 100.00 }
```
```xml
<account><id>1</id><accountNumber>ACC-001</accountNumber><balance>100.00</balance></account>
```

**Q: What's a JWT (JSON Web Token), structurally — what's actually inside one?**
A: Three base64url-encoded parts joined by dots: `header.payload.signature`. The **header** names the signing algorithm. The **payload** holds claims — standard ones like `sub` (subject/user id), `exp` (expiry timestamp), `iat` (issued-at), plus whatever custom claims the issuer wants (roles, tenant id). The **signature** is a cryptographic hash of the header+payload, signed with a secret/private key only the issuing server holds — this is what lets any server *verify* the token wasn't tampered with, without a database lookup, which is the entire reason JWTs scale statelessly across multiple servers.

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJrZXZpbiIsImV4cCI6MTcyMTY1NjAwMH0.4f8a...
└─── header ────────┘└──────── payload (claims) ─────────────┘└ signature ┘
decoded header:  {"alg":"HS256"}
decoded payload: {"sub":"kevin","exp":1721656000}
```
```java
// verifying — no DB lookup needed, just recompute the signature and compare
Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token); // throws if tampered or expired
```
A JWT is **not** encrypted by default — anyone can base64-decode and read the payload (try it on jwt.io) — it's only *signed*, so the guarantee is integrity ("this wasn't altered"), not confidentiality. Never put a password or a secret directly in a JWT payload thinking it's hidden.

**Q: What's OAuth2, and what's a JWT's relationship to it?**
A: OAuth2 is the *authorization framework* (the flow of granting access); a JWT is one common *format* the resulting access token happens to take — but OAuth2 access tokens don't have to be JWTs (some are opaque strings the server looks up in a DB instead), and JWTs get used outside OAuth2 entirely (a service just issuing its own signed session tokens, like `bank-demo` would need to if it added auth). Don't conflate the two — one's a protocol, one's a token format. Full OAuth2 flow + OIDC + mTLS coverage is in Part 11 below; this is just placing the two concepts relative to each other.

**Q: Session-based auth vs. token-based auth — the plain-English version?**
A: **Session** — after login, the server creates a record of "this user is logged in" and gives the client a reference (a session ID, usually in a cookie); the server looks that record up on every request. **Token-based** (JWT being the common case) — the server gives the client a self-contained, signed token; the client sends it on every request, and the server verifies it cryptographically without needing to look anything up in a shared store. Natural follow-up once JWT and OAuth2 are both on the table.

**Q: What's the difference between authentication and authorization, one more time, plain English?**
A: Authentication answers "who are you" (logging in). Authorization answers "what are you allowed to do, now that we know who you are" (permissions/roles). A valid JWT proves authentication; what it's allowed to do with that identity (an `@PreAuthorize` check, a role claim inside the token) is authorization — two separate questions, commonly conflated in casual conversation but worth keeping crisply separate out loud.

**Q: What's a webhook, in plain terms?**
A: The inverse of a normal API call — instead of you polling a server asking "anything new yet?", you register a URL of yours, and the *other* server calls *you* when something happens (a payment completes, an order ships). Turns a pull into a push, avoiding constant polling for events that are actually rare.

**Q: What's middleware, in the general web-backend sense?**
A: Code that runs *between* a request arriving and your actual route/controller handling it (or between the handler and the response going out) — logging, authentication checks, request parsing, rate limiting. Spring's proxy-based `@Transactional`/AOP (`Spring_Java_QA.md` Part 5) is one concrete implementation of the same idea; Express.js middleware functions or a Spring `Filter`/`Interceptor` are others. The pattern is universal even though the exact mechanism differs per framework.

---

## Part 1: Networking & Protocol Fundamentals

**Q: TCP vs. UDP — what's the actual tradeoff?**
A: TCP is connection-oriented and reliable — guarantees ordered delivery, retransmits lost packets, but that reliability costs latency and overhead. UDP is connectionless and unreliable — no delivery guarantee, no ordering, but minimal overhead. Almost all backend HTTP APIs run over TCP, since correctness matters more than the last millisecond of latency; UDP shows up where speed matters more than perfect delivery (video/voice calls, some DNS queries, real-time game state).

```
TCP: handshake → reliable, ordered stream → guaranteed delivery, higher latency
UDP: no handshake → fire packets → no guarantee, lower latency
```

**Q: Walk through the TCP three-way handshake.**
A: `SYN` (client: "I want to connect, here's my starting sequence number") → `SYN-ACK` (server: "acknowledged, here's mine") → `ACK` (client: "acknowledged, we're connected"). Only after this completes does actual data start flowing. This is also *why* a connection-per-request model (no keep-alive) is expensive — every single request pays this round trip before the real payload even starts.

```
Client                     Server
  |------ SYN ------------->|
  |<----- SYN-ACK ----------|
  |------ ACK ------------->|
  |------ (data) ---------->|   ← actual HTTP request only starts here
```

**Q: What happens during a TLS handshake, at a high level?**
A: Client and server negotiate a protocol version and cipher suite, the server presents its certificate (proving identity, signed by a trusted CA), and both sides derive a shared symmetric session key — used for the actual encrypted traffic afterward, since symmetric encryption is far cheaper than asymmetric per-byte. This is why HTTPS has extra latency on a fresh connection compared to HTTP — this whole exchange happens before the TCP-level request even begins, though TLS 1.3 cut this down to one round trip (down from two in TLS 1.2).

**Q: HTTP/1.1 vs. HTTP/2 vs. HTTP/3 — what actually changed?**
A: HTTP/1.1 — one request in flight per TCP connection at a time (mitigated in practice by opening multiple connections, or pipelining, which has its own problems). HTTP/2 — multiplexes many requests over a *single* TCP connection (no more head-of-line blocking at the HTTP layer), plus header compression and server push (the push part is spec-real but practically dead — Chrome/Firefox both dropped support around 2022; don't lean on it as a current technique). HTTP/3 — replaces TCP entirely with QUIC (built on UDP), removing head-of-line blocking at the *transport* layer too — a single dropped packet no longer stalls every other in-flight request on the connection.

**Q: What does a DNS lookup actually involve?**
A: Browser/OS cache check first → if miss, query a recursive resolver → which walks the hierarchy (root servers → TLD servers → the domain's authoritative nameserver) → returns an IP, cached at every level along the way per its TTL. This is why a very low DNS TTL is a common trick for fast failover (point traffic elsewhere quickly), at the cost of every client re-resolving more often.

---

## Part 2: Concurrency Models

**Q: Thread-per-request vs. event loop — what's the actual difference, and what does each cost you?**
A: Thread-per-request (traditional Java/Tomcat, Go's goroutine-per-request) — each incoming request gets its own thread (or lightweight thread), blocks freely while waiting on I/O, and the runtime schedules many of them. Simple mental model, but each OS thread has real memory/context-switch cost, so there's a practical ceiling on concurrent connections. Event loop (Node.js, Nginx) — a single thread (or small pool) runs a loop that never blocks; I/O operations register a callback and the loop moves on to the next event, only coming back once the I/O completes. Scales to huge numbers of concurrent connections cheaply, at the cost of: one long-running synchronous computation blocks *everything* else on that loop.

```
Thread-per-request:  [Thread 1: req A, blocked on DB]  [Thread 2: req B, blocked on DB]  ... N threads
Event loop:           [Loop: dispatch A's DB call → move to B → move to C → A's DB call resolves → resume A]
```

**Q: Blocking vs. non-blocking I/O?**
A: Blocking — the calling thread stops and waits until the I/O operation (disk read, network call) completes, doing nothing else in the meantime. Non-blocking — the call returns immediately (often with "not ready yet"), and the caller is notified (callback, future, poll) once data is actually available, freeing the thread to do other work in the meantime. This is the actual mechanism underneath both virtual threads (Java) and the event loop model — non-blocking I/O is what makes it *possible* for one thread to serve thousands of concurrent connections.

**Q: What's the actor model, and where have you probably seen it even without the name?**
A: Actors are independent units that only communicate by sending messages to each other's mailboxes — never by sharing memory directly — so there's no shared-state race condition to guard against by construction. Akka (Java/Scala) is the textbook implementation; Erlang/Elixir processes are the same idea at the language level. If you've built anything around a message queue where independent workers only communicate via messages, not shared memory, you've already used the same underlying instinct.

**Q: Optimistic vs. pessimistic concurrency control — when do you reach for each?**
A: Pessimistic — lock the resource before touching it (`SELECT ... FOR UPDATE`), guaranteeing no one else can interfere, at the cost of throughput under contention (everyone else waits). Optimistic — don't lock; instead, tag the resource with a version, and only commit if the version hasn't changed since you read it, retrying on conflict. Optimistic wins when conflicts are rare (most reads, few concurrent writes to the *same* row); pessimistic wins when conflicts are frequent enough that retry overhead would dominate. `bank-demo`'s `Account.version` field (`@Version`) is optimistic concurrency control in production code, not just theory.

---

## Part 3: Distributed Systems Fundamentals

**Q: What are the three main data replication strategies?**
A: **Leader-follower (single-leader)** — all writes go to one leader, followers replicate asynchronously or synchronously; simple, but the leader is a bottleneck and a single point of failure until a follower is promoted. **Multi-leader** — multiple nodes accept writes, replicating to each other; better write availability across regions, but you now have to resolve conflicting concurrent writes. **Leaderless (quorum-based)** — any node can accept a write or serve a read, and the client (or a coordinator) requires acknowledgment from a quorum of nodes (e.g., `W + R > N` guarantees overlap between write and read quorums) — Cassandra/DynamoDB-style.

```
Leader-follower:  Client → [Leader] → replicates to → [Follower A] [Follower B]
Multi-leader:     Client A → [Leader 1] ⇄ replicates ⇄ [Leader 2] ← Client B   (both accept writes)
Leaderless:       Client writes to 3 of 5 nodes (W=3), reads from 3 of 5 (R=3) — W+R > N=5 guarantees overlap
```

**Q: What's the difference between strong, eventual, and causal consistency?**
A: **Strong consistency** — every read sees the latest write, immediately, everywhere (expensive, often means CP in CAP terms). **Eventual consistency** — replicas converge to the same value *eventually*, but a read right after a write might see stale data briefly. **Causal consistency** — a middle ground: operations that are causally related (a reply to a comment) are seen in the correct order everywhere, but unrelated operations can be seen in different orders on different nodes. Picking the right one is a business decision, not just a technical one — this is the same judgment call as `Banking_Wealth_Domain_Playbook.md`'s CAP framing, generalized beyond banking: balances need strong consistency, a "likes" counter can tolerate eventual.

**Q: What's consistent hashing, and what problem does it solve?**
A: A hashing scheme where adding or removing a node only reshuffles a small fraction of keys (roughly `1/N`), instead of nearly all of them like a naive `hash(key) % N` would. Nodes and keys are both hashed onto the same conceptual ring; a key belongs to the next node clockwise from its hash position. This is why it shows up constantly in distributed caches (Redis Cluster, Memcached) and load balancers wanting session affinity — losing one node doesn't force a near-total cache invalidation.

```
naive hash(key) % N:      N changes from 4 to 5 → almost every key's owner changes
consistent hashing ring:  N changes from 4 to 5 → only the keys between the new node and its
                          clockwise neighbor move — everything else stays put
```

**Q: What do Raft/Paxos actually solve, at a level you'd explain out loud (not implement)?**
A: Getting a cluster of nodes to agree on a single value (or a single ordered log of values) even when some nodes fail or messages are delayed/lost — "distributed consensus." Raft is the more commonly cited one in interviews specifically because it was designed to be more understandable than Paxos: a cluster elects a leader (via a randomized-timeout voting round), and once elected, the leader replicates a log to followers, only considering an entry committed once a majority acknowledge it. This is the mechanism underneath things like etcd, Kafka's controller election, and Kubernetes' own state store.

**Q: What's a logical clock, and why can't you just use wall-clock timestamps to order distributed events?**
A: Clocks on different machines drift and are never perfectly synchronized, so "which event happened first" can't be reliably answered by comparing timestamps across nodes. A **Lamport clock** is a simple counter each node increments on every event and attaches to outgoing messages, taking `max(local, received) + 1` on receipt — giving a consistent *partial* ordering without needing synchronized clocks at all. Worth knowing the name exists even without implementing one; it's the conceptual ancestor of the version vectors used in real distributed databases.

---

## Part 4: Database Internals

**Q: What are ACID and transaction isolation levels — how do they relate to what's here?**
A: ACID (Atomicity, Consistency, Isolation, Durability) is the correctness contract a database transaction gives you; isolation levels (`READ_UNCOMMITTED` → `READ_COMMITTED` → `REPEATABLE_READ` → `SERIALIZABLE`) are the tunable *strength* of the "I" — how much of another transaction's concurrent changes you're allowed to see. Full breakdown with Spring's `@Transactional(isolation = ...)` syntax and a bank-relevant example is in `Spring_Java_QA.md` Part 13; the rest of this section is what sits underneath a transaction (indexing, pooling, replication) rather than the transaction guarantee itself.

**Q: How does a B-tree index actually make lookups fast?**
A: A B-tree (or the B+tree variant most real databases use) keeps data sorted in a balanced tree structure with a high branching factor, so finding any row takes `O(log n)` disk-page reads instead of scanning every row (`O(n)`). Each node holds many keys (not just 2, like a binary tree) specifically to keep the tree shallow, since each level down is a disk seek — minimizing tree depth is the entire point.

```sql
-- without an index: full table scan, O(n)
SELECT * FROM accounts WHERE account_number = 'ACC-001'; -- checks every row

CREATE INDEX idx_account_number ON accounts(account_number); -- B+tree built on this column

-- with the index: O(log n) — walks the tree instead of scanning every row
SELECT * FROM accounts WHERE account_number = 'ACC-001';
```

**Q: What's a covering index?**
A: An index that includes every column a query needs, so the database can answer entirely from the index itself without a second lookup into the actual table ("index-only scan"). Faster specifically because it avoids the extra random I/O of jumping from the index back to the table row.

```sql
CREATE INDEX idx_covering ON accounts(account_number) INCLUDE (owner_name, balance);
SELECT account_number, owner_name, balance FROM accounts WHERE account_number = 'ACC-001';
-- every selected column is in the index — never touches the underlying table
```

**Q: Connection pooling — why does it matter, and what's a common way it causes an outage?**
A: Opening a DB connection is expensive (TCP handshake, auth, session setup), so a pool keeps a set of connections open and hands them out/reclaims them per request instead of opening one per request. Common outage cause: pool size too small for real concurrency — requests queue waiting for a free connection, latency spikes, and it can cascade (slow requests hold connections longer, shrinking the effective pool further). `bank-demo` uses HikariCP (Spring Boot's default) — worth knowing the name.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10   # too small under real load = requests queue for a free connection
      connection-timeout: 30000
```

**Q: Read replicas — what's replication lag, and why does it matter for correctness?**
A: A read replica applies the primary's writes asynchronously, so there's a window where the replica is *behind* — reading from it right after a write can return stale data. This matters directly for a pattern like "create a resource, then immediately read it back" — if that read hits a lagging replica, the resource might appear not to exist yet. Common mitigations: read-your-own-writes consistency (route a user's own reads to the primary briefly after they write), or simply routing anything correctness-sensitive to the primary and reserving replicas for tolerant reads (dashboards, search).

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
A: Organizing tables to reduce data redundancy and avoid update anomalies, by progressively splitting data into related tables (1NF, 2NF, 3NF being the common levels referenced in interviews) connected by foreign keys. The tradeoff: fully normalized data means more joins, which is exactly why denormalization sometimes gets deliberately reintroduced at scale (Part 15 below).

```sql
-- denormalized — owner name repeated on every row, an update means N writes, N chances to drift
CREATE TABLE accounts (id, account_number, owner_name, owner_address, balance);

-- normalized — owner lives in one place, referenced by id
CREATE TABLE owners (id, name, address);
CREATE TABLE accounts (id, account_number, owner_id REFERENCES owners(id), balance);
```

---

## Part 5: API Design Principles

**Q: How do you design pagination for a large collection endpoint?**
A: **Offset-based** (`?page=3&size=20` or `?offset=40&limit=20`) — simple, but gets slower on large offsets (the DB still has to scan past all the skipped rows) and can skip/duplicate items if rows are inserted/deleted between page requests. **Cursor-based** (`?after=<opaque-token>`) — the token encodes a stable position (typically the last seen sortable key), avoiding both problems, at the cost of not being able to jump to an arbitrary page number.

```
Offset:  GET /accounts?offset=40&limit=20   -- DB still scans/skips the first 40 rows every time
Cursor:  GET /accounts?after=eyJpZCI6NDB9   -- opaque token decodes to "id > 40", stable under inserts
```

**Q: How do you keep an API backward-compatible while evolving it?**
A: Add fields, don't remove or repurpose them; make new fields optional with sensible defaults so old clients ignoring them still work; never change a field's meaning or type in place — add a new field and deprecate the old one on its own timeline instead. This is the same "expand/contract" instinct as a backward-compatible DB migration, applied to an API contract instead of a schema.

```json
// v1 response — adding "displayName" later is safe, old clients simply don't read it
{ "id": 1, "ownerName": "Kevin" }
{ "id": 1, "ownerName": "Kevin", "displayName": "Kevin (ACC-001)" }
```

**Q: What does "contract-first" API design mean, and why would a team choose it?**
A: Writing the OpenAPI spec (or similar) *before* implementation, then generating server stubs and client SDKs from it, instead of writing the code first and generating docs afterward (springdoc-openapi's approach, which `bank-demo` uses). Contract-first forces early agreement between frontend/backend/QA on the exact shape of an API before anyone builds against it — valuable on larger teams where frontend and backend work in parallel; code-first is faster for a small team or a single owner where the contract and implementation naturally stay in sync.

**Q: What's Swagger/OpenAPI, and why does it matter in practice?**
A: OpenAPI is a specification format for describing a REST API's endpoints, request/response schemas, and auth requirements in a machine-readable way (YAML/JSON) — the spec that "contract-first" above refers to. Swagger is the tooling built around that spec — most relevantly Swagger UI, which generates interactive, browsable documentation straight from the spec, and libraries like springdoc-openapi that generate the spec *automatically* from your Spring annotations rather than you hand-writing it. The practical payoff: documentation stays in sync with the actual code because it's generated, not hand-maintained.

```xml
<!-- bank-demo's real pom.xml — this one dependency is the entire integration -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```
```bash
# generated automatically from AccountController's existing @GetMapping/@PostMapping methods —
# zero extra annotations needed for it to show up
open http://localhost:8080/swagger-ui/index.html
curl http://localhost:8080/v3/api-docs   # the raw OpenAPI JSON spec
```

---

## Part 6: Caching Strategies

**Q: Cache-aside vs. write-through vs. write-behind — what's the actual code shape of each?**
A: Three different points at which the cache and the database get updated relative to each other.

```python
# cache-aside — app owns the logic, cache is populated lazily on a miss
def get_account(id):
    cached = cache.get(id)
    if cached: return cached
    value = db.query(id)
    cache.set(id, value)
    return value

# write-through — every write updates cache and DB together, synchronously
def update_account(id, data):
    db.write(id, data)
    cache.set(id, data)   # cache is never stale, but writes pay the cache-write cost too

# write-behind — write to cache immediately, DB catches up asynchronously
def update_account_fast(id, data):
    cache.set(id, data)
    queue.enqueue(lambda: db.write(id, data))  # faster write path, risk: cache fails before DB write lands
```

**Q: What's cache eviction, and what are the common policies?**
A: When a cache is full and a new item needs to be added, something has to be removed — the eviction policy decides what. **LRU (Least Recently Used)** — evict whatever hasn't been accessed in the longest time, the most common default. **LFU (Least Frequently Used)** — evict whatever's accessed least often overall, better when access frequency matters more than recency. **TTL-based** — items expire after a fixed time regardless of access pattern, simplest to reason about, common for data that's only valid for a known window (a quote, a session).

**Q: Your Redis cache crashes unexpectedly — how should your application behave?**
A: It should degrade, not go down with it — a cache is an optimization, and the moment it's treated as a hard dependency (the app can't serve a request without it), it's stopped being "just a cache." The standard shape: wrap the cache call with a short timeout and a fallback straight to the database on failure (a cache-aside read that catches the Redis exception and just queries the DB instead), so a dead cache turns into "everything's a bit slower" rather than "everything's down." At real scale this is exactly the circuit-breaker pattern applied to the cache dependency specifically — fail fast after a few timeouts instead of letting every request hang waiting on a cache that isn't coming back, and periodically retry to detect recovery.

```java
// fallback-to-DB on cache failure — the cache being down degrades performance, not availability
Account getAccount(Long id) {
    try {
        Account cached = cache.get(id, Account.class); // short timeout configured on the cache client itself
        if (cached != null) return cached;
    } catch (RedisConnectionException e) {
        log.warn("cache unavailable, falling back to DB for account {}", id);
    }
    Account account = accountRepository.findById(id).orElseThrow();
    try { cache.set(id, account); } catch (RedisConnectionException ignored) { /* best-effort only */ }
    return account;
}
```

---

## Part 7: Message Queues & Event-Driven Architecture

**Q: Queue vs. pub/sub vs. a streaming log (Kafka-style) — what's the structural difference?**
A: **Queue** (SQS, RabbitMQ classic) — a message is delivered to *one* consumer, then removed; good for distributing work across a pool of workers. **Pub/sub** (SNS, a classic message bus) — a message fans out to *every* subscriber, but once delivered/consumed, it's generally gone from the topic. **Streaming log** (Kafka) — durably stores the full ordered stream; multiple independent consumer groups can each read the same data at their own pace, and can replay from an earlier offset — neither a plain queue nor plain pub/sub can do this by default.

```
Queue:      [Producer] → [Queue] → ONE of [Worker A, Worker B, Worker C] gets each message
Pub/sub:    [Producer] → [Topic] → EVERY subscriber gets a copy, message then gone from the topic
Kafka log:  [Producer] → [Partitioned log, durably stored] → each consumer group tracks its OWN offset,
                          can replay from any earlier point independently of other groups
```

**Q: What's a dead-letter queue, and why do you need one?**
A: Where a message goes after it fails processing repeatedly (past a retry limit), instead of being retried forever or silently dropped. Keeps a poison message (malformed data, a bug that always throws) from blocking the queue for every other message behind it, while preserving it for later inspection instead of losing it.

```yaml
# a subscription config, matches the shape of Laku6's real messaging-manifest pattern
num_retries: 3
continue_on_error: true   # after 3 failed attempts, message routes to a dead-letter queue, processing continues
```

**Q: What's backpressure, and how do you handle a consumer that's slower than its producer?**
A: When a fast producer overwhelms a slower consumer, backpressure is any mechanism that pushes back rather than letting the consumer's queue grow unbounded until it crashes. Options: bounded queues that block the producer once full, rate-limiting the producer, or scaling out consumers (more instances in the consumer group). The wrong answer is an unbounded in-memory buffer — that's a memory leak with a friendlier name.

**Q: What is Kafka specifically, in plain terms, and how does it differ from the queue/pub-sub shapes above?**
A: A distributed event streaming platform — the "streaming log" row in the table above, concretely. Producers publish messages to named topics; consumers subscribe and read them; and — unlike a transient in-memory queue — Kafka durably stores the stream, so multiple independent consumers can read the same data, and can even replay it from an earlier point.

```java
// producer
kafkaTemplate.send("order-events", orderId.toString(), new OrderCreated(orderId));

// consumer
@KafkaListener(topics = "order-events")
void onOrderEvent(OrderCreated event) { notificationService.notify(event); }
```

**Q: What's a Kafka partition, and why does it matter?**
A: A topic is split into partitions, each an ordered, append-only log. Partitioning is what lets Kafka parallelize — different partitions can be consumed simultaneously by different consumers. **Kafka guarantees ordering within a partition, but not across partitions of the same topic.** If strict ordering matters for a given entity (e.g., all events for one order), you route by a key (e.g., order ID) so every event for that entity lands on the same partition, in order.

```java
// keying by orderId — every event for THIS order always lands on the same partition, in order
kafkaTemplate.send("order-events", /* key= */ orderId.toString(), event);
```

**Q: One Kafka partition has much higher traffic than others — how do you fix it?**
A: This is the direct tradeoff of the same partition-key choice above: keying by something with a very uneven distribution (one huge customer generating 90% of events, or a key with very few distinct values) sends most of the traffic to one partition while the others sit idle — you get ordering, but lose the parallelism partitioning was supposed to buy you. Fixes, in order of preference: pick a higher-cardinality, more evenly-distributed key if the ordering requirement allows it (e.g., order ID instead of customer ID, if per-order ordering is what actually matters, not per-customer); if the hot key's ordering truly can't change, add more partitions so the *other* keys spread out better even though the hot one is still concentrated; or, for a small number of known hot keys, salt the key deliberately (append a random suffix) to spread just that key's traffic — at the cost of losing strict ordering for it, so only do this if that entity's ordering doesn't actually matter.

**Q: Consumer lag keeps increasing — how would you investigate?**
A: Lag is simply "latest offset minus last-committed offset" — it grows whenever consumers process slower than producers publish. Check, in order: is the consumer actually up and healthy (a crashed/stuck consumer stops committing entirely, lag grows unbounded)? Is per-message processing slow (a slow downstream call, a slow DB write inside the listener) — profile one message end to end. Are there enough consumer instances for the partition count (a consumer group can't have more *active* consumers than partitions — extra instances sit idle, but too few means each one owns multiple partitions' worth of work)? And is the traffic itself just up (a real spike), in which case the fix is scaling consumers or partitions, not debugging a bug.

**Q: What's a Kafka consumer group?**
A: A set of consumers splitting the work of consuming a topic — each partition is assigned to exactly one consumer within the group at a time, so the group processes the topic in parallel, and Kafka automatically rebalances partition assignments when a consumer joins or leaves.

```java
@KafkaListener(topics = "order-events", groupId = "notification-service")
// 3 instances of this service, same groupId → Kafka splits the topic's partitions across all 3
```

**Q: How does Kafka handle a consumer crashing?**
A: Consumers commit their processing offset (position within a partition) back to Kafka after processing a message. If a consumer crashes and restarts — or its partitions get reassigned to another consumer in the group — processing resumes from the last committed offset instead of from the beginning.

**Q: Why choose Kafka/event-driven messaging over direct REST calls between services?**
A: REST calls are synchronous and create temporal coupling — both services need to be up and responsive at the same moment. An event-driven approach decouples that: the producer doesn't need the consumer to be available *right now*, just *eventually*. Directly the same resilience story as the circuit-breaker content in `Banking_Wealth_Domain_Playbook.md`.

```java
// REST — both services must be up RIGHT NOW, or this call fails
notificationClient.send(orderId); // temporal coupling

// event-driven — producer doesn't care if the consumer is up this second
kafkaTemplate.send("order-events", event); // fire and forget, consumer catches up whenever it's ready
```

---

## Part 8: Backend Architecture Patterns

**Q: Layered (N-tier) architecture — what does each layer actually own?**
A: Presentation/controller (HTTP in/out, no business logic) → business/service (the actual rules, transaction boundaries) → data access/repository (persistence, no business logic either). `bank-demo`'s `controller/service/repository` split is a real, working example of exactly this — each package owns one concern, matching the Single Responsibility Principle directly.

**Q: What's hexagonal architecture (ports and adapters), and what problem does it solve beyond a plain layered structure?**
A: The core business logic sits in the center, depending only on interfaces ("ports") it defines itself; everything external (a REST controller, a database, a message queue, a third-party API) is an "adapter" plugged into a port from the outside. The payoff over a plain layered architecture: the business logic has *zero* dependency on any specific delivery mechanism or storage technology — you could swap Postgres for MongoDB, or REST for gRPC, by writing a new adapter, without touching business logic at all. `AccountService` depending on the `AccountRepository` *interface* (Dependency Inversion, again) is a small-scale instance of the same idea.

```
        [REST Controller] ---\
                               \
[Kafka Consumer] ---------> [Port: AccountRepository interface] --> [Core: AccountService]
                               /                                          |
        [CLI command] -------/                                    [Port: NotificationService interface]
                                                                           |
                                                          [Adapter: SMS]  [Adapter: Email]  [Adapter: Console]
```

**Q: CQRS vs. plain CRUD — when does the added complexity actually pay off?**
A: Already introduced conceptually in `Banking_Wealth_Domain_Playbook.md` (paired with event sourcing) — worth the generic framing too: CQRS splits the *write* model from the *read* model, letting each be optimized independently (a normalized write model for correctness, a denormalized read model for fast queries). It earns its complexity when read and write patterns are genuinely different at scale (heavy read traffic with complex query needs, alongside simpler high-integrity writes) — for most CRUD services, a single model for both is simpler and entirely sufficient; reaching for CQRS by default is a common over-engineering mistake.

**Q: Orchestration vs. choreography in a microservices workflow?**
A: **Orchestration** — a central coordinator (an orchestrator service, or a workflow engine like Temporal/Camunda) explicitly calls each service in sequence and tracks the overall state. **Choreography** — no central coordinator; each service reacts to events from others and emits its own, with the overall flow emerging from these independent reactions. Orchestration is easier to reason about and debug (one place to look); choreography scales better organizationally (services stay decoupled) but the overall flow can become hard to trace without good tooling (distributed tracing becomes not-optional).

---

## Part 9: Observability

**Q: What are "the three pillars of observability"?**
A: **Logs** — discrete, timestamped events, good for "what exactly happened at this moment." **Metrics** — numeric measurements aggregated over time (request rate, error rate, latency percentiles), good for "is the system healthy right now, and is it trending worse." **Traces** — the path of a single request across multiple services, good for "where in this call chain did the time actually go" (directly the tool from `Banking_Wealth_Domain_Playbook.md` Scenario 1 and the performance-debugging methodology in Part 17 below).

**Q: What's structured logging, and why does it matter more at scale than a human-readable log line?**
A: Logging as machine-parseable key-value data (JSON) instead of a free-text sentence, so a log aggregator (ELK, Datadog) can filter/query/alert on specific fields reliably instead of grep-ing text patterns that might change.

```json
// structured — {"level":"ERROR","event":"withdrawal_failed","accountId":1,"reason":"insufficient_funds"}
// vs. free text — "ERROR: withdrawal failed for account 1 because of insufficient funds"
// the structured version is filterable/alertable on accountId or reason directly, the free text isn't
```

**Q: What's a correlation ID (or trace ID), and why does every request need one?**
A: A unique identifier generated at the entry point of a request and propagated through every downstream service call and log line it triggers, so you can pull every log/span related to one specific request across an entire distributed system, instead of guessing which log lines from five different services actually belong together.

```
Client → [API Gateway: generates trace-id=abc123] → [Service A: logs with trace-id=abc123]
                                                   → [Service B: logs with trace-id=abc123]
-- one grep for "abc123" across all services reconstructs the full request's path
```

**Q: SLA vs. SLO vs. SLI — what's the actual difference?**
A: **SLI** (Service Level Indicator) — the actual measured metric (e.g., "99.95% of requests succeeded last month"). **SLO** (Service Level Objective) — the internal target for that metric ("we aim for 99.9% success"). **SLA** (Service Level Agreement) — the external, often contractual, commitment to a customer, usually with a consequence attached if missed ("99.5% uptime or a service credit"). The SLO is deliberately set stricter than the SLA, giving a buffer before an internal target miss becomes a customer-facing, contractual breach.

---

## Part 10: Testing Strategy

**Q: What's "the testing pyramid," and why is the shape deliberate?**
A: Many fast, cheap unit tests at the base → fewer, slower integration tests in the middle → very few, slowest, most brittle end-to-end tests at the top. The shape reflects a tradeoff: unit tests are fast and pinpoint failures precisely but can't catch wiring/integration issues; e2e tests catch real-world issues but are slow, flaky, and expensive to maintain — so you want just enough of them to cover what only they can catch, not more.

```
        /\
       /e2e\      <- few, slow, brittle, but catch real integration issues no unit test can
      /------\
     /integr. \   <- bank-demo's @SpringBootTest, e.g. AccountServiceTransferTest
    /----------\
   / unit tests \ <- many, fast, isolated (Mockito-mocked dependencies)
  /--------------\
```

**Q: What's contract testing, and what problem does it solve that neither unit nor e2e tests do well?**
A: A consumer (e.g., a frontend, or a downstream service) defines the contract it expects from a provider API (request/response shapes) as an executable test; the provider runs that same contract against its real implementation in CI. Catches breaking API changes *before* they reach a shared staging environment, without needing a slow, flaky, fully-deployed end-to-end test across both services. Pact is the common tool name to recognize.

**Q: What's a load test actually measuring, and what's the difference between load testing and stress testing?**
A: **Load testing** — how the system behaves under an *expected* level of traffic, checking latency/error rate stay within target (validates capacity planning). **Stress testing** — deliberately pushing traffic *past* expected levels to find the actual breaking point and how the system fails (gracefully degrading vs. falling over completely) — this is where you find out if your circuit breakers and bulkheads (`Banking_Wealth_Domain_Playbook.md` Scenario 7) actually work under real pressure, not just in theory.

**Q: What would you use to test a Spring Boot service, named correctly rather than "some testing library"?**
A: JUnit 5 for test structure and assertions, Mockito for mocking dependencies in unit tests, and `@SpringBootTest` — or the more targeted "slice" annotations like `@WebMvcTest` or `@DataJpaTest` — for integration tests that load some or all of the Spring context. Full code examples are in `Spring_Java_QA.md` Part 15.

---

## Part 11: Authentication & Authorization Protocols

**Q: What's OAuth2, in plain terms — and what is it explicitly *not*?**
A: An **authorization** framework — it's about a user granting a third-party app limited access to their resources on another service ("let this app read my Google Calendar"), *not* an authentication protocol on its own (it doesn't define how to verify identity). The common flow: the app redirects the user to the resource owner's login, the user approves a scope of access, the app receives an authorization code, exchanges it server-side for an access token, then uses that token on API calls.

**Q: Then what's OIDC, and how does it relate to OAuth2?**
A: OpenID Connect is a thin identity layer built *on top of* OAuth2, adding what OAuth2 deliberately left out: authentication. It adds a standardized `id_token` (a JWT containing identity claims — who the user actually is) alongside OAuth2's access token. This is the "why" behind "Sign in with Google" buttons actually working as *login*, not just permission-granting.

```
OAuth2 alone:        app gets an access_token -> "I can act on your behalf for X"
OIDC (adds on top):  app also gets an id_token (JWT) -> "this user is definitely user@example.com"
```

**Q: What's mTLS, and where does it fit relative to normal HTTPS?**
A: Normal HTTPS/TLS only verifies the *server's* identity to the client (the padlock). Mutual TLS (mTLS) has *both* sides present certificates — the server also verifies the client's identity at the transport layer, before any application-level auth even runs. Common in service-to-service communication inside a regulated environment (bank-to-bank, internal microservice mesh) where you want strong identity guarantees that don't depend on an application remembering to check a token correctly.

---

## Part 12: Algorithm Design Techniques & Complexity Analysis

`Master_Question_Checklist.md` says nobody reported a *live coding* round for this stage — but "explain algorithm design techniques such as divide and conquer, dynamic programming, greedy algorithms and their complexity analysis" is a confirmed real OCBC question (Jul 2026) asked at the *conceptual* level, not as a coding exercise. This section is that concept-level answer — recognize the shape of each technique and talk through the complexity tradeoff, not implement one live.

**Q: What does Big-O notation actually describe?**
A: How an algorithm's runtime (or memory) grows as input size grows, ignoring constant factors — a way to compare algorithms' *scaling behavior*, not their exact speed on one specific machine. `O(1)` constant, `O(log n)` logarithmic (a balanced tree/index lookup), `O(n)` linear (a full scan), `O(n log n)` (a good sort), `O(n²)` quadratic (nested loops over the same data — the common "this got slow at scale" culprit).

```java
accounts.get(index);                              // O(1) — direct array access
accountRepository.findByAccountNumber(number);     // O(log n) — B+tree index lookup
for (Account a : accounts) { ... }                 // O(n) — one pass
for (Account a : accounts)
    for (Account b : accounts) { ... }             // O(n²) — the shape that quietly kills performance at scale
```

**Q: What's divide and conquer, and what's the classic example?**
A: Break a problem into smaller subproblems of the *same* shape, solve each recursively, then combine the results. Merge sort is the textbook example: split the array in half, recursively sort each half, merge the two sorted halves. The complexity comes from the recurrence `T(n) = 2T(n/2) + O(n)` (two half-size subproblems, plus linear work to merge) — which resolves to `O(n log n)`: `log n` levels of splitting, `O(n)` work to merge at each level.

```java
int[] mergeSort(int[] arr) {
    if (arr.length <= 1) return arr;                    // base case
    int mid = arr.length / 2;
    int[] left = mergeSort(Arrays.copyOfRange(arr, 0, mid));   // divide — same problem, half the size
    int[] right = mergeSort(Arrays.copyOfRange(arr, mid, arr.length));
    return merge(left, right);                          // conquer — combine two sorted halves, O(n)
}
// T(n) = 2T(n/2) + O(n)  ->  O(n log n)
```

**Q: What's dynamic programming, and when does it actually help?**
A: Break a problem into overlapping subproblems (the key difference from divide and conquer — the subproblems *repeat*), solve each one only once, and cache ("memoize") the result so it's never recomputed. Helps specifically when a naive recursive solution would redo the same work exponentially many times — the classic teaching example is Fibonacci: naive recursion is `O(2^n)` (recomputing `fib(n-2)` dozens of times across different call branches), memoized is `O(n)` (each value computed exactly once).

```java
// naive — O(2^n), recomputes fib(2) and fib(3) many times over as n grows
int fibNaive(int n) { return n <= 1 ? n : fibNaive(n-1) + fibNaive(n-2); }

// dynamic programming (memoized) — O(n), each subproblem solved exactly once
int fibDp(int n, Map<Integer, Integer> memo) {
    if (n <= 1) return n;
    if (memo.containsKey(n)) return memo.get(n);         // subproblem already solved — reuse it
    int result = fibDp(n-1, memo) + fibDp(n-2, memo);
    memo.put(n, result);
    return result;
}
```
Real-world framing if asked "where would you actually use this": anywhere you're computing something that depends on overlapping smaller versions of itself — pricing/optimization calculations, shortest-path variants, anything with a "cache the subresult" instinct, which is the same instinct behind caching in general (Part 6) just applied within a single computation instead of across requests.

**Q: What's a greedy algorithm, and what's the catch?**
A: Make the locally-optimal choice at each step, never reconsidering it, hoping (and for some problems, provably guaranteeing) that a sequence of locally-optimal choices adds up to a globally-optimal result. Fast — usually `O(n log n)` or better, no recursion/backtracking — but the catch is it doesn't work for every problem: greedy only produces a truly optimal answer when the problem has "optimal substructure" and the "greedy-choice property" (a local optimum provably leads toward a global one). Coin-change with well-behaved denominations (like most real currency systems) is the classic example that *does* work greedily; the general knapsack problem is the classic example where greedy gives a decent-but-not-always-optimal answer and you actually need dynamic programming instead.

```java
// greedy coin change — works correctly for canonical denominations (1, 5, 10, 25, ...)
int coinsNeeded(int amount, int[] denominations) { // sorted descending
    int count = 0;
    for (int coin : denominations) {
        count += amount / coin;   // take as many of the largest coin as fit — never reconsidered
        amount %= coin;
    }
    return count;
}
```

**Q: How do you talk through the complexity tradeoff between these three, in one breath?**
A: Divide and conquer — subproblems don't overlap, no need to cache anything, complexity comes from the split/combine recurrence (`O(n log n)` typical). Dynamic programming — subproblems *do* overlap, caching turns exponential into polynomial. Greedy — no subproblems at all, just a locally-optimal choice repeated, fastest of the three when it's provably correct for the problem, wrong (or merely approximate) when it isn't. If asked to pick one for an unfamiliar problem: check for overlapping subproblems first (DP candidate); if none, check whether a greedy choice is provably safe (often via an exchange argument); divide and conquer is usually the answer when the problem naturally splits into independent same-shaped halves (sorting, searching, closest-pair-of-points-style geometry problems).

---

## Part 13: Quick-Fire Round
Short, direct answers, under 15 seconds each:

- **What's idempotency, in one sentence, framework-agnostic?** An operation that produces the same end state no matter how many times it's applied — the general concept behind `bank-demo`'s Idempotency-Key mechanism, PUT's HTTP semantics, and a Kafka consumer that dedupes by message ID.
- **What's a load balancer's job, in one sentence?** Distribute incoming requests across multiple backend instances so no single instance is overwhelmed, and route around instances that fail health checks.
- **What's the difference between horizontal and vertical scaling, one more time, generically?** Vertical — bigger machine, hard ceiling. Horizontal — more machines, needs the app to be stateless (or state externalized) to actually work.
- **What's a bloom filter, and what's it for?** A probabilistic set membership check — can say "definitely not in the set" with certainty, or "possibly in the set" (with a tunable false-positive rate), using far less memory than storing the actual set. Common use: checking "might this key exist in the DB" before paying for an actual disk read.
- **What's the difference between a monolith, an SOA, and microservices?** Monolith — one deployable unit. SOA — a handful of larger, often shared-infrastructure services. Microservices — many small, independently deployable services, each owning its own data — the modern end of the same spectrum SOA started. (Full monolith-vs-microservices tradeoffs, API Gateway, service discovery: Part 16 below.)
- **What's graceful degradation?** Continuing to serve a reduced/simplified experience when a dependency fails, instead of a hard error — e.g., showing a cached price with a "may be stale" note instead of a blank screen.
- **An EC2 instance needs to access S3 securely — how do you configure it without storing credentials?** Attach an **IAM role** (via an instance profile) to the EC2 instance instead of putting an access key/secret in config or code. AWS automatically rotates short-lived temporary credentials to the instance behind the scenes, scoped to exactly the permissions the role's policy grants (e.g., read-only on one specific bucket) — nothing long-lived to leak, and nothing to rotate manually.

---

## Part 14: REST API & HTTP Fundamentals

The deeper version of Part 0's "what's a REST API" — worth having crisp, since "full stack" interviews test this directly.

**Q: What makes an API "RESTful"?**
A: Adherence to a small set of architectural constraints: **statelessness** (each request contains everything needed to process it — no server-side session state between requests), a **uniform interface** (consistent use of HTTP methods and resource-based URLs), **cacheability** (responses indicate whether they can be cached), and **resource-based architecture** (URLs represent nouns/resources, not actions — `/orders/123`, not `/getOrder?id=123`).

```
Resource-based, matches bank-demo's own AccountController:
GET    /accounts/1          → fetch account 1
POST   /accounts/1/deposit  → mutate account 1 (the action is the verb+path, not the noun)
```

**Q: Are HTTP methods idempotent? Which ones, and why does it matter?**
A: `GET`, `PUT`, and `DELETE` are idempotent — calling them multiple times with the same input produces the same result/end state. `POST` is not idempotent by default — calling it twice typically creates two resources. This is directly the same idempotency concept as `Banking_Wealth_Domain_Playbook.md` Scenario 2, applied at the HTTP-verb level: it's *why* `PUT` is the natural choice for an "update," and why a `POST`-based payment-creation endpoint needs its own explicit idempotency key rather than relying on the HTTP method alone.

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

## Part 15: Building Scalable Systems

**Q: How would you design a system to handle 10x the current traffic?**
The structure interviewers want to see, in order:
1. **Clarify first**: what's the current bottleneck — reads, writes, or both? What's the actual growth timeline (10x over a year vs. 10x tomorrow changes the answer)? This alone signals seniority — jumping straight to a solution without scoping the problem is a common junior tell.
2. **Stateless services + load balancer**: if your application servers don't hold session state locally, you can add more of them behind a load balancer and scale horizontally almost for free. This is why externalizing session state (e.g., to Redis) matters — it's what makes horizontal scaling possible in the first place.
3. **Scale the database** (usually the real bottleneck) — see the ranked list below.
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

**Q: How do you scale a database that's become a bottleneck?**
In rough order of "do this first, it's cheap" to "do this last, it's expensive":
1. **Indexing** — the single highest-leverage fix for slow queries; missing indexes on frequently filtered/joined columns are the most common real-world cause of a "slow database" (Part 4).
2. **Query optimization** — check execution plans (`EXPLAIN`), eliminate unnecessary joins, avoid `SELECT *`.
3. **Connection pooling** — make sure you're not exhausting DB connections under load (Part 4).
4. **Read replicas** — for read-heavy workloads, route reads to replicas and keep writes on the primary (Part 4).
5. **Caching** (Part 6) in front of the DB for hot data — trades a bit of staleness for a lot of load reduction.
6. **Denormalization** — duplicate some data to avoid expensive joins at read time (Part 4).
7. **Sharding/partitioning** — split data across multiple database instances by some key (e.g., customer ID) — solves scale, but adds meaningful complexity (cross-shard queries, rebalancing when a shard gets too big). Bring this up as an option, but be clear it's not step one.

```sql
-- #1 — the fix that's usually the actual answer
EXPLAIN SELECT * FROM accounts WHERE account_number = 'ACC-001';
-- "Seq Scan on accounts" in the output means: no index, full table scan every call
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
-- re-run EXPLAIN: "Index Scan using idx_accounts_account_number" — O(log n) instead of O(n)
```

**Q: How would you design a notification system?**
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

**Q: How would you design a rate limiter?**
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

**Q: What are the load balancing strategies worth naming?**
A: **Round robin** — simple, even distribution, cycles through servers in order. **Least connections** — routes to whichever server currently has the fewest active connections, better when requests vary widely in cost. **Consistent hashing** — routes the same key to the same server consistently, useful when you want cache locality or session affinity (the same mechanism as Part 3's consistent-hashing ring, applied to request routing instead of data placement).

---

## Part 16: Microservices Fundamentals

**Q: What's the difference between a monolith and microservices, in your own words?**
A: A monolith is built and deployed as a single unit — all modules share the same process and typically the same database. Microservices decompose the application into small, independently deployable services, each owning a specific business capability and typically its own data store. The tradeoff: microservices buy you independent scaling and deployment, at the cost of needing to solve problems (service discovery, distributed transactions, network reliability) that simply don't exist inside a single process.

```
Monolith:        [ Web + Orders + Payments + Inventory ]  ← one process, one deploy, one DB
Microservices:   [ Web ] → [ Orders ] → [ Payments ] → [ Inventory ]  ← 4 processes, 4 deploys, own DBs each,
                    talking over the network (REST/gRPC/Kafka) instead of an in-process method call
```

**Q: One microservice needs to talk to another — would you choose REST, gRPC, or Kafka? Why?**
A: Depends on the actual coupling the interaction needs, not a universal "best" choice. **REST** — the default for request/response where a human-readable, widely-interoperable contract matters (a public-facing API, or any call where debuggability with a plain browser/curl is worth more than raw speed); simplest to reason about and onboard onto. **gRPC** — request/response between *internal* services where performance matters (binary Protobuf payloads, HTTP/2 multiplexing) and both ends are your own code that can share a `.proto` contract — the tradeoff is it's harder to debug ad hoc (not human-readable on the wire) and less friendly to being called from a browser directly. **Kafka** — reach for this when the interaction isn't really request/response at all: the caller doesn't need an immediate answer, multiple consumers might care about the same event, or the two services shouldn't be temporally coupled (Part 7 above) — a "this happened" fact broadcast, not a question waiting on an answer.

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
A: Centralized, externalized configuration for a fleet of microservices — instead of each service carrying its own config file, they all pull configuration from a central config service at startup (and can refresh it without a redeploy). Same underlying problem as Spring profiles (`Spring_Java_QA.md` Part 11), just solved at fleet scale instead of per-service.

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

## Part 17: Incident Response & Debugging

**Q: Walk me through general incident response shape, backend or full-stack.**
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

**Q: Walk me through debugging a performance bottleneck, step by step.**
This is a methodology question as much as a tools question — interviewers are checking whether you have a repeatable process, not just a list of tool names.
1. **Establish a baseline first.** You can't say something is "slow" without knowing what "normal" looks like — response time percentiles (p50/p95/p99, not just averages, since averages hide the worst experiences), throughput, error rate.
2. **Isolate where in the stack the time is actually going** before diagnosing anything: frontend rendering, network/API call, or database? Distributed tracing (Part 9) tells you *which* service in a multi-service call chain is actually slow — don't assume it's the one that happens to be throwing an error.
3. **Backend-specific diagnosis**:
   - **Profilers** (JVisualVM, YourKit, JProfiler for Java) to see where CPU time is actually spent inside the application.
   - **Heap dumps** for memory-related slowness (excessive GC pauses from memory pressure).
   - **Thread dumps** for concurrency issues — deadlocks, threads stuck waiting on a lock or a slow downstream call.
   - **Slow query logs / `EXPLAIN` plans** for database-side bottlenecks (Part 4) — almost always the first place to look for a "the API got slow" report.
   - Frontend-specific diagnosis (Core Web Vitals, DevTools, bundle size, React Profiler) is in `General_Frontend_Engineering_QA.md`.
4. **Reproduce the issue in a controlled environment** if possible, rather than debugging blind against production — confirms your hypothesis and lets you test a fix safely.
5. **Fix the highest-leverage cause first**, not every possible inefficiency — the common real-world culprits, roughly in order of how often they're the actual answer: N+1 queries (`Spring_Java_QA.md` Part 12), missing database indexes, unbounded result sets without pagination, synchronous blocking calls that should have been async, chatty APIs (many small round trips where one batched call would do).
6. **Verify the fix against the baseline from step 1, and keep monitoring** — a fix that isn't measured against a before/after baseline is a guess, not a verified improvement.

```bash
# thread dump — the go-to for "requests are hanging, CPU isn't even high"
jstack <pid> > threads.txt
grep -A 5 "BLOCKED" threads.txt   # threads stuck waiting on a lock or a slow call
```
```java
// pagination instead of an unbounded result set
@GetMapping("/accounts")
Page<AccountResponse> listAccounts(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
    return accountRepository.findAll(PageRequest.of(page, size)).map(AccountResponse::from);
}
```

**Q: How would you roll out a risky change safely?**
A: Feature flags to decouple deploy from release (ship the code dark, turn it on separately), canary or blue-green deployment to expose the change to a small slice of traffic first, a gradual rollout percentage, and a fast kill switch to turn it off without a redeploy if something goes wrong.

```java
@ConditionalOnProperty("feature.new-pricing-engine.enabled") // code shipped dark, flipped on separately
@Service
class NewPricingEngine implements PricingEngine { ... }
```

**Q: How do you handle a memory leak?**
A: Start with a heap dump comparison over time to see what's growing unbounded. Common backend/Java causes: static collections that grow without bound, unclosed resources (connections, streams), or listener registrations that are never deregistered. (Frontend causes — uncleaned event listeners/intervals, closures holding large objects — are in `General_Frontend_Engineering_QA.md`.)

```java
// classic Java leak — a static collection nothing ever removes from
static final Map<String, Session> sessions = new HashMap<>(); // grows forever, never evicted

// fixed — bounded, or backed by something with expiry (Caffeine, Redis with TTL)
static final Cache<String, Session> sessions = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(30)).maximumSize(10_000).build();
```

**Q: How do you achieve zero-downtime deployment?**
A: Rolling updates with readiness probes gating when new instances receive traffic (`Spring_Java_QA.md` Part 18), and — often the harder part — making database migrations backward-compatible during the rollout window, so old and new application code can both run correctly against the schema at the same time. Commonly called the **expand/contract pattern**: add the new column/table first, deploy code that can use it, then remove the old one in a later, separate step — never rename or drop a column in the same deploy that starts using its replacement.

**Q: How do you prevent a cache stampede?** (many clients hitting a cold cache simultaneously and all missing at once, hammering the DB)
A: A lock or "single-flight" pattern so only one request actually goes to the DB on a cache miss while others wait for that result; staggering TTLs with a bit of random jitter so cached items don't all expire at exactly the same moment; and refreshing hot cache entries in the background just before they expire, rather than waiting for a hard expiry.

```java
// single-flight — only the first request on a miss hits the DB, others wait on the same future
private final Map<Long, CompletableFuture<Account>> inFlight = new ConcurrentHashMap<>();

CompletableFuture<Account> getAccount(Long id) {
    return inFlight.computeIfAbsent(id, key ->
        CompletableFuture.supplyAsync(() -> accountRepository.findById(key).orElseThrow())
            .whenComplete((r, e) -> inFlight.remove(key)));
}
```

---

## Part 18: Open-Ended System Design Prompts

Beyond the banking-specific scenarios in `Banking_Wealth_Domain_Playbook.md`, this is the *style* of open-ended, whole-stack prompt that shows up in full-stack loops at product-oriented companies — worth recognizing the shape even if OCBC's technical round leans more conversational/conceptual than this (`Master_Question_Checklist.md`).

- *"Design a REST API for a multi-tenant SaaS application — how do you handle tenant isolation at the data layer?"* The strong-signal move is addressing tenant isolation (separate schemas vs. a shared table with a `tenant_id` column and row-level security) before diving into endpoint design — interviewers are checking whether you reach for the hard part first.

  ```sql
  -- shared-table approach — every query MUST filter by tenant, or data leaks across tenants
  SELECT * FROM accounts WHERE tenant_id = :currentTenantId AND account_number = :number;
  -- row-level security makes this the DB's job, not every developer's job to remember:
  CREATE POLICY tenant_isolation ON accounts USING (tenant_id = current_setting('app.tenant_id')::uuid);
  ```

- *"Design a real-time collaborative editing feature — walk through every layer of the stack."* The strong-signal move is naming the concurrency problem immediately (two users editing simultaneously creates conflicts a simple REST endpoint can't resolve cleanly) rather than starting with UI details.
- *"How would you test a multi-step API flow where a later call can fail after an earlier one already mutated state?"* This is really the Saga/compensating-transaction question (`Banking_Wealth_Domain_Playbook.md` Part 1) wearing a testing hat — the strong answer explicitly raises what happens to already-mutated state on partial failure, not just "I'd write a test for each call."

You don't need full solutions memorized for these — the point is recognizing the shape (find the hard part first, state it explicitly, then build outward) if something like this comes up.

---

## Part 19: Software Development Life Cycle

**Q: Describe the software development life cycle (SDLC).** *(confirmed real OCBC question)*
A: The standard phases: **Requirements gathering** → **Design** (architecture, data model) → **Implementation** (coding) → **Testing** (unit, integration, UAT) → **Deployment** → **Maintenance** (monitoring, bug fixes, iteration). Worth adding: most real teams run this iteratively via Agile/Scrum rather than a single linear waterfall pass — short sprints cycling through design → build → test → review, with continuous integration/deployment blurring the line between "testing," "deployment," and "maintenance" rather than treating them as strictly sequential gates.

```
Waterfall: Requirements → Design → Build → Test → Deploy → Maintain   (once, in order)
Agile:     [ Requirements → Design → Build → Test → Review ]  ← repeated every 1-2 week sprint,
           with CI/CD blurring "test," "deploy," and "maintain" into one continuous pipeline
```

---

## Part 20: Security Fundamentals (Bank-Relevant)

Framework-agnostic security — the general concepts, worth taking seriously given the employer. (Spring-specific security — the filter chain, `@PreAuthorize` — is in `Spring_Java_QA.md` Part 17.)

**Q: What are the OWASP Top 10, and why would it matter for this role?**
A: OWASP maintains the industry-standard list of the most critical web application security risks. It was significantly updated in late 2025 (first major revision since 2021) — current list, in order: **Broken Access Control, Security Misconfiguration, Software Supply Chain Failures, Cryptographic Failures, Injection, Insecure Design, Authentication Failures, Software/Data Integrity Failures, Security Logging and Alerting Failures, and Mishandling of Exceptional Conditions.** For a bank, these aren't abstract — Broken Access Control is directly "can customer A see customer B's account," and Cryptographic Failures is directly "is money-movement data properly encrypted at rest and in transit." You don't need to recite the list verbatim, but recognizing a couple of these by name if security comes up will land well.

**Q: What's Cross-Site Scripting (XSS)?**
A: An attacker injects malicious script into a page that other users view — commonly through an unsanitized input field that gets rendered back into the page. Defense: escape/encode output based on context. React actually escapes text content by default when rendering — the real risk shows up specifically when that protection is deliberately bypassed (e.g., `dangerouslySetInnerHTML`, see `General_Frontend_Engineering_QA.md`).

```jsx
<div>{userComment}</div>                              // safe — React escapes this automatically
<div dangerouslySetInnerHTML={{__html: userComment}}/> // the escape hatch — XSS risk lands right here
```

**Q: What's Cross-Site Request Forgery (CSRF)?**
A: An attacker tricks a logged-in user's browser into submitting an unwanted request to your app, relying on the browser automatically attaching cookies to same-site requests. Defense: CSRF tokens (a random value tied to the session that must accompany state-changing requests) or the `SameSite` cookie attribute, which stops the cookie being sent on cross-site requests in the first place.

```
Set-Cookie: sessionId=abc123; SameSite=Strict; Secure
```
```html
<!-- a malicious page the victim visits while logged into your bank -->
<form action="https://bank.com/accounts/1/transfer" method="POST">
  <input name="toAccountId" value="999"><input name="amount" value="10000">
</form>
<script>document.forms[0].submit()</script>
<!-- SameSite=Strict stops the browser from attaching the bank's session cookie to this cross-site POST -->
```

**Q: What's SQL Injection, and how do you prevent it?**
A: Untrusted input gets concatenated directly into a SQL query, letting an attacker manipulate the query itself. Prevention: parameterized queries / prepared statements — which is what Spring Data JPA gives you by default when used correctly. Never string-concatenate user input into a query.

```java
// vulnerable — user input becomes part of the query itself
String sql = "SELECT * FROM accounts WHERE account_number = '" + input + "'";
// input = "' OR '1'='1" turns this into "... WHERE account_number = '' OR '1'='1'" — returns every row

// safe — parameterized, the input is always treated as data, never as SQL syntax
accountRepository.findByAccountNumber(input); // Spring Data JPA generates a PreparedStatement under the hood
```

**Q: What's CORS, and why does it exist?**
A: Cross-Origin Resource Sharing — a *browser-enforced* rule that blocks a web page from calling a different origin (domain/port/protocol) than the one that served it, unless the server explicitly allows it via response headers. Worth knowing it's a browser protection, not a server-side security control on its own — the server still needs its own authorization checks regardless of CORS configuration. Note this is genuinely different from the Same-Origin Policy it relaxes — see `General_Frontend_Engineering_QA.md` Part 6 for that distinction.

```
Access-Control-Allow-Origin: https://app.bank.com
Access-Control-Allow-Methods: GET, POST
```

**Q: How do you securely store passwords?**
A: Never plaintext, and never with reversible encryption. Hash with a slow, salted, purpose-built algorithm — bcrypt, scrypt, or Argon2 — not a fast general-purpose hash like plain SHA-256, which is fast enough to make large-scale brute-forcing practical.

```java
// storing
String hashed = new BCryptPasswordEncoder().encode(rawPassword); // salt is generated and embedded automatically

// verifying
boolean matches = new BCryptPasswordEncoder().matches(rawPassword, hashed);
```

---

## Part 21: CI/CD & Git Basics

Named in the JD (Jenkins, Bitbucket).

**Q: CI vs. CD — what's the actual difference?**
A: **Continuous Integration** — automatically building and testing every change (typically on every push/PR) to catch integration problems early, instead of discovering them when merging large batches of work later. **Continuous Delivery/Deployment** — automatically pushing changes that pass CI toward production: up to a manual approval gate (delivery), or fully automatically (deployment).

**Q: What does a typical Jenkins pipeline actually do?**
A: Triggered by a code push: pull the code → build → run automated tests → run static analysis/security scans → package a deployable artifact (often a Docker image) → deploy to an environment, usually gated behind manual approval for production. Defined as code (a `Jenkinsfile`), version-controlled alongside the application itself.

```groovy
// Jenkinsfile, the shape of it — bank-demo's own .github/workflows/ci.yml runs the same
// three real steps (checkout, mvn test, build), just on GitHub Actions instead of Jenkins
pipeline {
    stages {
        stage('Build')  { steps { sh 'mvn -B clean package -DskipTests' } }
        stage('Test')   { steps { sh 'mvn -B test' } }
        stage('Deploy') { steps { sh 'docker build -t bank-demo . && docker push ...' } }
    }
}
```

**Q: Merge vs. rebase — what's the practical difference?**
A: Merge creates a new commit joining two branch histories — preserves exactly what happened, including the branching, but produces a messier, non-linear history. Rebase replays your commits on top of the target branch, producing a clean, linear history — but it rewrites commit hashes, which is exactly why the standard guidance is "never rebase a branch other people are already working on."

```bash
git merge main   # creates a new merge commit — history shows the branch really happened
git rebase main  # replays your commits on top of main — linear history, but new commit hashes
```

**Q: How do you resolve a merge conflict?**
A: Git marks conflicting sections in the file with conflict markers; you manually decide which changes to keep (or combine both), remove the markers, then stage and commit the resolved file.

```
<<<<<<< HEAD
BigDecimal balance = BigDecimal.ZERO;
=======
BigDecimal balance = initialBalance;
>>>>>>> feature/initial-balance
```
```bash
# after manually picking (or combining) the right version and removing the markers above:
git add Account.java
git commit
```
