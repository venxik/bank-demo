# Master Interview Question Checklist
For: OCBC Full Stack Developer Technical Interview

The highest-signal document in this whole project: actual questions reported by real OCBC candidates, from two independent sources. Everything else in this project is organized by topic; this one is organized by "will this actually get asked" — read it first, then use it as the map to which other doc has the full answer.

---

## Confirmed Real OCBC Candidate Questions

Pulled from multiple independently-reported OCBC Software Developer/Engineer interviews (Singapore, Hong Kong, Kuala Lumpur). The pattern is consistent across reports, which is itself useful signal — this is a stable, repeated interview structure, not one-off variation:

1. **Self-introduction and walkthrough of past projects, including tech stack used** → `Self_Intro_And_Behavioral.md` §1.
2. **"What is the design pattern and how do you use it in Spring Boot?"** → `Spring_Java_QA.md` Part 5 — full worked example (Proxy/AOP + the self-invocation gotcha) plus 10 more patterns each with a Spring class or `bank-demo` file backing it.
3. **"There are two types of Autowired — explain what they are."** → `Spring_Java_QA.md` Part 10 — field vs. constructor injection, and *why* constructor injection is preferred. This one is confirmed to come up directly, word for word, across multiple reports — know it cold.
4. **"Why do we need to use Spring?"** → `Spring_Java_QA.md` Part 10.
5. **"What is Redis?"** → `Banking_Wealth_Domain_Playbook.md` (Redis section, Part 1).
6. **"Difference between SQL and NoSQL database, pros and cons"** → `Banking_Wealth_Domain_Playbook.md` (SQL vs. NoSQL section, Part 1).
7. **"Did you use microservices in your past projects? Please share the details."** → Have your Laku6 pricing-service example ready end-to-end (`Self_Intro_And_Behavioral.md` §1/§5; microservices concepts in `General_Backend_Engineering_QA.md` Part 16).
8. **"Describe the software development life cycle."** → `General_Backend_Engineering_QA.md` Part 19.
9. **A question on handling a performance issue** (reported from the Hong Kong office specifically) → `General_Backend_Engineering_QA.md` Part 17.
10. One Kuala Lumpur report mentions a more junior/digital-assessment format with situational scenario questions — less likely at your level/role, but worth knowing OCBC does use scenario-based formats elsewhere in the org.

**Read across all the reports**: nobody described live coding tests for this stage — every account describes a conversational, concept-level technical discussion. That matches what your original prep doc said. Don't over-index on LeetCode-style prep; index on being able to talk fluently through the concepts above.

**One more thing worth knowing, not a question but a real warning**: a Sep 2025 OCBC Singapore candidate reported going through multiple interview rounds after being transparent about current compensation from the start, then receiving a final offer significantly below that figure anyway. This isn't universal, but it's a real, recent, first-hand account — worth being proactive rather than passive about compensation expectations earlier in the process (e.g., in the follow-up email already sent), rather than assuming early transparency alone protects you from a mismatched final offer.

---

## Second Confirmed Source — Nodeflair Aggregation (107 reported OCBC Bank interview questions, pulled Jul 2026)

A separate, independent aggregation of real candidate-reported questions. Cross-checked against everything else in this project — most were already covered; the ones that weren't got a full new answer, listed below with where to find it:

1. **"What is the difference between String and StringBuffer in Java?"** → `Spring_Java_QA.md` Part 7.
2. **"Can you explain a design pattern and its importance in software development?"** → `Spring_Java_QA.md` Part 5.
3. **"Explain algorithm design techniques such as divide and conquer, dynamic programming, greedy algorithms and their complexity analysis."** → `General_Backend_Engineering_QA.md` Part 12 — a genuine gap until this source surfaced it; nothing else in this project covered algorithm design paradigms at all.
4. **"What are the core concepts of java programming including basic syntax, data types, control structures, memory management and exception handling?"** → `Spring_Java_QA.md` Part 0 — everything else in this project assumed Java syntax itself was already known; this is the from-scratch layer underneath that.
5. **"What is oauth2?"** → `General_Backend_Engineering_QA.md` Part 0 and Part 11.
6. **"What do you know about jwt?"** → `General_Backend_Engineering_QA.md` Part 0.
7. **"What is the difference between a Singleton and a Prototype bean?"** → `Spring_Java_QA.md` Part 10 — the bean-scopes answer now directly contrasts the two, not just names them.
8. **"Tell me about the java collections you are familiar with."** → `Spring_Java_QA.md` Part 1 and Part 8.
9. **"Tell me about the design patterns you have used in java programming?"** → Same as #2.
10. **"Explain the concept of microservice architecture."** → `Self_Intro_And_Behavioral.md`; `General_Backend_Engineering_QA.md` Part 16.
11. **"What are the differences between a hashtable and a hashmap?"** → `Spring_Java_QA.md` Part 8 — contrasts the legacy `Hashtable` against modern `HashMap` directly.
12. **"Design a hashmap."** → `Spring_Java_QA.md` Part 8 — a real "implement this" question, not just "explain how it works." Full working implementation (buckets, collision chaining, load-factor resize) there, since this is a genuine exception to "nobody reported live coding" — worth having ready to actually write out, not just describe.

---

## What This Means For Your Prep Priority

Given both sources above are real, repeated, confirmed signal: the two-Autowired-types question, the Spring design-pattern question, Redis, SQL vs. NoSQL, "describe a microservice you built," and the newer Nodeflair-sourced additions (algorithm design techniques, Java basics from scratch, Hashtable vs. HashMap, designing a HashMap, Singleton vs. Prototype) are near-certain to come up in some form. Weight your remaining prep time toward the docs this checklist points to first.
