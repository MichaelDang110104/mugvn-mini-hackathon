/**
 * Mock Movie Database
 * 50 diverse movies for recommendation system
 */

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

const GENRES = ['Drama', 'Action', 'Comedy', 'Thriller', 'Sci-Fi', 'Horror', 'Animation', 'Romance', 'Adventure', 'Mystery']

const OVERVIEWS = [
  'A groundbreaking story that will change how you see the world.',
  'An unforgettable journey filled with unexpected twists and turns.',
  'A masterpiece of cinema that transcends all boundaries.',
  'An emotional rollercoaster you won\'t forget.',
  'A visually stunning adventure that captures the imagination.',
  'A thought-provoking narrative about human connection.',
  'An intense exploration of love, loss, and redemption.',
  'A thrilling ride packed with spectacular action sequences.',
  'A heartwarming story about family and belonging.',
  'A dark and mysterious tale that keeps you guessing.',
]

const TITLES = [
  'Stellar Horizons', 'Midnight Chronicles', 'Crimson Echo', 'Ethereal Journey',
  'The Last Cipher', 'Silent Rebellion', 'Infinite Dreams', 'Phoenix Rising',
  'The Forgotten Gateway', 'Neon Requiem', 'Crystal Hearts', 'Shadowed Skies',
  'Beyond the Veil', 'Resonant Souls', 'The Eternal Code', 'Burning Tides',
  'Whispers in the Dark', 'Golden Hour', 'The Void Within', 'Shattered Mirrors',
  'Aurora Ascending', 'The Crimson Tide', 'Echoes of Tomorrow', 'The Silver Compass',
  'Quantum Leap', 'The Obsidian Path', 'Celestial Dance', 'The Iron Crown',
  'Forgotten Empires', 'The Sapphire Gate', 'Rogue Elements', 'The Final Frontier',
  'Temporal Shift', 'The Amber Chain', 'Boundless Sky', 'The Obsidian Tower',
  'Neon Knights', 'The Crimson Veil', 'Echoing Silence', 'The Emerald Key',
  'Infinite Loop', 'The Scarlet Thread', 'Dancing Shadows', 'The Jade Dragon',
  'Breaking Dawn', 'The Platinum Ring', 'Chaos Theory', 'The Ebony Crown',
  'Rising Storm', 'The Crystal Labyrinth',
]

const POSTER_COLORS = ['FF6B6B', '4ECDC4', '45B7D1', 'FFA07A', '98D8C8', 'F7DC6F', 'BB8FCE', '85C1E2']

export function generateMockMovie(index: number): MovieItem {
  const id = `movie_${String(index).padStart(3, '0')}`
  const title = TITLES[index % TITLES.length]
  const year = 2020 + Math.floor(index / 10)
  const genreCount = Math.floor(Math.random() * 2) + 2
  const genres: string[] = []
  for (let i = 0; i < genreCount; i++) {
    const randomGenre = GENRES[Math.floor(Math.random() * GENRES.length)]
    if (!genres.includes(randomGenre)) {
      genres.push(randomGenre)
    }
  }
  const rating = Math.round((6 + Math.random() * 4) * 10) / 10
  const overview = OVERVIEWS[index % OVERVIEWS.length]
  const posterColor = POSTER_COLORS[index % POSTER_COLORS.length]

  return {
    id,
    title: `${title} ${index}`,
    year,
    genres,
    rating,
    posterUrl: `https://via.placeholder.com/200x300/${posterColor}/FFFFFF?text=${encodeURIComponent(title.substring(0, 10))}`,
    overview,
    backdropUrl: `https://via.placeholder.com/1280x720/${posterColor}/FFFFFF?text=${encodeURIComponent(title)}`,
    language: ['en', 'es', 'fr'][Math.floor(Math.random() * 3)],
  }
}

export const MOCK_MOVIES: MovieItem[] = Array.from({ length: 50 }, (_, i) => generateMockMovie(i))

// Featured movies for hero banner
export const FEATURED_MOVIES = [MOCK_MOVIES[0], MOCK_MOVIES[12], MOCK_MOVIES[23], MOCK_MOVIES[35], MOCK_MOVIES[47]]

// Genre-grouped movies for genre sections
export const GENRE_COLLECTIONS: Record<string, MovieItem[]> = {}
GENRES.forEach(genre => {
  GENRE_COLLECTIONS[genre] = MOCK_MOVIES.filter(m => m.genres.includes(genre)).slice(0, 8)
})

// Trending movies (simulated by highest ratings + recent)
export const TRENDING_MOVIES = MOCK_MOVIES.sort((a, b) => b.rating - a.rating).slice(0, 15)

// Top movies for different time periods
export const TOP_TODAY_MOVIES = MOCK_MOVIES.sort(() => Math.random() - 0.5).slice(0, 12)
export const TOP_WEEK_MOVIES = MOCK_MOVIES.sort(() => Math.random() - 0.5).slice(0, 12)
