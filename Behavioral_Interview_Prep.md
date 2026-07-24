# Behavioral Interview Prep — With Real Examples
For: OCBC Full Stack Developer Technical Interview (and any later round)

---

## For Claude Code: Your Task — RESOLVED 22 Jul 2026

Both gaps below were investigated directly against Kevin's real repos at `/Users/kevin/Projects/laku6/`. Findings:

1. **Pricing-migration story** — resolved, and it changed the story. Verified via `git log`/`git show` in `lk6-sales-services`: the Go `pricing_manager` service was **not** built by Kevin — a teammate (Naufal Anbial Falah, commit `47d27eb2`, 27 Oct 2025) built the original service. Kevin's real, verified contribution (4–19 Jun 2026, all commits under his own `venxikkevin@gmail.com` identity) was building a `PricingSyncRun` observability layer on top of it and then finding and fixing two real, specific bugs while validating the Go service's output against the legacy Jupyter notebook (`ID-2c-pricing-v3`) baseline it was meant to eventually replace. Full real numbers below. **This is a stronger, more specific, fully verifiable story than "I migrated it" — but it is a different claim.** See the correction note in that section.
2. **gRPC vs. async-messaging disagreement story** — searched `git log --all --grep` across every repo in `/Users/kevin/Projects/laku6/` for any ADR, design-decision doc, or commit message referencing this exact debate. **Nothing found** — this kind of conversation wouldn't leave a git trace unless someone wrote it down, and nobody did. What *is* verifiable: Laku6's real stack does use both patterns in production side by side — `lk6-grpc-gateway` / `lk6-bifrost-gateway` for direct synchronous service calls, and `lk6-messaging-manifests` for an async publish/subscribe system with per-subscriber retry counts and a "sideline" (dead-letter-style) mechanism. That's real technical texture you can use regardless of which way your actual memory of the decision goes — but the specific disagreement and its outcome still needs to come from you; I'm not inventing one.

Everything else in this doc is unchanged from before.

---

## Where this version comes from
The first version of this doc was built on fill-in-the-blank templates because I didn't have your real stories. You then found two documents from a prior interview process (GovTech Singapore) and an actual recruiter interview debrief with real feedback you received — this version replaces the templates with those real stories, adapted from GovTech's context to OCBC's where the framing needs to change (their civic-mission angle doesn't apply here; the banking-domain and Java-stretch angle does). The two coaching lessons from your real debrief are the most important thing in this whole doc — they're proven weak spots, not hypothetical ones.

---

## The STAR Method

- **Situation** — 10–15 seconds of context.
- **Task** — your specific responsibility.
- **Action** — what *you* did, not "we." Specific steps.
- **Result** — outcome, ideally quantified, plus what you learned.

Target length: **1.5–2 minutes** per answer — long enough to be complete, short enough that you're not rambling.

---

## Two Real Coaching Lessons — Fix These First

These aren't hypothetical weak spots. They're from actual feedback on actual answers you gave in a real interview.

### 1. Disagreement with a superior — you were too quick to defer
**What happened**: asked how you'd resolve a disagreement with your superior, you said you'd present data backing your reasoning. Follow-up: "what if they strongly disagree anyway?" You said you wouldn't push further, since you'd already presented your reasoning.

**The problem**: this read as passive, not diplomatic. Leading with data was the right instinct, and not being combative is genuinely good — but stopping after one round signals you defer too fast for a senior-level expectation, which is "disagree and commit" *after* exhausting collaborative options, not after one exchange.

**Your improved answer, already validated**:
> "I believe healthy disagreement can lead to better outcomes, so I approach it constructively. First, I make sure I fully understand their perspective, since they may have context about business priorities or past experience I'm not aware of. Then I present my reasoning with data and concrete trade-offs. If we still disagree, I explore alternatives — a compromise, or a small proof of concept to gather real evidence. Ultimately, if they still feel strongly after we've explored options, I respect that they have the final call and commit fully — but only after a thorough discussion first."

Notice the structure: understand → present → explore alternatives → *then* commit. Four steps, not one. This is worth rehearsing until it's automatic, since it's your one confirmed real gap.

### 2. "How do you learn something new?" — you went too abstract
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

## Your Real Story Bank

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

**Secondary example, same theme**: your mobile (React Native) → backend (Golang) transition at Laku6 — fundamentals first, learned by building (the pricing migration below), leaned on senior code reviews. If asked *"wasn't that a big jump?"*: core engineering fundamentals transfer; your JS async background actually helped with Go's concurrency model; the bigger shift was mobile-thinking → backend/service-architecture thinking, not just syntax.

**Bridge to OCBC**: the same approach — study first, contribute small, expand fast — is exactly what you're applying to Java/Spring for this interview.

---

### Taking initiative — hardening the pricing service against its Jupyter baseline (Laku6)

**Correction from your other prep docs, read this first**: Consolidated_Prep §15 and the Countdown Plan currently frame this as *"you migrated pricing logic from Python/Jupyter to a Go service"* — verified git history shows that's not quite right. A teammate (Naufal Anbial Falah) built the original Go `pricing_manager` service in `lk6-sales-services` on 27 Oct 2025. Your own commits on this system don't start until 4 Jun 2026, seven months later. What you actually did in that window is real, specific, and verifiable — but it's "hardened and fixed the replacement service," not "built the migration." Worth deciding deliberately which framing you use, since a follow-up question ("did you build that from scratch?") would expose the gap if you claim the stronger version.

Real STAR, built from verified commits (`lk6-sales-services`, all authored by you, `venxikkevin@gmail.com`):

> **S:** Laku6's consumer-facing SKU pricing (`sku_pricing_2c`, powering the direct-order flow) had a legacy Jupyter notebook (`ID-2c-pricing-v3`) as its long-standing source of truth, and a newer Go-based sync service a teammate had built to eventually replace it — but the notebook couldn't be retired until the Go service's pricing output matched it exactly, and nobody had verified that yet.
> **T:** Make the sync pipeline observable and trustworthy, and close whatever gap was stopping it from matching the Jupyter baseline in production.
> **A:** Built a `PricingSyncRun` tracking system from scratch — domain model, DB columns, storage methods (create/complete/fail/bulk-update), and wired it into the pipeline's `UpdatePricingID` and `UpsertFinalPricingData` steps — so every hourly sync run became independently auditable instead of a black box. While validating output against the Jupyter baseline, found a real bug: the margin config had **cap and floor logic backwards** — fields meant to guarantee a *minimum* margin were coded as a *maximum cap* instead (e.g. non-smartphone buyback margin was configured at 0.60 with no floor when it should have been 0.30 with a 50,000 IDR floor; smartphone buyback margin was 0.15-with-a-cap when it should have been 0.60-with-a-floor). Fixed the config, renamed the misleading `Cap` fields to `Min` to stop the same mistake recurring, and added test coverage plus mock-based unit tests for the whole pipeline. Also fixed two UUID-insertion bugs the new tracking system had introduced (empty string being inserted where a null was needed) and responded to code-review feedback by extracting helpers to reduce cognitive complexity.
> **R:** The margin-logic bug had been silently mispricing **462 non-smartphone SKUs** before the fix (earbuds, smartwatches, tablets) — confirmed by comparing Go output against the Jupyter baseline. Smartphone-category pricing was independently verified at 100% parity. The observability layer you built (`PricingSyncRun`) is what made this discrepancy catchable in the first place — before it existed, a wrong price would ship silently every hourly sync with no record of what changed or why.

**Bonus, if it's useful**: several of these commits are co-authored with an AI pairing tool (Claude) per your own commit messages — genuine, current evidence for the "AI-native tooling" line already in your self-intro (Consolidated_Prep §15, point 4), if you want a concrete example ready rather than a general claim.

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
This one was assessed as your strongest interpersonal answer in the real debrief — concrete, and shows genuine collaborative instinct:

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
> **R:** *[Not found in repo history — searched every repo's git log for an ADR or design-decision doc, nothing exists. This has to come from your own memory. What is verifiable and safe to lean on regardless of how you recall the outcome: Laku6's actual production stack runs both patterns side by side — `lk6-grpc-gateway`/`lk6-bifrost-gateway` for direct synchronous service calls, and `lk6-messaging-manifests` for an async publish/subscribe system with per-subscriber retry counts and a dead-letter-style "sideline" mechanism. So whichever way the real decision went, the technical texture of your answer — "we ended up choosing X because of Y constraint, and the codebase today reflects that split" — has real infrastructure behind it either way.]*
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

If asked to connect this to the role: a race condition in payment processing is precisely the kind of concurrency/consistency issue your Banking Playbook's race-condition scenario covers conceptually — this is you having actually lived that scenario, not just studied it.

---

### Receiving difficult feedback — code review that led to mentorship
> **S:** A senior engineer flagged that some of your code was functional but hard to maintain, during a review.
> **T:** Respond well to the critique and actually improve.
> **A:** Asked for specifics rather than getting defensive, then refactored using better patterns — clearer naming, smaller functions, better separation of concerns.
> **R:** That engineer became an informal mentor afterward.

Strong because the result isn't just "I fixed the code" — it's a relationship that came out of handling criticism well.

---

### Red flags when joining a new project
Not strictly STAR, but a real, ready answer if asked directly (a confirmed real question from a past process, plausible at OCBC too given the "team structure and role expectations" close):
1. Unclear requirements or scope.
2. Lack of documentation or knowledge-sharing.
3. Poor communication channels — no standups, no clear way to raise blockers.

Close by naming how you personally address these proactively: ask clarifying questions early, document as you go, communicate blockers rather than sitting on them.

---

## Story Bank Flexibility Map
You don't need a separate story for every possible question — the same story can answer more than one, depending on which part you emphasize. Useful to know going in so you're not caught flat if a question doesn't exactly match a category above:

| Story | Can also answer |
|---|---|
| Carousell 3-month contract | Adapting quickly, working under pressure/deadline, prioritization (ramping up *and* delivering scope at once) |
| Pricing migration (Laku6) | Greatest achievement, initiative, technical problem-solving |
| Checkout payment bug | Pressure/deadline, technical problem-solving, debugging methodology |
| Unit testing disagreement | Conflict with a peer, collaboration, communication |
| Data migration failure | Mistake/failure, risk management, what you'd do differently |
| Offline functionality feature | Above and beyond, initiative, ownership |

---

## Motivation & "Why" Questions — Adapted for OCBC

### "Why are you interested in this role?"
Not GovTech's civic-mission framing — for OCBC, the honest and grounded version: the role is genuinely full-stack, matching your background; the domain is banking, playing directly to your CIMB Niaga/BI-FAST experience even though the specific product surface (wealth/trading vs. payments) is new; and Java specifically is a deliberate stretch you're taking on with your eyes open, which the interview itself is proof of, not a claim.

### "Why are you leaving your current role?"
Growth-framed only, same principle as before: grateful for the Laku6 experience, but want to deepen full-stack expertise, work on larger-scale systems in a regulated environment, build on the CIMB Niaga foundation rather than start over. Never mention salary or anything negative about Laku6.

### "Where do you see yourself in 3–5 years?"
**Handle carefully** — your real long-term interest is engineering management, but that should not be the headline answer, here or anywhere:
1. Lead (~80%) with technical growth — deeper full-stack and backend expertise, becoming someone trusted for complex problems.
2. Mention (~15%) interest in the people side — mentoring, cross-functional coordination — as a natural extension of technical growth, not a pivot away from it.
3. Note (~5%), only if pushed, that engineering management could be a long-term interest — 7-plus years out — but only after a strong technical foundation is built. "The best managers were excellent engineers first" is a fine line to close on if needed.
4. Tie it back to why *this* role — hands-on full-stack work in a new language and domain — is the right next step *now*.

Avoid: a 3-year timeline to management, "I don't want to code forever," anything that implies disinterest in hands-on work.

### Strengths and weaknesses
**Strengths** (pick two or three, each backed by a real story above): adaptability across stacks — mobile to backend, proven by the Carousell and Laku6 transitions; problem-solving under pressure — the checkout bug; clear communication — the unit-testing disagreement.
**Weakness**: your limited hands-on Java is the honest, low-risk one for *this* interview specifically — you already have a real plan for closing that gap (Section 3 of your Consolidated Prep doc), which is a far stronger answer than a vague, safe platitude.

### Salary
Deflect to market-research framing rather than leading with a number: you've researched the market and have a range in mind based on responsibilities and experience, but you're more focused on the role and the learning opportunity — then ask them for their budget range in return. **Don't reuse the GovTech-specific figure (S$6,800–7,500/month) here** — that was benchmarked for a different company and process; use whatever OCBC-specific range you've actually researched for this role instead.

### "Do you have other offers or other conversations happening?"
Be honest but strategic. You've genuinely been in an active search — other processes exist. Frame it as: yes, there are other conversations, but this role is a strong fit for the specific reasons already covered (full-stack, banking domain, Java as an intentional stretch) — without necessarily naming other companies unless asked directly.

### "Why Singapore?"
Professional growth in a mature tech ecosystem, the specific technical stretch this role offers, and Singapore's position as a regional tech hub. Acknowledge the CIMB Niaga/Laku6 experience with genuine gratitude while framing the move as seeking a bigger, more regulated-environment challenge.

### "Why should we hire you over other candidates?"
Strong fundamentals across mobile and backend, proven learning agility — React Native → Golang → the Carousell onboarding → now Java for this interview — and genuine motivation that actually lines up with the role, not generic enthusiasm.

---

## Questions to Ask Them
Your Consolidated Prep doc already has OCBC-specific technical questions to ask (Section 16). One general-purpose closer worth having ready regardless of round:
> "Based on our conversation today, do you have any concerns about my fit for this role that I can address?"
It signals confidence and gives you one last chance to handle an objection before the conversation ends.

---

## Pitfalls to Avoid
- Don't badmouth Laku6, Carousell, or CIMB Niaga, even in a conflict or failure story.
- Don't overclaim on things you're genuinely light on — this is the same honesty principle as the Java-experience framing in your Consolidated Prep doc, just applied more broadly.
- Keep answers to 1.5–2 minutes. Use STAR. Don't over-explain technical jargon (gRPC, race conditions) to a non-technical interviewer — one plain sentence, then move on.
- End every conflict or disagreement story on a collaborative, non-ego note.
- Don't lead with engineering-management ambition under any framing of the "where do you see yourself" question.
- Don't make salary the headline reason for wanting the role.

---

## Prep Checklist

**24 hours before:**
- Decide which framing you're using for the pricing story — "I migrated it" (your other docs' current framing) or the verified, more specific "I hardened and fixed the replacement service, catching a bug that mispriced 462 SKUs" (this doc's corrected version) — and make sure Consolidated_Prep §15 and the Countdown Plan say the same thing you'll say out loud.
- Recall the real gRPC-vs-messaging outcome from memory — repo archaeology couldn't find it; only you have it.
- Rehearse the two coaching-lesson answers (disagreement with a superior, learning process) out loud until they're automatic, not read.
- Prepare 3–5 questions for the interviewer.

**1 hour before:**
- Quiet, well-lit space; water nearby; notebook and pen.
- Close distracting tabs/apps, phone on silent.
- Join a few minutes early.

**During:**
- Brief pause before answering — you don't need to fill silence instantly.
- Use STAR; keep answers tight.
- Ask a clarifying question if something's ambiguous rather than guessing what they mean.

---

## Before the Interview
Most of the story bank above is real and ready. The pricing story is now fully verified with real numbers (462 SKUs) — the only decision left there is which framing to use, and to make sure your other docs match it. The gRPC story's outcome couldn't be found in any repo and needs to come from your own memory — that's on you, not something further searching will solve. Otherwise: rehearse the two coaching-lesson answers until they're automatic. That's a much shorter list than starting from scratch.
