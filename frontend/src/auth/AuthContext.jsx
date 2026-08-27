import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { getCurrentAccount, hasKnownSession, login as loginRequest, logout as logoutRequest } from '../services/authApi'

const AuthContext = createContext(null)

function normalizeAccount(account) {
  if (!account) return null
  const roles = account.role ? [`ROLE_${account.role}`] : (account.roles || []).map((role) => typeof role === 'string' ? role : role.authority)
  return { ...account, roles }
}

export function AuthProvider({ children }) {
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(hasKnownSession)

  useEffect(() => {
    if (!hasKnownSession()) {
      return undefined
    }
    let active = true
    getCurrentAccount()
      .then((current) => { if (active) setAccount(normalizeAccount(current)) })
      .catch(() => { if (active) setAccount(null) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    function handleAuthExpired() {
      setAccount(null)
    }
    window.addEventListener('librio:auth-expired', handleAuthExpired)
    return () => window.removeEventListener('librio:auth-expired', handleAuthExpired)
  }, [])

  const value = useMemo(() => ({
    account,
    loading,
    isReader: account?.roles.includes('ROLE_READER') || false,
    isLibrarian: account?.roles.includes('ROLE_LIBRARIAN') || false,
    async login(email, password) {
      const result = normalizeAccount(await loginRequest(email, password))
      setAccount(result)
      return result
    },
    async logout() {
      await logoutRequest()
      setAccount(null)
    },
  }), [account, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// The hook intentionally shares this module with its provider.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext)
}
