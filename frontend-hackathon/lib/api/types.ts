/**
 * API Types - Exact shapes matching the backend DTOs
 * These map 1:1 to the Java backend response classes.
 */

// --- Shared shapes (from backend SearchResponse.java) ---

export interface Availability {
  isAvailable: boolean
  region: string
}

export interface Reason {
  code: string
  label: string
}

export interface MovieSummary {
  id: string
  title: string
  posterUrl: string | null
  genres: string[]
  ratingAvg: number | null
  availability: Availability | null
}

export interface SearchItem {
  movie: MovieSummary
  score: number
  reasons: Reason[]
}

// --- GET /api/movies/search response ---

export interface BackendSearchResponse {
  items: SearchItem[]
  mode: 'semantic' | 'fallback_text' | 'cold_start'
  fallbackUsed: boolean
  query: string | null
  hint: string | null
}

// --- GET /api/movies/{movieId} response ---

export interface MovieDetail {
  id: string
  title: string
  overview: string | null
  fullplot: string | null
  genres: string[]
  posterUrl: string | null
  playbackUrl: string | null
  ratingAvg: number | null
  availability: Availability | null
  cast: string[] | null
  directors: string[] | null
  writers: string[] | null
  languages: string[] | null
  countries: string[] | null
  runtime: number | null
  year: number | null
  rated: string | null
}

export interface BackendMovieDetailResponse {
  movie: MovieDetail
  similarMovies: SearchItem[]
  mode: string
  fallbackUsed: boolean
}

// --- GET /api/recommendations response ---

export interface BackendRecommendationResponse {
  items: SearchItem[]
  mode: 'semantic' | 'personalized' | 'cold_start' | 'fallback_text'
  fallbackUsed: boolean
  generatedAt: string
}

// --- POST /api/events request ---

export interface BackendEventRequest {
  sessionId: string
  eventId: string
  eventType: 'search' | 'view' | 'click' | 'watch_start' | 'like' | 'save' | 'rate'
  movieId?: string | null
  queryText?: string | null
  eventValue?: number | null
  metadata?: Record<string, unknown> | null
  timestamp?: string | null
}

// --- POST /api/events response ---

export interface BackendEventResponse {
  accepted: boolean
  profileUpdated: boolean
  rerankedUsingRecentEvents: boolean
}

// --- Error response ---

export interface BackendErrorResponse {
  error: {
    code: 'VALIDATION_ERROR' | 'NOT_FOUND' | 'INTERNAL_ERROR'
    message: string
    details?: { field: string; reason: string }[]
  }
}

// --- Frontend MovieItem (used by UI components) ---
// This is the "view model" that components consume.
// We transform backend shapes into this for rendering.

export interface MovieItem {
  id: string
  title: string
  year: number
  genres: string[]
  rating: number
  posterUrl: string
  overview: string
  backdropUrl: string
  language: string
}
