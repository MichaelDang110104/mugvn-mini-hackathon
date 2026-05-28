'use client'

import React, { Suspense, useEffect } from 'react'
import Link from 'next/link'
import { AppHeader } from '@/components/layout/AppHeader'
import { ErrorStatePanel } from '@/components/states/StateComponents'
import { useMovieDetail } from '@/features/movie-detail/useMovieDetail'
import { useTrackEvent } from '@/hooks/useTrackEvent'
import { ArrowLeft, ExternalLink } from 'lucide-react'

interface MovieWatchPageProps {
  params: {
    id: string
  }
}

function isDirectVideoUrl(url: string) {
  try {
    const { pathname } = new URL(url)
    return /\.(mp4|webm|ogg|mov)(\?.*)?$/i.test(pathname)
  } catch {
    return /\.(mp4|webm|ogg|mov)(\?.*)?$/i.test(url)
  }
}

function MovieWatchContent({ movieId }: { movieId: string }) {
  const { movie, loading, error, refetch } = useMovieDetail(movieId)
  const trackEvent = useTrackEvent()

  useEffect(() => {
    if (movie?.playbackUrl) {
      trackEvent.watchStart(movieId)
    }
  }, [movie?.playbackUrl, movieId, trackEvent])

  if (loading) {
    return (
      <main className="min-h-screen bg-black">
        <AppHeader />
        <div className="mx-auto max-w-7xl px-4 py-6">
          <div className="aspect-video w-full animate-pulse rounded-2xl bg-gray-900" />
          <div className="mt-6 h-8 w-64 animate-pulse rounded bg-gray-900" />
        </div>
      </main>
    )
  }

  if (error || !movie) {
    return (
      <main className="min-h-screen bg-black">
        <AppHeader />
        <ErrorStatePanel title="Movie Not Found" message={error || 'The movie you are trying to watch could not be found.'} onRetry={refetch} />
      </main>
    )
  }

  if (!movie.playbackUrl) {
    return (
      <main className="min-h-screen bg-black">
        <AppHeader />
        <section className="mx-auto flex min-h-[70vh] max-w-3xl flex-col items-center justify-center px-4 text-center">
          <p className="mb-3 text-sm font-semibold uppercase tracking-[0.3em] text-gray-500">Playback unavailable</p>
          <h1 className="mb-4 text-3xl font-bold text-white sm:text-5xl">{movie.title}</h1>
          <p className="mb-8 text-gray-400">This movie does not have a playback URL yet.</p>
          <Link href={`/movie/${movieId}`} className="inline-flex items-center gap-2 rounded-lg bg-white px-5 py-3 font-bold text-black transition-colors hover:bg-gray-200">
            <ArrowLeft className="h-5 w-5" />
            Back to details
          </Link>
        </section>
      </main>
    )
  }

  const directVideo = isDirectVideoUrl(movie.playbackUrl)

  return (
    <main className="min-h-screen bg-black text-white">
      <AppHeader />

      <section className="mx-auto max-w-7xl px-4 py-4 sm:py-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <Link href={`/movie/${movieId}`} className="inline-flex items-center gap-2 text-sm font-medium text-gray-300 transition-colors hover:text-white">
            <ArrowLeft className="h-4 w-4" />
            Back to details
          </Link>

          <a
            href={movie.playbackUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 text-sm font-medium text-gray-400 transition-colors hover:text-white"
          >
            Open externally
            <ExternalLink className="h-4 w-4" />
          </a>
        </div>

        <div className="overflow-hidden rounded-2xl border border-white/10 bg-gray-950 shadow-2xl shadow-black">
          <div className="aspect-video w-full bg-black">
            {directVideo ? (
              <video src={movie.playbackUrl} controls autoPlay className="h-full w-full" poster={movie.backdropUrl} />
            ) : (
              <iframe
                src={movie.playbackUrl}
                title={`${movie.title} playback`}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowFullScreen
                className="h-full w-full border-0"
              />
            )}
          </div>
        </div>

        <div className="mt-6 max-w-4xl">
          <p className="mb-2 text-sm font-semibold uppercase tracking-[0.25em] text-red-400">Now watching</p>
          <h1 className="text-3xl font-bold sm:text-5xl">{movie.title}</h1>
          <p className="mt-3 max-w-3xl text-gray-400">{movie.overview}</p>
        </div>
      </section>
    </main>
  )
}

export default async function MovieWatchPage({ params }: MovieWatchPageProps) {
  const { id } = await params

  return (
    <Suspense
      fallback={
        <main className="min-h-screen bg-black">
          <AppHeader />
          <div className="mx-auto max-w-7xl px-4 py-6">
            <div className="aspect-video w-full animate-pulse rounded-2xl bg-gray-900" />
          </div>
        </main>
      }
    >
      <MovieWatchContent movieId={id} />
    </Suspense>
  )
}
