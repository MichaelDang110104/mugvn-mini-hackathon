'use client'

import React, { Suspense } from 'react'
import { useSearchParams } from 'next/navigation'
import { AppHeader } from '@/components/layout/AppHeader'
import { MovieGrid } from '@/components/movie/MovieGrid'
import { ModeBadge } from '@/components/feedback/Chips'
import { SkeletonGrid, ErrorStatePanel, EmptyStatePanel } from '@/components/states/StateComponents'
import { useSearchData } from '@/features/search/useSearchData'

function SearchPageContent() {
  const searchParams = useSearchParams()
  const query = searchParams.get('q') || ''
  const { results, searchMode, fallbackUsed, loading, error, refetch } = useSearchData(query)

  return (
    <main className="min-h-screen bg-black">
      <AppHeader />

      {/* Search Header */}
      <div className="border-b border-gray-800 px-4 py-6">
        <div className="max-w-7xl mx-auto">
          <h1 className="text-3xl font-bold text-white mb-2">Search Results</h1>

          {query && (
            <div className="flex items-center gap-3 flex-wrap">
              <p className="text-gray-400">
                Results for: <span className="text-white font-semibold">"{query}"</span>
              </p>
              <span className="text-gray-500">•</span>
              <p className="text-gray-400">
                {results.length} <span className="text-white">{results.length === 1 ? 'result' : 'results'}</span>
              </p>
              <span className="text-gray-500">•</span>
              <ModeBadge mode={searchMode} />
              {fallbackUsed && <span className="text-xs text-amber-400">(Fallback results)</span>}
            </div>
          )}

          {!query && (
            <p className="text-gray-400">
              Enter a search term above to discover movies
            </p>
          )}
        </div>
      </div>

      {/* Loading State */}
      {loading && results.length === 0 && (
        <Suspense fallback={<SkeletonGrid />}>
          <SkeletonGrid />
        </Suspense>
      )}

      {/* Error State */}
      {error && results.length === 0 && (
        <ErrorStatePanel title="Search Failed" message={error} onRetry={refetch} />
      )}

      {/* Empty State */}
      {!loading && !error && results.length === 0 && query && (
        <EmptyStatePanel
          title="No Movies Found"
          message={`We couldn't find any movies matching "${query}". Try a different search term.`}
        />
      )}

      {/* Results Grid */}
      {results.length > 0 && (
        <Suspense fallback={<SkeletonGrid />}>
          <MovieGrid movies={results} screen="search" component="results_grid" />
        </Suspense>
      )}

      {/* Footer */}
      <footer className="border-t border-gray-800 py-12 px-4 text-center text-gray-500 text-sm">
        <p>© 2024 MovieRecs. All rights reserved.</p>
      </footer>
    </main>
  )
}

export default function SearchPage() {
  return (
    <Suspense
      fallback={
        <main className="min-h-screen bg-black">
          <AppHeader />
          <SkeletonGrid />
        </main>
      }
    >
      <SearchPageContent />
    </Suspense>
  )
}
