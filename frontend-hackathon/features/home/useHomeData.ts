'use client'

import { useState, useEffect } from 'react'
import { fetchRecommendations } from '@/lib/api/client'
import { useSessionId } from '@/hooks/useSessionId'

interface Section {
  id: string
  title: string
  movies: any[]
  reasonChip?: string
  loading?: boolean
  error?: string | null
}

interface HomeDataState {
  sections: Section[]
  loading: boolean
  error: string | null
  recommendationMode: 'semantic' | 'fallback_text' | 'personalized' | 'cold_start'
  refetch: () => Promise<void>
}

export function useHomeData(): HomeDataState {
  const { sessionId, sessionReady } = useSessionId()
  const [sections, setSections] = useState<Section[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [recommendationMode, setRecommendationMode] = useState<HomeDataState['recommendationMode']>('personalized')

  const fetchData = async () => {
    if (!sessionReady || !sessionId) {
      return
    }

    try {
      setLoading(true)
      setError(null)

      const response = await fetchRecommendations({
        sessionId,
        limit: 50,
      })

      setSections(
        response.sections.map(section => ({
          id: section.id,
          title: section.title,
          movies: section.movies,
          reasonChip: section.reasonChip,
          loading: false,
          error: null,
        }))
      )

      setRecommendationMode(response.recommendationMode)
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch recommendations'
      setError(errorMessage)
      console.error('[v0] Home data fetch error:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (sessionReady) {
      fetchData()
    }
  }, [sessionId, sessionReady])

  return {
    sections,
    loading,
    error,
    recommendationMode,
    refetch: fetchData,
  }
}
