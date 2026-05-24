'use client'

import { useEffect, useMemo, useState } from 'react'
import { fetchOnboardingMovieOptions, fetchOnboardingOptions, submitOnboarding } from '@/lib/api/client'
import type { OnboardingMovieOption } from '@/lib/api/types'
import { getSessionId, setOnboardingComplete } from '@/lib/session/session-store'
import type { OnboardingFormValues } from './types'

const initialValues: OnboardingFormValues = {
  selectedGenres: [],
  selectedThemes: [],
  favoriteMovies: [],
  avoidedGenres: [],
  avoidedThemes: [],
  preferredLanguages: [],
  preferredEra: 'no_preference',
  preferredPace: 'balanced',
  freeTextTasteSummary: '',
}

export function useOnboardingForm() {
  const [values, setValues] = useState<OnboardingFormValues>(initialValues)
  const [genreOptions, setGenreOptions] = useState<string[]>([])
  const [movieOptions, setMovieOptions] = useState<OnboardingMovieOption[]>([])
  const [movieSearchQuery, setMovieSearchQuery] = useState('')
  const [loadingOptions, setLoadingOptions] = useState(true)
  const [loadingMovies, setLoadingMovies] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [movieError, setMovieError] = useState<string | null>(null)

  const sessionId = getSessionId()
  const canBrowseMovies = values.selectedGenres.length >= 3

  const canSubmit = useMemo(() => {
    return values.selectedGenres.length === 3 && values.selectedThemes.length >= 3 && values.favoriteMovies.length >= 1
  }, [values])

  useEffect(() => {
    let cancelled = false

    async function loadOptions() {
      try {
        setLoadingOptions(true)
        setError(null)
        const response = await fetchOnboardingOptions(sessionId)
        if (!cancelled) {
          setGenreOptions(response.genres)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load onboarding options')
        }
      } finally {
        if (!cancelled) {
          setLoadingOptions(false)
        }
      }
    }

    loadOptions()
    return () => {
      cancelled = true
    }
  }, [sessionId])

  useEffect(() => {
    let cancelled = false

    async function loadMovies() {
      if (!canBrowseMovies) {
        setMovieOptions([])
        setMovieError(null)
        return
      }

      try {
        setLoadingMovies(true)
        setMovieError(null)
        const movies = await fetchOnboardingMovieOptions({
          sessionId,
          query: movieSearchQuery,
          genres: values.selectedGenres,
          limit: 10,
        })
        if (!cancelled) {
          setMovieOptions(movies)
        }
      } catch (err) {
        if (!cancelled) {
          setMovieError(err instanceof Error ? err.message : 'Failed to load movies')
        }
      } finally {
        if (!cancelled) {
          setLoadingMovies(false)
        }
      }
    }

    const timeout = window.setTimeout(loadMovies, movieSearchQuery.trim() ? 250 : 0)
    return () => {
      cancelled = true
      window.clearTimeout(timeout)
    }
  }, [canBrowseMovies, movieSearchQuery, sessionId, values.selectedGenres])

  function addFavoriteMovie(movie: OnboardingMovieOption) {
    setValues(current => {
      if (current.favoriteMovies.some(selected => selected.movieId === movie.movieId)) {
        return current
      }

      return {
        ...current,
        favoriteMovies: [...current.favoriteMovies, { movieId: movie.movieId, title: movie.title }],
      }
    })
  }

  function removeFavoriteMovie(movieId: string) {
    setValues(current => ({
      ...current,
      favoriteMovies: current.favoriteMovies.filter(movie => movie.movieId !== movieId),
    }))
  }

  async function submit() {
    try {
      setSubmitting(true)
      setError(null)
      await submitOnboarding({
        sessionId,
        ...values,
      })
      setOnboardingComplete(true)
      window.location.href = '/home'
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save onboarding')
    } finally {
      setSubmitting(false)
    }
  }

  return {
    values,
    setValues,
    canSubmit,
    submit,
    submitting,
    error,
    genreOptions,
    loadingOptions,
    movieOptions,
    movieSearchQuery,
    setMovieSearchQuery,
    loadingMovies,
    movieError,
    canBrowseMovies,
    addFavoriteMovie,
    removeFavoriteMovie,
  }
}
