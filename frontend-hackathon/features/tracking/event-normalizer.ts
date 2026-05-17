/**
 * Event Normalizer
 * Converts raw frontend events to backend event schema
 */

import type { RawEvent } from './raw-event-factory'

export interface NormalizedEvent {
  eventId: string
  eventType: 'view' | 'click' | 'search' | 'like' | 'save' | 'rate'
  eventValue?: string
  eventUnit?: string
  screen: string
  component: string
  itemType: string
  metadata: Record<string, string | number | boolean | undefined>
  sessionId: string
  timestamp: string
}

function generateEventId(eventType: string, screen: string, component: string, movieId?: string, timestamp?: number): string {
  const ts = timestamp || Date.now()
  const id = movieId ? `${eventType}_${screen}_${component}_${movieId}_${ts}` : `${eventType}_${screen}_${component}_${ts}`
  // Sanitize for URL safety
  return `evt_${id.replace(/[^a-z0-9_]/gi, '_').substring(0, 100)}`
}

function mapActionToEventType(action: string): NormalizedEvent['eventType'] {
  const mapping: Record<string, NormalizedEvent['eventType']> = {
    impression: 'view',
    click: 'click',
    search: 'search',
    watch_start: 'view',  // Backend treats watch_start as view
    like: 'like',
    save: 'save',
    rating: 'rate',       // Backend expects 'rate', not 'rating'
  }
  return mapping[action] || 'click'
}

function extractEventValue(rawEvent: RawEvent): { value?: string; unit?: string } {
  switch (rawEvent.action) {
    case 'search':
      return {
        value: rawEvent.payload.query,
        unit: 'query',
      }
    case 'rating':
      return {
        value: String(rawEvent.payload.rating),
        unit: 'stars',
      }
    default:
      return {}
  }
}

export function normalizeEvent(rawEvent: RawEvent, sessionId: string): NormalizedEvent {
  const eventType = mapActionToEventType(rawEvent.action)
  const { value, unit } = extractEventValue(rawEvent)
  const movieId = rawEvent.payload.movieId as string | undefined

  const eventId = generateEventId(eventType, rawEvent.screen, rawEvent.component, movieId, rawEvent.timestamp)

  // Build metadata from all payload fields except the mapped ones
  const metadata: Record<string, string | number | boolean | undefined> = {
    ...rawEvent.payload,
  }

  return {
    eventId,
    eventType,
    eventValue: value,
    eventUnit: unit,
    screen: rawEvent.screen,
    component: rawEvent.component,
    itemType: rawEvent.itemType,
    metadata,
    sessionId,
    timestamp: new Date(rawEvent.timestamp).toISOString(),
  }
}
