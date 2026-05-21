'use client'

import React, { Suspense } from 'react'
import { AppHeader } from '@/components/layout/AppHeader'
import { MovieRow } from '@/components/movie/MovieRow'
import { ErrorStatePanel, SkeletonRow } from '@/components/states/StateComponents'
import { useMovieDetail, useMovieActions } from '@/features/movie-detail/useMovieDetail'
import { useTrackEvent } from '@/hooks/useTrackEvent'
import Image from 'next/image'
import { Heart, Bookmark, Star, Play } from 'lucide-react'
import { cn } from '@/lib/utils'

interface MovieDetailPageProps {
  params: {
    id: string
  }
}

function MovieDetailContent({ movieId }: { movieId: string }) {
  const { movie, similarMovies, relatedMovies, loading, error, refetch } = useMovieDetail(movieId)
  const { liked, saved, rating, like, save, setRating } = useMovieActions(movieId)
  const trackEvent = useTrackEvent()

  if (loading) {
    return (
      <main className="min-h-screen bg-black">
        <AppHeader />
        <div className="h-96 bg-gradient-to-b from-gray-800 to-black animate-pulse" />
        <SkeletonRow />
        <SkeletonRow />
      </main>
    )
  }

  if (error || !movie) {
    return (
      <main className="min-h-screen bg-black">
        <AppHeader />
        <ErrorStatePanel title="Movie Not Found" message={error || 'The movie you are looking for could not be found.'} onRetry={refetch} />
      </main>
    )
  }

  const handleWatch = () => {
    if (!movie.playbackUrl) {
      return
    }

    trackEvent.watchStart(movieId)
    window.open(movie.playbackUrl, '_blank', 'noopener,noreferrer')
  }

  const handleLike = async () => {
    await like()
    trackEvent.like(movieId)
  }

  const handleSave = async () => {
    await save()
    trackEvent.save(movieId)
  }

  const handleRating = async (value: number) => {
    await setRating(value)
    trackEvent.rating(movieId, value)
  }

  return (
    <main className="min-h-screen bg-black">
      <AppHeader />

      {/* Backdrop and Title Section */}
      <div className="relative">
        {/* Background Backdrop */}
        <div className="relative h-80 sm:h-96 md:h-[500px] overflow-hidden">
          <Image
            src={movie.backdropUrl}
            alt={movie.title}
            fill
            priority
            className="object-cover brightness-50"
            sizes="100vw"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black via-black/50 to-transparent" />
        </div>

        {/* Movie Info Card - Overlaid */}
        <div className="relative -mt-24 sm:-mt-32 md:-mt-40 px-4 z-10">
          <div className="max-w-6xl mx-auto flex flex-col sm:flex-row gap-6 sm:gap-8">
            {/* Poster */}
            <div className="flex-shrink-0 w-full sm:w-48 md:w-56">
              <div className="relative w-full" style={{ aspectRatio: '2/3' }}>
                <Image
                  src={movie.posterUrl}
                  alt={movie.title}
                  fill
                  className="object-cover rounded-lg shadow-2xl"
                  sizes="(max-width: 640px) 100vw, (max-width: 768px) 300px, 400px"
                />
              </div>
            </div>

            {/* Title and Meta */}
            <div className="flex-1 pt-8 sm:pt-0">
              <h1 className="text-3xl sm:text-4xl md:text-5xl font-bold text-white mb-2">{movie.title}</h1>

              <div className="flex items-center gap-4 mb-4 flex-wrap">
                <div className="flex items-center gap-1 text-yellow-400">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Star
                      key={i}
                      className={cn('w-4 h-4', i < Math.floor(movie.rating / 2) ? 'fill-yellow-400' : 'text-gray-600')}
                    />
                  ))}
                </div>
                <span className="text-gray-400 text-sm">{movie.rating.toFixed(1)}/10</span>
                <span className="text-gray-400 text-sm">•</span>
                <span className="text-gray-400 text-sm">{movie.year}</span>
                <span className="text-gray-400 text-sm">•</span>
                <span className="text-gray-400 text-sm">{movie.language}</span>
              </div>

              <div className="flex flex-wrap gap-2 mb-6">
                {movie.genres.map(genre => (
                  <span key={genre} className="px-3 py-1 bg-gray-800 rounded-full text-sm text-gray-300">
                    {genre}
                  </span>
                ))}
              </div>

              {/* Action Bar */}
              <div className="flex gap-3 mb-6 flex-wrap">
                <button
                  onClick={handleWatch}
                  disabled={!movie.playbackUrl}
                  className={cn(
                    'inline-flex items-center gap-2 px-6 py-2 rounded-lg font-bold transition-colors',
                    movie.playbackUrl
                      ? 'bg-white text-black hover:bg-gray-200'
                      : 'bg-gray-700 text-gray-300 cursor-not-allowed'
                  )}
                >
                  <Play className="w-5 h-5" />
                  {movie.playbackUrl ? 'Watch Now' : 'Playback Soon'}
                </button>

                <button
                  onClick={handleLike}
                  className={cn(
                    'inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors',
                    liked
                      ? 'bg-red-600 text-white hover:bg-red-700'
                      : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                  )}
                >
                  <Heart className={cn('w-5 h-5', liked && 'fill-current')} />
                  Like
                </button>

                <button
                  onClick={handleSave}
                  className={cn(
                    'inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors',
                    saved
                      ? 'bg-blue-600 text-white hover:bg-blue-700'
                      : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                  )}
                >
                  <Bookmark className={cn('w-5 h-5', saved && 'fill-current')} />
                  Save
                </button>
              </div>

              {/* Rating */}
              <div className="flex items-center gap-3">
                <span className="text-gray-400 text-sm">Rate:</span>
                <div className="flex gap-1">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <button
                      key={i}
                      onClick={() => handleRating(i + 1)}
                      className="p-1 hover:scale-110 transition-transform"
                    >
                      <Star
                        className={cn('w-5 h-5 transition-colors', rating && i < rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-600')}
                      />
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Overview Section */}
      <section className="max-w-6xl mx-auto px-4 py-12">
        <h2 className="text-2xl font-bold text-white mb-4">Overview</h2>
        <p className="text-gray-300 leading-relaxed max-w-3xl">{movie.overview}</p>
      </section>

      {/* Similar Movies */}
      {similarMovies.length > 0 && (
        <Suspense fallback={<SkeletonRow />}>
          <MovieRow title="Similar Movies" movies={similarMovies} screen="movie_detail" component="similar_movies" />
        </Suspense>
      )}

      {/* Related Movies */}
      {relatedMovies.length > 0 && (
        <Suspense fallback={<SkeletonRow />}>
          <MovieRow title="You May Also Like" movies={relatedMovies} screen="movie_detail" component="related_movies" />
        </Suspense>
      )}

      {/* Footer */}
      <footer className="border-t border-gray-800 py-12 px-4 text-center text-gray-500 text-sm mt-12">
        <p>© 2024 MovieRecs. All rights reserved.</p>
      </footer>
    </main>
  )
}

export default async function MovieDetailPage({ params }: MovieDetailPageProps) {
  const { id } = await params
  
  return (
    <Suspense
      fallback={
        <main className="min-h-screen bg-black">
          <AppHeader />
          <div className="h-96 bg-gradient-to-b from-gray-800 to-black animate-pulse" />
          <SkeletonRow />
        </main>
      }
    >
      <MovieDetailContent movieId={id} />
    </Suspense>
  )
}
