'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { getAuthToken, getOnboardingComplete } from '@/lib/session/session-store'

export default function RootPage() {
  const router = useRouter()

  useEffect(() => {
    const authToken = getAuthToken()

    if (!authToken) {
      router.replace('/login')
      return
    }

    router.replace(getOnboardingComplete() ? '/home' : '/onboarding')
  }, [router])

  return null
}
