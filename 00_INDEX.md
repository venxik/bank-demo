# OCBC Interview Prep — Index
Role: Full Stack Developer (Java, ReactJS), JR00009212 — OCBC Group Wealth
Interview: 3 Aug 2026, 3:00pm SGT, Microsoft Teams

You now have 12 documents. This is the front door — what's in each one, and a suggested order to go through them before the 3rd.

---

## The 12 Documents

**1. [OCBC_Interview_Consolidated_Prep](OCBC_Interview_Consolidated_Prep.md)**
Start here. Interview logistics, the Go→Java translation table, your honest Java-experience framing (has fill-in-the-blank spots — do these first, they take the longest to get right), Spring/Spring Boot fundamentals, design patterns, Redis, SQL vs. NoSQL, microservices, Docker/K8s primer, Liquibase primer, wealth/trading domain primer, Java/React refreshers, system design, self-intro, questions to ask, day-of checklist.

**2. [Banking_System_Design_Playbook](Banking_System_Design_Playbook.md)**
CAP theorem and other distributed-systems fundamentals explained from scratch, then banking-specific real-world scenarios (system down, payment idempotency, correct recipient, fraud detection, reconciliation, race conditions, audit trails), then the niche wealth/trading domain deep-dive and MAS TRM regulatory context.

**3. [Real_World_Engineering_Scenarios_QA](Real_World_Engineering_Scenarios_QA.md)**
The general (non-banking-specific) version of scenario questions: building a scalable system, incident response when something breaks, step-by-step performance-bottleneck debugging, plus quick-fire scenarios (safe rollouts, memory leaks, zero-downtime deploys, cache stampedes).

**4. [Spring_Java_Interview_QA](Spring_Java_Interview_QA.md)**
Q&A format, ~28 questions across core Java, concurrency, Spring Core, Spring Boot, Spring Data JPA, transactions, REST/MVC, testing, resilience, and security — plus a quick-fire round.

**5. [ReactJS_Interview_QA](ReactJS_Interview_QA.md)**
Same Q&A format for React: fundamentals, hooks, state management/Redux, performance, component patterns, and a React Native bridge section with two spots asking for your own specific example.

**6. [Master_Question_Checklist](Master_Question_Checklist.md)**
The highest-signal document. Part 0 lists actual questions reported by real OCBC candidates across Singapore/HK/KL — several are near-certain to come up. The rest fills gaps: microservices architecture (API Gateway, service discovery), REST fundamentals, SQL basics, open-ended full-stack system design prompts, SDLC, and behavioral questions.

**7. [Technical_Fundamentals_Gap_Fill](Technical_Fundamentals_Gap_Fill.md)**
The Java-and-beyond fundamentals doc, researched against current common Java interview question sources. Security (OWASP Top 10:2025, XSS, CSRF, SQL injection, CORS, JWT, password hashing), core OOP concepts, SOLID principles, classic design patterns implemented in real Java code (Singleton with thread-safety, Factory, Builder, Observer, Strategy, Decorator, Adapter), Java 8+ features (lambdas, streams, Optional), String/memory basics, other core-Java refreshers (access modifiers, this/super, try-with-resources, enums), a Collections Framework deep dive (Comparable vs. Comparator, fail-fast/fail-safe iterators, HashMap internals, ConcurrentHashMap), modern Java 17–21 features (records, sealed classes, pattern matching), then core JavaScript, HTML5/CSS3, a Kafka deep-dive, CI/CD and Git basics, Swagger/OpenAPI, and testing framework names. If Java feels rusty, this is the doc to actually sit down and read start to finish rather than spot-check.

**8. [Interview_Countdown_Plan](Interview_Countdown_Plan.md)**
The doc that turns the other 7 into an actual plan. A gap analysis checked directly against the JD (what's real experience vs. conceptual-only vs. genuinely missing), the exact blanks only you can fill (Java-experience anchor, React Native example, four behavioral STAR stories, the unanswered logistics questions), a calendarized day-by-day schedule from today to the 3rd — including two full 60-minute timed mock-interview runs, which nothing else in these docs actually rehearses — a timebox for what a real 1-hour interview's minute allocation probably looks like, optional cheap repo additions that would close a conceptual-only gap with real evidence (Swagger UI, a Dockerfile, a Redux slice, a Liquibase changelog), and a ranked list of what's actually most likely to go wrong. Start here to know what order to read the rest in and what's still missing; come back to it daily as your schedule.

**9. [Behavioral_Interview_Prep](Behavioral_Interview_Prep.md)**
Your real behavioral story bank — STAR-formatted, built from an actual past interview debrief plus git-verified detail pulled directly from your Laku6 repos (not templates). Covers the two proven coaching-lesson gaps from a real interview (deferring too fast under pushback, going too abstract on "how do you learn"), a full story bank (Carousell 3-month contract, the pricing-service work, the offline-functionality feature, two conflict stories, a real production payments bug, a mistake/failure story), motivation/"why" questions adapted for OCBC, and a pitfalls list. **Contains an open decision**: git history shows the pricing-service story is more accurately "hardened a teammate's Go service, caught a bug that mispriced 462 SKUs" than "migrated it from Jupyter" — Doc 1 §8/§15 now carry both framings side by side pending your call, made here in one place.

**10. [General_Backend_Engineering_QA](General_Backend_Engineering_QA.md)**
Everything else in this project is Java/Spring, React, or banking-domain-specific — this is the framework-agnostic layer underneath: networking (TCP/UDP, TLS handshake, HTTP/1.1 vs. 2 vs. 3, DNS), concurrency models (thread-per-request vs. event loop vs. actor model), distributed systems fundamentals (replication strategies, consistency models, consistent hashing, Raft/Paxos at a conceptual level, logical clocks), database internals (B-tree indexing, connection pooling, replication lag), API design (pagination, backward compatibility, contract-first), caching, message queues/event-driven architecture, backend architecture patterns (hexagonal, CQRS, orchestration vs. choreography), observability (the three pillars, correlation IDs, SLA/SLO/SLI), testing strategy (the pyramid, contract testing, load vs. stress testing), OAuth2/OIDC/mTLS, and a brief Big-O refresher. Read this if a question doesn't name Spring or React at all — "how would you design X" or "explain Y" asked at the pure-backend-engineering level.

**11. [General_Frontend_Engineering_QA](General_Frontend_Engineering_QA.md)**
Doc 10's mirror image for the other side of the stack — `ReactJS_Interview_QA.md` is React-specific, this is the layer underneath it: browser rendering fundamentals (critical rendering path, reflow vs. repaint, `requestAnimationFrame`), JS module systems and build tooling (ESM vs. CommonJS, tree-shaking, code splitting), Core Web Vitals and resource hints, framework-agnostic state management concepts (client vs. server state, stale-while-revalidate, URL as state), browser storage, frontend security (Same-Origin Policy vs. CORS, CSP, why client-side validation isn't a security control), accessibility (semantic elements, ARIA, WCAG/POUR), responsive design (viewport meta, container queries), browser networking (fetch, HTTP caching headers, WebSockets vs. SSE vs. polling), frontend testing strategy, rendering strategies (CSR/SSR/SSG/ISR, hydration), and frontend architecture patterns (MVC/MVVM, composition over inheritance). Read this if a question is framed as "frontend" or "web" without naming React specifically.

**12. [Project_Code_Walkthrough](Project_Code_Walkthrough.md)**
Different from every other doc: not concepts, the actual `bank-demo` repo, file by file, in the exact order to read it. Sixteen stops from `pom.xml` through the Dockerfile/CI, each one naming exactly which lines matter and why, plus a "question → file" lookup table at the end. Use this if asked to share your screen or "walk me through this code" — it's the script for talking through your *own* repo fluently instead of reciting Spring theory disconnected from anything runnable. Also carries the explicit honesty framing: `bank-demo` is interview prep you built, not a claim of professional Java experience — your real projects are Laku6 and CIMB Niaga.

---

## Suggested Order

If you're working through these over multiple sessions rather than all at once:

0. **[Doc 8](Interview_Countdown_Plan.md)** — read this one first, actually. It's the calendarized version of everything below, plus the gap analysis and the blanks-to-fill list.
1. **[Doc 1](OCBC_Interview_Consolidated_Prep.md)** first, in full — it's the spine everything else hangs off of. Fill in the blanks in the Java-experience section while you're there; don't skip them.
2. **[Doc 6](Master_Question_Checklist.md), Part 0 only** — read the confirmed real-candidate questions early, so everything after this lands with "oh, this is the thing that actually gets asked" context.
3. **[Doc 4](Spring_Java_Interview_QA.md) and [Doc 5](ReactJS_Interview_QA.md)** (Spring/Java and React Q&A) — the core technical content, best absorbed by talking the answers out loud, not just reading. Pair this session with **[Doc 12](Project_Code_Walkthrough.md)** — it's the guided tour through the actual code these two docs keep referencing.
4. **[Doc 2](Banking_System_Design_Playbook.md) and [Doc 3](Real_World_Engineering_Scenarios_QA.md)** (Banking Playbook and Real-World Scenarios) — once the fundamentals from step 3 are solid, these scenario-style questions will make more sense and stick better.
5. **[Doc 10](General_Backend_Engineering_QA.md) and [Doc 11](General_Frontend_Engineering_QA.md)** — read once each, straight through, ideally right after Doc 2/3 while distributed-systems thinking is already warmed up. Nothing here is fill-in-the-blank; it's pure breadth, but it's the layer a "backend depth" or "frontend depth" question can reach for without ever naming Spring or React.
6. **[Doc 7](Technical_Fundamentals_Gap_Fill.md)** (Gap Fill) and the **rest of Doc 6** — treat these as breadth/insurance, useful if you have time left, lower priority than 1 through 4 if you're short on it.

## One Thing Worth Doing Before the 3rd

Say Section 3 of Doc 1 (your Java-experience framing) and the two React-Native-specific answers in Doc 5 out loud, once, in your own words — not read silently. Those are the spots built specifically around *your* real experience rather than general concepts, and they're the ones most likely to sound rehearsed-and-hollow if you're reading them for the first time in the interview itself.
