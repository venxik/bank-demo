# General Backend Engineering Interview Q&A
For: OCBC Full Stack Developer Technical Interview

Everything else in this project is Java/Spring-specific, React-specific, or banking-domain-specific. This doc is the layer underneath all of that — the backend engineering fundamentals that are true regardless of language or framework, and that a "full stack" or "backend depth" question can reach for even when the interviewer isn't naming Spring at all. Same format as the rest: question, tight answer, code/pseudocode example. Examples are deliberately framework-neutral (pseudocode, bash, SQL, or plain diagrams) except where tying back to `bank-demo` genuinely helps ground the concept.

---

## Table of Contents
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
- Part 12: Algorithmic Complexity, Briefly
- Part 13: Quick-Fire Round

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
A: HTTP/1.1 — one request in flight per TCP connection at a time (mitigated in practice by opening multiple connections, or pipelining, which has its own problems). HTTP/2 — multiplexes many requests over a *single* TCP connection (no more head-of-line blocking at the HTTP layer), plus header compression and server push. HTTP/3 — replaces TCP entirely with QUIC (built on UDP), removing head-of-line blocking at the *transport* layer too — a single dropped packet no longer stalls every other in-flight request on the connection.

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
A: **Strong consistency** — every read sees the latest write, immediately, everywhere (expensive, often means CP in CAP terms). **Eventual consistency** — replicas converge to the same value *eventually*, but a read right after a write might see stale data briefly. **Causal consistency** — a middle ground: operations that are causally related (a reply to a comment) are seen in the correct order everywhere, but unrelated operations can be seen in different orders on different nodes. Picking the right one is a business decision, not just a technical one — this is the same judgment call as the Banking Playbook's CAP framing, generalized beyond banking: balances need strong consistency, a "likes" counter can tolerate eventual.

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

---

## Part 6: Caching Strategies

**Q: Cache-aside vs. write-through vs. write-behind — recap with the actual code shape of each.**
A: Already covered in Real_World_Engineering_Scenarios_QA with the one-line definitions — repeating with the shape of each so the difference is concrete, not just named.

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
A: Already introduced conceptually in the Banking Playbook (paired with event sourcing) — worth the generic framing too: CQRS splits the *write* model from the *read* model, letting each be optimized independently (a normalized write model for correctness, a denormalized read model for fast queries). It earns its complexity when read and write patterns are genuinely different at scale (heavy read traffic with complex query needs, alongside simpler high-integrity writes) — for most CRUD services, a single model for both is simpler and entirely sufficient; reaching for CQRS by default is a common over-engineering mistake.

**Q: Orchestration vs. choreography in a microservices workflow?**
A: **Orchestration** — a central coordinator (an orchestrator service, or a workflow engine like Temporal/Camunda) explicitly calls each service in sequence and tracks the overall state. **Choreography** — no central coordinator; each service reacts to events from others and emits its own, with the overall flow emerging from these independent reactions. Orchestration is easier to reason about and debug (one place to look); choreography scales better organizationally (services stay decoupled) but the overall flow can become hard to trace without good tooling (distributed tracing becomes not-optional).

---

## Part 9: Observability

**Q: What are "the three pillars of observability"?**
A: **Logs** — discrete, timestamped events, good for "what exactly happened at this moment." **Metrics** — numeric measurements aggregated over time (request rate, error rate, latency percentiles), good for "is the system healthy right now, and is it trending worse." **Traces** — the path of a single request across multiple services, good for "where in this call chain did the time actually go" (directly the tool from the Banking Playbook's Scenario 1 and the performance-debugging methodology in Real_World_Engineering_Scenarios_QA).

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
A: **Load testing** — how the system behaves under an *expected* level of traffic, checking latency/error rate stay within target (validates capacity planning). **Stress testing** — deliberately pushing traffic *past* expected levels to find the actual breaking point and how the system fails (gracefully degrading vs. falling over completely) — this is where you find out if your circuit breakers and bulkheads (Banking Playbook, Scenario 7) actually work under real pressure, not just in theory.

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

## Part 12: Algorithmic Complexity, Briefly

Per Master_Question_Checklist Part 0, nobody reported a live coding round for this stage — this is here for the occasional "what's the complexity of X" aside, not LeetCode prep.

**Q: What does Big-O notation actually describe?**
A: How an algorithm's runtime (or memory) grows as input size grows, ignoring constant factors — a way to compare algorithms' *scaling behavior*, not their exact speed on one specific machine. `O(1)` constant, `O(log n)` logarithmic (a balanced tree/index lookup), `O(n)` linear (a full scan), `O(n log n)` (a good sort), `O(n²)` quadratic (nested loops over the same data — the common "this got slow at scale" culprit).

```java
accounts.get(index);                              // O(1) — direct array access
accountRepository.findByAccountNumber(number);     // O(log n) — B+tree index lookup
for (Account a : accounts) { ... }                 // O(n) — one pass
for (Account a : accounts)
    for (Account b : accounts) { ... }             // O(n²) — the shape that quietly kills performance at scale
```

---

## Part 13: Quick-Fire Round
Short, direct answers, under 15 seconds each:

- **What's idempotency, in one sentence, framework-agnostic?** An operation that produces the same end state no matter how many times it's applied — the general concept behind `bank-demo`'s Idempotency-Key mechanism, PUT's HTTP semantics, and a Kafka consumer that dedupes by message ID.
- **What's a load balancer's job, in one sentence?** Distribute incoming requests across multiple backend instances so no single instance is overwhelmed, and route around instances that fail health checks.
- **What's the difference between horizontal and vertical scaling, one more time, generically?** Vertical — bigger machine, hard ceiling. Horizontal — more machines, needs the app to be stateless (or state externalized) to actually work.
- **What's a bloom filter, and what's it for?** A probabilistic set membership check — can say "definitely not in the set" with certainty, or "possibly in the set" (with a tunable false-positive rate), using far less memory than storing the actual set. Common use: checking "might this key exist in the DB" before paying for an actual disk read.
- **What's the difference between a monolith, an SOA, and microservices?** Monolith — one deployable unit. SOA — a handful of larger, often shared-infrastructure services. Microservices — many small, independently deployable services, each owning its own data — the modern end of the same spectrum SOA started.
- **What's graceful degradation?** Continuing to serve a reduced/simplified experience when a dependency fails, instead of a hard error — e.g., showing a cached price with a "may be stale" note instead of a blank screen.
