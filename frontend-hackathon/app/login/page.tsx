'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { loginWithBackend } from '@/lib/api/auth-client'
import { setAuthToken, setOnboardingComplete, setSessionId } from '@/lib/session/session-store'

export default function LoginPage() {
  const router = useRouter()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const data = await loginWithBackend(email, password)
      
      // Save the JWT token
      setAuthToken(data.token)
      
      // Update the sessionId to be the authenticated userId! 
      // This maps all future mock events to the real user ID.
      setSessionId(data.userId)
      setOnboardingComplete(Boolean(data.onboardingComplete))
      
      router.push(data.onboardingComplete ? '/home' : '/onboarding')
    } catch (err) {
      setError('Login failed. Please check your credentials.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-950 p-4">
      <div className="w-full max-w-md rounded-2xl bg-zinc-900 p-8 shadow-xl border border-zinc-800">
        <h1 className="mb-6 text-2xl font-bold text-white text-center">Login to Track Actions</h1>
        
        {error && (
          <div className="mb-4 rounded-lg bg-red-900/50 p-3 text-sm text-red-200 border border-red-800">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-zinc-400 mb-1">Email</label>
            <input
              type="email"
              required
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="user@example.com"
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-zinc-400 mb-1">Password</label>
            <input
              type="password"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            {loading ? 'Logging in...' : 'Log In'}
          </button>
        </form>
      </div>
    </div>
  )
}
