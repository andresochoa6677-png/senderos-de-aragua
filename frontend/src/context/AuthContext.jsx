import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { login as apiLogin, fetchMe, logout as apiLogout, fetchOnboardingEstado, onUnauthorized } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [estadoOnboarding, setEstadoOnboarding] = useState(null)
  const [loading, setLoading] = useState(true)

  const refreshOnboarding = useCallback(async () => {
    try {
      const estado = await fetchOnboardingEstado()
      setEstadoOnboarding(estado)
    } catch {
      setEstadoOnboarding(null)
    }
  }, [])

  useEffect(() => {
    const limpiar = onUnauthorized(() => {
      setUser(null)
      setEstadoOnboarding(null)
    })
    return limpiar
  }, [])

  useEffect(() => {
    let activo = true
    const cargar = async () => {
      try {
        const usuario = await fetchMe()
        if (!activo) return
        setUser(usuario)
        await refreshOnboarding()
      } catch {
        /* sin sesión activa */
      } finally {
        if (activo) setLoading(false)
      }
    }
    cargar()
    return () => { activo = false }
  }, [refreshOnboarding])

  const iniciarSesion = useCallback(async (email, password, recordar = false) => {
    const usuario = await apiLogin(email, password, recordar)
    setUser(usuario)
    await refreshOnboarding()
    return usuario
  }, [refreshOnboarding])

  const cerrarSesion = useCallback(async () => {
    await apiLogout()
    setUser(null)
    setEstadoOnboarding(null)
  }, [])

  const value = { user, estadoOnboarding, loading, iniciarSesion, cerrarSesion, refreshOnboarding }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
