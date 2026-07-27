# ReactJS Interview Q&A
For: OCBC Full Stack Developer Technical Interview

Same format as the Spring/Java doc: question, tight interview-ready answer, then a short code example. This is closer to home for you given React Native — the last section leans into that directly.

---

## Core React

**Q: What is React, in one sentence, and what problem does it solve?**
A: A JavaScript library for building UIs out of composable, reusable components, using a declarative model — you describe what the UI should look like for a given state, and React figures out how to update the DOM to match — instead of manually mutating the DOM imperatively.

```jsx
// declarative — describe the end state, React figures out the DOM diff
function AccountRow({ account }) {
  return <tr><td>{account.ownerName}</td><td>{account.balance}</td></tr>
}

// vs. the imperative equivalent you'd otherwise hand-write
const row = document.createElement('tr')
row.innerHTML = `<td>${account.ownerName}</td><td>${account.balance}</td>`
```

**Q: What is the virtual DOM, and why does it matter?**
A: An in-memory, lightweight representation of the real DOM. When state changes, React builds a new virtual DOM tree, diffs it against the previous one ("reconciliation"), and applies only the minimal set of real DOM updates needed — far cheaper than re-rendering the whole page, and it's what lets you just describe the UI without manually tracking what changed.

```jsx
// balance changes from 100 to 150 — React diffs the two trees,
// finds only the <td> text differs, and patches just that one DOM node,
// not the whole <tr> or the table around it
<tr><td>Kevin</td><td>100</td></tr>  // previous render
<tr><td>Kevin</td><td>150</td></tr>  // next render
```

**Q: JSX — what is it, actually?**
A: Syntactic sugar for a plain function call that builds the virtual DOM tree — it looks like HTML embedded in JavaScript, but it's really just a more readable way to write that call. Historically (React ≤16) it compiled to `React.createElement(...)`; since React 17's "automatic runtime" — what `bank-demo`'s own `@vitejs/plugin-react` uses by default, and what this project's React 19 actually runs — it compiles to `_jsx(...)` imported straight from `react/jsx-runtime` instead, specifically so you no longer need `import React from 'react'` in every file just to use JSX. The mental model (JSX → a function call → a virtual DOM node) is unchanged; only which function gets called under the hood is different.

```jsx
// what you write
const el = <h1 className="title">bank-demo</h1>

// classic runtime (React <=16, or explicit opt-out) — needs React in scope
const el = React.createElement('h1', { className: 'title' }, 'bank-demo')

// automatic runtime (React 17+, this project's default) — compiler injects this import itself
import { jsx as _jsx } from 'react/jsx-runtime'
const el = _jsx('h1', { className: 'title', children: 'bank-demo' })
```

---

## Hooks

**Q: What problem do hooks solve, and why did React move away from class components?**
A: Before hooks, only class components could hold state and lifecycle logic, which pushed toward class hierarchies and made it hard to reuse stateful logic between components — patterns like HOCs and render props existed specifically to work around that limitation. Hooks let function components hold state and side effects directly, and let you extract and reuse stateful logic as plain functions (custom hooks) instead.

```jsx
// before hooks — state only existed on a class
class Counter extends React.Component {
  state = { count: 0 }
  render() { return <button onClick={() => this.setState({ count: this.state.count + 1 })}>{this.state.count}</button> }
}

// with hooks — same thing, plain function
function Counter() {
  const [count, setCount] = useState(0)
  return <button onClick={() => setCount(count + 1)}>{count}</button>
}
```

**Q: `useState` — what does it return, and what's a common mistake with it?**
A: Returns a `[value, setter]` pair. Common mistake: mutating state directly (`state.push(x)` on an array) instead of calling the setter with a new value/reference. React compares references to decide whether to re-render, so mutating in place silently breaks re-rendering.

```jsx
const [accounts, setAccounts] = useState([])

accounts.push(newAccount)      // mutates in place...
setAccounts(accounts)          // ...then hands back the SAME reference — React bails out, no re-render

setAccounts([...accounts, newAccount]) // correct — a new array reference, triggers re-render
```

**Q: `useEffect` — what does the dependency array do, and what's the classic footgun?**
A: The dependency array tells React when to re-run the effect — it re-runs whenever any listed value changes since the last render. Footguns: (1) omitting a dependency you actually use inside the effect → a stale closure, reading an old value instead of the current one; (2) using an empty array `[]` to mean "run once on mount" when the effect actually depends on props/state that can change; (3) forgetting the cleanup function (the function you `return` from the effect) for anything needing teardown — subscriptions, timers, event listeners — leading to leaks or duplicate listeners on re-render.

```jsx
useEffect(() => {
  const timer = setInterval(() => refresh(accountId), 5000)
  return () => clearInterval(timer) // cleanup — without this, a new timer stacks on every render
}, [accountId]) // re-runs only when accountId changes — omit it and you'd read a stale accountId forever
```

**Q: `useMemo` vs. `useCallback` — what's the actual difference?**
A: Both memoize something across renders based on a dependency array, avoiding unnecessary recomputation. `useMemo` memoizes a computed *value*. `useCallback` memoizes a *function reference* — `useCallback(fn, deps)` is essentially shorthand for `useMemo(() => fn, deps)`. You reach for `useCallback` specifically when passing a function to a memoized child (`React.memo`) and need the reference to stay stable, or the memoization is defeated.

```jsx
const sorted = useMemo(() => accounts.toSorted((a, b) => a.balance - b.balance), [accounts]) // memoized value

const handleDeposit = useCallback((id, amount) => {
  dispatch(deposit({ id, amount }))
}, [dispatch]) // memoized function reference — stable across renders
```

**Q: What are the "Rules of Hooks," and why do they exist?**
A: (1) Only call hooks at the top level — never inside loops, conditions, or nested functions. (2) Only call hooks from React function components or custom hooks. This exists because React tracks hooks by *call order* between renders — there's no name-based lookup — so a conditionally-called hook would shift the order and corrupt every hook's state after it.

```jsx
// WRONG — hook inside a condition, call order shifts between renders
if (accounts.length > 0) {
  const [selected, setSelected] = useState(null)
}

// correct — hook always called, condition lives inside
const [selected, setSelected] = useState(null)
if (accounts.length > 0) { /* use selected here */ }
```

**Q: What's a custom hook? Give a simple example.**
A: A plain JavaScript function, by convention prefixed `use`, that calls other hooks internally to encapsulate reusable stateful logic — e.g., a `useFetch(url)` hook wrapping `useState` + `useEffect` to handle fetching, loading, and error state, so every component needing to fetch data doesn't repeat that boilerplate.

```jsx
function useFetch(url) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    fetch(url).then(r => r.json()).then(setData).catch(err => setError(err.message))
  }, [url])
  return { data, error }
}

// usage
const { data: accounts, error } = useFetch('/accounts')
```

---

## State Management

**Q: When do you lift state up, vs. reach for Context, vs. reach for Redux?**
A: **Lift state up** when two sibling components need to share state and a common parent can hold it — simplest option, try this first. **Context API** when state needs to reach many components at different nesting depths without prop-drilling (current theme, logged-in user) — but Context re-renders every consumer on any change, so it's not ideal for high-frequency updates. **Redux** (or similar) for complex, frequently-updated, cross-cutting state with non-trivial update logic, where you want predictable, centralized, debuggable state transitions — the added ceremony pays off at a certain scale of app complexity.

```jsx
// lift state up — parent holds it, both children read/write through props
function App() {
  const [selectedId, setSelectedId] = useState(null)
  return <>
    <AccountList onSelect={setSelectedId} />
    <AccountDetail id={selectedId} />
  </>
}
```

**Q: Explain Redux's core concepts: store, action, reducer.**
A: **Store** — the single source of truth holding the app's entire state tree. **Action** — a plain object describing "what happened" (`{ type: 'ADD_ITEM', payload: {...} }`), the only way to trigger a state change. **Reducer** — a pure function `(state, action) => newState` that computes the next state from the current state and an action, without mutating the existing state. The store dispatches actions to the reducer(s) and updates itself with the result.

```js
// action
{ type: 'accounts/fetch/fulfilled', payload: [{ id: 1, balance: 100 }] }

// reducer
function accountsReducer(state = { items: [] }, action) {
  switch (action.type) {
    case 'accounts/fetch/fulfilled':
      return { ...state, items: action.payload } // new object, never mutate `state` directly
    default:
      return state
  }
}
```

**Q: Why must reducers be pure functions?**
A: Purity (same input always produces the same output, no side effects, no mutating arguments) is what makes state transitions predictable and debuggable — you can replay the same action sequence from the same initial state and always get the same result, which is the foundation of Redux DevTools' time-travel debugging.

```js
// impure — mutates the argument, side effect (console.log), same input can produce different output
function bad(state, action) {
  state.items.push(action.payload) // mutation!
  console.log('added item')        // side effect!
  return state
}

// pure — same input always produces the same output, no mutation
function good(state, action) {
  return { ...state, items: [...state.items, action.payload] }
}
```

**Q: What's Redux Toolkit, and why does most modern Redux code use it?**
A: The official, opinionated toolset wrapping Redux's core APIs to eliminate most boilerplate (hand-written action types/creators, manual immutable updates with spread operators). `createSlice` generates actions and a reducer together, and lets you write reducer logic that *looks* mutating but is safely converted to an immutable update under the hood (via Immer).

```js
// bank-demo's actual accountsSlice.js, verbatim
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'

export const fetchAccounts = createAsyncThunk('accounts/fetch', async () => {
  const res = await fetch('/accounts')
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(body?.message ?? `Request failed (${res.status})`)
  }
  return body
})

const accountsSlice = createSlice({
  name: 'accounts',
  initialState: { items: [], error: null },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchAccounts.fulfilled, (state, action) => {
        state.items = action.payload // looks like a mutation — Immer converts it to an immutable update
        state.error = null
      })
      .addCase(fetchAccounts.rejected, (state, action) => {
        state.error = action.error.message // the .rejected case is what actually uses the `error` field
      })
  },
})
```

---

## Performance

**Q: What does `React.memo` do, and when would you use it?**
A: A higher-order component that memoizes a functional component — skips re-rendering it if its props haven't changed (shallow comparison by default). Use it for components that render often as children of a frequently-updating parent, but whose own props rarely change — pure "presentational" list items are the classic case.

```jsx
const AccountRow = React.memo(function AccountRow({ account }) {
  return <tr><td>{account.ownerName}</td><td>{account.balance}</td></tr>
})
// parent re-renders 10x/sec (e.g. a live price ticker) — this row only re-renders
// when ITS OWN `account` prop actually changes, not on every parent render
```

**Q: Why does React ask for a `key` prop in lists, and what happens if you use the array index?**
A: Keys let React's reconciliation match elements between renders to the *same* underlying item, instead of assuming position equals identity. Array index as key is fine for a static list, but breaks badly if the list is reordered, filtered, or has items inserted/removed — React can match the wrong DOM node to the wrong item, causing state to attach to the wrong row or form inputs to show the wrong value after reordering.

```jsx
// risky — if accounts get reordered/filtered, index 0 might now be a different account,
// but React thinks it's still "the same" row and reuses its DOM node/state
{accounts.map((a, index) => <AccountRow key={index} account={a} />)}

// correct — a stable identity that travels with the actual item
{accounts.map((a) => <AccountRow key={a.id} account={a} />)}
```

**Q: What's list virtualization, and when do you need it?**
A: Only rendering the DOM nodes for items currently visible in the viewport (plus a small buffer), instead of every item in a long list — dramatically improves scroll performance for lists with hundreds or thousands of items. `react-window` / `react-virtualized` are the standard libraries.

```jsx
import { FixedSizeList } from 'react-window'

<FixedSizeList height={400} itemCount={accounts.length} itemSize={35}>
  {({ index, style }) => <div style={style}>{accounts[index].ownerName}</div>}
</FixedSizeList>
// only the ~12 rows that fit in 400px actually exist in the DOM, however long `accounts` is
```

---

## Component Patterns

**Q: Controlled vs. uncontrolled form components?**
A: Controlled — the input's value is driven entirely by React state (`value={state}` + `onChange`), React is the single source of truth. Uncontrolled — the DOM itself holds the value, read on demand via a ref (`useRef`) rather than tracked in state on every keystroke. Controlled is the more idiomatic default; uncontrolled is occasionally simpler for very basic forms or integrating non-React code.

```jsx
// controlled — matches bank-demo's CreateAccountForm
const [ownerName, setOwnerName] = useState('')
<input value={ownerName} onChange={(e) => setOwnerName(e.target.value)} />

// uncontrolled — DOM holds the value, read only when needed
const nameRef = useRef(null)
<input ref={nameRef} defaultValue="" />
// later: nameRef.current.value
```

**Q: What are HOCs and render props, and why are hooks generally preferred now?**
A: Higher-Order Components (a function taking a component and returning an enhanced one) and render props (a component taking a function as a prop to control what it renders) were the pre-hooks patterns for sharing logic. Both work, but tend to produce deeply nested component trees ("wrapper hell") and make prop origins harder to trace. Custom hooks solve the same reuse problem more directly, without adding extra components to the tree.

```jsx
// HOC — wraps a component, adds a prop
function withLoading(Component) {
  return function Wrapped(props) {
    if (props.loading) return <Spinner />
    return <Component {...props} />
  }
}
const AccountListWithLoading = withLoading(AccountList)

// hook equivalent — no extra component in the tree
function AccountList() {
  const { data, loading } = useFetch('/accounts')
  if (loading) return <Spinner />
  return <table>{/* ... */}</table>
}
```

**Q: What's an error boundary, and why is it still one of the few reasons to write a class component?**
A: A component that catches JavaScript errors thrown anywhere in its child subtree during rendering and renders a fallback UI instead of letting the error crash the whole page. It's implemented via two class-only lifecycle APIs — `static getDerivedStateFromError` (compute the fallback state) and `componentDidCatch` (side effect: log the error) — hooks have no equivalent, which is why this is one of the last remaining reasons to write a class component in modern React. Wrapping independent page sections in their own boundary means one section crashing doesn't blank-screen the rest of the app (the general "contain the blast radius" principle covered framework-agnostically in `General_Frontend_Engineering_QA.md` Part 16).

```jsx
// an error boundary around one section, not the whole app
class AccountsSectionBoundary extends React.Component {
  state = { hasError: false }
  static getDerivedStateFromError() { return { hasError: true } }
  componentDidCatch(error, info) { logToSentry(error, info) }
  render() {
    if (this.state.hasError) return <p>Accounts unavailable right now. <button onClick={() => this.setState({ hasError: false })}>Retry</button></p>
    return this.props.children
  }
}
// usage: <AccountsSectionBoundary><AccountList /></AccountsSectionBoundary>
// AccountList crashing no longer blanks the whole page — just that section
```

Worth knowing the limits cold: error boundaries catch render-time errors in descendants, but **not** errors inside event handlers (those need a plain `try`/`catch`), not errors in the boundary component itself, and not async errors (a rejected promise in a `useEffect` won't be caught — that needs its own `.catch`/error state).

---

## React Native Bridge — your strength, likely to come up

**Q: What's actually different between React and React Native, beyond "one renders to DOM and one doesn't"?**
A: Same core — components, hooks, JSX, the reconciliation algorithm — the difference is the renderer target. React DOM renders to real HTML/CSS DOM elements; React Native renders to native platform views (`UIView` on iOS, `View` on Android) via a bridge (or the newer JSI-based architecture) instead of HTML. Practically: no CSS — styling is a JS `StyleSheet` API with a Flexbox-only layout model; no `<div>`/`<span>` — you use `<View>`/`<Text>`; and you're constrained to whatever native platform APIs are exposed (camera, filesystem, notifications) rather than the browser API surface.

```jsx
// React DOM
<div className="row"><span>{account.ownerName}</span></div>

// React Native — same component model, different renderer target
<View style={styles.row}><Text>{account.ownerName}</Text></View>
const styles = StyleSheet.create({ row: { flexDirection: 'row' } }) // JS object, not CSS
```

**Q: What's the trickiest thing to unlearn moving between React Native and React DOM?**
A: This one's genuinely yours to answer from experience — it'll land far better with a real specific than a generic one. The commonly-true starting point most people land on: styling assumptions (React Native defaults `flexDirection: 'column'`; web CSS flex containers default to `row`) and the absence of the full CSS cascade/media queries you'd normally lean on for responsive web layout. Have your own concrete example ready — this is exactly the kind of question where visible hands-on experience shows.

```jsx
// the classic gotcha — identical-looking code, opposite default layout direction
// React Native: children stack top-to-bottom unless you set flexDirection: 'row'
<View style={{ flexDirection: 'column' /* default in RN */ }}>...</View>

// Web: children flow left-to-right unless you set flex-direction: column
// .row { display: flex; /* default is row on the web */ }
```

---

## Quick-Fire Round
Short, direct answers, under 15 seconds each:

- **What does `React.StrictMode` do?** Runs extra checks and warnings in development (like double-invoking some functions) to surface side-effect bugs early — has no effect in production builds. `<StrictMode><App /></StrictMode>`.
- **What's the difference between `props` and `state`?** `props` are passed in from a parent and are read-only from the child's perspective (`<AccountRow account={a} />`); `state` is owned and managed internally by the component (`useState`).
- **What's "lifting state up"?** Moving shared state to the nearest common ancestor of the components that need it, so they stay in sync through props instead of duplicating state.
- **What does `useRef` do besides holding DOM references?** Holds any mutable value that persists across renders without triggering a re-render when it changes — `const renderCount = useRef(0); renderCount.current++` — useful for timers, previous-value tracking, or instance-variable-like state.
- **What's prop drilling?** Passing a prop through several layers of components that don't use it themselves, just to get it to a deeply nested child — the problem Context (or state management libraries) exists to solve.
- **Function components vs. class components — which should you write today?** Function components with hooks — the current standard; class components are legacy but still appear in older codebases you may need to read.
