/**
 * API Client Module
 * Makes real HTTP calls to the Spring Boot backend.
 * Transforms backend responses into frontend-friendly MovieItem shapes.
 *
 * Backend base URL is configurable via NEXT_PUBLIC_API_BASE_URL env var.
 * Defaults to http://localhost:9000 for local development.
 */

import type {
    MovieItem,
    BackendSearchResponse,
    BackendMovieDetailResponse,
    BackendRecommendationResponse,
    BackendEventRequest,
    BackendEventResponse,
    BackendOnboardingRequest,
    BackendOnboardingResponse,
    SearchItem,
    MovieDetail,
} from './types'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:9000'

// ─── Session header helpers ─────────────────────────────────────────────────

function buildHeaders(sessionId?: string): Record<string, string> {
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
    }
    if (sessionId) {
        headers['X-Session-Id'] = sessionId
    }
    return headers
}

/**
 * After every response, check if the backend minted a new session
 * and persist it in localStorage if so.
 */
function captureSessionFromResponse(response: Response): string | null {
    const backendSessionId = response.headers.get('X-Session-Id')
    if (backendSessionId) {
        try {
            localStorage.setItem('movie_app_session_id', backendSessionId)
        } catch {
            // localStorage unavailable
        }
    }
    return backendSessionId
}

// ─── Transform helpers: Backend → Frontend MovieItem ─────────────────────

const PLACEHOLDER_POSTER = 'https://via.placeholder.com/200x300/333333/FFFFFF?text=No+Poster'
const PLACEHOLDER_BACKDROP = 'https://via.placeholder.com/1280x720/333333/FFFFFF?text=No+Backdrop'

function searchItemToMovieItem(item: SearchItem): MovieItem {
    const m = item.movie
    return {
        id: m.id || '',
        title: m.title || 'Untitled',
        year: 0, // Not available in search items
        genres: m.genres || [],
        rating: m.ratingAvg ?? 0,
        posterUrl: m.posterUrl || PLACEHOLDER_POSTER,
        overview: '', // Not available in search items
        backdropUrl: PLACEHOLDER_BACKDROP,
        language: '',
    }
}

function movieDetailToMovieItem(detail: MovieDetail): MovieItem & { language: string; overview: string; playbackUrl: string | null } {
    return {
        id: detail.id || '',
        title: detail.title || 'Untitled',
        year: detail.year ?? 0,
        genres: detail.genres || [],
        rating: detail.ratingAvg ?? 0,
        posterUrl: detail.posterUrl || PLACEHOLDER_POSTER,
        overview: detail.overview || detail.fullplot || '',
        backdropUrl: detail.posterUrl || PLACEHOLDER_BACKDROP, // Use poster as backdrop fallback
        language: detail.languages?.[0] || '',
        playbackUrl: detail.playbackUrl,
    }
}

// ─── Response interfaces consumed by frontend hooks ─────────────────────

interface RecommendationSection {
    id: string
    title: string
    reasonChip?: string
    movies: MovieItem[]
}

export interface RecommendationsResponse {
    sessionId: string
    recommendationMode: 'semantic' | 'fallback_text' | 'personalized' | 'cold_start'
    sections: RecommendationSection[]
}

export interface SearchResponse {
    sessionId: string
    query: string
    results: MovieItem[]
    searchMode: 'semantic' | 'fallback_text'
    fallbackUsed: boolean
    hitCount: number
}

export interface MovieDetailResponse {
    sessionId: string
    movie: MovieItem & { language: string; overview: string; playbackUrl: string | null }
    similarMovies: MovieItem[]
    relatedMovies: MovieItem[]
}

export interface EventResponse {
    eventId: string
    accepted: boolean
}

// ─── Input interfaces ───────────────────────────────────────────────────

export interface FetchRecommendationsInput {
    sessionId: string
    limit?: number
    offset?: number
}

export interface FetchSearchInput {
    sessionId: string
    q: string
    limit?: number
    offset?: number
}

export interface FetchMovieDetailInput {
    sessionId: string
    movieId: string
    region?: string
}

export interface PostEventInput {
    sessionId: string
    eventId: string
    eventType: string
    screen: string
    component: string
    itemType: string
    eventValue?: string
    eventUnit?: string
    metadata: Record<string, string | number | boolean | undefined>
    timestamp: string
}

export async function submitOnboarding(input: BackendOnboardingRequest): Promise<BackendOnboardingResponse> {
    const response = await fetch(`${API_BASE_URL}/api/onboarding`, {
        method: 'POST',
        headers: buildHeaders(input.sessionId),
        body: JSON.stringify(input),
    })

    captureSessionFromResponse(response)

    if (!response.ok) {
        throw new Error(`Onboarding failed: ${response.status}`)
    }

    return response.json()
}

/**
 * GET /api/recommendations
 * Fetch home page recommendations and transform into section-based layout.
 */
export async function fetchRecommendations(input: FetchRecommendationsInput): Promise<RecommendationsResponse> {
    const params = new URLSearchParams()
    if (input.sessionId) params.set('sessionId', input.sessionId)
    if (input.limit) params.set('limit', String(input.limit))
    params.set('context', 'homepage')

    const response = await fetch(`${API_BASE_URL}/api/recommendations?${params}`, {
        method: 'GET',
        headers: buildHeaders(input.sessionId),
    })

    const sessionId = captureSessionFromResponse(response) || input.sessionId

    if (!response.ok) {
        throw new Error(`Recommendations request failed with status ${response.status}`)
    }

    const data: BackendRecommendationResponse = await response.json()

    const allMovies = data.items.map(searchItemToMovieItem)
    const sections: RecommendationSection[] = []

    if (allMovies.length > 0) {
        sections.push({
            id: 'recommended_for_you',
            title: 'Recommended For You',
            reasonChip: data.mode === 'personalized' ? 'Based on your activity' : 'Popular picks',
            movies: allMovies.slice(0, 12),
        })

        if (allMovies.length > 12) {
            sections.push({
                id: 'trending_now',
                title: 'Trending Now',
                movies: allMovies.slice(12, 24),
            })
        }

        if (allMovies.length > 24) {
            sections.push({
                id: 'continue_exploring',
                title: 'Continue Exploring',
                movies: allMovies.slice(24),
            })
        }
    }

    return {
        sessionId,
        recommendationMode: data.mode,
        sections,
    }
}

/**
 * GET /api/movies/search
 * Fetch search results.
 */
export async function fetchSearchResults(input: FetchSearchInput): Promise<SearchResponse> {
    const params = new URLSearchParams()
    if (input.q) params.set('q', input.q)
    if (input.sessionId) params.set('sessionId', input.sessionId)
    if (input.limit) params.set('limit', String(input.limit))

    const response = await fetch(`${API_BASE_URL}/api/movies/search?${params}`, {
        method: 'GET',
        headers: buildHeaders(input.sessionId),
    })

    const sessionId = captureSessionFromResponse(response) || input.sessionId

    if (!response.ok) {
        throw new Error(`Search request failed with status ${response.status}`)
    }

    const data: BackendSearchResponse = await response.json()

    const results = data.items.map(searchItemToMovieItem)

    return {
        sessionId,
        query: data.query || input.q,
        results,
        searchMode: data.mode === 'cold_start' ? 'semantic' : (data.mode as 'semantic' | 'fallback_text'),
        fallbackUsed: data.fallbackUsed,
        hitCount: results.length,
    }
}

/**
 * GET /api/movies/{movieId}
 * Fetch movie detail page data.
 */
export async function fetchMovieDetail(input: FetchMovieDetailInput): Promise<MovieDetailResponse> {
    const params = new URLSearchParams()
    if (input.sessionId) params.set('sessionId', input.sessionId)
    if (input.region) params.set('region', input.region)

    const response = await fetch(`${API_BASE_URL}/api/movies/${input.movieId}?${params}`, {
        method: 'GET',
        headers: buildHeaders(input.sessionId),
    })

    const sessionId = captureSessionFromResponse(response) || input.sessionId

    if (!response.ok) {
        if (response.status === 404) {
            throw new Error('Movie not found')
        }
        throw new Error(`Movie detail request failed with status ${response.status}`)
    }

    const data: BackendMovieDetailResponse = await response.json()

    const movie = movieDetailToMovieItem(data.movie)
    const similarMovies = data.similarMovies.map(searchItemToMovieItem)

    return {
        sessionId,
        movie,
        similarMovies,
        relatedMovies: [], // Backend doesn't have a separate relatedMovies list
    }
}

/**
 * POST /api/events
 * Post a single event.
 * Transforms frontend event shape into backend EventRequest.
 */
export async function postEvent(input: PostEventInput): Promise<EventResponse> {
    // Map frontend event types to backend event types
    const eventTypeMap: Record<string, string> = {
        view: 'view',
        click: 'click',
        search: 'search',
        watch_start: 'watch_start',
        like: 'like',
        save: 'save',
        rating: 'rate',
        rate: 'rate',
    }

    const backendEventType = eventTypeMap[input.eventType] || input.eventType

    // Build the backend request body
    const movieId = input.metadata?.movieId as string | undefined
    const body: BackendEventRequest = {
        sessionId: input.sessionId,
        eventId: input.eventId,
        eventType: backendEventType as BackendEventRequest['eventType'],
        movieId: movieId || null,
        queryText: (input.eventType === 'search' ? input.eventValue : null) || (input.metadata?.query as string | undefined) || null,
        eventValue: input.eventType === 'rating' || input.eventType === 'rate'
            ? (input.eventValue ? parseInt(input.eventValue, 10) : null)
            : null,
        metadata: {
            source: input.screen || 'unknown',
            component: input.component,
            ...(input.metadata || {}),
        },
        timestamp: input.timestamp || new Date().toISOString(),
    }

    try {
        const response = await fetch(`${API_BASE_URL}/api/events`, {
            method: 'POST',
            headers: buildHeaders(input.sessionId),
            body: JSON.stringify(body),
        })

        if (!response.ok) {
            console.warn('[api] Event post failed with status', response.status)
            // Don't throw for event failures — they should be best-effort
            return { eventId: input.eventId, accepted: false }
        }

        const data: BackendEventResponse = await response.json()

        return {
            eventId: input.eventId,
            accepted: data.accepted,
        }
    } catch (error) {
        console.warn('[api] Event post error:', error)
        return { eventId: input.eventId, accepted: false }
    }
}

/**
 * Batch post events via POST /api/events/batch.
 * Keeps the payload batched; on failure, the caller can re-queue and retry later.
 */
export async function postEventsBatch(input: { sessionId: string; events: PostEventInput[] }): Promise<{ accepted: number; failed: number }> {
    const eventTypeMap: Record<string, string> = {
        view: 'view',
        click: 'click',
        search: 'search',
        watch_start: 'watch_start',
        like: 'like',
        save: 'save',
        rating: 'rate',
        rate: 'rate',
    }

    const backendEvents = input.events.map(e => {
        const movieId = e.metadata?.movieId as string | undefined
        return {
            sessionId: e.sessionId,
            eventId: e.eventId,
            eventType: eventTypeMap[e.eventType] || e.eventType,
            movieId: movieId || null,
            queryText: e.eventType === 'search' ? (e.eventValue || e.metadata?.query) : null,
            eventValue: (e.eventType === 'rating' || e.eventType === 'rate') && e.eventValue
                ? parseInt(e.eventValue, 10)
                : null,
            metadata: { source: e.screen, component: e.component, ...e.metadata },
            timestamp: e.timestamp || new Date().toISOString(),
        }
    })

    try {
        const response = await fetch(`${API_BASE_URL}/api/events/batch`, {
            method: 'POST',
            headers: buildHeaders(input.sessionId),
            body: JSON.stringify(backendEvents),
        })

        if (!response.ok) {
            console.warn('[api] Batch event post failed with status', response.status)
            return { accepted: 0, failed: input.events.length }
        }

        return await response.json()
    } catch (error) {
        console.warn('[api] Batch event post error:', error)
        return { accepted: 0, failed: input.events.length }
    }
}
