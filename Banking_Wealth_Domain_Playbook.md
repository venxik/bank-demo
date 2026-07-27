# Banking & Wealth Domain Playbook
For: OCBC Full Stack Developer Technical Interview — Wealth/Trading Platforms

Everything banking/wealth-domain-specific lives in this one doc: distributed-systems fundamentals explained from scratch (CAP, ACID/BASE, 2PC/Saga, idempotency, event sourcing/CQRS, circuit breaker, message delivery, Redis, SQL vs. NoSQL — all with the banking lens applied), real-world "what would you actually do" banking scenarios, and a niche deep-dive into wealth/trading vocabulary, OMS/settlement, MAS TRM, and AML/KYC. Read Part 1 first even if some of it feels familiar — the scenarios in Part 2 lean on it directly, and Part 3's wealth-platform design checklist points back to Part 1 throughout rather than repeating it. General (non-banking) versions of scalability/caching/system-design content are in `General_Backend_Engineering_QA.md` — this doc only covers the domain-specific lens on top of that.

---

## Table of Contents
- Part 1: Fundamentals You Need Cold
- Part 2: Real-World Banking Engineering Scenarios
- Part 3: Niche — Banking & Wealth Domain Deep Dive

---

## Part 1: Fundamentals You Need Cold

### CAP Theorem
In a distributed system, when a **network partition** happens (nodes can't talk to each other — and in real systems, this *will* happen eventually), you have to choose between:

- **Consistency (C)**: every read gets the most recent write, or an error. All nodes agree on the data.
- **Availability (A)**: every request gets a response — just not a guarantee it's the latest data.

**Partition tolerance (P) isn't really optional** in a real distributed system — networks fail, so you must handle it somehow. That means the *actual* practical choice is **CP vs. AP**:

- **CP system**: during a partition, reject or block requests it can't guarantee are consistent. Example: a ledger service refuses to confirm a transfer if it can't verify the current balance is up to date.
- **AP system**: during a partition, keep serving requests, accepting some nodes might be briefly stale, and reconcile later. Example: a "recently viewed transactions" list showing slightly stale data for a few seconds is a fine tradeoff for staying available.

```java
// CP — refuse rather than risk an overdraw during a partition
if (!replicaIsUpToDate()) {
    throw new ServiceUnavailableException("cannot confirm balance is current");
}

// AP — serve what you have, label it as possibly stale
return new RecentTransactionsView(cachedTransactions, /* possiblyStale= */ true);
```

**Concrete banking example**: an account-balance check before a debit must be CP — you cannot let a customer overdraw because two replicas briefly disagreed. A cached "last 10 transactions" view can be AP.

**Interview tip**: if asked "would you pick consistency or availability for X," never answer with just one word — answer *"it depends on what X touches: anything involving money movement or balances needs consistency; most reads (search, notifications, cached views) can tolerate staleness for availability."* That's the answer that shows judgment, not memorization.

### ACID vs. BASE
- **ACID** (Atomicity, Consistency, Isolation, Durability) — the traditional relational-DB transaction guarantee: a transaction fully happens or fully doesn't, the DB stays valid, concurrent transactions don't corrupt each other's view, and once committed it survives a crash.
- **BASE** (Basically Available, Soft state, Eventually consistent) — the NoSQL-world alternative: trade strict guarantees for availability and horizontal scale.

```sql
-- ACID in practice: both updates commit together or neither does
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT; -- if the process crashes before this line, both updates are rolled back on restart
```

### Distributed Transactions: 2PC vs. Saga
- **Two-Phase Commit (2PC)**: a coordinator asks every participant to "prepare" (lock their piece of the transaction), and only commits once everyone confirms they're ready; if anyone fails, everyone aborts. Strong consistency, but **blocking** — if the coordinator or a participant crashes mid-transaction, resources can stay locked indefinitely.
- **Saga pattern**: break a distributed transaction into a sequence of local transactions, each with a **compensating action** to undo it if a later step fails. Common in microservices because it avoids 2PC's tight coupling and blocking — at the cost of eventual (not immediate) consistency and you having to write the undo logic yourself.
  - Example: book a trade → reserve funds → execute order → settle. If settlement fails, a compensating transaction releases the reserved funds instead of the whole chain being atomically rolled back by a coordinator.

```java
// Saga, pseudocode — each step has a matching compensating action
try {
    bookTrade();
    reserveFunds();          // compensating action: releaseFunds()
    executeOrder();          // compensating action: cancelOrder()
    settle();
} catch (StepFailedException e) {
    // run compensations for every step that already succeeded, in reverse order
    releaseFunds();
    cancelOrder();
}
```

### Idempotency (the concept — Part 2 has the full scenario)
An idempotent operation produces the same result no matter how many times it's applied. This matters anywhere a request can be retried — and in distributed systems, almost everything can be retried (client timeout, network blip), so "what happens if this exact request arrives twice" is a question worth having an answer to for any mutating endpoint. `bank-demo`'s `AccountService` implements exactly this — see Scenario 2 below for the full mechanism.

### Event Sourcing & CQRS
- **Event sourcing**: instead of storing current state directly, store the sequence of events that led to it (`AccountOpened`, `Deposited $100`, `Withdrew $30`) — current state is derived by replaying events. This gives you a complete, tamper-evident audit trail *for free*, which is exactly why it shows up constantly in finance.
- **CQRS** (Command Query Responsibility Segregation): separate the model used to *write* data (commands) from the model used to *read* it (queries), so each can be scaled/optimized independently. Often paired with event sourcing — writes append events, a separate read model is built (and may lag slightly) for fast queries.

```java
// event sourcing — balance is DERIVED, never stored/mutated directly
record AccountOpened(Long accountId, BigDecimal openingBalance) {}
record Deposited(Long accountId, BigDecimal amount) {}
record Withdrew(Long accountId, BigDecimal amount) {}

BigDecimal currentBalance(List<Object> events) {
    BigDecimal balance = BigDecimal.ZERO;
    for (Object e : events) {
        balance = switch (e) {
            case AccountOpened ev -> ev.openingBalance();
            case Deposited ev -> balance.add(ev.amount());
            case Withdrew ev -> balance.subtract(ev.amount());
            default -> balance;
        };
    }
    return balance; // replay = current state, and you get the full history for free
}
```

### Circuit Breaker Pattern
Wraps a call to a potentially-failing dependency. Tracks the failure rate; once it crosses a threshold, the circuit "opens" and fails fast — without even calling the dependency — for a cooldown period, then lets a few trial requests through ("half-open") to check if the dependency has recovered. Prevents a struggling downstream service from being hammered further and cascading the failure upstream. (Resilience4j is the standard Java library for this — see the Spring Q&A doc.)

```java
@CircuitBreaker(name = "coreBankingService", fallbackMethod = "fallback")
public BigDecimal getBalance(Long accountId) {
    return coreBankingClient.fetchBalance(accountId);
}
private BigDecimal fallback(Long accountId, Throwable t) {
    return lastKnownBalance(accountId); // served while the circuit is open, not a hard error
}
```

### Message Delivery Semantics
- **At-most-once**: a message might be lost, but never processed twice.
- **At-least-once**: a message is never lost, but might be processed more than once — which is exactly why idempotent consumers matter (Part 2, Scenario 2).
- **Exactly-once**: the hard one to actually achieve at the transport level — in practice it's usually built from at-least-once delivery + idempotent processing on the consumer side, not true exactly-once transport.

```java
// at-least-once delivery + idempotent consumer = effectively exactly-once
void onMessage(PaymentEvent event) {
    if (idempotencyRepository.existsById(event.idempotencyKey())) {
        return; // already processed this exact message, safe to ignore the redelivery
    }
    processPayment(event);
    idempotencyRepository.save(event.idempotencyKey());
}
```

### Redis, for This Domain Specifically
In-memory key-value store — cache, session store, pub/sub broker, lightweight queue. Core structures: strings, hashes, lists, sets, sorted sets. Volatile unless persistence is configured (RDB/AOF) — never a substitute for a relational DB when strong consistency or complex queries matter, which is exactly why it pairs with, rather than replaces, the SQL default below.

**Wealth-domain angle**: real-time or near-real-time pricing/quote caching is a classic Redis use case in trading platforms — worth mentioning if asked how you'd cache frequently-changing market data. General backend caching patterns (cache-aside, write-through, write-behind, eviction policies) are in `General_Backend_Engineering_QA.md` Part 6 — this is just the domain-specific reason to reach for it here.

```java
// cache-aside for a quote — short TTL, since prices move fast and staleness has a real cost here
BigDecimal getQuote(String symbol) {
    BigDecimal cached = redis.opsForValue().get("quote:" + symbol);
    if (cached != null) return cached;
    BigDecimal fresh = marketDataClient.fetchQuote(symbol);
    redis.opsForValue().set("quote:" + symbol, fresh, Duration.ofSeconds(2));
    return fresh;
}
```

### SQL vs. NoSQL, for This Domain Specifically

| | SQL (relational) | NoSQL |
|---|---|---|
| Structure | Fixed schema, tables, rows | Flexible schema |
| Consistency | Strong, ACID | Often eventual, BASE |
| Best for | Complex relationships, transactions | High write throughput, flexible/evolving data |
| Examples | PostgreSQL, MySQL | MongoDB, Cassandra, DynamoDB, Redis |

**For this role specifically**: order books, trade blotters, and position-keeping (who owns what, at what cost basis) are exactly the kind of state that demands ACID guarantees — justify SQL as default for anything touching an order's lifecycle or account/position state. NoSQL fits supporting data: audit logs, market data snapshots, high-volume event streams. This is a good moment to mention your BI-FAST integration at CIMB Niaga — same underlying discipline (financial consistency requirements), different product surface (payments vs. wealth/trading).

```sql
-- SQL: an order's lifecycle demands ACID — a transfer either fully happens or fully doesn't
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;
```
```json
// NoSQL: a market-data snapshot — schema can vary document to document, no migration to add a field
{ "symbol": "AAPL", "price": 189.32, "timestamp": "2026-07-22T14:00:00Z", "source": "NYSE" }
```

---

## Part 2: Real-World Banking Engineering Scenarios

These are the "walk me through what you'd actually do" questions — the ones that separate someone who's memorized definitions from someone who's actually thought about running a system in production.

### Scenario 1 — "The system is down. Walk me through how you'd check."
1. **Confirm scope first**: fully down or degraded? Hit health/liveness/readiness endpoints before anything else.
2. **Dashboards**: check monitoring (Grafana/Datadog-equivalent) for error rate, latency, and throughput anomalies — pinpoint *when* it started.
3. **Centralized logs**: search for error spikes and stack traces around that time window.
4. **Distributed tracing**: if it's a multi-service call chain, use tracing (Jaeger/Zipkin-equivalent) to isolate *which* service in the chain is actually failing — don't assume it's the one throwing the visible error.
5. **Recent changes**: check recent deploys/config changes first — most incidents correlate with a recent change, and a rollback is often the fastest mitigation (root-cause *after* service is restored, not before).
6. **Upstream dependencies**: DB connection pool exhaustion, message queue backlog, third-party API outage, DNS issues.
7. **Communicate early**: update a status page or notify stakeholders before you have the full root cause — silence reads worse than "we're investigating."
8. **Mitigate, then root-cause**: circuit-break the failing dependency, scale up, or roll back to restore service first; do the deep root-cause analysis after.
9. **Postmortem**: blameless root-cause writeup, and a runbook update so the next person (possibly you) resolves it faster.

```bash
# step 1, in bank-demo's own terms — always the first command, not a monitoring dashboard
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

*This discipline is identical whether the service is Go or Java — the specific tools differ (Actuator/Micrometer in Spring vs. whatever you use in Go), but the shape of the response doesn't change.*

### Scenario 2 — "How do you make a payment idempotent?"
1. The client generates a unique **idempotency key** once per logical operation (e.g., a UUID created the moment the user hits "Send") and sends it with the request.
2. The server checks a dedup store (DB table or Redis) for that key before processing:
   - Key not seen → process the payment, store the key + result (success/failure + response body), with a TTL.
   - Key already seen → return the stored result immediately, **without reprocessing**.
3. Enforce a second line of defense at the DB layer: a unique constraint on the payment reference number, since app-level checks alone can race under concurrent requests.
4. This matters most on retries: the client times out waiting for a response (but the server actually succeeded), the client retries — without idempotency, that's a double-debit.

```bash
# bank-demo implements exactly this — replaying the same key returns the
# original result instead of crediting twice
curl -X POST http://localhost:8080/accounts/1/deposit -H "Content-Type: application/json" \
  -H "Idempotency-Key: dep-key-1" -d '{"amount":50.00}'
# calling this again with the SAME Idempotency-Key does not double-credit the account
```

```java
// the mechanism, as implemented in AccountService.java's executeIdempotent() helper —
// deposit/withdraw/transfer all route through this before doing their real work
return idempotencyRecordRepository.findById(idempotencyKey)
        .map(record -> deserialize(record, responseType))     // key seen before -> replay, never reprocess
        .orElseGet(() -> {
            T response = action.get();                        // key not seen -> actually run the operation
            idempotencyRecordRepository.save(new IdempotencyRecord(
                    idempotencyKey, accountId, serialize(response), 200));
            return response;
        });
```

*Tie-in: real-time payment rails like BI-FAST are built around exactly this problem — retries are expected at the network layer, so the initiation API has to be safe to call twice. If your CIMB Niaga work touched this, it's your strongest concrete example.*

### Scenario 3 — "How do you make sure money goes to the correct recipient?"
1. **Resolve before you move money**: real-time rails (BI-FAST, PayNow) do a pre-transfer lookup — resolve the account number/proxy to the account holder's *name*, and show it to the sender for confirmation before they commit. This catches typos and wrong-account-number errors before anything moves.
2. **Validate the identifier itself**: check-digit / format validation to reject malformed account numbers early, before a network call.
3. **Add friction for new beneficiaries**: an extra confirmation step, cooling-off period, or step-up authentication the first time you send to a new recipient — most fraud involves a first-time payee, so this is a high-leverage control.
4. **Immutable audit trail**: log who approved the transfer, exactly what confirmation screen they saw, and when — so if something does go wrong, you can reconstruct precisely what the user confirmed.
5. **Recall mechanisms**: some rails support a request-for-recall shortly after sending — not always successful, but worth knowing exists.

```java
// step 1, in code: resolve and show the name BEFORE moving anything
AccountHolder holder = lookupService.resolve(destinationAccountNumber);
if (!userConfirms(holder.displayName())) {
    return; // abort — nothing has moved yet
}
transferService.execute(sourceAccount, destinationAccountNumber, amount);
```

### Scenario 4 — "How do you detect and notify unusual money transfers?"
1. **Rules-based detection** (fast to build, easy to reason about): amount thresholds, velocity (X transfers in Y minutes), new device/location, first-time-large-transfer-to-new-beneficiary, suspiciously round amounts.
2. **Behavioral baselining**: build a "normal" profile per user (typical amounts, times, recipients) and flag deviations from *their own* baseline — more sophisticated, often ML-assisted, but conceptually still anomaly detection.
3. **Architecture**: transaction events flow through a stream (Kafka) into a near-real-time scoring service running in parallel with (or just ahead of) transaction completion — output is one of approve / hold-for-review / decline / step-up-auth.
4. **Notification**: push/SMS/email triggered off the same event — ideally *before* funds fully settle if the risk score is high enough to warrant a hold.
5. **Human-in-the-loop for ambiguous cases**: automated systems flag; a fraud/ops team makes the final call on genuinely unclear cases. Full automation on an irreversible action (blocking a legitimate large purchase) has real customer-trust cost.
6. **This is also a compliance obligation, not just a feature**: banks are legally required to detect and report suspicious activity (Suspicious Transaction Reports) under AML regulations — "unusual transfer detection" isn't optional polish, it's regulatory.

```java
// rules-based check, simplest version of #1
boolean isSuspicious(Transfer t, UserProfile baseline) {
    return t.amount().compareTo(THRESHOLD) > 0
        || transfersInLastMinutes(t.userId(), 10) > VELOCITY_LIMIT
        || (t.isNewBeneficiary() && t.amount().compareTo(baseline.typicalAmount().multiply(TEN)) > 0);
}
```

### Scenario 5 — "Two systems show different balances for the same account — how do you reconcile?"
1. Identify the source of truth (usually the core ledger).
2. Run scheduled reconciliation jobs comparing both systems' records at a defined cutoff, flagging discrepancies ("breaks").
3. Categorize breaks: **timing differences** (a transaction posted in one system, not yet propagated to the other — usually self-resolves next cycle) vs. **genuine errors** (missing or double-counted transaction — needs a human).
4. Alert only on breaks that don't clear within an expected window, to avoid alert fatigue on normal timing lag.
5. An immutable audit log makes this tractable — you can replay event history to see exactly where the two systems diverged.

```sql
-- step 2, the actual comparison — every mismatch is a "break" to investigate
SELECT a.account_id, a.balance AS ledger_balance, b.balance AS reporting_balance
FROM ledger_system a
JOIN reporting_system b ON a.account_id = b.account_id
WHERE a.balance <> b.balance;
```

### Scenario 6 — "How do you prevent a race condition on an account balance (two simultaneous withdrawals overdrawing the account)?"
1. **Pessimistic locking**: `SELECT ... FOR UPDATE` on the account row for the transaction's duration — simple, but can hurt throughput under contention.
2. **Optimistic locking**: a version number on the row; the update only succeeds if the version hasn't changed since you read it, retry on conflict. Better throughput, more application-side complexity.
3. **Avoid a mutable balance entirely**: an event-sourced ledger, where balance is *derived* by summing events, never directly mutated — removes this entire class of race condition, at the cost of read complexity (mitigated with CQRS: maintain a materialized "current balance" view updated as events arrive).
4. **Serialize processing per account**: route all operations for a given account through a single ordered stream (e.g., a Kafka partition keyed by account ID) so they process one at a time, in order, per account — a common event-driven pattern.

```sql
-- #1, pessimistic — locks the row until the transaction commits
SELECT balance FROM accounts WHERE id = 1 FOR UPDATE;
```

```java
// #2, optimistic — exactly how bank-demo's Account.java does it via @Version
@Version
private long version; // Hibernate adds "AND version = ?" to the UPDATE's WHERE clause

// two transactions both read version=3; whichever commits first bumps it to 4 —
// the second UPDATE now matches zero rows, Hibernate throws OptimisticLockingFailureException
// instead of silently letting the second writer overwrite the first (lost update)
```

### Scenario 7 — "A downstream dependency is failing intermittently — how do you handle it?"
1. **Circuit breaker** (Part 1) — fail fast instead of piling up threads/connections waiting on a slow dependency.
2. **Timeouts on every external call** — no unbounded waits, ever.
3. **Retries with exponential backoff + jitter**, but *only* for idempotent operations (this is exactly why Scenario 2 matters).
4. **Graceful degradation**: can you serve a slightly stale cached response instead of a hard failure? (e.g., last-known balance with a "may not reflect very recent activity" note, instead of a blank error page.)
5. **Bulkheading**: isolate resource pools per dependency, so one slow dependency doesn't exhaust the thread pool shared by everything else.

```java
// #3 — retry with backoff + jitter, only because this call is idempotent (Scenario 2)
@Retry(name = "coreBankingService")
@CircuitBreaker(name = "coreBankingService")
public BigDecimal getBalance(Long accountId) {
    return coreBankingClient.fetchBalance(accountId); // safe to retry — a GET, no side effects
}
```

### Scenario 8 — "How would you design an audit trail / ledger for financial transactions?"
1. **Append-only**: never update or delete a record — corrections are new offsetting entries, not edits. This satisfies both audit requirements and double-entry bookkeeping (every transaction has a matching debit and credit).
2. **Enforce immutability at the storage layer**, not just as an application-level promise.
3. **Every entry captures who/what/when**: the actor (user or system), the action, timestamp, and enough context to reconstruct intent later.
4. **This is event sourcing, essentially** — a good moment to connect this scenario back to Part 1 explicitly if it comes up.

```sql
-- append-only ledger table — no UPDATE or DELETE statements ever touch this table,
-- a correction is a new row with a reversed amount, not an edit to the original
CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount NUMERIC NOT NULL,       -- positive = credit, negative = debit
    actor VARCHAR NOT NULL,
    reason VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- current balance is always a query, never a stored/mutated column:
-- SELECT SUM(amount) FROM ledger_entries WHERE account_id = 1;
```

---

## Part 3: Niche — Banking & Wealth Domain Deep Dive

### Wealth & Trading Vocabulary
*This is the domain gap flagged from the actual JD. You don't need deep expertise — you need enough to sound like you did your homework and can ask an intelligent question, not enough to bluff a trader.*

| Term | What it means |
|---|---|
| **OTC (Over-The-Counter)** | Trades negotiated directly between two parties, not through a centralized exchange. Common for bonds, derivatives, some FX. Contrast with exchange-traded instruments like listed equities. |
| **Bonds** | Debt instruments: the issuer borrows money from the investor, pays periodic interest (a coupon), and returns the principal at maturity. Platform-side, this means handling coupon schedules, accrued interest, and settlement dates. |
| **Cash Equities** | Straightforward buying/selling of shares (as opposed to derivatives). Typically settles T+1 or T+2 (see Settlement, below). |
| **Funds** | Pooled investment vehicles (mutual funds, unit trusts). Priced by **NAV** (Net Asset Value), usually once a day — not continuously traded. Workflow is subscription/redemption, not buy/sell order matching. |
| **IPO (Initial Public Offering)** | A private company issues shares to the public for the first time. Platform-side: handling subscription periods, allocation logic (often oversubscribed), and conversion from "application" to "holding." |
| **Loans & Deposits** | Not "trading" instruments, but wealth products offered alongside investments (e.g., structured deposits) — relevant because a wealth platform often needs to show a client's *full* position across trading and banking products. |
| **Front-to-back integration** | Front office (client-facing dealing/advisory) → middle office (risk, compliance checks) → back office (settlement, accounting). A trade flows through this pipeline; the JD's "front-office and order-management workflows" phrase is pointing at the front-to-middle part of this chain. |
| **SGX (Singapore Exchange)** | The exchange itself — where listed equities, bonds, and derivatives actually trade in Singapore. Relevant as the counterparty system a local OMS ultimately routes orders to/settles against. |
| **CDP (Central Depository)** | Singapore's central securities depository — holds the official record of who owns what listed security (a CDP account, not the broker, is the legal holder of record for direct SGX holdings). Comes up when discussing custody/settlement, since "who's the system of record for ownership" is a real design question. |

**If asked "what's your experience with capital markets/wealth products"** — be honest, then pivot: *"I haven't worked directly on trading or wealth products, but I have worked on systems with the same underlying demands — BI-FAST integration at CIMB Niaga required the same rigor around consistency and auditability that I'd expect an order management workflow to need. I'd expect the domain vocabulary to be the fastest part to pick up; the harder part — building for correctness under regulatory and financial-consistency constraints — is exactly what I've already done."*

### Order Management System (OMS), in more depth
- **Order types**: *market order* (execute immediately at the best available price), *limit order* (execute only at a specified price or better), *stop order* (triggers a market/limit order once a price threshold is crossed).
- **Order lifecycle / state machine**: New → (Routed) → Partially Filled → Filled, or → Cancelled / Rejected / Expired. Every transition should be an auditable event — same pattern as any status-driven workflow you've built.
- **Allocation**: for a large institutional trade executed in bulk, splitting the executed quantity across multiple underlying client accounts after the fact.

```java
// the state machine, as an enum + an explicit allowed-transitions map —
// same shape as any status-driven workflow, just with finance-specific states
enum OrderStatus { NEW, ROUTED, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED, EXPIRED }

static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
    NEW, Set.of(ROUTED, CANCELLED, REJECTED),
    ROUTED, Set.of(PARTIALLY_FILLED, FILLED, CANCELLED, EXPIRED),
    PARTIALLY_FILLED, Set.of(FILLED, CANCELLED)
);

void transition(Order order, OrderStatus next) {
    if (!ALLOWED_TRANSITIONS.getOrDefault(order.status(), Set.of()).contains(next)) {
        throw new IllegalStateException("cannot go from " + order.status() + " to " + next);
    }
    order.setStatus(next); // + emit an OrderStatusChanged event — this is the audit trail
}
```

### Settlement
- **T+1 / T+2**: the number of business days after the trade date that settlement (actual exchange of cash for securities) occurs. Different instrument classes settle on different cycles.
- **DVP (Delivery versus Payment)**: a settlement mechanism ensuring securities transfer happens simultaneously with (conditional on) payment transfer — neither party is exposed to the other defaulting mid-settlement. It's essentially a real-world Two-Phase Commit (Part 1) — worth explicitly drawing that connection if it comes up.
- **Custodian banks**: hold securities on behalf of clients/institutions; settlement often routes through custodians rather than directly between counterparties.

```java
// DVP, pseudocode — this is 2PC in a real-world settlement system:
// both legs prepare, and only commit together, or neither commits
void settleDvp(Trade trade) {
    boolean securitiesReady = custodian.prepareTransfer(trade.security(), trade.quantity());
    boolean paymentReady = bank.prepareTransfer(trade.buyer(), trade.amount());
    if (securitiesReady && paymentReady) {
        custodian.commitTransfer();
        bank.commitTransfer();
    } else {
        custodian.abort();
        bank.abort(); // neither side ends up exposed to the other's failure
    }
}
```

### Reconciliation in Trading
- **Trade matching**: comparing your firm's record of a trade against the counterparty's or the exchange's, confirming agreement on price, quantity, instrument, and settlement date *before* settlement.
- **Breaks**: any mismatch that fails matching — investigated and resolved before settlement date, since an unresolved break can cause a failed settlement. Same underlying pattern as Scenario 5 above, applied to trades instead of account balances.

### Regulatory Context — MAS Technology Risk Management (TRM) Guidelines
Worth knowing at a conceptual level, since OCBC is a Singapore-headquartered bank and this shapes *how engineering gets done there*, not just how compliance operates separately from it.

- Issued by the Monetary Authority of Singapore; substantially revised in 2021 with further updates since. Technically a "Guideline" (best practice, not law itself), but tightly coupled to binding MAS Notices (e.g., on cyber hygiene, outsourcing) that institutions must actually comply with.
- Covers six broad areas: **Risk Governance, Cybersecurity Controls, Third-Party/Outsourcing Management, Operational Resilience, Systems Development, and Incident Reporting.**
- Board and senior management are explicitly accountable for technology risk — it's framed as a governance issue, not purely an engineering one.
- **What this means for you day-to-day as an engineer**: expect emphasis on secure SDLC practices (code review, vulnerability scanning, patch management), a defined incident-reporting process with escalation timelines, resilience/recovery planning for critical systems, and rigorous change management — exactly why disciplined migration tooling (Liquibase) and proper CI/CD matter as much as they do in this environment.
- You don't need to name the guideline unless it comes up naturally — but if a question veers toward "how would you handle a security incident" or "what's your approach to a production change," answering with visible *governance-awareness* (who needs to know, how fast, documented how) will land better than a purely technical answer.

### AML/KYC — brief awareness
- **KYC (Know Your Customer)**: verifying customer identity and risk profile before onboarding or allowing certain transactions.
- **AML (Anti-Money Laundering)**: ongoing monitoring for suspicious transaction patterns, tied directly to Scenario 4 above — the "unusual transfer" detection isn't just a fraud-prevention feature, it's a legal reporting obligation in most jurisdictions, including Singapore.

### If Asked to Design Part of a Wealth Platform
Everything above, pointed at a single design prompt — the checklist to run through out loud, each item pointing back to where it's already covered in full:
- **Consistency over availability** for anything touching money movement, balances, or order state (favor CP over AP — Part 1, CAP Theorem).
- **Auditability**: every state change traceable — append-only ledgers or event sourcing (Part 1) are common patterns, doubly important where regulators can ask "show me the history of this order."
- **Idempotency**: critical for payment and order-submission APIs (Part 1 + Part 2, Scenario 2) — retries must not double-process a transaction or double-submit an order. Directly relevant to your BI-FAST integration experience.
- **Order state machine**: New → Partially Filled → Filled/Cancelled/Rejected, with clear rules about which transitions are valid — the full implementation is above, under OMS.
- **Security**: encryption at rest/in transit, strict access control, PCI DSS-style compliance awareness if relevant.
- **Scalability**: read-heavy (price/quote lookups) vs. write-heavy (order submission) paths often need different scaling strategies — read replicas, Redis caching (above) for the former, queue-based load leveling (Kafka) for the latter. General scalability patterns are in `General_Backend_Engineering_QA.md` Part 15 — this is just the domain-specific lens on the same tradeoffs.
