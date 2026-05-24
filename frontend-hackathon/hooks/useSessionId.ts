/**
 * useSessionId Hook
 * Provides access to session ID with proper initialization
 */

import { useState, useEffect } from 'react'
import { getSessionId, setSessionId, clearSessionId, getOnboardingComplete } from '@/lib/session/session-store'

export function useSessionId() {
  const [sessionId, setLocalSessionId] = useState<string>('')
  const [sessionReady, setSessionReady] = useState(false)
  const [onboardingComplete, setLocalOnboardingComplete] = useState(false)

  useEffect(() => {
    const id = getSessionId()
    setLocalSessionId(id)
    setLocalOnboardingComplete(getOnboardingComplete())
    setSessionReady(true)
  }, [])

  return {
    sessionId,
    sessionReady,
    onboardingComplete,
    setSessionId: (id: string) => {
      setSessionId(id)
      setLocalSessionId(id)
    },
    clearSessionId: () => {
      clearSessionId()
      setLocalSessionId('')
      setLocalOnboardingComplete(false)
    },
  }
}
