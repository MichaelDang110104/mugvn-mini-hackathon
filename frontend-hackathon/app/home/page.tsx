'use client'

import React, { Suspense } from 'react'
import { AppHeader } from '@/components/layout/AppHeader'
import { HeroBanner } from '@/components/movie/HeroBanner'
import { MovieRow } from '@/components/movie/MovieRow'
import { SkeletonRow, ErrorStatePanel } from '@/components/states/StateComponents'
import { useHomeData } from '@/features/home/useHomeData'

export default function HomePage() {
  const { sections, loading, error, refetch } = useHomeData()

  if (loading && sections.length === 0) {
    return (
      <main className="min-h-screen bg-black">
        <AppHeader />
        <div className="animate-pulse">
          <div className="h-96 bg-gray-800" />
          <SkeletonRow />
          <SkeletonRow />
          <SkeletonRow />
        </div>
      </main>
    )
  }

  if (error && sections.length === 0) {
    return (
      <main className="min-h-screen bg-black">
        <AppHeader />
        <ErrorStatePanel title="Failed to Load Recommendations" message={error} onRetry={refetch} />
      </main>
    )
  }

  // Get featured movie for hero
  const featuredMovie = sections[0]?.movies[0]

  return (
    <main className="min-h-screen bg-black">
      <AppHeader />

      {/* Hero Banner */}
      {featuredMovie && <HeroBanner movie={featuredMovie} />}

      {/* Sections */}
      <div className="space-y-4 pb-12">
        {sections.map(section => (
          <Suspense key={section.id} fallback={<SkeletonRow />}>
            {section.movies && section.movies.length > 0 ? (
              <MovieRow title={section.title} movies={section.movies} screen="home" component={section.id} />
            ) : null}
          </Suspense>
        ))}
      </div>

      {/* Footer */}
      <footer className="border-t border-gray-800 py-12 px-4 text-center text-gray-500 text-sm">
        <p>© 2024 MovieRecs. All rights reserved.</p>
      </footer>
    </main>
  )
}
