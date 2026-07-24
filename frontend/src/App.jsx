import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import './App.css'
import { fetchAccounts } from './accountsSlice'

const API = '/accounts'

async function apiCall(url, options) {
  const res = await fetch(url, options)
  const body = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(body?.message ?? `Request failed (${res.status})`)
  }
  return body
}

function CreateAccountForm({ onCreated, onError }) {
  const [accountNumber, setAccountNumber] = useState('')
  const [ownerName, setOwnerName] = useState('')
  const [initialBalance, setInitialBalance] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    try {
      await apiCall(API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountNumber, ownerName, initialBalance }),
      })
      setAccountNumber('')
      setOwnerName('')
      setInitialBalance('')
      onCreated()
    } catch (err) {
      onError(err.message)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="panel">
      <h2>Create account</h2>
      <input placeholder="account number" value={accountNumber}
             onChange={(e) => setAccountNumber(e.target.value)} required />
      <input placeholder="owner name" value={ownerName}
             onChange={(e) => setOwnerName(e.target.value)} required />
      <input placeholder="initial balance" type="number" step="0.01" value={initialBalance}
             onChange={(e) => setInitialBalance(e.target.value)} required />
      <button type="submit">Create</button>
    </form>
  )
}

function AccountRow({ account, accounts, onChanged, onError }) {
  const [amount, setAmount] = useState('')
  const [toAccountId, setToAccountId] = useState('')

  async function move(action) {
    try {
      await apiCall(`${API}/${account.id}/${action}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ amount }),
      })
      setAmount('')
      onChanged()
    } catch (err) {
      onError(err.message)
    }
  }

  async function transfer() {
    try {
      await apiCall(`${API}/${account.id}/transfer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ toAccountId, amount }),
      })
      setAmount('')
      setToAccountId('')
      onChanged()
    } catch (err) {
      onError(err.message)
    }
  }

  return (
    <tr>
      <td>{account.id}</td>
      <td>{account.accountNumber}</td>
      <td>{account.ownerName}</td>
      <td>{account.balance}</td>
      <td title="optimistic lock version">{account.version}</td>
      <td className="actions">
        <input placeholder="amount" type="number" step="0.01" value={amount}
               onChange={(e) => setAmount(e.target.value)} />
        <button onClick={() => move('deposit')}>Deposit</button>
        <button onClick={() => move('withdraw')}>Withdraw</button>
        <select value={toAccountId} onChange={(e) => setToAccountId(e.target.value)}>
          <option value="">to account…</option>
          {accounts.filter((a) => a.id !== account.id).map((a) => (
            <option key={a.id} value={a.id}>{a.accountNumber}</option>
          ))}
        </select>
        <button onClick={transfer} disabled={!toAccountId}>Transfer</button>
      </td>
    </tr>
  )
}

function App() {
  const dispatch = useDispatch()
  const accounts = useSelector((state) => state.accounts.items)
  const [error, setError] = useState(null)

  function refresh() {
    dispatch(fetchAccounts())
  }

  useEffect(() => {
    refresh()
  }, [])

  return (
    <div className="app">
      <h1>bank-demo</h1>
      {error && <div className="error" onClick={() => setError(null)}>{error} (click to dismiss)</div>}

      <CreateAccountForm onCreated={refresh} onError={setError} />

      <table>
        <thead>
          <tr>
            <th>id</th><th>account #</th><th>owner</th><th>balance</th><th>version</th><th>actions</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map((a) => (
            <AccountRow key={a.id} account={a} accounts={accounts} onChanged={refresh} onError={setError} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default App
