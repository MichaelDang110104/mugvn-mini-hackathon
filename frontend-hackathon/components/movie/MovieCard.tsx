'use client'

import React from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { useImpressionTracker } from '@/hooks/useImpressionTracker'
import { useTrackEvent } from '@/hooks/useTrackEvent'
import { Star } from 'lucide-react'

interface MovieCardProps {
  movieId: string
  title: string
  posterUrl: string
  genres: string[]
  ratingAvg: number
  year?: number
  reasonLabel?: string
  onClick?: () => void
  tracking: {
    screen: string
    component: string
    position?: number
    rowTitle?: string
  }
}

export const MovieCard: React.FC<MovieCardProps> = ({
  movieId,
  title,
  posterUrl,
  genres,
  ratingAvg,
  year,
  reasonLabel,
  onClick,
  tracking,
}) => {
  const impressionRef = useImpressionTracker({
    movieId,
    screen: tracking.screen,
    component: tracking.component,
    position: tracking.position,
    rowTitle: tracking.rowTitle,
  })

  const trackEvent = useTrackEvent()

  const handleClick = () => {
    trackEvent.click(movieId, tracking.screen, tracking.component, {
      position: tracking.position,
      rowTitle: tracking.rowTitle,
    })

    if (onClick) {
      onClick()
    }
  }

  const genreLabel = genres.slice(0, 2).join(', ')

  return (
    <Link href={`/movie/${movieId}`} onClick={handleClick}>
      <div
        ref={impressionRef}
        className="group cursor-pointer transition-transform duration-200 hover:scale-105 focus-within:ring-2 focus-within:ring-blue-500 rounded-lg overflow-hidden"
      >
        <div className="relative w-full bg-gray-800 rounded-lg overflow-hidden">
          {/* Poster Image */}
          <div className="relative w-full" style={{ aspectRatio: '2/3' }}>
            <Image
              src={posterUrl}
              alt={title}
              fill
              className="object-cover group-hover:brightness-75 transition-all duration-200"
              sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
              priority={false}
            />

            {/* Overlay on hover */}
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex items-end p-3">
              <div className="text-white text-sm font-semibold line-clamp-2">{title}</div>
            </div>
          </div>

          {/* Rating Badge */}
          <div className="absolute top-2 right-2 bg-black/80 rounded-full px-2 py-1 flex items-center gap-1">
            <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
            <span className="text-xs font-bold text-white">{ratingAvg.toFixed(1)}</span>
          </div>
        </div>

        {/* Card Info */}
        <div className="mt-2 px-1">
          <h3 className="text-sm font-semibold text-white line-clamp-2 group-hover:text-blue-400 transition-colors">{title}</h3>

          <p className="text-xs text-gray-400 mt-1 line-clamp-1">{genreLabel}</p>

          {year && <p className="text-xs text-gray-500 mt-0.5">{year}</p>}

          {reasonLabel && <p className="text-xs bg-blue-600/30 text-blue-300 px-2 py-1 rounded mt-2 inline-block line-clamp-1">{reasonLabel}</p>}
        </div>
      </div>
    </Link>
  )
}
