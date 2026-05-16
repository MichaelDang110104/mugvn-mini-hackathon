'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { Search, Home } from 'lucide-react'
import { cn } from '@/lib/utils'

interface AppHeaderProps {
  onSearchClick?: () => void
}

export const AppHeader: React.FC<AppHeaderProps> = ({ onSearchClick }) => {
  const router = useRouter()
  const [isScrolled, setIsScrolled] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 50)
    }

    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchQuery.trim()) {
      router.push(`/search?q=${encodeURIComponent(searchQuery.trim())}`)
      setSearchQuery('')
    }
  }

  return (
    <header
      className={cn(
        'sticky top-0 z-40 w-full transition-all duration-300',
        isScrolled ? 'bg-black/95 backdrop-blur-md border-b border-gray-700/50 py-3' : 'bg-gradient-to-b from-black to-black/80 py-4'
      )}
    >
      <div className="max-w-7xl mx-auto px-4 flex items-center justify-between gap-4">
        {/* Logo / Home */}
        <Link href="/home" className="flex items-center gap-2 font-bold text-white hover:text-blue-400 transition-colors flex-shrink-0">
          <Home className="w-6 h-6" />
          <span className="hidden sm:inline">MovieRecs</span>
        </Link>

        {/* Search Bar */}
        <form onSubmit={handleSearchSubmit} className="flex-1 max-w-md mx-auto">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search movies..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              onClick={onSearchClick}
              className="w-full bg-gray-800/50 border border-gray-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
            />
          </div>
        </form>

        {/* Nav Links */}
        <nav className="flex items-center gap-2 sm:gap-4">
          <Link
            href="/search"
            className="inline-flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-gray-300 hover:text-white transition-colors"
          >
            <Search className="w-4 h-4" />
            <span className="hidden sm:inline">Browse</span>
          </Link>
        </nav>
      </div>
    </header>
  )
}
