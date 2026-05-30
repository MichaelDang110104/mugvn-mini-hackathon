'use client'

import { useState, useEffect } from 'react'
import { fetchHomeFeed } from '@/lib/api/client'

interface Section {
  id: string
  title: string
  type: string
  movies: any[]
  reasonChip?: string
  loading?: boolean
  error?: string | null
}

interface HomeDataState {
  sections: Section[]
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

export function useHomeData(): HomeDataState {
  const [sections, setSections] = useState<Section[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = async () => {
    try {
      setLoading(true)
      setError(null)

      const response = await fetchHomeFeed()

      setSections(
        response.sections.map(section => ({
          id: section.sectionId,
          title: section.title,
          type: section.type,
          movies: section.movies,
          loading: false,
          error: null,
        }))
      )
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch home feed'
      setError(errorMessage)
      console.error('[v0] Home feed fetch error:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  return {
    sections,
    loading,
    error,
    refetch: fetchData,
  }
}
