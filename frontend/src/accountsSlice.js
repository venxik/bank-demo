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
        state.items = action.payload
        state.error = null
      })
      .addCase(fetchAccounts.rejected, (state, action) => {
        state.error = action.error.message
      })
  },
})

export default accountsSlice.reducer
