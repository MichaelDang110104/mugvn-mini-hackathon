/**
 * Event Queue & Flusher
 * Buffers, dedupes, persists, and flushes events
 */

import type { RawEvent } from './raw-event-factory'
import { normalizeEvent, type NormalizedEvent } from './event-normalizer'
import { postEventsBatch } from '@/lib/api/client'

const QUEUE_STORAGE_KEY = 'movie_app_event_queue'
const FLUSHED_EVENTS_KEY = 'movie_app_flushed_events'
const DEDUPE_WINDOW_MS = 5000 // 5 seconds
const AUTO_FLUSH_INTERVAL_MS = 5000 // 5 seconds
const BATCH_SIZE_LIMIT = 20

interface QueueState {
    queue: NormalizedEvent[]
    lastFlushTime: number
    dedupeMap: Record<string, number> // impression deduplication
    recentSubmitMap: Record<string, number> // recent submit prevention
}

let globalState: QueueState = {
    queue: [],
    lastFlushTime: 0,
    dedupeMap: {},
    recentSubmitMap: {},
}

let autoFlushTimer: NodeJS.Timeout | null = null
let sessionId: string = ''

/**
 * Load queue from localStorage
 */
function loadQueueFromStorage(): void {
    if (typeof window === 'undefined') return

    try {
        const stored = localStorage.getItem(QUEUE_STORAGE_KEY)
        if (stored) {
            const parsed = JSON.parse(stored)
            globalState = {
                ...globalState,
                ...parsed,
            }
        }
    } catch (error) {
        console.warn('[v0] Failed to load queue from storage:', error)
    }
}

/**
 * Save queue to localStorage
 */
function saveQueueToStorage(): void {
    if (typeof window === 'undefined') return

    try {
        const toStore = {
            queue: globalState.queue,
            lastFlushTime: globalState.lastFlushTime,
            dedupeMap: {},
            recentSubmitMap: {},
        }
        localStorage.setItem(QUEUE_STORAGE_KEY, JSON.stringify(toStore))
    } catch (error) {
        console.warn('[v0] Failed to save queue to storage:', error)
    }
}

/**
 * Check if impression should be deduplicated
 */
function shouldDedupeImpression(eventType: string, screen: string, component: string, movieId?: string): boolean {
    if (eventType !== 'view' || !movieId) return false

    const dedupeKey = `${screen}_${component}_${movieId}`
    const lastSeen = globalState.dedupeMap[dedupeKey] || 0
    const now = Date.now()

    if (now - lastSeen < DEDUPE_WINDOW_MS) {
        return true // Dedupe
    }

    globalState.dedupeMap[dedupeKey] = now
    return false
}

/**
 * Check if submit action should be prevented (double-submit)
 */
function shouldPreventDoubleSubmit(eventType: string, movieId?: string): boolean {
    if (!['like', 'save', 'rate', 'watch_start'].includes(eventType)) return false

    const key = `${eventType}_${movieId}`
    const lastSubmit = globalState.recentSubmitMap[key] || 0
    const now = Date.now()

    if (now - lastSubmit < DEDUPE_WINDOW_MS) {
        return true // Prevent
    }

    globalState.recentSubmitMap[key] = now
    return false
}

/**
 * Initialize the queue system
 */
export function initializeQueue(sid: string): void {
    sessionId = sid
    loadQueueFromStorage()
    setupAutoFlush()
    setupPageUnload()
}

/**
 * Queue a raw event
 */
export function queueEvent(rawEvent: RawEvent, eventSessionId: string): void {
    // Normalize the event
    const normalized = normalizeEvent(rawEvent, eventSessionId)

    // Check deduplication for impressions
    if (normalized.eventType === 'view') {
        const movieId = normalized.metadata.movieId as string | undefined
        if (shouldDedupeImpression(normalized.eventType, normalized.screen, normalized.component, movieId)) {
            console.log('[v0] Deduped impression:', movieId)
            return
        }
    }

    // Check for double-submit
    const movieId = normalized.metadata.movieId as string | undefined
    if (shouldPreventDoubleSubmit(normalized.eventType, movieId)) {
        console.log('[v0] Prevented double-submit:', normalized.eventType, movieId)
        return
    }

    // Add to queue
    globalState.queue.push(normalized)
    saveQueueToStorage()

    console.log('[v0] Event queued:', normalized.eventType, normalized.metadata.movieId)

    // Immediate flush for high-value events
    if (['like', 'save', 'rate', 'watch_start'].includes(normalized.eventType)) {
        flushQueue(eventSessionId)
    }
}

/**
 * Flush queued events
 */
export async function flushQueue(eventSessionId: string): Promise<void> {
    if (globalState.queue.length === 0) return

    const toFlush = globalState.queue.splice(0, BATCH_SIZE_LIMIT)
    saveQueueToStorage()

    console.log('[v0] Flushing', toFlush.length, 'events')

    try {
        const response = await postEventsBatch({
            sessionId: eventSessionId,
            events: toFlush.map(e => ({
                sessionId: e.sessionId,
                eventId: e.eventId,
                eventType: e.eventType,
                screen: e.screen,
                component: e.component,
                itemType: e.itemType,
                eventValue: e.eventValue,
                eventUnit: e.eventUnit,
                metadata: e.metadata,
                timestamp: e.timestamp,
            })),
        })

        globalState.lastFlushTime = Date.now()
        saveQueueToStorage()

        console.log('[v0] Flushed:', response.accepted, 'accepted,', response.failed, 'failed')
    } catch (error) {
        // Re-queue events on failure
        globalState.queue.unshift(...toFlush)
        saveQueueToStorage()
        console.error('[v0] Flush failed:', error)
    }
}

/**
 * Clear the queue
 */
export function clearQueue(): void {
    globalState.queue = []
    globalState.dedupeMap = {}
    globalState.recentSubmitMap = {}
    saveQueueToStorage()
}

/**
 * Get current queue length
 */
export function getQueueLength(): number {
    return globalState.queue.length
}

/**
 * Setup auto-flush timer
 */
function setupAutoFlush(): void {
    if (autoFlushTimer) {
        clearInterval(autoFlushTimer)
    }

    autoFlushTimer = setInterval(() => {
        if (globalState.queue.length > 0) {
            flushQueue(sessionId)
        }
    }, AUTO_FLUSH_INTERVAL_MS)
}

/**
 * Setup page unload handler
 */
function setupPageUnload(): void {
    if (typeof window === 'undefined') return

  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:9000'

    window.addEventListener('beforeunload', () => {
        if (globalState.queue.length > 0) {
            // Use sendBeacon for best-effort delivery — send each event individually
            // since the backend only has POST /api/events (single event endpoint)
            for (const e of globalState.queue) {
                const movieId = e.metadata?.movieId as string | undefined
                const body = JSON.stringify({
                    sessionId: e.sessionId,
                    eventId: e.eventId,
                    eventType: e.eventType,
                    movieId: movieId || null,
                    queryText: e.eventType === 'search' ? e.eventValue : null,
                    eventValue: e.eventType === 'rate' && e.eventValue ? parseInt(e.eventValue, 10) : null,
                    metadata: { source: e.screen, component: e.component, ...e.metadata },
                    timestamp: e.timestamp,
                })

                try {
                    const blob = new Blob([body], { type: 'application/json' })
                    navigator.sendBeacon(`${apiBaseUrl}/api/events`, blob)
                } catch (error) {
                    console.warn('[api] sendBeacon failed:', error)
                }
            }
        }
    })
}

/**
 * Get queue diagnostics
 */
export function getQueueDiagnostics() {
    return {
        queueLength: globalState.queue.length,
        lastFlushTime: globalState.lastFlushTime,
        dedupeMapSize: Object.keys(globalState.dedupeMap).length,
        recentSubmitMapSize: Object.keys(globalState.recentSubmitMap).length,
    }
}
