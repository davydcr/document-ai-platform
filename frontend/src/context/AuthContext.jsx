import { createContext, useState, useEffect } from 'react'
import { authService } from '../services/documentService'

export const AuthContext = createContext()

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    const userData = localStorage.getItem('user')

    if (token && userData) {
      try {
        const parsedUser = JSON.parse(userData)
        setUser(parsedUser)
        setIsAuthenticated(true)
        setIsAdmin(parsedUser.roles?.includes('ADMIN') || false)
      } catch (error) {
        localStorage.removeItem('user')
        localStorage.removeItem('accessToken')
      }
    }

    setLoading(false)
  }, [])

  const login = (user, accessToken, refreshToken) => {
    localStorage.setItem('user', JSON.stringify(user))
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    setUser(user)
    setIsAuthenticated(true)
    setIsAdmin(user.roles?.includes('ADMIN') || false)
  }

  const logout = () => {
    localStorage.removeItem('user')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUser(null)
    setIsAuthenticated(false)
    setIsAdmin(false)
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isAdmin, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
