/**
 * useRecommendationRefresh Hook
 * Manages recommendation refresh after high-value user events
 * Debounces multiple events to prevent excessive refetching
 */

import { useCallback, useRef } from 'react'

const DEBOUNCE_DELAY = 3000 // 3 seconds

export function useRecommendationRefresh(onRefresh: () => Promise<void>) {
  const debounceTimerRef = useRef<NodeJS.Timeout | null>(null)
  const isRefreshingRef = useRef(false)

  const triggerRefresh = useCallback(
    (immediate = false) => {
      // Clear any pending debounce
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }

      if (immediate && !isRefreshingRef.current) {
        isRefreshingRef.current = true
        onRefresh().finally(() => {
          isRefreshingRef.current = false
        })
      } else if (!immediate) {
        // Debounce the refresh
        debounceTimerRef.current = setTimeout(() => {
          if (!isRefreshingRef.current) {
            isRefreshingRef.current = true
            onRefresh().finally(() => {
              isRefreshingRef.current = false
            })
          }
        }, DEBOUNCE_DELAY)
      }
    },
    [onRefresh]
  )

  return {
    triggerRefresh,
    isRefreshing: isRefreshingRef.current,
  }
}
