# Technical Fundamentals — Gap Fill
For: OCBC Full Stack Developer Technical Interview

A final pass specifically hunting for gaps against the JD and against what a "full stack" technical round typically covers beyond Spring/React themselves. Everything here is either named directly in the JD (JavaScript, HTML5, CSS3, Jenkins, Bitbucket, Swagger/OpenAPI, MQ/Kafka) or is foundational enough (security, core OOP, SOLID) that it's a real risk to leave unprepared for a Java role specifically — OOP and SOLID especially, since they're the most likely "explain a concept" fallback questions if an interviewer wants to probe fundamentals beyond Spring itself.

---

## Table of Contents
- Part 1: Security Fundamentals (Bank-Relevant)
- Part 2: Core OOP Concepts (The Four Pillars + Classic Interview Questions)
- Part 3: SOLID Principles
- Part 4: Classic Design Patterns — Implemented in Java
- Part 5: Java 8+ Features (Lambdas, Streams, Optional)
- Part 6: Strings & Memory Basics
- Part 7: Other Core Java Worth Refreshing
- Part 8: Collections Framework Deep Dive
- Part 9: Modern Java (17–21) Features
- Part 10: Core JavaScript Fundamentals
- Part 11: HTML5 & CSS3 Fundamentals
- Part 12: Kafka & Messaging Deep Dive
- Part 13: CI/CD & Git Basics
- Part 14: API Documentation — Swagger/OpenAPI
- Part 15: Testing Frameworks, Named Correctly

---

## Part 1: Security Fundamentals (Bank-Relevant)

This is the single biggest gap across your other docs — worth taking seriously given the employer.

**Q: What are the OWASP Top 10, and why would it matter for this role?**
A: OWASP maintains the industry-standard list of the most critical web application security risks. It was significantly updated in late 2025 (first major revision since 2021) — current list, in order: **Broken Access Control, Security Misconfiguration, Software Supply Chain Failures, Cryptographic Failures, Injection, Insecure Design, Authentication Failures, Software/Data Integrity Failures, Security Logging and Alerting Failures, and Mishandling of Exceptional Conditions.** For a bank, these aren't abstract — Broken Access Control is directly "can customer A see customer B's account," and Cryptographic Failures is directly "is money-movement data properly encrypted at rest and in transit." You don't need to recite the list verbatim, but recognizing a couple of these by name if security comes up will land well.

**Q: What's Cross-Site Scripting (XSS)?**
A: An attacker injects malicious script into a page that other users view — commonly through an unsanitized input field that gets rendered back into the page. Defense: escape/encode output based on context. React actually escapes text content by default when rendering — the real risk shows up specifically when that protection is deliberately bypassed (e.g., `dangerouslySetInnerHTML`).

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
A: Cross-Origin Resource Sharing — a *browser-enforced* rule that blocks a web page from calling a different origin (domain/port/protocol) than the one that served it, unless the server explicitly allows it via response headers. Worth knowing it's a browser protection, not a server-side security control on its own — the server still needs its own authorization checks regardless of CORS configuration.

```
Access-Control-Allow-Origin: https://app.bank.com
Access-Control-Allow-Methods: GET, POST
```

**Q: JWT vs. traditional session-based auth?**
A: A JWT is self-contained — claims are encoded and signed directly in the token, so a server can validate it without a lookup, which is why it scales well statelessly across multiple servers. Session-based auth keeps state server-side, with the client holding only a reference (session ID). Tradeoff: JWTs are harder to revoke early since nothing is being tracked server-side — usually mitigated with short expiry plus a refresh-token flow.

```
JWT (self-contained, no DB lookup needed to validate):
header.payload.signature
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJrZXZpbiIsImV4cCI6MTcyMTY1NjAwMH0.signature-here

Session (server holds the real data, client holds just a reference):
Cookie: sessionId=abc123   →  server-side store: { abc123: { userId: 1, role: "user" } }
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

## Part 2: Core OOP Concepts

The absolute baseline for a Java interview — likely to surface in some form even in a concept-level discussion, and worth being crisp on precisely because it's the kind of thing that's easy to *use* correctly by instinct but surprisingly easy to fumble explaining cleanly out loud, under pressure, for the first time in months.

**Q: What are the four pillars of OOP?**
A: **Encapsulation** — bundling data with the methods that operate on it, and hiding internal state behind a controlled interface. **Abstraction** — exposing only what's relevant to the caller, hiding implementation detail behind an interface or abstract class. **Inheritance** — a class acquiring fields/behavior from a parent class. **Polymorphism** — the same method call behaving differently depending on the actual runtime type of the object.

**Q: Give a concrete example of encapsulation — ideally from something you've actually built.**
A: In the `bank-demo` project, `Account.balance` is a private field with no public setter — the only way to change it is through `deposit()`/`withdraw()`, which enforce the "can't go negative" rule. If `balance` were public, nothing would stop a caller from setting it directly and bypassing that rule entirely. That's encapsulation doing real work, not just a textbook definition.

```java
private BigDecimal balance;               // private — nothing outside this class can touch it directly
public void withdraw(BigDecimal amount) {
    if (amount.compareTo(balance) > 0) throw new InsufficientFundsException();
    balance = balance.subtract(amount);   // the ONLY path that can ever change balance
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
A: "Favor composition over inheritance" is the standard modern guidance: build a class by *holding* references to other objects and delegating to them, rather than extending a parent class purely to reuse its behavior. Inheritance couples you tightly to a parent's implementation (changes ripple downward, and deep hierarchies get fragile — the classic "fragile base class" problem); composition stays swappable and easier to test. This connects directly to Dependency Inversion below — `AccountService` *holds* an `AccountRepository` (composition, injected) rather than extending some concrete repository base class.

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
A: On a **variable** — the reference can't be reassigned after initialization (this is exactly what "supports immutability with `final` fields" meant in the constructor-injection discussion in your Spring doc). On a **method** — it can't be overridden by a subclass. On a **class** — it can't be extended at all (`String` is the famous example).

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

- **D — Dependency Inversion Principle**: high-level modules shouldn't depend directly on low-level modules — both should depend on abstractions. This is precisely what Spring's DI container exists to make easy — `AccountService` depends on the `AccountRepository` *interface*, never on a concrete implementation, and connects directly to constructor-injection-over-field-injection from your Spring doc.

```java
class AccountService {
    private final AccountRepository repository; // depends on the INTERFACE, not JpaAccountRepositoryImpl
    AccountService(AccountRepository repository) { this.repository = repository; }
}
```

**Your bridge**: Go's implicit interface satisfaction already nudges you toward small, focused interfaces — ISP in Java is basically the Go idiom "the bigger the interface, the weaker the abstraction," just enforced explicitly with `implements` instead of structurally.

---

## Part 4: Classic Design Patterns — Implemented in Java

Your Consolidated Prep doc already covers these patterns framed around *Spring's* use of them ("Spring beans are singletons by default"). This is the other half — actually implementing them yourself, which is the version worth having ready if an interviewer says "implement a singleton" rather than "does Spring use the singleton pattern."

**Q: Implement a thread-safe Singleton. What are the different ways, and which is actually recommended?**
A: Roughly in order from "commonly taught" to "actually recommended":

*Eager initialization* — created at class-load time, thread-safe by construction (class loading is inherently synchronized by the JVM), but the instance exists even if never used:
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
The `volatile` keyword is not optional here — without it, another thread can observe a partially-constructed object due to instruction reordering. If asked to explain *why* `volatile` is there, that's the answer.

*Enum singleton* — Joshua Bloch's (author of *Effective Java*) recommended approach:
```java
public enum Singleton {
    INSTANCE;
    public void doSomething() { /* ... */ }
}
```
Thread-safe for free, and the only approach also safe against reflection- and serialization-based attacks that can otherwise create a second instance of a "singleton."

**Your bridge to Spring**: this is exactly what "Spring beans are singletons by default" is quietly doing on your behalf — the IoC container handles this bookkeeping so you never hand-write any of the above in application code. Recognizing that the container is solving the same problem you'd otherwise solve manually is a strong way to connect the two halves of this topic.

**Q: Implement the Factory pattern.**
A: A method that creates and returns an object without the caller needing to know the concrete class being instantiated:
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
Directly mirrors a `@Bean` method in a Spring `@Configuration` class (like `seedData` in `BankDemoApplication`) — same idea, Spring just calls it for you at startup instead of you calling it explicitly.

**Q: Implement the Builder pattern.**
A: Solves the "constructor with eight optional parameters" problem by assembling an object step by step:
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
Comparable to the functional options pattern in Go, as your Consolidated Prep doc already notes.

**Q: What's the Observer pattern, and where have you probably already used it without naming it?**
A: An object (the "subject") keeps a list of dependents ("observers") and notifies them automatically when its state changes. You've almost certainly used it already: any event listener — a button's `onClick`, a Kafka consumer reacting to a topic, a React component re-rendering when state changes — is the Observer pattern in practice, even when nobody calls it that out loud.

**Q: What's the Strategy pattern, and how does it map to something from your own background?**
A: Define a family of interchangeable algorithms behind a common interface, and swap which one is used at runtime:
```java
interface PricingStrategy { BigDecimal price(Order order); }
class StandardPricing implements PricingStrategy { /* ... */ }
class DiscountPricing implements PricingStrategy { /* ... */ }
```
Directly the same shape as swapping an implementation behind an interface in Go — which your Consolidated Prep doc already flags as a natural talking point from the pricing-service work at Laku6.

**Q: What's the Decorator pattern?**
A: Wraps an object to add behavior without modifying its class or affecting other instances of it. Java's I/O classes are the textbook example — `new BufferedReader(new FileReader(file))`, where each layer adds behavior around the one it wraps. Worth recognizing by name even if you haven't hand-implemented one recently.

**Q: What's the Adapter pattern?**
A: Converts one interface into another that a client expects, letting otherwise-incompatible interfaces work together — e.g., wrapping a third-party library's awkward API behind an interface matching the rest of your codebase's conventions, so the rest of your code doesn't need to know the third-party shape underneath.

---

## Part 5: Java 8+ Features (Lambdas, Streams, Optional)

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

## Part 6: Strings & Memory Basics

**Q: Why is `String` immutable in Java, and what's the "String pool"?**
A: Once created, a `String`'s value can never change — `.concat()`, `.toUpperCase()`, and similar all return a *new* `String` rather than modifying the original. This immutability is what enables the **String pool**: a special memory area where literal strings are cached and reused, since an immutable value is always safe to share. `String a = "hi"; String b = "hi";` — both references point to the *same* pooled object. It's also why `String` is safe to use as a `HashMap` key without risking the map's internal structure breaking if a key's hash changed after insertion — it can't change.

**Q: `String` vs. `StringBuilder` vs. `StringBuffer`?**
A: `String` is immutable — every concatenation in a loop creates a new object, which gets expensive fast. `StringBuilder` is mutable and built for exactly that case — repeated modification without the object churn — but isn't thread-safe. `StringBuffer` is the same idea with synchronized methods, trading some performance for thread-safety. In practice, `StringBuilder` is the default choice unless you specifically need thread-safety, and even then a proper concurrency primitive is often a better fit than reaching for `StringBuffer`.

**Q: Stack vs. heap in the JVM — what actually lives where?**
A: Each thread has its own **stack**, holding local variables and method call frames — primitives and object *references* live here, automatically cleaned up as methods return. The **heap** is shared across all threads and holds every object created with `new` — this is what the garbage collector manages. `Account account = new Account(...)` puts the reference `account` on the stack, but the actual `Account` object itself lives on the heap.

---

## Part 7: Other Core Java Worth Refreshing

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
A: A fixed set of named instances that are genuine objects — an enum can have fields, a constructor, and methods, even different behavior per constant. It's type-safe (the compiler rejects an invalid value, unlike a raw `String` or `int` "status code" would), and — per Part 4 above — it's the basis of the recommended modern Singleton implementation.

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

**Q: When would you reach for `ConcurrentHashMap` over a synchronized `HashMap`?**
A: `Collections.synchronizedMap(new HashMap<>())` locks the *entire* map on every operation — simple, but serializes all access and becomes a bottleneck under real concurrency. `ConcurrentHashMap` locks at a much finer grain internally, allowing genuinely concurrent reads and writes to different parts of the map, and offers atomic compound operations (`compute()`, `merge()`, `putIfAbsent()`) that would otherwise need external synchronization to do safely — a typical use case is an in-process cache or counter map that many request threads read and update at once.

Worth knowing when *not* to reach for it, too: `bank-demo`'s actual idempotency mechanism (`IdempotencyRecordRepository`, backed by an `idempotency_records` table with `idempotencyKey` as the primary key) deliberately does **not** use a `ConcurrentHashMap` for this. A database-enforced unique constraint survives a restart and works correctly across multiple instances of the service running behind a load balancer; an in-memory `ConcurrentHashMap` would lose every idempotency record the moment the process restarted, and wouldn't be shared across instances at all. This is exactly the tradeoff the Banking Playbook's idempotency scenario points at — `ConcurrentHashMap` solves in-process thread-safety, not distributed, durable uniqueness, and a real payment idempotency check needs the latter.

---

## Part 9: Modern Java (17–21) Features

Worth at least recognizing by name — current interview-prep sources note these increasingly show up even in concept-level rounds, since the interview is happening on a Java 21 timeline regardless of which version the codebase you'd inherit actually targets.

**Q: What's a `record`?**
A: A concise way to declare an immutable data-carrier class — the compiler generates the constructor, `equals()`, `hashCode()`, `toString()`, and accessors for you:
```java
public record AccountSummary(Long id, String ownerName, BigDecimal balance) {}
```
Good instinct-check: if asked to "make this class immutable" today, in modern Java a record is usually the right reach — `bank-demo`'s `AccountResponse` hand-writes exactly what a record would generate automatically, which is worth pointing out if it comes up.

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
A: Already covered in your Spring/Java doc under Project Loom — worth reconnecting as part of the same "modern Java" cluster: lightweight, JVM-managed threads that close the gap between Java's traditional heavyweight-thread model and Go's goroutines, and one of the most consistently cited "2026 interview" topics across current sources.

---

## Part 10: Core JavaScript Fundamentals

The JD lists JavaScript separately from ReactJS — worth having the language itself solid, not just the framework.

**Q: What's a closure?**
A: A function that retains access to variables from its enclosing scope even after that outer function has finished executing. Classic example: a counter factory function that returns an increment function, which keeps its own private running count via closure.

```js
function makeCounter() {
  let count = 0                       // private — no way to reach this from outside
  return () => ++count                // this inner function "closes over" count
}
const counter = makeCounter()
counter() // 1
counter() // 2 — count persisted even though makeCounter() already returned
```

**Q: How does the JavaScript event loop handle async code?**
A: JS is single-threaded but non-blocking. Synchronous code runs on the call stack; async operations (timers, network calls, promises) are handed off to the runtime, and their callbacks are queued to run once the call stack is empty. Promise callbacks (microtasks) are drained *before* the next macrotask (like a `setTimeout` callback) — the classic interview gotcha is explaining why `Promise.resolve().then(fn)` runs before `setTimeout(fn, 0)`.

```js
console.log('1')
setTimeout(() => console.log('2'), 0) // macrotask — queued for later
Promise.resolve().then(() => console.log('3')) // microtask — runs before the next macrotask
console.log('4')
// output: 1, 4, 3, 2 — not 1, 2, 3, 4, even with a 0ms timeout
```

**Q: `var` vs. `let` vs. `const`?**
A: `var` is function-scoped, hoisted and initialized as `undefined`. `let`/`const` are block-scoped, hoisted but not initialized (accessing before declaration throws — the "temporal dead zone"). `const` additionally locks the *binding* from reassignment — it doesn't make the value deeply immutable, you can still mutate an object's properties.

```js
if (true) { var x = 1 }
console.log(x) // 1 — var leaked out of the block, function-scoped not block-scoped

const account = { balance: 100 }
account.balance = 150 // fine — mutating the object's property, not reassigning the binding
// account = {}       // TypeError — can't reassign a const binding
```

**Q: `this` binding — and why do arrow functions behave differently?**
A: In a regular function, `this` depends on *how* the function is called (implicit binding from the caller, or explicit via `call`/`apply`/`bind`). Arrow functions have no `this` of their own — they inherit it lexically from the enclosing scope at the point they're defined, which is exactly why they're the default choice for callbacks and event handlers: no more "why is `this` undefined inside my callback."

```js
class AccountWidget {
  balance = 100
  // regular function — `this` depends on how logBalance is CALLED, breaks as a callback
  logBalance() { console.log(this.balance) }
  // arrow function — `this` is captured lexically from the class, safe to pass as a callback
  logBalanceArrow = () => console.log(this.balance)
}
const w = new AccountWidget()
setTimeout(w.logBalance, 100)      // undefined — `this` is lost, called as a plain function
setTimeout(w.logBalanceArrow, 100) // 100 — arrow function kept `this` bound to the instance
```

**Q: Promises vs. async/await?**
A: A Promise represents the eventual result of an async operation (pending → fulfilled/rejected). `async/await` is syntactic sugar over promises letting you write async code that reads top-to-bottom like synchronous code, avoiding deeply nested `.then()` chains.

```js
// promise chain
fetch('/accounts').then(res => res.json()).then(accounts => console.log(accounts)).catch(err => console.error(err))

// same thing, async/await — reads top to bottom like sync code
async function loadAccounts() {
  try {
    const res = await fetch('/accounts')
    const accounts = await res.json()
    console.log(accounts)
  } catch (err) { console.error(err) }
}
```

**Q: Debounce vs. throttle?**
A: **Debounce** delays execution until a pause in events — e.g., wait until the user stops typing for 300ms before firing a search request. **Throttle** executes at most once per fixed interval regardless of event frequency — e.g., a scroll handler that runs at most every 100ms. Different problems: debounce waits for quiet, throttle enforces a steady ceiling.

```js
function debounce(fn, delay) {
  let timer
  return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), delay) }
}
const search = debounce((query) => fetchResults(query), 300) // fires once, 300ms after typing stops

function throttle(fn, interval) {
  let last = 0
  return (...args) => { const now = Date.now(); if (now - last >= interval) { last = now; fn(...args) } }
}
const onScroll = throttle(() => updatePosition(), 100) // fires at most once every 100ms
```

---

## Part 11: HTML5 & CSS3 Fundamentals

Also named explicitly and separately in the JD.

**Q: What's the CSS box model?**
A: Every element is content → padding → border → margin, from the inside out. `box-sizing: border-box` (the common modern default) makes width/height include padding and border rather than adding them on top of a specified width — avoids a lot of layout-math surprises.

```css
.card {
  box-sizing: border-box; /* width includes padding+border, doesn't add on top of it */
  width: 200px;
  padding: 16px;
  border: 1px solid #ccc; /* WITHOUT border-box, real rendered width would be 200+32+2 = 234px */
}
```

**Q: Flexbox vs. Grid — when do you reach for each?**
A: Flexbox is one-dimensional — ideal for laying items out in a single row or column (navbars, button groups, centering content). Grid is two-dimensional — ideal when you need to control rows and columns together (overall page layout, card grids). Commonly combined: Grid for page structure, Flexbox for the components inside it.

```css
.navbar { display: flex; justify-content: space-between; align-items: center; } /* one dimension: a row */

.dashboard {
  display: grid;
  grid-template-columns: 200px 1fr; /* two dimensions: a fixed sidebar column + a flexible content column */
  grid-template-rows: 60px 1fr;
}
```

**Q: How do media queries and responsive design work?**
A: A layout that adapts to different screen sizes, typically via a fluid layout plus CSS media queries (`@media (max-width: 768px) { ... }`) applying different styles based on viewport. Mobile-first is the standard modern approach: write base styles for small screens, then layer on complexity for larger screens with `min-width` queries, rather than starting desktop-first and squeezing down.

```css
.account-table { display: block; }         /* base — mobile styles, no query needed */
@media (min-width: 768px) {                /* mobile-first — layer on complexity for bigger screens */
  .account-table { display: table; }
}
```

**Q: What's semantic HTML, and why does it matter?**
A: Using elements for their actual meaning (`<nav>`, `<article>`, `<button>`) instead of generic `<div>`s for everything. Matters for accessibility (screen readers rely on semantic structure to navigate a page) and for SEO (search engines weight semantic structure when parsing a page).

```html
<!-- non-semantic — a screen reader has no idea any of this is meaningful -->
<div class="nav"><div class="link">Accounts</div></div>

<!-- semantic — a screen reader announces "navigation", a button is keyboard-operable for free -->
<nav><button>Accounts</button></nav>
```

---

## Part 12: Kafka & Messaging Deep Dive

You've mentioned Kafka experience in your background per the original prep doc — worth being able to go one level deeper than "I've used it" if pushed, especially since MQ/Kafka is explicitly named in the JD.

**Q: What is Kafka, in plain terms?**
A: A distributed event streaming platform. Producers publish messages to named topics; consumers subscribe and read them; and — unlike a transient in-memory queue — Kafka durably stores the stream, so multiple independent consumers can read the same data, and can even replay it from an earlier point.

```java
// producer
kafkaTemplate.send("order-events", orderId.toString(), new OrderCreated(orderId));

// consumer
@KafkaListener(topics = "order-events")
void onOrderEvent(OrderCreated event) { notificationService.notify(event); }
```

**Q: What's a partition, and why does it matter?**
A: A topic is split into partitions, each an ordered, append-only log. Partitioning is what lets Kafka parallelize — different partitions can be consumed simultaneously by different consumers. **Kafka guarantees ordering within a partition, but not across partitions of the same topic.** If strict ordering matters for a given entity (e.g., all events for one order), you route by a key (e.g., order ID) so every event for that entity lands on the same partition, in order.

```java
// keying by orderId — every event for THIS order always lands on the same partition, in order
kafkaTemplate.send("order-events", /* key= */ orderId.toString(), event);
```

**Q: What's a consumer group?**
A: A set of consumers splitting the work of consuming a topic — each partition is assigned to exactly one consumer within the group at a time, so the group processes the topic in parallel, and Kafka automatically rebalances partition assignments when a consumer joins or leaves.

```java
@KafkaListener(topics = "order-events", groupId = "notification-service")
// 3 instances of this service, same groupId → Kafka splits the topic's partitions across all 3
```

**Q: How does Kafka handle a consumer crashing?**
A: Consumers commit their processing offset (position within a partition) back to Kafka after processing a message. If a consumer crashes and restarts — or its partitions get reassigned to another consumer in the group — processing resumes from the last committed offset instead of from the beginning.

**Q: Why choose Kafka/event-driven messaging over direct REST calls between services?**
A: REST calls are synchronous and create temporal coupling — both services need to be up and responsive at the same moment. An event-driven approach decouples that: the producer doesn't need the consumer to be available *right now*, just *eventually*. Directly the same resilience story as the circuit-breaker and graceful-degradation content in the Real-World Scenarios doc, one layer up the stack.

```java
// REST — both services must be up RIGHT NOW, or this call fails
notificationClient.send(orderId); // temporal coupling

// event-driven — producer doesn't care if the consumer is up this second
kafkaTemplate.send("order-events", event); // fire and forget, consumer catches up whenever it's ready
```

---

## Part 13: CI/CD & Git Basics

Named in the JD (Jenkins, Bitbucket) and not covered elsewhere.

**Q: CI vs. CD — what's the actual difference?**
A: **Continuous Integration** — automatically building and testing every change (typically on every push/PR) to catch integration problems early, instead of discovering them when merging large batches of work later. **Continuous Delivery/Deployment** — automatically pushing changes that pass CI toward production: up to a manual approval gate (delivery), or fully automatically (deployment).

**Q: What does a typical Jenkins pipeline actually do?**
A: Triggered by a code push: pull the code → build → run automated tests → run static analysis/security scans → package a deployable artifact (often a Docker image, tying back to the Docker/K8s primer) → deploy to an environment, usually gated behind manual approval for production. Defined as code (a `Jenkinsfile`), version-controlled alongside the application itself.

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

---

## Part 14: API Documentation — Swagger/OpenAPI

Named directly in the JD, not covered elsewhere.

**Q: What's Swagger/OpenAPI, and why does it matter in practice?**
A: OpenAPI is a specification format for describing a REST API's endpoints, request/response schemas, and auth requirements in a machine-readable way (YAML/JSON). Swagger is the tooling built around that spec — most relevantly Swagger UI, which generates interactive, browsable documentation straight from the spec, and libraries like springdoc-openapi that generate the spec *automatically* from your Spring annotations rather than you hand-writing it. The practical payoff: documentation stays in sync with the actual code because it's generated, not hand-maintained — and frontend/QA get a live way to explore and test endpoints without a separately-maintained Postman collection drifting out of date.

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

## Part 15: Testing Frameworks, Named Correctly

Brief, but worth having the exact names ready rather than saying "some testing library."

**Q: What would you use to test a Spring Boot service?**
A: JUnit 5 for test structure and assertions, Mockito for mocking dependencies in unit tests, and `@SpringBootTest` — or the more targeted "slice" annotations like `@WebMvcTest` or `@DataJpaTest` — for integration tests that load some or all of the Spring context.

```java
@ExtendWith(MockitoExtension.class) // unit test — JUnit 5 + Mockito, no Spring context
class AccountServiceTest {
    @Mock AccountRepository repository;
    @InjectMocks AccountService service;
    @Test void withdrawThrowsWhenInsufficientFunds() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Account(...)));
        assertThrows(InsufficientFundsException.class, () -> service.withdraw(1L, tooMuch));
    }
}
```

**Q: What would you use to test a React app?**
A: Jest as the test runner and assertion library, React Testing Library (RTL) for rendering components and interacting with them the way a user actually would — querying by visible text or accessibility role rather than internal implementation details. RTL's philosophy specifically discourages testing implementation details, since tests written that way survive refactors better than ones coupled to internal component state.

```jsx
import { render, screen, fireEvent } from '@testing-library/react'

test('deposit button triggers a deposit', () => {
  render(<AccountRow account={{ id: 1, balance: 100 }} />)
  fireEvent.change(screen.getByPlaceholderText('amount'), { target: { value: '50' } })
  fireEvent.click(screen.getByText('Deposit'))
  // queries by what a USER sees (placeholder text, button label), not internal component state
})
```
