# Interview Countdown Plan
For: OCBC Full Stack Developer (Java, ReactJS), JR00009212 — OCBC Group Wealth
Today: 22 Jul 2026. Interview: 3 Aug 2026, 3:00pm SGT / 2:00pm WIB. **12 days out.**

This is the 8th document — the one that turns the other 7 (already excellent, already comprehensive) into an actual day-by-day plan, with a hard look at what's genuinely still missing. Read `00_INDEX.md` first if you haven't already; this doc assumes you know what's in the other 7.

---

## Part 1: Gap Analysis Against the JD

Everything below is checked against the actual JD requirements, not against "is it a good interview topic in general."

| JD requirement | Status | Where |
|---|---|---|
| 10+ yrs Java/J2EE | **Honest framing scripted, but blanks unfilled** | Consolidated Prep §3 — you haven't written your actual answer yet |
| Agile/SDLC | Covered | Master Checklist Part 7 |
| Spring ecosystem / microservices | Strong — conceptual + a real working demo | All Spring docs + `bank-demo` itself |
| REST/JSON/Swagger/XML | **Closed 22 Jul** — springdoc-openapi wired in, live Swagger UI + `/v3/api-docs` | `pom.xml`, README |
| ReactJS/JS/HTML5/CSS3 | Covered conceptually + a real (if minimal) working frontend | ReactJS QA + `frontend/src/App.jsx` |
| Redux specifically | **Closed 22 Jul** — account list now runs through a real Redux Toolkit slice (`createSlice` + async thunk), not just conceptual | `frontend/src/accountsSlice.js`, `store.js`, README |
| Docker/Kubernetes | **Partially closed 22 Jul** — Dockerfile added and build+run verified end-to-end (real container, health-checked); Kubernetes itself still conceptual only, no K8s manifests in this repo | `Dockerfile`, Consolidated Prep §9 |
| MQ/Kafka | Conceptual + real prior experience (Laku6) to cite | Gap Fill Part 12 |
| Liquibase | **Closed 22 Jul** — `ddl-auto` is now `validate`, schema lives in `db/changelog/`, verified against the entities by actually booting the app | `application.yml`, `db/changelog/`, Consolidated Prep §10 |
| Jira/Confluence/Jenkins/Bitbucket | **Partially closed 22 Jul** — a CI pipeline now exists and runs on push (`mvn test`, frontend lint+build); it's GitHub Actions, not Jenkins specifically, since this repo has no Jenkins server behind it — still a legitimate "yes, I've wired a CI pipeline" talking point | `.github/workflows/ci.yml` |
| Wealth/trading domain (preferred) | Covered as a primer, honestly framed as a gap | Consolidated Prep §11, Banking Playbook Part 3 |

**Read on this table**: four of the five Part-4 gap-fills are now real code in this repo, not just talked-about — see Part 4 below for exactly what was built and how it was verified (real `mvn test` runs, a real `docker build`+`docker run`+health-check cycle, a real `npm ci` dry run). Kafka/MQ and the DevOps toolchain items still rest on honest framing plus real prior experience to cite, not repo evidence — that's fine, they're lower-frequency questions than the ones already closed.

---

## Part 2: Blanks Only You Can Fill

Nobody else can write these for you, and they're the parts most likely to sound hollow if you're inventing them live in the room. Do these before anything else — Day 1 below is built around it.

1. **Consolidated Prep §3** — your actual Java experience anchor:
   - What was the Java thing?
   - Roughly when / how long?
   - What did you build or touch?
2. **ReactJS QA — React Native bridge section** — your own specific example of the trickiest thing to unlearn moving between RN and React DOM.
3. **Behavioral STAR stories** — now developed with real, git-verified detail in `Behavioral_Interview_Prep.md` (a separate doc, not originally part of this 8-doc set — go read it). One decision still sits with you, nobody else's to make: that doc found your pricing-service story is more accurately "hardened and fixed a teammate's Go service, caught a bug that mispriced 462 SKUs" rather than "migrated it from Jupyter" — both versions are now written into Consolidated Prep §8/§15 side by side. Pick one before mock #1 and make sure every doc says the same thing. The gRPC-vs-messaging disagreement story's real outcome also still needs to come from your memory — repo archaeology found no trace of it.
4. **Logistics follow-up** — per Consolidated Prep, you asked Lam Yan Kay 4 questions on 15 Jul (permanent/contract, WFH, location, team), routed to Lee Kin Kit on 16 Jul, no reply as of prep time. Decide now: nudge before the 3rd, or raise briefly at the end of the interview as the doc already scripts. Don't leave this as a live decision under interview pressure.

---

## Part 3: Day-by-Day Schedule

Paced at roughly 45–60 min/day. Matches the existing "Suggested Order" in `00_INDEX.md`, just calendarized with two additions the other docs don't have: a personal-blanks day up front, and two full mock-interview runs (nothing in the existing 7 docs actually rehearses end-to-end under time pressure — reading answers isn't the same skill as producing them on the clock).

| Date | Focus |
|---|---|
| **Wed 22 Jul (today)** | Fill in Part 2's blanks above — all of them, in writing, in your own words. Re-verify the demo still runs (`mvn spring-boot:run`, `cd frontend && npm run dev`, hit the curl commands in README) — it's been a while since "verified end-to-end" was written. |
| **Thu 23 Jul** | Consolidated Prep, start to finish. Say §3 (Java framing) and the self-intro (§15) out loud once, using what you wrote yesterday. |
| **Fri 24 Jul** | Master Checklist Part 0 first (confirmed real candidate questions), then the rest of that doc. |
| **Sat 25 Jul** | Spring_Java_Interview_QA out loud — and pair each answer with the real line of code in `bank-demo` that demonstrates it. Use `Project_Code_Walkthrough.md` (new) as the reading order through the actual repo, `SPRING_ANNOTATIONS.md` as the annotation-indexed lookup. |
| **Sun 26 Jul** | ReactJS_Interview_QA out loud, alongside `frontend/src/App.jsx`. Rehearse your React Native answer from Part 2. |
| **Mon 27 Jul** | Banking_System_Design_Playbook Parts 1 & 2 (fundamentals + the 8 scenarios). |
| **Tue 28 Jul** | Banking Playbook Part 3 (wealth/trading domain) + Real_World_Engineering_Scenarios_QA + General_Backend_Engineering_QA and General_Frontend_Engineering_QA (both new — read straight through once each; the framework-agnostic layer underneath everything else, no blanks to fill, pure breadth). |
| **Wed 29 Jul** | Technical_Fundamentals_Gap_Fill — Parts 2–4 (OOP/SOLID/design patterns) carefully, since the design-pattern question is confirmed real (Master Checklist Part 0, #2); skim the rest for recognition, not memorization. |
| **Thu 30 Jul** | **Mock interview #1** — full 60-minute timed run using Part 5 below. Solo out loud or with a friend. Write down every place you hesitated or rambled. |
| **Fri 31 Jul** | Fix yesterday's weak spots specifically. Part 4's gap-fills are already built (22 Jul) — spend any spare time actually looking at that code (`db/changelog/`, `accountsSlice.js`, `Dockerfile`) instead of building something new. |
| **Sat 1 Aug** | **Mock interview #2** — full 60-minute timed run again. This time it should feel noticeably tighter than #1. |
| **Sun 2 Aug** | Light review only. Reread Master Checklist Part 0 and the Day Of Checklist (Consolidated Prep §17). No new material — this is a rest day, not a cram day. |
| **Mon 3 Aug** | Interview day. Follow Consolidated Prep §17 exactly. |

---

## Part 4: Closed 22 Jul — Real Evidence Instead of Just Honest Framing

All five were built directly into `bank-demo` and verified, not just added and hoped-for:

1. **springdoc-openapi** — one dependency in `pom.xml`. Verified: `/v3/api-docs` and `/swagger-ui/index.html` both return 200 against a live-running instance.
2. **Dockerfile** — multi-stage build (`maven:3.9-eclipse-temurin-17` build stage → `eclipse-temurin:17-jre-jammy` runtime stage). Verified: `docker build` succeeded, `docker run` produced a container that passed its own health check and ran Liquibase migrations inside the container — not just "it built," it actually ran.
3. **CI config** — `.github/workflows/ci.yml`, two jobs (backend `mvn test`, frontend `npm ci && npm run lint && npm run build`). Verified: ran the exact same commands locally, including a clean-room `npm ci` in a scratch directory to make sure the lockfile actually supports it (it initially didn't — a stale lockfile from `npm install` was missing some optional platform packages; regenerated it from scratch to fix).
4. **Redux slice** — `frontend/src/accountsSlice.js` (`createSlice` + one async thunk, `fetchAccounts`) and `store.js`, wired into `App.jsx` via `useSelector`/`useDispatch`. Per-row local state (amount inputs, transfer target) deliberately stayed local — only the shared account list moved to the store, matching what the plan actually called for. Verified: build + lint clean; **not** verified in an actual rendered browser — the Chrome extension wasn't connected this session, so this is build-level and API-level verification only. Worth opening `localhost:5173` yourself once before citing it as "I've built this."
5. **Liquibase** — `db/changelog/db.changelog-master.yaml` + one changeset (`accounts`, `idempotency_records`, matching the entities column-for-column), `ddl-auto` switched from `update` to `validate`. Verified: Hibernate's `validate` mode means a schema mismatch fails startup loudly — the app booting clean *is* the proof the changelog matches the entities, checked both via plain `mvn spring-boot:run` and inside the Docker container.

A stale process from an earlier session was still bound to port 8080 running the old pre-Liquibase code during testing — killed it before verifying, otherwise the curl checks would've silently been hitting the wrong build.

---

## Part 5: The 1-Hour Timebox

Used for the two mock runs above. Based on Consolidated Prep §1's phase list, with minutes assigned so a rehearsal run has something to actually clock itself against:

| Phase | Minutes | Source |
|---|---|---|
| Self-intro + high-level project walkthrough | 10 | Consolidated Prep §15 |
| Deep dive into 1–2 CV projects (they will pick one and push) | 15 | Have the Laku6 pricing-service story and the CIMB Niaga BI-FAST story both ready to go deep on. **Decide before mock #1 which pricing-story framing you're using — "migrated it" vs. the git-verified "hardened it, caught a 462-SKU bug" — see `Behavioral_Interview_Prep.md` and Consolidated Prep §8/§15, both now carry both versions pending your call.** |
| Core Java / Spring Q&A | 15 | Spring_Java_Interview_QA + Master Checklist Part 0 |
| Database & caching (SQL vs. NoSQL, Redis) | 8 | Banking Playbook §7, Consolidated Prep §6–7 |
| Microservices experience & design | 8 | Consolidated Prep §8, Master Checklist Part 2 |
| Closing — team/role discussion, your questions | 4 | Consolidated Prep §16 |

Real interviews won't hit these splits exactly — the point of timing a mock run against this is to notice if you're spending 25 minutes on self-intro and project walkthrough alone, which is the single most common way a strong candidate runs out of runway on a 1-hour call.

---

## Part 6: Biggest Risks, Ranked

1. **The Java-experience question lands rambling instead of crisp** — because it's the one question where an honest, non-defensive answer matters most and it's currently unrehearsed (blank). Mitigated by Day 1 + both mock runs.
2. **"Have you actually used Docker/Liquibase/Redux/Swagger" gets a bare "no"** — largely closed as of 22 Jul (Part 4): all four now have real, verified code in `bank-demo` you can point to. Remaining risk is thinner: Kubernetes specifically (no manifests, Docker only) and Jenkins specifically (CI exists, but as GitHub Actions, not Jenkins) still need the honest-pivot structure from the wealth-domain gap, not a "yes, here's the repo" answer.
3. **Logistics question surfaces awkwardly** if still unanswered by the 3rd — decide now whether to nudge beforehand (Part 2, item 4).
4. **Running out of time on self-intro/project walkthrough**, leaving no runway for the technical depth that's actually being assessed — mitigated by Part 5's timebox during mock runs.
