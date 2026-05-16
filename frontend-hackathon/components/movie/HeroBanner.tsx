'use client'

import React from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { Play, Info } from 'lucide-react'
import type { MovieItem } from '@/lib/api/mock-data'

interface HeroBannerProps {
  movie: MovieItem
}

export const HeroBanner: React.FC<HeroBannerProps> = ({ movie }) => {
  return (
    <div className="relative w-full h-screen max-h-96 sm:max-h-[500px] md:max-h-[600px] overflow-hidden">
      {/* Background Image */}
      <Image
        src={movie.backdropUrl}
        alt={movie.title}
        fill
        priority
        className="object-cover brightness-50 transition-brightness duration-300 hover:brightness-75"
        sizes="100vw"
      />

      {/* Gradient Overlay */}
      <div className="absolute inset-0 bg-gradient-to-r from-black via-black/50 to-transparent" />

      {/* Content */}
      <div className="absolute inset-0 flex flex-col justify-end md:justify-center p-6 md:p-12">
        <div className="max-w-2xl">
          <h1 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-bold text-white mb-2 md:mb-4 drop-shadow-lg">{movie.title}</h1>

          <p className="text-gray-300 text-sm sm:text-base md:text-lg mb-4 md:mb-6 line-clamp-3 drop-shadow-lg">{movie.overview}</p>

          <div className="flex flex-wrap items-center gap-2 mb-4 md:mb-6">
            {movie.genres.map(genre => (
              <span key={genre} className="px-3 py-1 bg-white/20 backdrop-blur-sm text-white text-xs sm:text-sm rounded-full font-medium">
                {genre}
              </span>
            ))}
            <div className="flex items-center gap-1 px-3 py-1 bg-yellow-400/20 backdrop-blur-sm text-yellow-300 text-xs sm:text-sm rounded-full font-semibold">
              <span>★</span>
              {movie.rating.toFixed(1)}
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-4">
            <Link
              href={`/movie/${movie.id}`}
              className="inline-flex items-center gap-2 px-6 py-2 sm:px-8 sm:py-3 bg-white text-black rounded-lg font-bold hover:bg-gray-200 transition-colors"
            >
              <Play className="w-5 h-5" />
              <span className="hidden sm:inline">Watch Now</span>
              <span className="sm:hidden">Play</span>
            </Link>

            <button className="inline-flex items-center gap-2 px-6 py-2 sm:px-8 sm:py-3 bg-white/20 backdrop-blur-sm text-white rounded-lg font-bold hover:bg-white/30 transition-colors">
              <Info className="w-5 h-5" />
              <span className="hidden sm:inline">More Info</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
