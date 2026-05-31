'use client'

import type { FavoriteMovieSelection, OnboardingMovieOption } from '@/lib/api/types'

interface FavoriteMoviesPickerProps {
  searchQuery: string
  onSearchQueryChange: (value: string) => void
  selectedMovies: FavoriteMovieSelection[]
  options: OnboardingMovieOption[]
  loading: boolean
  error: string | null
  canSearch: boolean
  onAddMovie: (movie: OnboardingMovieOption) => void
  onRemoveMovie: (movieId: string) => void
}

export function FavoriteMoviesPicker({
  searchQuery,
  onSearchQueryChange,
  selectedMovies,
  options,
  loading,
  error,
  canSearch,
  onAddMovie,
  onRemoveMovie,
}: FavoriteMoviesPickerProps) {
  return (
    <div className="space-y-3">
      <div className="space-y-1">
        <p className="text-sm font-medium text-white">Favorite movies</p>
        <p className="text-sm text-gray-400">
          Search your collection or leave the box empty to browse 10 random picks from your selected genres.
        </p>
      </div>

      <input
        value={searchQuery}
        onChange={event => onSearchQueryChange(event.target.value)}
        placeholder={canSearch ? 'Search available movies' : 'Select at least 3 genres first'}
        disabled={!canSearch}
        className="w-full rounded-md border border-gray-700 bg-gray-900 px-3 py-2 text-white outline-none disabled:cursor-not-allowed disabled:opacity-60"
      />

      {selectedMovies.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {selectedMovies.map(movie => (
            <button
              key={movie.movieId}
              type="button"
              onClick={() => onRemoveMovie(movie.movieId)}
              className="rounded-full border border-red-500 bg-red-500/15 px-3 py-2 text-sm text-white"
            >
              {movie.title} x
            </button>
          ))}
        </div>
      ) : null}

      {error ? <p className="text-sm text-red-400">{error}</p> : null}

      <div className="rounded-xl border border-gray-800 bg-gray-950/70">
        {loading ? <p className="px-4 py-6 text-sm text-gray-400">Loading movies...</p> : null}

        {!loading && !canSearch ? <p className="px-4 py-6 text-sm text-gray-400">Pick 3 genres to unlock movie suggestions.</p> : null}

        {!loading && canSearch && options.length === 0 ? (
          <p className="px-4 py-6 text-sm text-gray-400">No movies found for the current filters.</p>
        ) : null}

        {!loading && canSearch && options.length > 0 ? (
          <div className="divide-y divide-gray-800">
            {options.map(movie => {
              const isSelected = selectedMovies.some(selected => selected.movieId === movie.movieId)
              return (
                <button
                  key={movie.movieId}
                  type="button"
                  disabled={isSelected}
                  onClick={() => onAddMovie(movie)}
                  className="flex w-full items-center justify-between gap-4 px-4 py-3 text-left transition hover:bg-gray-900 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <div className="space-y-1">
                    <p className="text-sm font-medium text-white">{movie.title}</p>
                    <p className="text-xs text-gray-400">
                      {[movie.year, movie.genres.join(', ')].filter(Boolean).join(' · ')}
                    </p>
                  </div>
                  <span className="text-sm text-red-400">{isSelected ? 'Selected' : 'Add'}</span>
                </button>
              )
            })}
          </div>
        ) : null}
      </div>
    </div>
  )
}
