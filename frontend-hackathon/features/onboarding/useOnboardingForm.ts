'use client'

import { useMemo, useState } from 'react'
import { submitOnboarding } from '@/lib/api/client'
import { getSessionId, setOnboardingComplete } from '@/lib/session/session-store'
import type { OnboardingFormValues } from './types'

const initialValues: OnboardingFormValues = {
  selectedGenres: [],
  selectedThemes: [],
  favoriteTitles: [''],
  avoidedGenres: [],
  avoidedThemes: [],
  preferredLanguages: [],
  preferredEra: 'no_preference',
  preferredPace: 'balanced',
  freeTextTasteSummary: '',
}

export function useOnboardingForm() {
  const [values, setValues] = useState<OnboardingFormValues>(initialValues)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const canSubmit = useMemo(() => {
    const filledTitles = values.favoriteTitles.filter(title => title.trim().length > 0)
    return values.selectedGenres.length === 3 && values.selectedThemes.length >= 3 && filledTitles.length >= 1
  }, [values])

  async function submit() {
    try {
      setSubmitting(true)
      setError(null)
      const sessionId = getSessionId()
      await submitOnboarding({
        sessionId,
        ...values,
        favoriteTitles: values.favoriteTitles.filter(title => title.trim().length > 0),
      })
      setOnboardingComplete(true)
      window.location.href = '/home'
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save onboarding')
    } finally {
      setSubmitting(false)
    }
  }

  return { values, setValues, canSubmit, submit, submitting, error }
}
