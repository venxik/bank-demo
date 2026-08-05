# Spring & Java Interview Q&A
For: OCBC Full Stack Developer Technical Interview

**The single Java/Spring reference for this project** — nothing Java- or Spring-related lives in any other doc. Format: question, tight interview-ready answer, code example. Where useful, a Go analogy is included since that's your daily-driver language — use these to bridge naturally rather than reciting them verbatim.

---

## Table of Contents
- Part 0: Java Basics, Fast Recap
- Part 1: Core Java
- Part 2: Core OOP Concepts
- Part 3: SOLID Principles
- Part 4: Concurrency
- Part 5: Design Patterns — Spring Usage + From-Scratch Implementation
- Part 6: Java 8+ Features (Lambdas, Streams, Optional)
- Part 7: Strings, Memory & Other Core Java
- Part 8: Collections Framework Deep Dive
- Part 9: Modern Java (17–21) Features
- Part 10: Spring Core
- Part 11: Spring Boot
- Part 12: Spring Data JPA
- Part 13: Transactions
- Part 14: REST / Spring MVC
- Part 15: Testing
- Part 16: Microservices / Resilience
- Part 17: Security Basics
- Part 18: Docker & Kubernetes
- Part 19: Liquibase
- Part 20: Quick-Fire Round

---

## Part 0: Java Basics, Fast Recap

**Confirmed real question** (real OCBC candidate report, Jul 2026): *"What are the core concepts of java programming including basic syntax, data types, control structures, memory management and exception handling?"* — a genuinely broad, from-scratch question. Everything past this section assumes Java syntax itself is already known and jumps straight to generics/streams/collections internals — this is the layer underneath that, in case the question really is asked at this basic a level.

**Q: What are Java's primitive data types?**
A: Eight: `byte`, `short`, `int`, `long` (whole numbers, 8/16/32/64-bit), `float`, `double` (decimals, 32/64-bit), `char` (a single UTF-16 character), `boolean` (`true`/`false`). Everything else — `String`, `List`, your own classes — is a reference type: a variable holding a pointer to an object on the heap, not the value itself.

```java
int accountId = 1;              // primitive — the value itself lives on the stack
BigDecimal balance = new BigDecimal("100.00"); // reference — the variable holds a pointer to a heap object
```

**Q: What are the control structures every Java program is built from?**
A: `if`/`else if`/`else`, `switch` (including the modern arrow form), `for`, `while`, `do-while`, and `for-each` for iterating a collection.

```java
for (Account a : accounts) {                    // for-each — iterate a collection
    if (a.getBalance().signum() < 0) continue;   // if/else + continue
    switch (a.getStatus()) {                      // modern switch expression
        case ACTIVE -> process(a);
        case FROZEN, CLOSED -> skip(a);
        default -> throw new IllegalStateException();
    }
}
```

**Q: What does Java's memory management actually look like, plain and complete?**
A: Two regions — **stack** (per-thread, local variables and call frames) and **heap** (shared, every `new` object, GC-managed) — full breakdown with the `String`/`StringBuilder` angle in Part 7 below, memory-leak debugging in `General_Backend_Engineering_QA.md`. The one-line version: you never manually `free()`/`delete` in Java; the tradeoff for that convenience is GC pause behavior becoming something you have to understand and tune under real load.

**Q: What's the basic shape of exception handling in Java?**
A: `try` the risky code, `catch` the specific exception type(s) you can meaningfully handle, `finally` for cleanup that must run either way (though try-with-resources, Part 7 below, replaced most manual `finally` blocks), and `throw`/`throws` to raise or declare an exception.

```java
try {
    account.withdraw(amount);
} catch (InsufficientFundsException e) {
    return ResponseEntity.status(409).body(e.getMessage()); // handle the specific, expected failure
} finally {
    auditLog.record("withdrawal attempt", accountId); // runs whether it succeeded or threw
}
```
Full checked-vs-unchecked distinction (and why it matters more than the mechanics above) is in Part 1 below — this is just the syntax shape; that's the judgment call layered on top of it.

---

## Part 1: Core Java

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

**Q: JDK vs. JRE vs. JVM — what's the actual relationship?**
A: **JVM** — the virtual machine that actually executes bytecode (what the previous answer describes). **JRE** — JVM + the standard library classes needed to *run* a compiled Java program, nothing to build one. **JDK** — JRE + the compiler (`javac`) and other dev tools, needed to *write and build* Java. You install a JDK to develop; a deployed production server historically only needed a JRE, though modern JDKs (11+) bundle everything and the standalone-JRE distribution has largely been discontinued — worth knowing the three names even though the JRE-only split isn't really a live packaging choice anymore.

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
A: `HashMap` — no ordering guarantee, O(1) average lookup. `LinkedHashMap` — preserves insertion order, same performance plus a small linked-list overhead. `TreeMap` — sorted by key, O(log n) operations (red-black tree backed). Full internals (how `HashMap` actually hashes/buckets, `Hashtable`/`ConcurrentHashMap`, implementing one from scratch) are in Part 8.

```java
Map<String, Account> byNumber = new HashMap<>();        // fastest, no order
Map<String, Account> insertionOrdered = new LinkedHashMap<>(); // remembers put() order
Map<String, Account> sorted = new TreeMap<>();           // always iterates key-sorted
```

---

## Part 2: Core OOP Concepts

The absolute baseline for a Java interview — likely to surface in some form even in a concept-level discussion, and worth being crisp on precisely because it's the kind of thing that's easy to *use* correctly by instinct but surprisingly easy to fumble explaining cleanly out loud, under pressure, for the first time in months.

**Q: What are the four pillars of OOP?**
A: **Encapsulation** — bundling data with the methods that operate on it, and hiding internal state behind a controlled interface. **Abstraction** — exposing only what's relevant to the caller, hiding implementation detail behind an interface or abstract class. **Inheritance** — a class acquiring fields/behavior from a parent class. **Polymorphism** — the same method call behaving differently depending on the actual runtime type of the object.

**Q: Give a concrete example of encapsulation — ideally from something you've actually built.**
A: In the `bank-demo` project, `Account.balance` is a private field with no public setter — the only way to change it from outside the class is through `credit()`/`debit()`. `Account` itself just mutates the number; the actual business rule ("can't go negative") is enforced one layer up, in `AccountService.withdraw()`, which checks the balance and throws `InsufficientFundsException` *before* ever calling `debit()`. Worth being precise about that split if asked to walk through it: encapsulation is what makes `balance` unreachable except through those two methods; the invariant itself lives in the service layer, not inside `Account`. If `balance` were public, nothing would stop a caller from setting it directly and bypassing the service-layer check entirely.

```java
// entity/Account.java — encapsulates the field, but doesn't itself enforce "can't go negative"
private BigDecimal balance;
public void credit(BigDecimal amount) { this.balance = this.balance.add(amount); }
public void debit(BigDecimal amount) { this.balance = this.balance.subtract(amount); } // no guard here

// service/AccountService.java — this is where the actual business rule lives
public AccountResponse withdraw(Long id, AmountRequest request, String idempotencyKey) {
    Account account = findAccountOrThrow(id);
    if (account.getBalance().compareTo(request.amount()) < 0) {
        throw new InsufficientFundsException(id); // the invariant check, one layer above the entity
    }
    account.debit(request.amount());
    ...
}
// account.balance = new BigDecimal("999999"); // would not even compile from outside the class
```

**Q: Abstract class vs. interface — what's the actual difference, and when do you pick one over the other?**
A: An **interface** defines a contract — method signatures with no implementation (though default methods have been allowed since Java 8) — and a class can implement multiple interfaces. An **abstract class** can hold state (fields), constructors, and a mix of implemented and unimplemented (`abstract`) methods — but a class can only extend one. Rule of thumb: use an interface to define *capability*; use an abstract class when there's genuinely shared state or shared implementation you want subclasses to inherit, not just a shared contract. Modern Java (and Spring especially) leans heavily toward interfaces — `AccountRepository extends JpaRepository` in `bank-demo` is exactly this: a contract with zero implementation written by you.

```java
interface NotificationService { void notify(Account account, String message); } // pure contract

abstract class BaseReport {
    protected final ReportRepository repo;         // shared state, inherited by every subclass
    BaseReport(ReportRepository repo) { this.repo = repo; }
    abstract String render();                       // each subclass fills this in differently
    void save() { repo.save(render()); }             // shared implementation, written once
}
```

**Q: Method overloading vs. method overriding?**
A: **Overloading** — multiple methods in the same class, same name, different parameter lists — resolved at *compile time* based on the arguments you pass ("compile-time polymorphism"). **Overriding** — a subclass supplying its own implementation of a method already defined in its parent — resolved at *runtime* based on the object's actual type ("runtime polymorphism"). Overriding is what lets you call a method on a variable declared as the parent type and get the child's behavior back.

```java
// overloading — same name, different parameter lists, decided at compile time
void deposit(BigDecimal amount) { ... }
void deposit(BigDecimal amount, String note) { ... }

// overriding — decided at runtime by the object's actual type
class Notification { void send(String msg) { System.out.println("generic: " + msg); } }
class SmsNotification extends Notification {
    @Override void send(String msg) { System.out.println("sms: " + msg); }
}
Notification n = new SmsNotification();
n.send("hi"); // prints "sms: hi" — runtime type wins, even though the variable is typed as Notification
```

**Q: What's the contract between `equals()` and `hashCode()`, and why does breaking it cause real bugs?**
A: If two objects are `.equals()`, they *must* return the same `hashCode()`. Break this — override `equals()` but forget `hashCode()` — and hash-based collections quietly misbehave: a `HashSet`/`HashMap` uses `hashCode()` first to find the right bucket, and only then calls `equals()` within that bucket, so two "equal" objects with different hash codes can end up treated as distinct. It compiles fine, often even looks fine in a quick manual test, and only breaks once you actually lean on a `HashSet`'s dedup guarantee — a genuinely sneaky bug class.

```java
class Account {
    private String accountNumber;
    @Override public boolean equals(Object o) {
        return o instanceof Account a && a.accountNumber.equals(this.accountNumber);
    }
    // forgot hashCode()! — two "equal" Accounts can land in different HashSet buckets
    // and BOTH end up in the set, silently breaking the dedup guarantee
    @Override public int hashCode() { return accountNumber.hashCode(); } // the fix — must stay consistent with equals()
}
```

**Q: Composition vs. inheritance — which should you default to?**
A: "Favor composition over inheritance" is the standard modern guidance: build a class by *holding* references to other objects and delegating to them, rather than extending a parent class purely to reuse its behavior. Inheritance couples you tightly to a parent's implementation (changes ripple downward, and deep hierarchies get fragile — the classic "fragile base class" problem); composition stays swappable and easier to test. This connects directly to Dependency Inversion in Part 3 — `AccountService` *holds* an `AccountRepository` (composition, injected) rather than extending some concrete repository base class.

```java
// composition — matches bank-demo's real AccountService
class AccountService {
    private final AccountRepository repository; // HOLDS a repository, doesn't extend one
    AccountService(AccountRepository repository) { this.repository = repository; } // swappable in tests
}
```

**Q: `static` vs. instance members?**
A: `static` belongs to the class itself — one copy shared across every instance, accessible without creating an object. Instance members belong to each individual object — every `new Account(...)` gets its own copy of `balance`. Rule of thumb: if something doesn't depend on a particular instance's state, it's a reasonable `static` candidate (utility methods, constants) — but reaching for `static` on things that *do* depend on instance state is a common way to accidentally wreck testability, since static state is hard to mock and easy to leak between tests.

```java
class Account {
    static final BigDecimal MIN_BALANCE = BigDecimal.ZERO; // one copy, shared by every Account
    private BigDecimal balance;                             // one copy PER Account instance
}
```

**Q: What does `final` do, in its three contexts?**
A: On a **variable** — the reference can't be reassigned after initialization (this is exactly what "supports immutability with `final` fields" means in the constructor-injection discussion in Part 10). On a **method** — it can't be overridden by a subclass. On a **class** — it can't be extended at all (`String` is the famous example).

```java
final AccountRepository repository; // variable — can't reassign after the constructor sets it
class Account {
    final void debit(BigDecimal amount) { ... } // method — no subclass can override this
}
final class Account { ... } // class — nothing can extend Account at all
```

**Q: What makes a class immutable, and why does it matter?**
A: All fields `private final`, no setters, and if a field is itself a mutable object (a `List`, a `Date`), the constructor makes a defensive copy rather than storing the caller's reference directly. Matters because immutable objects are inherently thread-safe — nothing to race over if nothing can change — and easier to reason about generally, since you never have to ask "could this have changed underneath me since I last read it."

```java
public final class AccountSummary {
    private final Long id;
    private final BigDecimal balance;
    private final List<String> tags; // mutable type — needs a defensive copy, not the caller's reference

    public AccountSummary(Long id, BigDecimal balance, List<String> tags) {
        this.id = id;
        this.balance = balance;
        this.tags = List.copyOf(tags); // defensive copy — caller mutating their list can't affect this object
    }
    // no setters anywhere — every field is set once, in the constructor, forever
}
```

---

## Part 3: SOLID Principles

Extremely common in Java interviews specifically, and builds directly on the OOP concepts above.

- **S — Single Responsibility Principle**: a class should have exactly one reason to change. `bank-demo` actually demonstrates this across three classes rather than one: `AccountController` only handles HTTP concerns, `AccountService` only handles business logic, `AccountRepository` only handles persistence.

```java
@RestController class AccountController { /* HTTP in/out only */ }
@Service class AccountService { /* business rules only */ }
interface AccountRepository extends JpaRepository<Account, Long> { /* persistence only */ }
```

- **O — Open/Closed Principle**: open for extension, closed for modification — you should be able to add new behavior without editing and re-testing existing code. E.g., a daily-withdrawal-limit rule would ideally be a new class implementing a small `WithdrawalRule` interface, plugged in alongside the existing logic, rather than more `if` statements bolted onto `Account.withdraw()`.

```java
// closed for modification — Account.withdraw() never changes as rules are added
interface WithdrawalRule { void check(Account account, BigDecimal amount); }
class DailyLimitRule implements WithdrawalRule { public void check(Account a, BigDecimal amt) { ... } }
// a NEW rule = a new class, plugged into the list — not another `if` in withdraw()
```

- **L — Liskov Substitution Principle**: a subtype must be usable anywhere its base type is expected, without breaking correctness. A concrete violation: a subclass overriding a method to throw an exception the base type's contract never promised — code written against the base type has no reason to expect it and breaks.

```java
class Notification { void send(String msg) { ... } }
class BrokenNotification extends Notification {
    @Override void send(String msg) { throw new UnsupportedOperationException(); } // violates LSP —
    // any code holding a `Notification` and calling send() has no reason to expect this to blow up
}
```

- **I — Interface Segregation Principle**: prefer several small, focused interfaces over one large general-purpose one, so implementers aren't forced to implement methods they don't need.

```java
// violates ISP — a read-only reporting client is forced to implement write()/delete() it'll never use
interface AccountOperations { Account read(Long id); void write(Account a); void delete(Long id); }

// segregated — implement only what you actually need
interface AccountReader { Account read(Long id); }
interface AccountWriter { void write(Account a); }
```

- **D — Dependency Inversion Principle**: high-level modules shouldn't depend directly on low-level modules — both should depend on abstractions. This is precisely what Spring's DI container exists to make easy (Part 10) — `AccountService` depends on the `AccountRepository` *interface*, never on a concrete implementation.

```java
class AccountService {
    private final AccountRepository repository; // depends on the INTERFACE, not JpaAccountRepositoryImpl
    AccountService(AccountRepository repository) { this.repository = repository; }
}
```

**Your bridge**: Go's implicit interface satisfaction already nudges you toward small, focused interfaces — ISP in Java is basically the Go idiom "the bigger the interface, the weaker the abstraction," just enforced explicitly with `implements` instead of structurally.

---

## Part 4: Concurrency

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

**Q: Your REST API takes 30 seconds because it calls three downstream services — how do you optimize it?**
A: First check whether the three calls actually depend on each other's results — if not, the 30 seconds is probably three sequential blocking calls that should be running concurrently instead. `CompletableFuture` fans them out and joins on whichever finishes last, turning "sum of three latencies" into "max of three latencies."

```java
// sequential — 30s total if each downstream call takes ~10s
var a = callServiceA(); // blocks ~10s
var b = callServiceB(); // blocks ~10s
var c = callServiceC(); // blocks ~10s

// concurrent — ~10s total, all three in flight at once
CompletableFuture<A> fa = CompletableFuture.supplyAsync(this::callServiceA);
CompletableFuture<B> fb = CompletableFuture.supplyAsync(this::callServiceB);
CompletableFuture<C> fc = CompletableFuture.supplyAsync(this::callServiceC);
CompletableFuture.allOf(fa, fb, fc).join();
var result = combine(fa.join(), fb.join(), fc.join());
```
If they genuinely must run in sequence (call 2 needs call 1's output), the next lever is cutting per-call latency itself — caching a slow-changing downstream response (Part 6 of `General_Backend_Engineering_QA.md`), or adding a timeout so one hanging dependency doesn't block the whole request indefinitely.

**Q: What's a race condition, and how do you prevent one in Java?**
A: When the outcome of concurrent operations depends on unpredictable timing/interleaving, producing incorrect results. Prevention: the `synchronized` keyword/blocks, `java.util.concurrent` locks (`ReentrantLock`), atomic classes (`AtomicInteger`, etc.), or — best of all — avoiding shared mutable state entirely via immutable objects or message-passing. Same philosophy as Go's "don't communicate by sharing memory, share memory by communicating." (The banking-specific version of this — account-balance races, optimistic vs. pessimistic locking — is in `Banking_Wealth_Domain_Playbook.md`, Scenario 6.)

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

## Part 5: Design Patterns — Spring Usage + From-Scratch Implementation

This is a **confirmed real OCBC question** — "What is the design pattern and how do you use it in Spring Boot?" (a design-pattern question generally, confirmed across two independent sources). Lead with the Proxy/AOP answer below since it's concrete and you can point at real code for it; then name 2–3 more by pattern name, showing both how Spring uses it *and* how you'd implement it by hand if asked to — that combination is what separates "I recognize the name" from "I actually understand it."

### Go-to answer: Proxy pattern (AOP)

Spring's answer to "add behavior around a method without touching its code." `@Transactional`, `@Cacheable`, `@Async`, `@PreAuthorize` — all the same mechanism: Spring wraps your bean in a dynamic proxy at startup. Two flavors:
- **JDK dynamic proxy** — bean implements an interface → proxy implements the same interface, delegates to the real object.
- **CGLIB proxy** — no interface → proxy subclasses your concrete class.

Call a `@Transactional` method → hits the proxy first → proxy opens a transaction → delegates to the real method → commits (or rolls back on an unchecked exception) → returns.

**Classic gotcha, worth knowing cold**: self-invocation bypasses the proxy. Call a `@Transactional` method from another method *in the same class* and you're calling `this.method()` directly, never through the proxy — the transaction silently doesn't apply. In `bank-demo`, `AccountService.java` has `@Transactional` on `createAccount()`, `deposit()`, `withdraw()`, and `transfer()` (lines 51, 68, 79, 107) — each only works because they're invoked from `AccountController`, *outside* the class, through the real proxy. (The idempotency helper, `executeIdempotent()`, is a private method called *from inside* those four — it deliberately has no `@Transactional` of its own, since it just runs inside whichever transaction the calling public method already opened.)

### Singleton
**Spring usage**: Spring bean default scope. One instance per container (not JVM-wide, per-`ApplicationContext`). `bank-demo`'s `ConsoleNotificationService`/`SmsNotificationService` get this free from `@Component` — no hand-written `private constructor + static getInstance()`.

**Implement it yourself** — worth having ready if an interviewer says "implement a singleton" rather than "does Spring use the singleton pattern," roughly in order from "commonly taught" to "actually recommended":

*Eager initialization* — thread-safe by construction, but the instance exists even if never used:
```java
public class Singleton {
    private static final Singleton INSTANCE = new Singleton();
    private Singleton() {}
    public static Singleton getInstance() { return INSTANCE; }
}
```

*Double-checked locking* — the classic "clever" answer, lazy-loaded, but easy to get subtly wrong:
```java
public class Singleton {
    private static volatile Singleton instance;
    private Singleton() {}
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```
The `volatile` keyword is not optional here — without it, another thread can observe a partially-constructed object due to instruction reordering.

*Enum singleton* — Joshua Bloch's (author of *Effective Java*) recommended approach, thread-safe for free and the only one safe against reflection/serialization attacks:
```java
public enum Singleton {
    INSTANCE;
    public void doSomething() { /* ... */ }
}
```

**The bridge**: "Spring beans are singletons by default" is quietly doing all of the above on your behalf — the IoC container handles this bookkeeping so you never hand-write it in application code.

### Factory
**Spring usage**: three places — `@Bean` methods in `@Configuration` classes (each one's a Factory Method); `ApplicationContext`/`BeanFactory` itself (you ask for a bean, don't know or care how it's built); `FactoryBean<T>` (a bean whose job is producing *other* beans).

**Implement it yourself**:
```java
interface Notification { void send(String message); }
class EmailNotification implements Notification { public void send(String m) { /* ... */ } }
class SmsNotification implements Notification { public void send(String m) { /* ... */ } }

class NotificationFactory {
    static Notification create(String type) {
        return switch (type) {
            case "email" -> new EmailNotification();
            case "sms" -> new SmsNotification();
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}
```

### Template Method
**Spring usage**: `JdbcTemplate`, `RestTemplate`, `TransactionTemplate`. Fixed skeleton (open connection, handle exceptions, close connection) lives in the template; you supply only the variable part (a `RowMapper`, a callback). "Template" in the class name isn't decoration — it's literally the pattern.

### Strategy
**Spring usage**: `bank-demo`'s `NotificationService` — `AccountService`'s constructor takes `List<NotificationService>`, Spring auto-collects every implementing bean (`ConsoleNotificationService` + `SmsNotificationService`), and deposit/withdraw/transfer fan out to all of them without knowing which channels exist.

**Implement it yourself** — a family of interchangeable algorithms behind a common interface, swapped at runtime:
```java
interface PricingStrategy { BigDecimal price(Order order); }
class StandardPricing implements PricingStrategy { /* ... */ }
class DiscountPricing implements PricingStrategy { /* ... */ }
```
Directly the same shape as swapping an implementation behind an interface in Go — a natural talking point if you've done this with the pricing-service work at Laku6 (`Self_Intro_And_Behavioral.md`).

### Repository
**Spring usage**: Spring Data JPA. `AccountRepository extends JpaRepository<Account, Long>` — you write an interface, zero implementation, Spring Data generates it at runtime. (Also secretly another Proxy pattern instance — the generated repository implementation *is* a dynamic proxy.) You already do the rough equivalent with GORM or sqlx.

### Observer
**Spring usage**: `ApplicationEventPublisher` + `@EventListener`/`ApplicationListener`. Publish an event, any number of decoupled listeners react — the publisher never knows who's listening.

**Where you've probably already used it without naming it**: any event listener — a button's `onClick`, a Kafka consumer reacting to a topic, a React component re-rendering when state changes — is the Observer pattern in practice.

### Builder
**Spring usage**: fluent object construction. `HttpSecurity`'s chained `.authorizeHttpRequests(...).csrf(...)`, `WebClient.builder()`, `ResponseEntity.ok().body(x)`.

**Implement it yourself** — solves the "constructor with eight optional parameters" problem:
```java
public class Order {
    private final String id;
    private final String customer;
    private final boolean express;

    private Order(Builder b) {
        this.id = b.id;
        this.customer = b.customer;
        this.express = b.express;
    }

    public static class Builder {
        private String id;
        private String customer;
        private boolean express;
        public Builder id(String id) { this.id = id; return this; }
        public Builder customer(String c) { this.customer = c; return this; }
        public Builder express(boolean e) { this.express = e; return this; }
        public Order build() { return new Order(this); }
    }
}
// usage: new Order.Builder().id("1").customer("Kevin").express(true).build();
```
Comparable to the functional options pattern in Go.

### Front Controller
**Spring usage**: `DispatcherServlet`. Single entry point for every HTTP request, routes to the right handler. This is Spring MVC's architecture-level pattern, one level up from any single annotation.

### Adapter
**Spring usage**: `HandlerAdapter`. Lets `DispatcherServlet` invoke wildly different handler types (`@Controller` methods, the old-style `Controller` interface) through one uniform interface.

**General shape**: converts one interface into another that a client expects, letting otherwise-incompatible interfaces work together — e.g., wrapping a third-party library's awkward API behind an interface matching the rest of your codebase's conventions.

### Decorator
Wraps an object to add behavior without modifying its class or affecting other instances of it. Java's I/O classes are the textbook example — `new BufferedReader(new FileReader(file))`, where each layer adds behavior around the one it wraps. Worth recognizing by name even if you haven't hand-implemented one recently — no Spring-specific angle needed here, but it shows up constantly in library design.

---

## Part 6: Java 8+ Features (Lambdas, Streams, Optional)

If the Java you last used seriously predates 2014, this is very likely the single biggest "things have moved on" gap — and these come up constantly in modern Java interviews, independent of Spring.

**Q: What's a lambda expression?**
A: A concise way to write an instance of a functional interface inline, without a named class:
```java
// before lambdas
Comparator<String> byLength = new Comparator<String>() {
    public int compare(String a, String b) { return a.length() - b.length(); }
};
// with a lambda
Comparator<String> byLength = (a, b) -> a.length() - b.length();
```

**Q: What's a functional interface?**
A: An interface with exactly one abstract method (default/static methods don't count against that) — this single-method constraint is what makes it possible for a lambda to stand in for an instance of it. `Runnable`, `Comparator`, and the `java.util.function` package (`Function<T,R>`, `Predicate<T>`, `Supplier<T>`, `Consumer<T>`) are the common ones. `@FunctionalInterface` is an optional annotation that documents intent and gets the compiler to enforce the one-method rule.

**Q: What's the Streams API, and what problem does it solve?**
A: A way to process collections declaratively — describing *what* transformation you want, rather than writing an imperative loop for *how* to do it:
```java
List<String> names = accounts.stream()
    .filter(a -> a.getBalance().compareTo(BigDecimal.ZERO) > 0)
    .map(Account::getOwnerName)
    .sorted()
    .collect(Collectors.toList());
```
Same underlying instinct as functional-style chaining you've likely used over Go slices or JS's `.filter().map()` — the syntax differs, the mental model doesn't.

**Q: What's `Optional`, and what problem is it actually solving?**
A: A container that may or may not hold a value, used as a return type to make "this might not have a result" explicit in the method signature — instead of returning `null` and hoping every caller remembers to check. This isn't hypothetical for you: `accountRepository.findById(id)` in `bank-demo` returns `Optional<Account>` for exactly this reason, and `.orElseThrow(...)` in `AccountService.findAccountOrThrow()` is `Optional` doing real work in code you already have.

**Q: What's a method reference, and how does it relate to a lambda?**
A: Shorthand for a lambda that does nothing but call an existing method — `Account::getOwnerName` instead of `a -> a.getOwnerName()`. Purely syntactic sugar for that one specific case.

---

## Part 7: Strings, Memory & Other Core Java

**Q: Why is `String` immutable in Java, and what's the "String pool"?**
A: Once created, a `String`'s value can never change — `.concat()`, `.toUpperCase()`, and similar all return a *new* `String` rather than modifying the original. This immutability is what enables the **String pool**: a special memory area where literal strings are cached and reused, since an immutable value is always safe to share. `String a = "hi"; String b = "hi";` — both references point to the *same* pooled object. It's also why `String` is safe to use as a `HashMap` key without risking the map's internal structure breaking if a key's hash changed after insertion — it can't change.

**Q: `String` vs. `StringBuilder` vs. `StringBuffer`?**
A: `String` is immutable — every concatenation in a loop creates a new object, which gets expensive fast. `StringBuilder` is mutable and built for exactly that case — repeated modification without the object churn — but isn't thread-safe. `StringBuffer` is the same idea with synchronized methods, trading some performance for thread-safety. In practice, `StringBuilder` is the default choice unless you specifically need thread-safety, and even then a proper concurrency primitive is often a better fit than reaching for `StringBuffer`.

**Q: Stack vs. heap in the JVM — what actually lives where?**
A: Each thread has its own **stack**, holding local variables and method call frames — primitives and object *references* live here, automatically cleaned up as methods return. The **heap** is shared across all threads and holds every object created with `new` — this is what the garbage collector manages. `Account account = new Account(...)` puts the reference `account` on the stack, but the actual `Account` object itself lives on the heap.

**Q: What are Java's access modifiers, from most to least restrictive?**
A: `private` (this class only) → *default* / package-private, no keyword at all (same package only) → `protected` (same package, plus subclasses anywhere) → `public` (anywhere). Worth knowing "default" is a real, easily-forgotten access level — omitting a modifier isn't accidental public access, it's its own deliberate level.

**Q: What do `this` and `super` actually do?**
A: `this` refers to the current instance — commonly used to disambiguate a field from a constructor parameter sharing its name (`this.balance = balance;`), or to call another constructor in the same class (`this(...)`, which must be the first line if used). `super` refers to the parent class — `super(...)` calls the parent's constructor (also must be the first line if used), and `super.methodName()` calls the parent's version of a method you've overridden.

**Q: What's try-with-resources, and what does a class need to support it?**
A: A `try` block that automatically closes a resource when the block exits — success or failure — with no `finally { resource.close(); }` boilerplate needed:
```java
try (Connection conn = dataSource.getConnection()) {
    // use conn
} // conn.close() runs automatically here, even if an exception was thrown
```
Requires the resource to implement `AutoCloseable` (or its subtype, `Closeable`).

**Q: What's an `enum` good for beyond a fixed list of constants?**
A: A fixed set of named instances that are genuine objects — an enum can have fields, a constructor, and methods, even different behavior per constant. It's type-safe (the compiler rejects an invalid value, unlike a raw `String` or `int` "status code" would), and — per Part 5 above — it's the basis of the recommended modern Singleton implementation.

**Q: Your application suddenly starts throwing `OutOfMemoryError` — how would you debug it?**
A: First distinguish *which* OOM — the message names it. `Java heap space` — the heap itself is exhausted, the common one. `GC overhead limit exceeded` — the JVM is spending almost all its time GC'ing and reclaiming almost nothing, a symptom of a near-full heap rather than a separate problem. `Metaspace` — class metadata exhausted, common with dynamic class generation or a classloader leak on repeated hot-redeploys. For heap exhaustion specifically: capture a heap dump (`-XX:+HeapDumpOnOutOfMemoryError` so one is captured automatically the moment it happens, rather than trying to reproduce it live), open it in a profiler (Eclipse MAT, VisualVM), and look at the dominator tree — what's retaining the most memory, and via what reference chain. Classic Java-specific causes: a `static` collection that keeps growing (nothing ever evicts from it), a cache with no size bound or eviction policy, `ThreadLocal` values never cleared (especially dangerous in a pooled-thread server, since the thread — and its `ThreadLocal` — outlives any single request), or listeners/subscribers registered but never deregistered.

```java
// classic static-collection leak — this map has no eviction, no TTL, no bound
private static final Map<String, Session> sessions = new HashMap<>(); // grows forever, never shrinks

// ThreadLocal leak — pooled threads reuse the same Thread, so a never-cleared
// ThreadLocal silently keeps last request's data alive on that thread, forever
private static final ThreadLocal<User> currentUser = new ThreadLocal<>();
// ... must call currentUser.remove() at the end of every request, or it leaks
```

---

## Part 8: Collections Framework Deep Dive

This came back consistently across current interview-question sources as one of the highest-frequency Core Java topics — worth being as fluent here as in OOP itself.

**Q: `Comparable` vs. `Comparator` — what's the difference, and when do you use each?**
A: `Comparable` (the `compareTo()` method) defines a class's *natural* ordering, implemented on the class itself — so there's exactly one default sort order (`Integer` and `String` both implement it). `Comparator` (the `compare()` method) defines an *external*, custom ordering — you can write as many different `Comparator`s as you need for the same class without touching the class itself. Rule of thumb: `Comparable` when a class has one obvious natural order; `Comparator` when you need to sort the same objects several different ways depending on context — accounts by balance on one screen, by owner name on another.

**Q: What's the difference between fail-fast and fail-safe iterators?**
A: **Fail-fast** iterators (the default for `ArrayList`, `HashMap`, and most of `java.util`) detect structural modification during iteration and throw `ConcurrentModificationException` immediately, rather than risk returning corrupted results. **Fail-safe** iterators (used by `java.util.concurrent` collections like `ConcurrentHashMap` and `CopyOnWriteArrayList`) iterate over a snapshot or a segmented structure instead, so concurrent modification doesn't throw — the tradeoff is the iterator may not reflect the very latest state, and fail-safe collections generally cost more memory or write overhead to provide that guarantee.

**Q: How would you safely remove elements from a list while iterating over it?**
A: Use the iterator's own `remove()` method, not the collection's:
```java
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (shouldRemove(it.next())) {
        it.remove();
    }
}
```
Calling `list.remove(...)` directly inside a for-each loop is exactly what triggers `ConcurrentModificationException`.

**Q: At a high level, how does `HashMap` actually work internally?**
A: Keys are hashed (`hashCode()`) to determine which "bucket" they land in; collisions within a bucket are handled as a linked list — and since Java 8, a bucket that grows past a threshold (and holds `Comparable` keys) converts to a balanced tree, improving worst-case lookup from O(n) to O(log n) for that bucket. This connects straight back to the `equals()`/`hashCode()` contract from Part 2 — an inconsistent implementation of that contract is the single most common root cause of a `HashMap` silently behaving wrong.

**Q: What are the differences between `Hashtable` and `HashMap`?** *(confirmed real OCBC question, Jul 2026)*
A: `Hashtable` is the legacy (pre-Java-2) synchronized map — every method locks the whole table, same throughput problem as `Collections.synchronizedMap` below, and it doesn't allow `null` keys or `null` values (throws `NullPointerException` if you try). `HashMap` is the modern, unsynchronized replacement — faster in single-threaded use, allows one `null` key and multiple `null` values, and is the correct default choice today. `Hashtable` shows up almost exclusively in legacy codebases now; naming `ConcurrentHashMap` as the modern answer for "I need this thread-safe" (not `Hashtable`) is the answer that signals current knowledge rather than a decade-old textbook.

```java
Hashtable<String, Account> legacy = new Hashtable<>();
legacy.put(null, account); // throws NullPointerException — Hashtable rejects null keys outright

HashMap<String, Account> modern = new HashMap<>();
modern.put(null, account); // fine — HashMap allows exactly one null key
```

**Q: Design a HashMap.** *(confirmed real OCBC question, Jul 2026 — a real "implement this" ask, not just "explain how it works")*
A: The core structure: an array of buckets, each bucket holding entries that hashed to the same index (a linked list is enough for an interview-level implementation; production `HashMap` upgrades a long bucket to a tree, per the internals question above, but that's an optimization detail, not the core idea). Three operations to get right: hash-and-index on `put`/`get`, collision handling within a bucket, and resizing once the map gets too full (load factor).

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
Talking points while walking through it: average-case `O(1)` for `put`/`get` assuming a decent hash function spreads keys evenly across buckets; worst case `O(n)` if every key collides into one bucket (a broken/constant `hashCode()` — ties directly back to the `equals()`/`hashCode()` contract question); resizing amortizes to `O(1)` per operation even though any single resize is `O(n)`, the same amortized-cost argument as a Go slice's `append` growing its backing array.

**Q: When would you reach for `ConcurrentHashMap` over a synchronized `HashMap`?**
A: `Collections.synchronizedMap(new HashMap<>())` locks the *entire* map on every operation — simple, but serializes all access and becomes a bottleneck under real concurrency. `ConcurrentHashMap` locks at a much finer grain internally, allowing genuinely concurrent reads and writes to different parts of the map, and offers atomic compound operations (`compute()`, `merge()`, `putIfAbsent()`) that would otherwise need external synchronization to do safely — a typical use case is an in-process cache or counter map that many request threads read and update at once.

Worth knowing when *not* to reach for it, too: `bank-demo`'s actual idempotency mechanism (`IdempotencyRecordRepository`, backed by an `idempotency_records` table with `idempotencyKey` as the primary key) deliberately does **not** use a `ConcurrentHashMap` for this. A database-enforced unique constraint survives a restart and works correctly across multiple instances of the service running behind a load balancer; an in-memory `ConcurrentHashMap` would lose every idempotency record the moment the process restarted, and wouldn't be shared across instances at all. `ConcurrentHashMap` solves in-process thread-safety, not distributed, durable uniqueness, and a real payment idempotency check needs the latter (`Banking_Wealth_Domain_Playbook.md`, Scenario 2).

---

## Part 9: Modern Java (17–21) Features

Worth at least recognizing by name — current interview-prep sources note these increasingly show up even in concept-level rounds, since the interview is happening on a Java 21 timeline regardless of which version the codebase you'd inherit actually targets.

**Q: What's a `record`?**
A: A concise way to declare an immutable data-carrier class — the compiler generates the constructor, `equals()`, `hashCode()`, `toString()`, and accessors for you:
```java
public record AccountSummary(Long id, String ownerName, BigDecimal balance) {}
```
Good instinct-check: if asked to "make this class immutable" today, in modern Java a record is usually the right reach — `bank-demo`'s own `AccountResponse` already *is* one (`public record AccountResponse(Long id, String accountNumber, ...)`), which is worth pointing out directly if it comes up: no hand-written constructor, accessors, `equals()`, or `hashCode()` anywhere in that file.

**Q: What's a sealed class or interface?**
A: Restricts which classes are allowed to extend or implement it, declared explicitly:
```java
public sealed interface Shape permits Circle, Square {}
```
Useful when you want the compiler to guarantee every possible subtype has been handled somewhere — pairs naturally with pattern matching in a `switch`, since the compiler can verify exhaustiveness for you.

**Q: What's pattern matching for `switch`?**
A: Lets a `switch` match on an object's *type* directly, instead of a chain of `instanceof` checks and manual casts:
```java
String describe(Object obj) {
    return switch (obj) {
        case Integer i -> "int: " + i;
        case String s -> "string: " + s;
        default -> "unknown";
    };
}
```

**Q: What are virtual threads, and why mention them again here?**
A: Already covered in Part 4 under Project Loom — worth reconnecting as part of the same "modern Java" cluster: lightweight, JVM-managed threads that close the gap between Java's traditional heavyweight-thread model and Go's goroutines, and one of the most consistently cited "2026 interview" topics across current sources.

---

## Part 10: Spring Core

**Q: Why do we need to use Spring?** *(confirmed real OCBC question, word for word)*
A: Two layers to a complete answer — most people only give the first one. **Layer 1, IoC/DI**: without a container, you're wiring every object graph by hand (`new AccountService(new AccountRepository(), new EmailNotifier())`), every dependency is a hardcoded concrete class, and swapping an implementation or mocking one for a test means touching every place it's constructed. Spring's container builds and wires that graph for you from annotations. **Layer 2 — and this is the part "why not just do DI myself" misses**: Spring isn't only a DI container, it's a coherent, battle-tested answer to nearly everything else a real backend needs, so you're not hand-rolling or gluing together separate libraries for each: Spring MVC (REST/web layer), Spring Data (repository abstraction over JDBC/JPA — write an interface, get a working implementation), declarative transaction management via AOP (`@Transactional` — Part 5), Spring Security, and Spring Boot's auto-configuration + embedded server (`java -jar`, no XML, no separate Tomcat install) plus Actuator (production health/metrics endpoints out of the box). `bank-demo` is the proof: a REST API, JPA persistence, declarative transactions, bean validation, centralized exception handling, and auto-generated OpenAPI docs — wired together with a handful of annotations and almost no boilerplate. The realistic alternative to "use Spring" isn't "no framework," it's assembling that same list of concerns from separate, less-integrated pieces yourself.

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

**Q: What are the Spring bean scopes, and what's the difference between a Singleton and a Prototype bean?** *(the Singleton-vs-Prototype framing is a confirmed real OCBC question, Jul 2026)*
A: `singleton` (the default) — the container creates exactly one instance for the whole application context, and every injection point gets that same shared instance. `prototype` — the container creates a brand-new instance every single time one is requested/injected, and critically, Spring hands it off and stops managing its lifecycle after that (no `@PreDestroy` callback, unlike singleton beans). Web-specific scopes also exist: `request` (one per HTTP request) and `session` (one per HTTP session). Reach for `prototype` specifically when a bean holds per-use mutable state that must never be shared across callers — a stateful builder-like object, for instance; every other bean in `bank-demo` (`AccountService`, the `NotificationService` implementations) is a singleton, which is the right default the overwhelming majority of the time.

```java
@Service                       // singleton (default) — one instance, shared by every class that needs it
public class AccountService { }

@Scope("prototype")            // a NEW instance every time this is injected or requested from the context
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

**Q: Field injection vs. constructor injection — which do you prefer, and why?** *(confirmed to come up directly, word for word, across multiple OCBC reports as "there are two types of Autowired, explain what they are" — know this one cold)*
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

**Q: A circular dependency error appears after deployment — what causes it, and how do you fix it?**
A: Bean A's constructor needs Bean B, and Bean B's constructor needs Bean A — Spring can't construct either first, and throws `BeanCurrentlyInCreationException` at startup (not a compile-time error, since the cycle is only visible once Spring tries to build the object graph). It's usually a sign two things are too tightly coupled and should be redesigned — extract the shared piece both depend on into a third bean, or invert one of the two dependencies. If a genuine, unavoidable cycle exists (rare, and usually still a design smell), `@Lazy` on one of the constructor parameters defers that dependency's actual resolution until it's first used instead of at construction time, breaking the deadlock.

```java
// circular — A needs B, B needs A, neither can be constructed first
@Service class ServiceA { ServiceA(ServiceB b) {} }
@Service class ServiceB { ServiceB(ServiceA a) {} }
// -> BeanCurrentlyInCreationException: Error creating bean with name 'serviceA'

// quick unblock — defers resolving ServiceA until first actual use, not at construction
@Service class ServiceB { ServiceB(@Lazy ServiceA a) {} }
// real fix: usually means A and B are too coupled — extract the shared logic into a ServiceC both depend on
```

**Q: `@Bean` vs. `@Component` — what's the actual difference, and why does `bank-demo` have zero `@Bean` methods?**
A: Both register a bean with the container, but at different points of control. `@Component` (and its specializations `@Service`/`@Repository`/`@RestController`) goes *on the class itself* — you're annotating code you own, and Spring finds it via component-scanning. `@Bean` goes on a *method* inside an `@Configuration` class — you write the method body yourself, and whatever it returns becomes the bean. That extra level of control is exactly what `@Bean` is for, and neither reason to reach for it applies in this project: (1) wiring a **third-party class** you don't own and can't put an annotation on (an external SDK's client, a library's HTTP client builder) — every class in `bank-demo` (`AccountService`, `ConsoleNotificationService`, etc.) is the project's own code, so a stereotype annotation works directly; (2) a bean whose construction needs real **imperative logic** — conditional setup, values pulled from config, calling a builder with multiple steps — that a bare constructor can't express. Every bean here is a plain `new SomeClass(dependencies)` with nothing conditional about it, so there's nothing for a `@Bean` method to add.

```java
// @Component — goes on your own class, Spring finds it by scanning the package
@Service
public class AccountService { AccountService(AccountRepository repo) { ... } }

// @Bean — goes on a method, for exactly the two cases above
@Configuration
public class ThirdPartyConfig {
    @Bean
    public S3Client s3Client(@Value("${aws.region}") String region) {
        // S3Client is AWS SDK code — can't put @Component on a class you don't own
        // and construction needs a builder + a config value, not a bare constructor
        return S3Client.builder().region(Region.of(region)).build();
    }
}
```
If `bank-demo` ever called a real third-party payment gateway SDK, *that* client is where a `@Bean` method would first show up — worth saying out loud if asked, since it shows you know *when* to reach for it, not just what it does.

---

## Part 11: Spring Boot

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
A: A set of production-ready endpoints exposed out of the box for monitoring and management — health checks, metrics, environment info, thread dumps. `/actuator/health` is exactly the endpoint you'd hit first when checking "is the system down" (`Banking_Wealth_Domain_Playbook.md`, Scenario 1).

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

**Q: What's Spring Batch, and why would a wealth/trading backend care?**
A: Spring's framework for **batch processing**: reading large volumes of data, processing/transforming it, writing it out — with built-in chunking, retry, skip logic, and job restart. Think: end-of-day reconciliation jobs, overnight settlement processing, bulk pricing updates — exactly the kind of job a wealth/trading backend runs regularly (named directly in the JD). If you've written any Go batch/cron job that processes records in chunks with retry logic, that's the same shape of problem.

**Q: Your scheduled job executes multiple times after deploying multiple instances — why?**
A: `@Scheduled` runs independently on every instance that has the bean — Spring has no built-in awareness that four instances of the same service are all running the same JVM-local cron, so a job meant to run "once per interval" ends up running once *per instance*, every interval. Invisible with one instance; appears the moment you scale out. Fix: a distributed lock so only one instance's execution actually proceeds per scheduled firing — either a library built for exactly this (ShedLock, wrapping the method and backing the lock with a shared DB table or Redis), or hand-rolled via a DB row with `SELECT ... FOR UPDATE`/a unique constraint that only one instance can successfully claim per interval.

```java
@Scheduled(cron = "0 0 * * * *")
@SchedulerLock(name = "reconcileDailyBalances", lockAtLeastFor = "5m", lockAtMostFor = "30m")
void reconcileDailyBalances() {
    // ShedLock ensures only ONE instance actually runs this per scheduled firing, others skip it
}
```

**Q: Your Spring Boot application's startup time increased from 15 seconds to 2 minutes — how would you investigate?**
A: Turn on startup timing first (`--debug`, or `spring.startup.enabled=true` + Actuator's `/actuator/startup` endpoint in newer Boot versions) — it breaks down exactly how long each auto-configuration and bean initialization step took, instead of guessing. Common real causes, roughly in order of likelihood: a new dependency pulled in heavyweight auto-configuration that didn't exist before (check what actually changed in `pom.xml` since it was fast); a bean doing real work in its constructor or an `@PostConstruct`/`@EventListener(ApplicationReadyEvent)` — a slow network call, a large file read, an eager cache warm — that used to be fast (dev DB) and is now slow (real network to prod DB/dependency); component-scanning a much larger package tree than needed; or Liquibase/Flyway running a large new migration on every boot. The fix follows directly from wherever the timing breakdown points — it's rarely "Spring itself got slow."

---

## Part 12: Spring Data JPA

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

## Part 13: Transactions

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
A: Control what one transaction can see of another's uncommitted or concurrent changes, from weakest to strongest: `READ_UNCOMMITTED` (allows dirty reads) → `READ_COMMITTED` → `REPEATABLE_READ` → `SERIALIZABLE` (fully isolated, most expensive). Most databases default to `READ_COMMITTED`. Directly relevant to the race-condition scenario in `Banking_Wealth_Domain_Playbook.md`.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void closeOfDayReconciliation() {
    // strongest isolation — nobody else's concurrent change can be seen mid-transaction
}
```

---

## Part 14: REST / Spring MVC

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

**Q: A Spring Boot service starts returning random 500 errors — where do you begin?**
A: "Random" (not consistently reproducible on a given input) points away from a plain logic bug and toward something environmental or a race condition — that distinction shapes where you look next. Start with the actual stack trace in logs: a 500 with no caught, specific exception type means it fell through to the catch-all handler (the `Exception.class -> 500` branch in `GlobalExceptionHandler`), so the real cause is whichever exception is in that log line, not "500" itself. Common actual causes behind *random* 500s, roughly in order of likelihood: a connection pool exhausted under concurrent load (intermittent, load-dependent); a downstream dependency timing out occasionally, not always; an `OptimisticLockingFailureException` under concurrent writes to the same row (also intermittent by nature — Part 13 below); or a null value that only appears for certain rows (a field nullable in the DB that the code assumed was always populated). A bug that fails 100% of the time on a specific input is a logic bug to fix directly; one that fails occasionally under load is almost always concurrency or a flaky dependency, and the fix looks completely different.

---

## Part 15: Testing

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
Framework names to have ready rather than saying "some testing library": JUnit 5 for test structure and assertions, Mockito for mocking, `@SpringBootTest` or the more targeted "slice" annotations (`@WebMvcTest`, `@DataJpaTest`) for integration tests.

---

## Part 16: Microservices / Resilience

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
A: Spring Boot Actuator + Micrometer. Micrometer is a vendor-neutral metrics facade (think SLF4J, but for metrics), and Actuator exposes a `/actuator/prometheus` endpoint a Prometheus server can scrape. This is exactly the toolchain that answers "how do you check if the system is down" from `Banking_Wealth_Domain_Playbook.md`.

```bash
curl http://localhost:8080/actuator/prometheus | grep http_server_requests
# http_server_requests_seconds_count{method="GET",uri="/accounts",status="200"} 42.0
```

---

## Part 17: Security Basics

**Q: Authentication vs. authorization — and where does Spring Security fit?**
A: Authentication = confirming *who you are* (login). Authorization = confirming *what you're allowed to do* once authenticated (permissions/roles). Spring Security handles both via a filter chain intercepting requests before they reach your controllers — commonly configured with JWT-based authentication for stateless REST APIs (no server-side session), validating the token on each request and populating a `SecurityContext` used downstream for authorization checks like `@PreAuthorize`. (General security fundamentals — OWASP Top 10, XSS, CSRF, SQL injection, CORS, JWT structure, password hashing — are framework-agnostic and live in `General_Backend_Engineering_QA.md`, not here.)

```java
@PreAuthorize("hasRole('ADMIN')")     // authorization — checked after the token is already validated
@DeleteMapping("/accounts/{id}")
public void deleteAccount(@PathVariable Long id) { ... }

http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/accounts/**").authenticated()  // authentication — must present a valid token
    .anyRequest().permitAll());
```

---

## Part 18: Docker & Kubernetes

*Named directly in the JD ("containerisation/orchestration technologies (Docker, Kubernetes)").* If you already containerize your Go/Node services for deployment, most of this will feel familiar; the vocabulary below is what to have ready either way.

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

## Part 19: Liquibase

*Named explicitly in the JD's DevOps toolchain list.*

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

## Part 20: Quick-Fire Round
Short, direct answers you should be able to give in under 15 seconds each:

- **What's the default HTTP method Spring Boot Actuator's `/health` responds to?** GET.
- **What annotation turns a class into a REST controller?** `@RestController` (combines `@Controller` + `@ResponseBody`).
- **What's `@Value` used for?** Injecting a single configuration property from `application.yml`/`.properties` into a field — e.g. `@Value("${server.port}") private int port;`.
- **What's the difference between `@RequestParam` and `@PathVariable`?** `@RequestParam` reads a query string parameter (`?id=5` → `@RequestParam Long id`); `@PathVariable` reads a segment of the URL path (`/users/{id}` → `@PathVariable Long id`).
- **What does `@ConditionalOnProperty` do?** Only registers a bean/config if a specified property is set to a given value — e.g. `@ConditionalOnProperty("feature.new-pricing.enabled")` — core to how Spring Boot auto-configuration decides what to wire up.
- **What's the purpose of a DTO vs. an Entity?** Entities map to DB tables and carry persistence concerns (`@Entity class Account`); DTOs shape data specifically for API request/response payloads (`record AccountResponse(...)`), decoupling your API contract from your DB schema.
