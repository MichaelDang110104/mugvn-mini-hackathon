/**
 * useImpressionTracker Hook
 * Automatically track impressions when elements become visible
 * Uses Intersection Observer API with configurable visibility rules
 */

import { useEffect, useRef } from 'react'
import { useTrackEvent } from './useTrackEvent'

interface ImpressionTrackerOptions {
  movieId: string
  screen: string
  component: string
  position?: number
  rowTitle?: string
  visibilityThreshold?: number // percentage (0-100)
  minimumVisibleTime?: number // milliseconds
}

export function useImpressionTracker(options: ImpressionTrackerOptions) {
  const ref = useRef<HTMLDivElement>(null)
  const trackEvent = useTrackEvent()
  const visibilityThreshold = options.visibilityThreshold || 50
  const minimumVisibleTime = options.minimumVisibleTime || 800
  const trackedRef = useRef(false)
  const visibleStartRef = useRef(0)

  useEffect(() => {
    if (!ref.current) return

    const observer = new IntersectionObserver(
      entries => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            // Element became visible
            if (visibleStartRef.current === 0) {
              visibleStartRef.current = Date.now()
            }

            // Check if visible for long enough
            const visibleDuration = Date.now() - visibleStartRef.current
            if (visibleDuration >= minimumVisibleTime && !trackedRef.current) {
              trackEvent.impression(options.movieId, options.screen, options.component, {
                position: options.position,
                rowTitle: options.rowTitle,
              })
              trackedRef.current = true
            }
          } else {
            // Element became hidden
            visibleStartRef.current = 0
          }
        })
      },
      {
        threshold: visibilityThreshold / 100,
        rootMargin: '0px',
      }
    )

    observer.observe(ref.current)

    return () => {
      observer.disconnect()
    }
  }, [options.movieId, options.screen, options.component, options.position, options.rowTitle, trackEvent, visibilityThreshold, minimumVisibleTime])

  return ref
}
