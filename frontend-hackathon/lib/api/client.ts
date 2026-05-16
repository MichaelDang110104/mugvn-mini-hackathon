/**
 * API Client Module
 * Provides mock API functions matching the backend contract
 * Designed for easy migration to real backend endpoints
 */

import {
  MOCK_MOVIES,
  FEATURED_MOVIES,
  TRENDING_MOVIES,
  TOP_TODAY_MOVIES,
  TOP_WEEK_MOVIES,
  GENRE_COLLECTIONS,
  type MovieItem,
} from './mock-data'

// Simulated delay for realistic behavior
const SIMULATED_DELAY = 300 // ms

interface RecommendationSection {
  id: string
  title: string
  reasonChip?: string
  movies: MovieItem[]
}

interface RecommendationsResponse {
  sessionId: string
  recommendationMode: 'semantic' | 'fallback_text' | 'personalized' | 'cold_start'
  sections: RecommendationSection[]
}

interface SearchResponse {
  sessionId: string
  query: string
  results: MovieItem[]
  searchMode: 'semantic' | 'fallback_text'
  fallbackUsed: boolean
  hitCount: number
}

interface MovieDetailResponse {
  sessionId: string
  movie: MovieItem & { language: string; overview: string }
  similarMovies: MovieItem[]
  relatedMovies: MovieItem[]
}

interface EventResponse {
  eventId: string
  accepted: boolean
}

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

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

function shouldReturnFallback(): boolean {
  return Math.random() < 0.1 // 10% chance of fallback
}

function selectRandomMovies(count: number, exclude?: string[]): MovieItem[] {
  const available = MOCK_MOVIES.filter(m => !exclude?.includes(m.id))
  const result: MovieItem[] = []
  for (let i = 0; i < count && available.length > 0; i++) {
    const idx = Math.floor(Math.random() * available.length)
    result.push(available[idx])
    available.splice(idx, 1)
  }
  return result
}

/**
 * Fetch home page recommendations
 */
export async function fetchRecommendations(input: FetchRecommendationsInput): Promise<RecommendationsResponse> {
  await sleep(SIMULATED_DELAY)

  const useFallback = shouldReturnFallback()
  const mode: RecommendationsResponse['recommendationMode'] = useFallback ? 'fallback_text' : 'personalized'

  const sections: RecommendationSection[] = [
    {
      id: 'recommended_for_you',
      title: 'Recommended For You',
      reasonChip: 'Based on your activity',
      movies: selectRandomMovies(12),
    },
    {
      id: 'trending_now',
      title: 'Trending Now',
      movies: TRENDING_MOVIES.slice(0, 12),
    },
    {
      id: 'top_today',
      title: 'Top Today',
      movies: TOP_TODAY_MOVIES.slice(0, 12),
    },
    {
      id: 'top_this_week',
      title: 'Top This Week',
      movies: TOP_WEEK_MOVIES.slice(0, 12),
    },
    {
      id: 'because_you_liked',
      title: 'Because You Liked Stellar Horizons 0',
      reasonChip: 'Similar to movies you liked',
      movies: selectRandomMovies(12),
    },
  ]

  // Add genre sections
  const genreEntries = Object.entries(GENRE_COLLECTIONS)
  for (let i = 0; i < Math.min(2, genreEntries.length); i++) {
    const [genre, movies] = genreEntries[i]
    sections.push({
      id: `genre_${genre.toLowerCase()}`,
      title: `${genre} Movies`,
      movies: movies.slice(0, 12),
    })
  }

  sections.push({
    id: 'continue_exploring',
    title: 'Continue Exploring',
    movies: selectRandomMovies(12),
  })

  return {
    sessionId: input.sessionId,
    recommendationMode: mode,
    sections,
  }
}

/**
 * Fetch search results
 */
export async function fetchSearchResults(input: FetchSearchInput): Promise<SearchResponse> {
  await sleep(SIMULATED_DELAY)

  const query = input.q.toLowerCase()
  let results: MovieItem[] = []

  // Simple mock search: match title or overview
  results = MOCK_MOVIES.filter(m => m.title.toLowerCase().includes(query) || m.overview.toLowerCase().includes(query))

  const fallbackUsed = results.length < 3 || shouldReturnFallback()
  const searchMode = fallbackUsed ? 'fallback_text' : 'semantic'

  if (fallbackUsed && results.length < 3) {
    // Return random movies as fallback
    results = selectRandomMovies(12)
  } else if (results.length === 0) {
    results = selectRandomMovies(12)
  }

  return {
    sessionId: input.sessionId,
    query: input.q,
    results: results.slice(0, input.limit || 50),
    searchMode,
    fallbackUsed,
    hitCount: results.length,
  }
}

/**
 * Fetch movie detail page data
 */
export async function fetchMovieDetail(input: FetchMovieDetailInput): Promise<MovieDetailResponse> {
  await sleep(SIMULATED_DELAY)

  const movie = MOCK_MOVIES.find(m => m.id === input.movieId) || MOCK_MOVIES[0]
  const similarMovies = selectRandomMovies(12, [movie.id])
  const relatedMovies = selectRandomMovies(12, [movie.id, ...similarMovies.map(m => m.id)])

  return {
    sessionId: input.sessionId,
    movie: {
      ...movie,
      language: movie.language,
      overview: movie.overview,
    },
    similarMovies,
    relatedMovies,
  }
}

/**
 * Post a single event (designed for batch endpoint later)
 */
export async function postEvent(input: PostEventInput): Promise<EventResponse> {
  // In real implementation, this would be batched
  // For now, just accept and return
  console.log('[v0] Event posted:', {
    eventId: input.eventId,
    eventType: input.eventType,
    movieId: input.metadata.movieId,
  })

  return {
    eventId: input.eventId,
    accepted: true,
  }
}

/**
 * Batch post events (future endpoint)
 */
export async function postEventsBatch(input: { sessionId: string; events: PostEventInput[] }): Promise<{ accepted: number; failed: number }> {
  await sleep(100)

  console.log('[v0] Batch posted:', input.events.length, 'events')

  return {
    accepted: input.events.length,
    failed: 0,
  }
}
