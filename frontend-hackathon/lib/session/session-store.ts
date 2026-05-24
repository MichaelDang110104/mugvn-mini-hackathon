/**
 * Session Storage Module
 * Provides centralized session persistence using localStorage
 * Handles client-side session ID generation and retrieval
 */

const SESSION_ID_KEY = 'movie_app_session_id'
const AUTH_TOKEN_KEY = 'movie_app_auth_token'
const ONBOARDING_COMPLETE_KEY = 'movie_app_onboarding_complete'

/**
 * Generate a unique session ID using crypto.randomUUID()
 */
export function generateSessionId(): string {
  if (typeof window === 'undefined') {
    // Fallback for SSR environments
    return `session_${Date.now()}_${Math.random().toString(36).substring(7)}`
  }
  try {
    return crypto.randomUUID()
  } catch {
    // Fallback if crypto is unavailable
    return `session_${Date.now()}_${Math.random().toString(36).substring(7)}`
  }
}

/**
 * Retrieve session ID from localStorage, generate and store if missing
 */
export function getSessionId(): string {
  if (typeof window === 'undefined') {
    return ''
  }

  try {
    const stored = localStorage.getItem(SESSION_ID_KEY)
    if (stored) {
      return stored
    }

    const newId = generateSessionId()
    localStorage.setItem(SESSION_ID_KEY, newId)
    return newId
  } catch (error) {
    // localStorage not available (private mode, etc)
    return generateSessionId()
  }
}

/**
 * Manually set a session ID (mostly for testing)
 */
export function setSessionId(sessionId: string): void {
  if (typeof window === 'undefined') {
    return
  }

  try {
    localStorage.setItem(SESSION_ID_KEY, sessionId)
  } catch (error) {
    // localStorage not available
    console.warn('[v0] Failed to set session ID in localStorage:', error)
  }
}

/**
 * Clear the stored session ID
 */
export function clearSessionId(): void {
  if (typeof window === 'undefined') {
    return
  }

  try {
    localStorage.removeItem(SESSION_ID_KEY)
  } catch (error) {
    // localStorage not available
    console.warn('[v0] Failed to clear session ID from localStorage:', error)
  }
}

export function getAuthToken(): string | null {
  if (typeof window === 'undefined') return null
  try {
    return localStorage.getItem(AUTH_TOKEN_KEY)
  } catch {
    return null
  }
}

export function setAuthToken(token: string): void {
  if (typeof window === 'undefined') return
  try {
    localStorage.setItem(AUTH_TOKEN_KEY, token)
  } catch (error) {
    console.warn('[v0] Failed to set auth token:', error)
  }
}

export function getOnboardingComplete(): boolean {
  if (typeof window === 'undefined') return false
  try {
    return localStorage.getItem(ONBOARDING_COMPLETE_KEY) === 'true'
  } catch {
    return false
  }
}


export function setOnboardingComplete(value: boolean): void {
  if (typeof window === 'undefined') return
  try {
    localStorage.setItem(ONBOARDING_COMPLETE_KEY, value ? 'true' : 'false')
  } catch (error) {
    console.warn('[v0] Failed to set onboarding completion:', error)
  }
}

export function clearAuthToken(): void {
  if (typeof window === 'undefined') return
  try {
    localStorage.removeItem(AUTH_TOKEN_KEY)
  } catch (error) {
    console.warn('[v0] Failed to clear auth token:', error)
  }
}
