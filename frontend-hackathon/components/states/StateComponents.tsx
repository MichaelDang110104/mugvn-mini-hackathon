'use client'

import React from 'react'
import { AlertCircle, RotateCcw } from 'lucide-react'

export const SkeletonRow: React.FC = () => {
  return (
    <section className="w-full py-6 px-4">
      <div className="h-6 bg-gray-700/50 rounded w-32 mb-4 animate-pulse" />
      <div className="flex gap-4 overflow-hidden">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="flex-shrink-0" style={{ width: '200px' }}>
            <div className="w-full bg-gray-700/50 rounded-lg animate-pulse" style={{ aspectRatio: '2/3' }} />
            <div className="h-4 bg-gray-700/50 rounded mt-2 animate-pulse" />
            <div className="h-3 bg-gray-700/50 rounded mt-1 w-3/4 animate-pulse" />
          </div>
        ))}
      </div>
    </section>
  )
}

export const SkeletonGrid: React.FC = () => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 px-4 py-6">
      {Array.from({ length: 12 }).map((_, i) => (
        <div key={i} className="animate-pulse">
          <div className="w-full bg-gray-700/50 rounded-lg" style={{ aspectRatio: '2/3' }} />
          <div className="h-4 bg-gray-700/50 rounded mt-2" />
          <div className="h-3 bg-gray-700/50 rounded mt-1 w-3/4" />
        </div>
      ))}
    </div>
  )
}

interface ErrorStatePanelProps {
  title?: string
  message?: string
  onRetry?: () => void
}

export const ErrorStatePanel: React.FC<ErrorStatePanelProps> = ({
  title = 'Something went wrong',
  message = 'We were unable to load this content. Please try again.',
  onRetry,
}) => {
  return (
    <div className="w-full py-8 px-4 flex items-center justify-center">
      <div className="text-center max-w-md">
        <AlertCircle className="w-12 h-12 text-red-400 mx-auto mb-4" />
        <h3 className="text-lg font-semibold text-white mb-2">{title}</h3>
        <p className="text-sm text-gray-400 mb-4">{message}</p>
        {onRetry && (
          <button
            onClick={onRetry}
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            Try Again
          </button>
        )}
      </div>
    </div>
  )
}

interface EmptyStatePanelProps {
  title?: string
  message?: string
}

export const EmptyStatePanel: React.FC<EmptyStatePanelProps> = ({ title = 'No results found', message = 'Try adjusting your search criteria.' }) => {
  return (
    <div className="w-full py-8 px-4 flex items-center justify-center">
      <div className="text-center max-w-md">
        <div className="w-12 h-12 bg-gray-700/50 rounded-full mx-auto mb-4" />
        <h3 className="text-lg font-semibold text-white mb-2">{title}</h3>
        <p className="text-sm text-gray-400">{message}</p>
      </div>
    </div>
  )
}
