'use client'

import React from 'react'
import { MovieCard } from './MovieCard'
import type { MovieItem } from '@/lib/api/mock-data'

interface MovieGridProps {
  movies: MovieItem[]
  screen: string
  component: string
  onCardClick?: (movieId: string) => void
}

export const MovieGrid: React.FC<MovieGridProps> = ({ movies, screen, component, onCardClick }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 px-4 py-6">
      {movies.map((movie, index) => (
        <MovieCard
          key={movie.id}
          movieId={movie.id}
          title={movie.title}
          posterUrl={movie.posterUrl}
          genres={movie.genres}
          ratingAvg={movie.rating}
          year={movie.year}
          onClick={() => onCardClick?.(movie.id)}
          tracking={{
            screen,
            component,
            position: index,
          }}
        />
      ))}
    </div>
  )
}
