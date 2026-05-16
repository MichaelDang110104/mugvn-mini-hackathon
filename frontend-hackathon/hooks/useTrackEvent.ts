/**
 * useTrackEvent Hook
 * Provides easy event tracking from components
 */

import { useEffect, useCallback } from 'react'
import {
  createImpressionEvent,
  createClickEvent,
  createSearchEvent,
  createWatchStartEvent,
  createLikeEvent,
  createSaveEvent,
  createRatingEvent,
} from '@/features/tracking/raw-event-factory'
import { queueEvent, initializeQueue } from '@/features/tracking/event-queue'
import { getSessionId } from '@/lib/session/session-store'

let queueInitialized = false

export function useTrackEvent() {
  // Initialize queue on first hook use
  useEffect(() => {
    if (!queueInitialized) {
      const sessionId = getSessionId()
      initializeQueue(sessionId)
      queueInitialized = true
    }
  }, [])

  const sessionId = getSessionId()

  return {
    impression: useCallback(
      (movieId: string, screen: string, component: string, options?: { position?: number; rowTitle?: string }) => {
        const event = createImpressionEvent(movieId, screen, component, options)
        queueEvent(event, sessionId)
      },
      [sessionId]
    ),

    click: useCallback(
      (movieId: string, screen: string, component: string, options?: { position?: number; rowTitle?: string }) => {
        const event = createClickEvent(movieId, screen, component, options)
        queueEvent(event, sessionId)
      },
      [sessionId]
    ),

    search: useCallback((query: string, resultCount: number) => {
      const event = createSearchEvent(query, resultCount)
      queueEvent(event, sessionId)
    }, [sessionId]),

    watchStart: useCallback((movieId: string) => {
      const event = createWatchStartEvent(movieId)
      queueEvent(event, sessionId)
    }, [sessionId]),

    like: useCallback((movieId: string) => {
      const event = createLikeEvent(movieId)
      queueEvent(event, sessionId)
    }, [sessionId]),

    save: useCallback((movieId: string) => {
      const event = createSaveEvent(movieId)
      queueEvent(event, sessionId)
    }, [sessionId]),

    rating: useCallback((movieId: string, rating: number) => {
      const event = createRatingEvent(movieId, rating)
      queueEvent(event, sessionId)
    }, [sessionId]),
  }
}
