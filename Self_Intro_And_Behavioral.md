# Self-Introduction & Behavioral Prep
For: OCBC Full Stack Developer Technical Interview, JR00009212

Everything about **you** as a candidate lives here and only here: your self-intro, the honest Java-experience framing, every real STAR story, motivation/"why" questions, questions to ask them, pitfalls, and the day-before checklist for this content specifically. Technical Q&A content lives in the other docs (`Spring_Java_QA.md`, `General_Backend_Engineering_QA.md`, etc.) — this doc is purely about your own story, said in your own voice.

---

## Table of Contents
1. Self-Introduction (~2 minutes)
2. Honest Framing: Your Java Experience
3. The STAR Method
4. Two Real Coaching Lessons — Fix These First
5. Your Real Story Bank
6. Story Bank Flexibility Map
7. Motivation & "Why" Questions
8. Questions to Ask Them
9. Pitfalls to Avoid
10. Prep Checklist

---

## 1. Self-Introduction (~2 minutes)

1. **Current role**: Senior Software Engineer at Laku6/Carousell Group, primary stack Golang backend + React Native mobile.
2. **Notable project**: your pricing-service work at Laku6 — **pick one framing, decide now, not live in the room** (full detail with real numbers in §5, "Taking initiative"):
   - **"I migrated it"** — simpler, matches what you've said before, but a follow-up question ("did you build that from scratch?") exposes that a teammate actually built the original Go service.
   - **"I hardened and fixed it"** — verified via git history: a teammate (Naufal Anbial Falah) built the original Go `pricing_manager` service in Oct 2025; your own commits start Jun 2026. You built an observability layer on top of it and caught a real margin-config bug that had silently mispriced 462 SKUs. More specific, fully defensible under follow-up, and it's the one with a real number to cite — the tradeoff is it's a different (more precise) claim than "I built the migration."
3. **Banking background, framed honestly**: prior experience at Bank CIMB Niaga, including BI-FAST integration. This is your strongest differentiator for a bank interview — but be precise that it's payments infrastructure, not wealth/trading. Frame it as: *"That gave me direct experience with the consistency, auditability, and idempotency demands of regulated financial systems — which I'd expect map closely to what an order-management platform needs, even though the specific product domain here is new to me."*
4. **Breadth**: comfortable across Python/Django, Next.js, TypeScript, increasingly building with AI-native tooling (Anthropic API, agentic workflows) — shows you stay current. (Real, current evidence for this line: several of your own Laku6 commits are co-authored with an AI pairing tool per the commit messages themselves — a concrete example ready if pushed, not just a general claim.)
5. **Why this role**: full-stack scope (Java + ReactJS) matches your full-stack background even though Java specifically is new; the regulated-finance domain plays to your CIMB Niaga experience even though the product surface (wealth vs. payments) is a genuine gap you're upfront about closing.

---

## 2. Honest Framing: Your Java Experience

The JD lists *"10+ years of hands-on software engineering experience in enterprise Java/J2EE applications"* as a **must-have**. Your actual background is coursework, side projects, or brief professional use — not primary daily-driver experience. Here's how to frame that honestly and still land well, rather than getting caught improvising in the room.

**Don't do this**: imply years of production Java you don't have, or dodge the question when asked directly. If they ask "how many years have you written Java professionally," a vague answer reads worse than a direct one.

**Do this — a three-part structure:**

1. **Name it plainly, without over-apologizing.** *"My hands-on Java has been through professional mobile work at CIMB Niaga — the BizChannel business banking app's BI-FAST integration — plus this bank-demo Spring Boot project I built for this interview. I won't pretend it's 10+ years of enterprise Java/J2EE, but it's real, not just coursework."*
2. **Immediately pivot to why that's lower-risk than it sounds.** *"The reason I'm comfortable with this role despite that is Java and Go solve the same problems — dependency injection, interfaces, concurrency, REST — with different syntax. The learning curve for me is vocabulary and ecosystem conventions, not the underlying engineering judgment."*
3. **Give one concrete anchor.** Use the BI-FAST mobile story below (§5) — it's real, professional, and directly on-domain for a bank interview.

**Filled in — the real anchor** (full STAR version in §5, "Real professional Java — the BI-FAST mobile integration"):
- What was the Java thing? → Developed the mobile side of BI-FAST for CIMB Niaga's BizChannel app (the business/corporate-client app, not retail) — integrating a vendor's third-party SDK to connect the mobile app to the core banking backend, in Java, inside BizChannel's existing legacy codebase.
- Roughly when / how long? _______________________ — **still blank, fill this in**, an interviewer will ask
- What did you build or touch? → The vendor-SDK integration itself, plus getting productive inside legacy code you didn't write. Result/outcome (did it ship, any specific bug or blocker you solved, timeline) — **still blank, fill this in before mock #1**, see §5 for exactly what's missing.

Rehearse saying this out loud once. The goal isn't to sound like you have 10 years of Java — it's to sound like someone who knows exactly what they have and isn't rattled by the gap.

**Your genuine current-week evidence, if useful as the anchor**: this entire interview-prep project (`bank-demo`) — a real Spring Boot app you built and can point to line-by-line (see `Project_Code_Walkthrough.md`) — is legitimate, honest proof you can write and reason about real Java/Spring code, even though it's prep, not professional experience. Framed correctly (per `Project_Code_Walkthrough.md`'s own honesty note), it's a stronger anchor than a vague coursework memory.

---

## 3. The STAR Method

- **Situation** — 10–15 seconds of context.
- **Task** — your specific responsibility.
- **Action** — what *you* did, not "we." Specific steps.
- **Result** — outcome, ideally quantified, plus what you learned.

Target length: **1.5–2 minutes** per answer — long enough to be complete, short enough that you're not rambling.

---

## 4. Two Real Coaching Lessons — Fix These First

These aren't hypothetical weak spots. They're from actual feedback on actual answers you gave in a real interview.

### Disagreement with a superior — you were too quick to defer
**What happened**: asked how you'd resolve a disagreement with your superior, you said you'd present data backing your reasoning. Follow-up: "what if they strongly disagree anyway?" You said you wouldn't push further, since you'd already presented your reasoning.

**The problem**: this read as passive, not diplomatic. Leading with data was the right instinct, and not being combative is genuinely good — but stopping after one round signals you defer too fast for a senior-level expectation, which is "disagree and commit" *after* exhausting collaborative options, not after one exchange.

**Your improved answer, already validated**:
> "I believe healthy disagreement can lead to better outcomes, so I approach it constructively. First, I make sure I fully understand their perspective, since they may have context about business priorities or past experience I'm not aware of. Then I present my reasoning with data and concrete trade-offs. If we still disagree, I explore alternatives — a compromise, or a small proof of concept to gather real evidence. Ultimately, if they still feel strongly after we've explored options, I respect that they have the final call and commit fully — but only after a thorough discussion first."

Notice the structure: understand → present → explore alternatives → *then* commit. Four steps, not one. This is worth rehearsing until it's automatic, since it's your one confirmed real gap.

### "How do you learn something new?" — you went too abstract
**What happened**: you used a painting analogy (learn to draw lines first, then add detail) to describe your learning process.

**The problem**: creative, but it didn't describe an actual method, and "step by step, without rushing" can read as slow ramp-up — a real concern at senior level, and directly relevant now given Java is genuinely new to you for this interview.

**Your improved answer, adapted for OCBC**:
> "I'm a hands-on learner who balances speed with depth. First, I spend focused time with documentation to build a mental model of core concepts. Second, I apply it immediately by building something real, even if small. Third, I lean on people — reviewing existing code, asking targeted questions rather than working in isolation. For this interview itself, that's exactly the process I've used to get from 'mostly Go experience' to being able to talk through Spring Boot, JPA, and design patterns fluently in a few weeks."

That last line is a genuinely strong, true closer for this specific interview — it turns the interview itself into live proof of the answer.

### Bonus lesson: "How deep do you go into understanding underlying implementations?"
You considered saying you're not someone who digs into internals, preferring to use a tool's provided solution. **Don't say this explicitly** — it reads as surface-level understanding or an inability to debug when abstractions leak, which is a red flag at senior level.

**Better framing** — "strategic depth," not "shallow depth":
> "I'm pragmatic about technical depth — I go as deep as the situation calls for. For core technologies in the stack, I invest in understanding internals because it pays off in debugging, performance, and architecture decisions. For well-established tools being used for their intended purpose, I focus on using them effectively rather than exploring every internal detail — and I go deeper the moment I hit a wall: a tricky bug, a performance issue, an architecture decision that needs it."

---

## 5. Your Real Story Bank

### Real professional Java — the BI-FAST mobile integration (CIMB Niaga)

This is your strongest possible answer to "have you used Java professionally" and "tell me about your BI-FAST work" — it's real, professional, on-domain for a bank interview, and directly Java. It also doubles as your Java-experience anchor for §2. **Result is still blank below — fill it in from memory before mock #1, this is the single most important gap left in this whole document.**

> **S:** CIMB Niaga's BizChannel app (the business/corporate-banking client app, not the retail app) needed BI-FAST payment rail support added on the mobile side. The integration point was a vendor's third-party SDK/tooling connecting the mobile app to the core banking backend, and the app itself was an existing, legacy codebase you hadn't built.
> **T:** Get productive fast in unfamiliar vendor tooling *and* an unfamiliar legacy codebase at the same time, and deliver the mobile-side BI-FAST integration in Java.
> **A:** _______________________ (how did you actually approach learning the vendor's SDK — docs, vendor support, trial and error? What specifically was hard about the legacy code — undocumented modules, an unusual architecture, old dependency versions? Did you hit a specific bug or integration blocker with the vendor tool, and how did you resolve it?)
> **R:** _______________________ (did it ship? Roughly when? Any specific outcome — a bug you caught, a deadline you hit, feedback you got? Even one concrete detail turns this from "I did some integration work" into a real story.)

**Why this matters more than it looks like**: this single story closes two gaps at once — it's real evidence against the "have you used Java professionally" question (§2), and it's your most on-domain answer for "tell me about your BI-FAST experience" (flagged as a likely deep-dive in `Interview_Countdown_Plan.md`). Don't let it stay half-built.

---

### Learning quickly / adapting fast — primary story: the Carousell 3-month contract
This is your strongest adaptability story, and it's real:

> **S:** Brought in on a 3-month contract at Carousell to help deliver a mobile project — new codebase, new team, tight deadline, no slow ramp-up allowed.
> **T:** Get productive and start delivering meaningful work fast, in an unfamiliar environment.
> **A:** Spent the first 2–3 days intensively studying the codebase and docs rather than jumping straight into tickets, identified the key people for each area of the codebase, asked clarifying questions early instead of guessing, and deliberately started with small, low-risk contributions before taking on critical features.
> **R:** Contributing meaningful code within week 1, working independently by week 3, successfully delivered the project's scope within the 3-month timeline. Carousell wanted to extend the engagement, but you had a prior commitment to return to your previous role.
> **Learning:** adaptability is as much about context and relationship-building as it is about technical learning.

**Likely follow-ups, already answered**:
- *"Why did you leave after 3 months?"* → It was always a fixed 3-month contract for a specific project. They wanted to extend; you had a prior commitment to return. Say this plainly and positively — it's not a red flag, it's a fact.
- *"How does Laku6 compare to Carousell?"* → Carousell: larger, more mature and structured. Laku6: smaller, faster-moving, more end-to-end ownership. Both valuable, in different ways.

**Secondary example, same theme**: your mobile (React Native) → backend (Golang) transition at Laku6 — fundamentals first, learned by building (the pricing story below), leaned on senior code reviews. If asked *"wasn't that a big jump?"*: core engineering fundamentals transfer; your JS async background actually helped with Go's concurrency model; the bigger shift was mobile-thinking → backend/service-architecture thinking, not just syntax.

**Bridge to OCBC**: the same approach — study first, contribute small, expand fast — is exactly what you're applying to Java/Spring for this interview.

---

### Taking initiative — hardening the pricing service against its Jupyter baseline (Laku6)

Real STAR, built from verified commits (`lk6-sales-services`, all authored by you, `venxikkevin@gmail.com`). This is your headline "greatest achievement" / "initiative" story — pick the framing decided in §1 and use it consistently everywhere you talk about this project.

> **S:** Laku6's consumer-facing SKU pricing (`sku_pricing_2c`, powering the direct-order flow) had a legacy Jupyter notebook (`ID-2c-pricing-v3`) as its long-standing source of truth, and a newer Go-based sync service a teammate had built to eventually replace it — but the notebook couldn't be retired until the Go service's pricing output matched it exactly, and nobody had verified that yet.
> **T:** Make the sync pipeline observable and trustworthy, and close whatever gap was stopping it from matching the Jupyter baseline in production.
> **A:** Built a `PricingSyncRun` tracking system from scratch — domain model, DB columns, storage methods (create/complete/fail/bulk-update), and wired it into the pipeline's `UpdatePricingID` and `UpsertFinalPricingData` steps — so every hourly sync run became independently auditable instead of a black box. While validating output against the Jupyter baseline, found a real bug: the margin config had **cap and floor logic backwards** — fields meant to guarantee a *minimum* margin were coded as a *maximum cap* instead (e.g. non-smartphone buyback margin was configured at 0.60 with no floor when it should have been 0.30 with a 50,000 IDR floor; smartphone buyback margin was 0.15-with-a-cap when it should have been 0.60-with-a-floor). Fixed the config, renamed the misleading `Cap` fields to `Min` to stop the same mistake recurring, and added test coverage plus mock-based unit tests for the whole pipeline. Also fixed two UUID-insertion bugs the new tracking system had introduced (empty string being inserted where a null was needed) and responded to code-review feedback by extracting helpers to reduce cognitive complexity.
> **R:** The margin-logic bug had been silently mispricing **462 non-smartphone SKUs** before the fix (earbuds, smartwatches, tablets) — confirmed by comparing Go output against the Jupyter baseline. Smartphone-category pricing was independently verified at 100% parity. The observability layer you built (`PricingSyncRun`) is what made this discrepancy catchable in the first place — before it existed, a wrong price would ship silently every hourly sync with no record of what changed or why.

**Backup achievement examples**, if a second one is needed or this one doesn't fit the question: Meta Conversion API integration (via n8n), Shopify/Liquid template work, BigQuery SQL work for e-commerce analytics. These aren't developed into full STAR here — flag if you want one built out.

---

### Going above and beyond — the offline-functionality feature
> **S:** The mobile app had no offline functionality — a gap nobody had flagged as a priority.
> **T:** Nothing formally assigned; you identified this yourself.
> **A:** Built a proof-of-concept unprompted, got buy-in from the team, then implemented local caching and request queueing.
> **R:** It became the team's standard pattern for handling offline scenarios going forward.

Strong because it's unprompted *and* it has a lasting result (became the standard pattern) — that second part is what elevates this above "I did some extra work."

---

### Disagreement with a peer — unit testing approaches
This one was assessed as your strongest interpersonal answer in a real interview debrief — concrete, and shows genuine collaborative instinct:

> **S:** You and another engineer used two different approaches to writing unit tests.
> **T:** Resolve the difference without it becoming a standoff.
> **A:** Prioritized understanding the other engineer's reasoning first, then shared your own thinking.
> **R:** Found a middle ground that improved team productivity and results.

Why this landed well: seeking to understand first shows emotional intelligence, finding middle ground shows synthesis over ego, and tying it to productivity shows business focus. Use this as your default "conflict with a peer" answer — it's already proven to work.

### Disagreement with a senior engineer — gRPC vs. async messaging
Your second conflict story, more technical, useful if they want a second example or something more architectural:

> **S:** Disagreement with a senior engineer over direct gRPC calls vs. async messaging (RabbitMQ/Kafka-style) for a new service.
> **T:** Reach the right technical decision without it becoming a personality clash.
> **A:** Proposed evaluating both options against actual requirements — latency needs, traffic spikes, failure handling — instead of arguing from opinion. Documented the trade-offs and asked clarifying questions about load and criticality.
> **R:** *[The real outcome needs to come from your memory — a full search of every repo's git log for an ADR or design-decision doc found nothing, which makes sense: this kind of conversation wouldn't leave a git trace unless someone wrote it down. What's independently verifiable and safe to lean on regardless of how you recall the outcome: Laku6's real production stack runs both patterns side by side — `lk6-grpc-gateway`/`lk6-bifrost-gateway` for direct synchronous service calls, and `lk6-messaging-manifests` for an async publish/subscribe system with per-subscriber retry counts and a dead-letter-style "sideline" mechanism. Whichever way the real decision went, "we ended up choosing X because of Y constraint, and the codebase today reflects that split" has real infrastructure behind it either way.]*
> **Learning:** technical disagreements should be about the problem, not ego; commit fully once the team lands on a decision.

**Delivery note**: keep the gRPC/messaging explanation to one plain sentence for a non-deeply-technical interviewer — the point of the story is how you handled it, not a systems-design lecture.

---

### A mistake or failure — the data migration disruption
> **S:** Underestimated the complexity of a data migration project early on.
> **T:** Migrate the data correctly and without disrupting the live service.
> **A:** The underestimation caused a brief service disruption. You rolled back, properly remapped the edge cases you'd missed, and ran a second, more careful attempt.
> **R:** The second attempt succeeded.
> **Learning:** always prototype against real data (not clean sample data), document edge cases explicitly before migrating, and always have a rollback plan ready before you start, not improvised after something breaks.

Good honest failure story: real impact, owned without excuse, concrete change in practice afterward — exactly what a failure answer needs.

---

### Working under pressure — the checkout payment bug
Especially relevant given OCBC is a bank — this story is *already* about a payments bug:

> **S:** A critical production bug was affecting checkout.
> **T:** Diagnose and fix it fast, under real pressure, without making things worse.
> **A:** Stayed calm, reproduced the issue methodically, traced it to a race condition in payment processing, fixed it, and deployed with monitoring in place to confirm the fix held.
> **R:** Resolved within roughly 3 hours; documented the incident afterward.

If asked to connect this to the role: a race condition in payment processing is precisely the kind of concurrency/consistency issue `Banking_Wealth_Domain_Playbook.md`'s race-condition scenario covers conceptually — this is you having actually lived that scenario, not just studied it.

---

### Receiving difficult feedback — code review that led to mentorship
> **S:** A senior engineer flagged that some of your code was functional but hard to maintain, during a review.
> **T:** Respond well to the critique and actually improve.
> **A:** Asked for specifics rather than getting defensive, then refactored using better patterns — clearer naming, smaller functions, better separation of concerns.
> **R:** That engineer became an informal mentor afterward.

Strong because the result isn't just "I fixed the code" — it's a relationship that came out of handling criticism well.

---

### Red flags when joining a new project
Not strictly STAR, but a real, ready answer if asked directly:
1. Unclear requirements or scope.
2. Lack of documentation or knowledge-sharing.
3. Poor communication channels — no standups, no clear way to raise blockers.

Close by naming how you personally address these proactively: ask clarifying questions early, document as you go, communicate blockers rather than sitting on them.

---

## 6. Story Bank Flexibility Map
You don't need a separate story for every possible question — the same story can answer more than one, depending on which part you emphasize:

| Story | Can also answer |
|---|---|
| BI-FAST mobile integration (CIMB Niaga) | "Have you used Java professionally," "tell me about BI-FAST," learning unfamiliar tooling, working with legacy code |
| Carousell 3-month contract | Adapting quickly, working under pressure/deadline, prioritization (ramping up *and* delivering scope at once) |
| Pricing story (Laku6) | Greatest achievement, initiative, technical problem-solving |
| Checkout payment bug | Pressure/deadline, technical problem-solving, debugging methodology |
| Unit testing disagreement | Conflict with a peer, collaboration, communication |
| Data migration failure | Mistake/failure, risk management, what you'd do differently |
| Offline functionality feature | Above and beyond, initiative, ownership |

---

## 7. Motivation & "Why" Questions

### "Why are you interested in this role?"
The honest and grounded version: the role is genuinely full-stack, matching your background; the domain is banking, playing directly to your CIMB Niaga/BI-FAST experience (including real, professional mobile-side Java on that integration — §5) even though the specific product surface (wealth/trading vs. payments) is new; and deep enterprise Java/J2EE specifically is a deliberate stretch you're taking on with your eyes open, which the interview itself is proof of, not a claim.

### "Why are you leaving your current role?"
Growth-framed only: grateful for the Laku6 experience, but want to deepen full-stack expertise, work on larger-scale systems in a regulated environment, build on the CIMB Niaga foundation rather than start over. Never mention salary or anything negative about Laku6.

### "Where do you see yourself in 3–5 years?"
**Handle carefully** — your real long-term interest is engineering management, but that should not be the headline answer, here or anywhere:
1. Lead (~80%) with technical growth — deeper full-stack and backend expertise, becoming someone trusted for complex problems.
2. Mention (~15%) interest in the people side — mentoring, cross-functional coordination — as a natural extension of technical growth, not a pivot away from it.
3. Note (~5%), only if pushed, that engineering management could be a long-term interest — 7-plus years out — but only after a strong technical foundation is built. "The best managers were excellent engineers first" is a fine line to close on if needed.
4. Tie it back to why *this* role — hands-on full-stack work in a new language and domain — is the right next step *now*.

Avoid: a 3-year timeline to management, "I don't want to code forever," anything that implies disinterest in hands-on work.

### Strengths and weaknesses
**Strengths** (pick two or three, each backed by a real story above): adaptability across stacks — mobile to backend, proven by the Carousell and Laku6 transitions; problem-solving under pressure — the checkout bug; clear communication — the unit-testing disagreement.
**Weakness**: your limited hands-on Java is the honest, low-risk one for *this* interview specifically — you already have a real plan for closing that gap (§2 above), which is a far stronger answer than a vague, safe platitude.

### Salary
Deflect to market-research framing rather than leading with a number: you've researched the market and have a range in mind based on responsibilities and experience, but you're more focused on the role and the learning opportunity — then ask them for their budget range in return.

### "Do you have other offers or other conversations happening?"
Be honest but strategic. You've genuinely been in an active search — other processes exist. Frame it as: yes, there are other conversations, but this role is a strong fit for the specific reasons already covered (full-stack, banking domain, Java as an intentional stretch) — without necessarily naming other companies unless asked directly.

### "Why Singapore?"
Professional growth in a mature tech ecosystem, the specific technical stretch this role offers, and Singapore's position as a regional tech hub. Acknowledge the CIMB Niaga/Laku6 experience with genuine gratitude while framing the move as seeking a bigger, more regulated-environment challenge.

### "Why should we hire you over other candidates?"
Strong fundamentals across mobile and backend, proven learning agility — React Native → Golang → the Carousell onboarding → now Java for this interview — and genuine motivation that actually lines up with the role, not generic enthusiasm.

---

## 8. Questions to Ask Them

**Technical/team questions:**
1. What does the current architecture look like — monolith, microservices, or a mix, and how far along is the modernization the JD mentions?
2. What's the split between building new wealth-product features vs. maintaining/supporting production trading workflows?
3. How does the team handle real-time or near-real-time market data / pricing feeds in the architecture?
4. What's the biggest technical challenge the team is working through right now?
5. What would success look like in the first six months in this role?

**General-purpose closer, regardless of round:**
> "Based on our conversation today, do you have any concerns about my fit for this role that I can address?"
It signals confidence and gives you one last chance to handle an objection before the conversation ends.

**Logistics — only if not answered before the interview:**
You asked these on 15 Jul (permanent vs. contract, work arrangement, location/relocation, team structure/reporting line) and they were routed to Lee Kin Kit with no reply yet. If they're still unanswered by 3 Aug, it's fair to raise once, briefly, near the end: *"I'd sent a few questions about the role setup ahead of this call — happy to follow up separately if now isn't the moment, but wanted to flag it in case it's useful context for our conversation."* Don't lead with this; it's a wrap-up item, not an opener.

---

## 9. Pitfalls to Avoid
- Don't badmouth Laku6, Carousell, or CIMB Niaga, even in a conflict or failure story.
- Don't overclaim on things you're genuinely light on — same honesty principle as the Java-experience framing in §2, applied more broadly.
- Keep answers to 1.5–2 minutes. Use STAR. Don't over-explain technical jargon (gRPC, race conditions) to a non-technical interviewer — one plain sentence, then move on.
- End every conflict or disagreement story on a collaborative, non-ego note.
- Don't lead with engineering-management ambition under any framing of the "where do you see yourself" question.
- Don't make salary the headline reason for wanting the role.

---

## 10. Prep Checklist

**24 hours before:**
- Confirm your pricing-story framing decision from §1 is made and consistent — don't decide live in the room.
- Recall the real gRPC-vs-messaging outcome from your own memory — nothing more to search for, it's not in any repo.
- Rehearse the two coaching-lesson answers (§4) out loud until they're automatic, not read.
- Say §1 (self-intro) and §2 (Java framing) out loud once, in your own words.
- Prepare 3–5 questions for the interviewer (§8).

**1 hour before:**
- Quiet, well-lit space; water nearby; notebook and pen.
- Close distracting tabs/apps, phone on silent.
- Join a few minutes early.

**During:**
- Brief pause before answering — you don't need to fill silence instantly.
- Use STAR; keep answers tight.
- Ask a clarifying question if something's ambiguous rather than guessing what they mean.
