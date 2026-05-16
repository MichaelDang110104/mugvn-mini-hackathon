'use client'

import React from 'react'
import { AlertCircle, Zap, Brain, Sparkles } from 'lucide-react'
import { cn } from '@/lib/utils'

interface ReasonChipProps {
  label: string
  className?: string
}

export const ReasonChip: React.FC<ReasonChipProps> = ({ label, className }) => {
  return <span className={cn('inline-block bg-blue-600/30 text-blue-300 px-3 py-1 rounded-full text-xs font-semibold', className)}>{label}</span>
}

interface ModeBadgeProps {
  mode: 'semantic' | 'fallback_text' | 'personalized' | 'cold_start'
  className?: string
}

export const ModeBadge: React.FC<ModeBadgeProps> = ({ mode, className }) => {
  const modeConfig = {
    semantic: { label: 'Smart Search', icon: Brain, color: 'bg-blue-600/20 text-blue-300' },
    fallback_text: { label: 'Text Search', icon: AlertCircle, color: 'bg-amber-600/20 text-amber-300' },
    personalized: { label: 'For You', icon: Sparkles, color: 'bg-purple-600/20 text-purple-300' },
    cold_start: { label: 'Trending', icon: Zap, color: 'bg-orange-600/20 text-orange-300' },
  }

  const config = modeConfig[mode]
  const IconComponent = config.icon

  return (
    <div className={cn(`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold ${config.color}`, className)}>
      <IconComponent className="w-3.5 h-3.5" />
      {config.label}
    </div>
  )
}

interface QuickStateStripProps {
  mode: 'semantic' | 'fallback_text' | 'personalized' | 'cold_start'
  className?: string
}

export const QuickStateStrip: React.FC<QuickStateStripProps> = ({ mode, className }) => {
  const messages = {
    semantic: 'Showing personalized recommendations based on your taste',
    fallback_text: 'Showing results from our collection',
    personalized: 'Recommendations tailored just for you',
    cold_start: 'Showing trending movies right now',
  }

  return (
    <div className={cn('w-full bg-gray-800/50 border-b border-gray-700 px-4 py-3', className)}>
      <p className="text-sm text-gray-300">{messages[mode]}</p>
    </div>
  )
}
