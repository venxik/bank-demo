import { configureStore } from '@reduxjs/toolkit'
import accountsReducer from './accountsSlice'

export const store = configureStore({
  reducer: { accounts: accountsReducer },
})
