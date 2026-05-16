'use client'

import React, { useRef } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { MovieCard } from './MovieCard'
import type { MovieItem } from '@/lib/api/mock-data'

interface MovieRowProps {
  title: string
  movies: MovieItem[]
  screen: string
  component: string
  reasonChip?: string
}

export const MovieRow: React.FC<MovieRowProps> = ({ title, movies, screen, component, reasonChip }) => {
  const scrollContainerRef = useRef<HTMLDivElement>(null)

  const scroll = (direction: 'left' | 'right') => {
    if (!scrollContainerRef.current) return

    const scrollAmount = 350
    const target = direction === 'left' ? scrollContainerRef.current.scrollLeft - scrollAmount : scrollContainerRef.current.scrollLeft + scrollAmount

    scrollContainerRef.current.scrollTo({
      left: target,
      behavior: 'smooth',
    })
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowLeft') {
      scroll('left')
    } else if (e.key === 'ArrowRight') {
      scroll('right')
    }
  }

  return (
    <section className="w-full py-6">
      <div className="flex items-center justify-between mb-4 px-4">
        <div>
          <h2 className="text-xl font-bold text-white">{title}</h2>
          {reasonChip && <p className="text-xs text-gray-400 mt-1">{reasonChip}</p>}
        </div>
      </div>

      {/* Horizontal scroll container */}
      <div className="relative group">
        <div
          ref={scrollContainerRef}
          className="flex gap-4 overflow-x-auto scroll-smooth px-4 pb-4"
          style={{ scrollBehavior: 'smooth' }}
          onKeyDown={handleKeyDown}
          tabIndex={0}
          role="region"
          aria-label={`${title} movie list`}
        >
          {movies.map((movie, index) => (
            <div key={movie.id} className="flex-shrink-0" style={{ width: '200px' }}>
              <MovieCard
                movieId={movie.id}
                title={movie.title}
                posterUrl={movie.posterUrl}
                genres={movie.genres}
                ratingAvg={movie.rating}
                year={movie.year}
                reasonLabel={index === 0 ? reasonChip : undefined}
                tracking={{
                  screen,
                  component,
                  position: index,
                  rowTitle: title,
                }}
              />
            </div>
          ))}
        </div>

        {/* Left scroll button */}
        <button
          onClick={() => scroll('left')}
          className="absolute left-0 top-1/2 -translate-y-1/2 z-10 bg-black/60 hover:bg-black/80 text-white p-2 rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
          aria-label="Scroll left"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>

        {/* Right scroll button */}
        <button
          onClick={() => scroll('right')}
          className="absolute right-0 top-1/2 -translate-y-1/2 z-10 bg-black/60 hover:bg-black/80 text-white p-2 rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
          aria-label="Scroll right"
        >
          <ChevronRight className="w-5 h-5" />
        </button>
      </div>
    </section>
  )
}
