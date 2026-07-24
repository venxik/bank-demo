# bank-demo

Spring Boot 3.3.4 / Java 17 REST API for bank accounts, plus a minimal React frontend. Built for OCBC interview prep — each file demonstrates one interview concept. **Verified end-to-end**: builds with Maven, runs, every endpoint below was hit with real curl calls, and the frontend was driven with Playwright (screenshots checked, not just "it compiled").

## Run

Backend:

```
mvn spring-boot:run
```

Or build the jar and run it directly:

```
mvn clean package
java -jar target/bank-demo-0.0.1-SNAPSHOT.jar
```

Starts on `:8080`. H2 in-memory DB, console at `/h2-console` (JDBC URL `jdbc:h2:mem:bankdemo`, user `sa`, no password).

Frontend (needs backend running):

```
cd frontend
npm install
npm run dev
```

Starts on `:5173`, dev server proxies `/accounts` to `:8080` (see `frontend/vite.config.js`) — no CORS config needed on the backend.

Docker (backend only, multi-stage build):

```
docker build -t bank-demo .
docker run -p 8080:8080 bank-demo
```

## Concept map

| File | Concept |
|---|---|
| `service/AccountService.java` | Constructor injection, `@Transactional`, idempotency-key mechanism, `transfer` (Saga discussion bridge), generic idempotency helper |
| `antipattern/BrokenAccountLookupService.java` | Field injection vs constructor injection — not wired into any controller, exists only to contrast against `AccountService` |
| `notification/NotificationService.java` + impls | Strategy pattern; SOLID (OCP/DIP/ISP/LSP) walkthrough in the interface Javadoc; Singleton via Spring's default bean scope. Wired into `AccountService` — deposit/withdraw/transfer all fan out to every registered channel |
| `entity/Account.java` | `@Version` optimistic locking — Javadoc explains the lost-update race it prevents and why transfer locks accounts in ascending-id order |
| `entity/IdempotencyRecord.java` | Dedup store backing the idempotency mechanism (Banking Playbook Scenario 2) |
| `entity/Account.java` vs `dto/AccountResponse.java` | Entity/DTO separation |
| `exception/GlobalExceptionHandler.java` | Centralized error handling via `@RestControllerAdvice`, including `OptimisticLockingFailureException` -> 409 |
| `dto/CreateAccountRequest.java`, `dto/AmountRequest.java`, `dto/TransferRequest.java` | Bean Validation (`@NotNull`, `@DecimalMin`) |
| `repository/*.java` | Spring Data JPA repositories |
| `application.yml` | H2 config, Actuator exposure, springdoc auto-detected from classpath |
| `db/changelog/` | Liquibase changelog (master + one changeset) — schema-as-code instead of `ddl-auto`; Hibernate runs in `validate` mode against it |
| `Dockerfile` | Multi-stage build — Maven/JDK build stage, slim JRE runtime stage |
| `.github/workflows/ci.yml` | GitHub Actions: `mvn test` on the backend, `npm run lint` + `npm run build` on the frontend |
| `frontend/src/accountsSlice.js` + `store.js` | Redux Toolkit — `createSlice`, one async thunk (`fetchAccounts`) handling the fetch lifecycle via `extraReducers` |
| `frontend/src/App.jsx` | Account list now reads from the Redux store (`useSelector`/`useDispatch`); per-row form state (amount, transfer target) stays local — only the shared list moved to global state |
| `src/test/.../AccountServiceTransferTest.java` | `@SpringBootTest` covering transfer happy path, self-transfer guard, insufficient funds, idempotent replay |

Swagger UI: `http://localhost:8080/swagger-ui/index.html` (generated from the controllers by springdoc, zero annotations added). Raw OpenAPI JSON: `/v3/api-docs`.

## Endpoints

```bash
# health
curl http://localhost:8080/actuator/health

# create an account
curl -X POST http://localhost:8080/accounts -H "Content-Type: application/json" \
  -d '{"accountNumber":"ACC-001","ownerName":"Kevin Test","initialBalance":100.00}'

# get / list
curl http://localhost:8080/accounts/1
curl http://localhost:8080/accounts

# deposit — Idempotency-Key optional; replaying the same key returns the
# original result instead of crediting twice
curl -X POST http://localhost:8080/accounts/1/deposit -H "Content-Type: application/json" \
  -H "Idempotency-Key: dep-key-1" -d '{"amount":50.00}'

# withdraw — 409 if balance insufficient
curl -X POST http://localhost:8080/accounts/1/withdraw -H "Content-Type: application/json" \
  -d '{"amount":30.00}'
```

Validation failures return `400` with a `details` list of field errors. Unknown account IDs return `404`. Duplicate account numbers and insufficient funds return `409`.

## Known limitation

H2 is in-memory, so data still resets every restart — intentional for a demo. Schema itself is no longer freehanded by Hibernate, though: Liquibase (`db/changelog/`) owns it, and `ddl-auto` is `validate`, not `update`.

## Not built (out of scope for this demo)

- distributed transactions / an actual Saga implementation — `transfer` is a bridge into that discussion, not an implementation of it
- authentication/authorization — every endpoint is open, intentionally, to keep the demo focused
