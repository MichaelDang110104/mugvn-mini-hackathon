'use client'

import { useState, useEffect } from 'react'
import { fetchMovieDetail } from '@/lib/api/client'
import { useSessionId } from '@/hooks/useSessionId'
import type { MovieItem } from '@/lib/api/mock-data'

interface MovieDetailState {
  movie: (MovieItem & { language: string; overview: string; playbackUrl: string | null }) | null
  similarMovies: MovieItem[]
  relatedMovies: MovieItem[]
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

export function useMovieDetail(movieId: string, region?: string): MovieDetailState {
  const { sessionId, sessionReady } = useSessionId()
  const [movie, setMovie] = useState<MovieDetailState['movie']>(null)
  const [similarMovies, setSimilarMovies] = useState<MovieItem[]>([])
  const [relatedMovies, setRelatedMovies] = useState<MovieItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = async () => {
    if (!sessionReady || !sessionId || !movieId) {
      return
    }

    try {
      setLoading(true)
      setError(null)

      const response = await fetchMovieDetail({
        sessionId,
        movieId,
        region,
      })

      setMovie(response.movie)
      setSimilarMovies(response.similarMovies)
      setRelatedMovies(response.relatedMovies)
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch movie details'
      setError(errorMessage)
      console.error('[v0] Movie detail fetch error:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (sessionReady && movieId) {
      fetchData()
    }
  }, [movieId, sessionId, sessionReady, region])

  return {
    movie,
    similarMovies,
    relatedMovies,
    loading,
    error,
    refetch: fetchData,
  }
}

interface MovieActionsState {
  liked: boolean
  saved: boolean
  rating: number | null
  liking: boolean
  saving: boolean
  rating_pending: boolean
  like: () => Promise<void>
  save: () => Promise<void>
  setRating: (rating: number) => Promise<void>
}

export function useMovieActions(movieId: string): MovieActionsState {
  const [liked, setLiked] = useState(false)
  const [saved, setSaved] = useState(false)
  const [rating, setRatingState] = useState<number | null>(null)
  const [liking, setLiking] = useState(false)
  const [saving, setSaving] = useState(false)
  const [rating_pending, setRatingPending] = useState(false)

  // These are optimistic updates - in a real app, call API
  const like = async () => {
    setLiking(true)
    try {
      // Optimistic update
      setLiked(!liked)
      // In real app: await postLike(movieId)
      await new Promise(resolve => setTimeout(resolve, 300))
    } finally {
      setLiking(false)
    }
  }

  const save = async () => {
    setSaving(true)
    try {
      // Optimistic update
      setSaved(!saved)
      // In real app: await postSave(movieId)
      await new Promise(resolve => setTimeout(resolve, 300))
    } finally {
      setSaving(false)
    }
  }

  const setRating = async (newRating: number) => {
    setRatingPending(true)
    try {
      // Optimistic update
      setRatingState(newRating)
      // In real app: await postRating(movieId, newRating)
      await new Promise(resolve => setTimeout(resolve, 300))
    } finally {
      setRatingPending(false)
    }
  }

  return {
    liked,
    saved,
    rating,
    liking,
    saving,
    rating_pending,
    like,
    save,
    setRating,
  }
}
