# OCBC Interview Prep — Index
Role: Full Stack Developer (Java, ReactJS), JR00009212 — OCBC Group Wealth
Interview: 3 Aug 2026, 3:00pm SGT, Microsoft Teams

**Restructured 24 Jul 2026.** This project used to be 15 files with real duplication — the same topic (JWT, design patterns, HashMap, the pricing-service story) explained in 2-4 different places, and two files that weren't even in this index. It's now 9 files, each with exactly one job. Every topic lives in exactly one doc — if you're looking for something and it's not where this index says, it doesn't exist yet, it's not hiding in a second file.

---

## The 9 Documents

**1. [Master_Question_Checklist](Master_Question_Checklist.md)**
Read this first. The highest-signal document — actual questions reported by real OCBC candidates, from two independent sources, with a pointer to exactly which other doc has the full answer for each one. Short, and the closest thing to "here's what's actually going to get asked."

**2. [Interview_Countdown_Plan](Interview_Countdown_Plan.md)**
The schedule. A gap analysis checked against the JD, the exact blanks only you can fill, a calendarized day-by-day plan with two full 60-minute mock-interview runs, a timebox for a real 1-hour interview's minute allocation, a log of what's already been closed with real repo evidence (Swagger, Docker, CI, Redux, Liquibase), and a ranked list of what's most likely to go wrong. Tells you *when* to read everything else and *what's* still missing.

**3. [Self_Intro_And_Behavioral](Self_Intro_And_Behavioral.md)**
Everything about **you** as a candidate, and only that — self-intro, the honest Java-experience framing (has fill-in-the-blank spots, do these first), every real STAR story (git-verified, not templates), motivation/"why" questions, questions to ask them, pitfalls. One canonical version of the pricing-service story, decided in one place — not three.

**4. [Spring_Java_QA](Spring_Java_QA.md)**
The single Java/Spring reference. Java basics from scratch, core Java, OOP, SOLID, concurrency, design patterns (both how Spring uses each one *and* how to implement it from scratch — unified, not split across two docs), Java 8+, strings/memory, Collections deep dive (including Hashtable vs. HashMap and a full "design a HashMap" implementation), modern Java 17-21, Spring Core/Boot/Data JPA/Transactions/REST/Testing/Resilience/Security, Docker & Kubernetes, Liquibase. Nothing Java- or Spring-related exists in any other doc.

**5. [ReactJS_QA](ReactJS_QA.md)**
React: fundamentals, hooks, state management/Redux, performance, component patterns, and a React Native bridge section with a spot for your own specific example.

**6. [Banking_Wealth_Domain_Playbook](Banking_Wealth_Domain_Playbook.md)**
Everything banking/wealth-domain-specific: distributed-systems fundamentals with the banking lens applied (CAP, ACID/BASE, 2PC/Saga, idempotency, event sourcing, circuit breaker, Redis, SQL vs. NoSQL), 8 real-world banking scenarios, wealth/trading vocabulary, OMS/settlement, MAS TRM, AML/KYC, and a wealth-platform system-design checklist.

**7. [General_Backend_Engineering_QA](General_Backend_Engineering_QA.md)**
Everything framework-agnostic on the backend side, one-stop: API/auth 101 (REST, JWT structure, OAuth2, webhooks), networking, concurrency models, distributed systems (replication, consistency models, consistent hashing, Raft/Paxos), database internals, API design, caching, message queues (including a Kafka deep-dive), backend architecture patterns, microservices fundamentals (API Gateway, service discovery, 12-factor), observability, incident response & debugging methodology, testing strategy, algorithm design techniques (divide & conquer, DP, greedy), open-ended system-design prompts, SDLC, security fundamentals (OWASP, XSS, CSRF, SQLi, CORS, passwords), and CI/CD & Git. Read this if a question doesn't name Spring at all.

**8. [General_Frontend_Engineering_QA](General_Frontend_Engineering_QA.md)**
Doc 7's mirror image for the frontend: browser rendering fundamentals, JS module systems & build tooling, Core Web Vitals, framework-agnostic state management, browser storage, frontend security, accessibility, responsive design, browser networking, frontend testing, rendering strategies (CSR/SSR/SSG/ISR), frontend architecture patterns, core JavaScript fundamentals, HTML5/CSS3, and frontend-specific debugging (blank-screen crashes, performance diagnosis, memory leaks). Read this if a question is framed as "frontend" without naming React.

**9. [Project_Code_Walkthrough](Project_Code_Walkthrough.md)**
Not concepts — the actual `bank-demo` repo. Four views into the same code: a 16-stop reading order (`pom.xml` through the Dockerfile/CI, in the sequence a real request flows through the app), a Quick Lookup table (question → which file has the answer), an Annotation Index (look up `@Transactional`, get every place it's used), and a Concept Index (look up "bean scope," get the explanation). Use this if asked to share your screen or "walk me through this code." Carries the honesty framing: `bank-demo` is interview prep you built, not a claim of professional Java experience — your real projects are Laku6 and CIMB Niaga.

---

## Suggested Order

If you're working through these over multiple sessions rather than all at once:

1. **[Doc 1](Master_Question_Checklist.md)** — 10 minutes, confirmed real questions, so everything after this lands with "oh, this is the thing that actually gets asked" context.
2. **[Doc 2](Interview_Countdown_Plan.md)** — the calendarized plan for everything below.
3. **[Doc 3](Self_Intro_And_Behavioral.md)** — fill in the blanks (Java-experience anchor, the pricing-story framing decision, the gRPC-story outcome) before anything else; they take the longest to get right and nobody else can write them for you.
4. **[Doc 4](Spring_Java_QA.md) and [Doc 5](ReactJS_QA.md)** — the core technical content, best absorbed by talking the answers out loud. Pair this session with **[Doc 9](Project_Code_Walkthrough.md)** — the guided tour through the actual code these two docs keep referencing.
5. **[Doc 6](Banking_Wealth_Domain_Playbook.md)** — once Doc 4's fundamentals are solid, these scenario-style questions will make more sense and stick better.
6. **[Doc 7](General_Backend_Engineering_QA.md) and [Doc 8](General_Frontend_Engineering_QA.md)** — read once each, straight through. Nothing here is fill-in-the-blank; it's pure breadth, but it's the layer a "backend depth" or "frontend depth" question can reach for without ever naming Spring or React.

## One Thing Worth Doing Before the 3rd

Say the Java-experience framing and the self-intro (both in Doc 3) out loud, once, in your own words — not read silently. Those are the spots built specifically around *your* real experience rather than general concepts, and they're the ones most likely to sound rehearsed-and-hollow if you're reading them for the first time in the interview itself.
