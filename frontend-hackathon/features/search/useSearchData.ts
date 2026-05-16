'use client'

import { useState, useEffect } from 'react'
import { fetchSearchResults } from '@/lib/api/client'
import { useSessionId } from '@/hooks/useSessionId'
import { useTrackEvent } from '@/hooks/useTrackEvent'
import type { MovieItem } from '@/lib/api/mock-data'

interface SearchDataState {
  results: MovieItem[]
  searchMode: 'semantic' | 'fallback_text'
  fallbackUsed: boolean
  hitCount: number
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

export function useSearchData(query: string): SearchDataState {
  const { sessionId, sessionReady } = useSessionId()
  const trackEvent = useTrackEvent()
  const [results, setResults] = useState<MovieItem[]>([])
  const [searchMode, setSearchMode] = useState<'semantic' | 'fallback_text'>('semantic')
  const [fallbackUsed, setFallbackUsed] = useState(false)
  const [hitCount, setHitCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchData = async () => {
    if (!sessionReady || !sessionId || !query.trim()) {
      setResults([])
      setHitCount(0)
      return
    }

    try {
      setLoading(true)
      setError(null)

      const response = await fetchSearchResults({
        sessionId,
        q: query,
        limit: 50,
      })

      setResults(response.results)
      setSearchMode(response.searchMode)
      setFallbackUsed(response.fallbackUsed)
      setHitCount(response.hitCount)

      // Track search event
      trackEvent.search(query, response.results.length)
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to search'
      setError(errorMessage)
      console.error('[v0] Search error:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [query, sessionId, sessionReady])

  return {
    results,
    searchMode,
    fallbackUsed,
    hitCount,
    loading,
    error,
    refetch: fetchData,
  }
}
