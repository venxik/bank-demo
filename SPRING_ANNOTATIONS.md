# Spring Boot Annotations Used In This Project

Beginner cheat sheet. Every annotation below actually appears in this codebase — file/line noted so you can go look at real usage.

## App bootstrap

### `@SpringBootApplication`
`BankDemoApplication.java:6`
Marks the entry point class. Bundles three annotations into one:
- `@Configuration` — this class can define beans
- `@EnableAutoConfiguration` — Spring guesses config from what's on the classpath (e.g. sees JPA + H2 → wires a DataSource)
- `@ComponentScan` — scans this package and sub-packages for `@Component`/`@Service`/`@RestController` etc. and registers them as beans

One per app, on the class with `main()`.

## Dependency injection / bean registration

Spring keeps a container of objects ("beans") it creates and wires together for you, instead of you calling `new` everywhere.

### `@Component`
`ConsoleNotificationService.java:15`, `SmsNotificationService.java:14`
Generic "let Spring manage this class" marker. Spring creates one instance (singleton by default) and makes it injectable elsewhere.

### `@Service`
`AccountService.java:26`
Same mechanics as `@Component` — it's just a more specific name for "this is business-logic layer", so the code documents itself. Spring treats it identically to `@Component` under the hood.

### Constructor injection (no annotation needed)
`AccountService.java:37-45`, `AccountController.java:22-24`
When a class has exactly one constructor, Spring automatically injects the required beans as arguments — no `@Autowired` needed. This is why `AccountService(...)` just lists `AccountRepository`, `NotificationService`, etc. and Spring supplies real instances at startup.

Note in `AccountService`: the constructor takes `List<NotificationService>` — Spring collects **every** bean implementing that interface into the list automatically (`ConsoleNotificationService` + `SmsNotificationService` today).

## Web layer (HTTP in/out)

### `@RestController`
`AccountController.java:16`
Combines `@Controller` (this class handles HTTP requests) + `@ResponseBody` (return values get serialized straight to JSON in the response body, instead of being treated as a view name to render).

### `@RequestMapping("/accounts")`
`AccountController.java:17`
Sets a base URL path for every endpoint method in the class. Every method below inherits the `/accounts` prefix.

### `@GetMapping`, `@PostMapping`
`AccountController.java:26,32,37,42,49,56`
Shorthand for `@RequestMapping(method = GET)` / `(method = POST)`. Maps an HTTP verb + path to a method. `@GetMapping("/{id}")` combined with the class-level mapping means `GET /accounts/{id}`.

### `@PathVariable`
`AccountController.java:38`
Pulls a value out of the URL path. `@GetMapping("/{id}")` + `@PathVariable Long id` means whatever's in `{id}` in the URL becomes the `id` parameter.

### `@RequestBody`
`AccountController.java:27`
Deserializes the incoming JSON request body into a Java object (here, `CreateAccountRequest`).

### `@RequestHeader`
`AccountController.java:45`
Reads an HTTP header value. `@RequestHeader(value = "Idempotency-Key", required = false)` reads the `Idempotency-Key` header, or `null` if the caller didn't send one.

### `@Valid`
`AccountController.java:27`
Tells Spring to run Bean Validation on the object right after deserializing it (see `@NotNull`/`@DecimalMin` below). If validation fails, Spring throws `MethodArgumentNotValidException` before your method body ever runs — that's what `GlobalExceptionHandler.handleValidation` catches.

### `@RestControllerAdvice`
`GlobalExceptionHandler.java:17`
Global exception handler for every `@RestController` in the app — `= @ControllerAdvice + @ResponseBody`. One place to turn exceptions into HTTP error responses instead of try/catch in every controller method.

### `@ExceptionHandler(SomeException.class)`
`GlobalExceptionHandler.java:20,26,32,38,45,51,60`
Inside a `@RestControllerAdvice` (or `@Controller`), marks a method as "run this when `SomeException` (or a subclass) escapes a controller method." Spring matches the most specific exception type.

## Validation (on DTO fields)

### `@NotNull`
`TransferRequest.java:8,11`
Field must not be `null`, checked when `@Valid` runs.

### `@DecimalMin("0.01")`
`TransferRequest.java:12`
Numeric field must be `>=` the given value — here, rejects zero/negative transfer amounts.

(Both come from `jakarta.validation.constraints`, the standard Bean Validation API — not Spring-specific, but Spring wires it in via `@Valid`.)

## Persistence (JPA / database)

### `@Entity`
`Account.java:7`
Marks a class as mapped to a database table — JPA/Hibernate will manage its lifecycle (insert/update/delete rows for it).

### `@Table(name = "accounts")`
`Account.java:8`
Optional — overrides the table name. Without it, JPA would default to the class name (`Account`/`account`).

### `@Id`
`Account.java:11`
Marks the primary-key field.

### `@GeneratedValue(strategy = GenerationType.IDENTITY)`
`Account.java:12`
The database auto-generates the ID (e.g. auto-increment column) — you don't set it yourself before saving.

### `@Column(nullable = false, unique = true)`
`Account.java:15,18,21,24`
Customizes the mapped database column. `nullable = false` → `NOT NULL` constraint. `unique = true` → unique constraint. `updatable = false` (line 24, `createdAt`) → this column is set once on insert and never touched by later `UPDATE`s.

### `@Version`
`Account.java:36`
Optimistic locking. JPA auto-increments this field on every `UPDATE` and includes the old value in the `WHERE` clause. If another transaction already bumped it, your update matches zero rows and Hibernate throws `OptimisticLockingFailureException` instead of silently overwriting someone else's change. See the longer comment right above it in `Account.java` for the concurrency reasoning.

### `JpaRepository<Account, Long>` (interface, not an annotation)
`AccountRepository.java:8`
Not an annotation, but shows up alongside these so worth naming: extend this and Spring Data JPA generates the implementation for you at runtime (`save`, `findById`, `findAll`, etc. all "just work" with zero code). Method names like `findByAccountNumber` get auto-implemented too, by parsing the method name.

## Transactions

### `@Transactional`
`AccountService.java:51,68,79,107`
Wraps the method in a database transaction: if the method completes normally, the transaction commits; if it throws an unchecked exception, everything the method did gets rolled back. E.g. in `transfer()`, if `to.credit(...)` somehow failed after `from.debit(...)` already ran, the debit would be rolled back too — no half-finished transfer.
