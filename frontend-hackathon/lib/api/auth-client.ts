import { getAuthToken } from '../session/session-store'

interface LoginResponse {
  token: string
  userId: string
  email: string
  onboardingComplete: boolean
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:9000'

export async function loginWithBackend(email: string, password: string = 'any'): Promise<LoginResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email, password })
    })

    if (!response.ok) {
      throw new Error('Login failed')
    }

    const data: LoginResponse = await response.json()
    return data
  } catch (error) {
    console.error('Error logging in:', error)
    throw error
  }
}

export function getAuthHeaders() {
  const token = getAuthToken()
  if (token) {
    return { 'Authorization': `Bearer ${token}` }
  }
  return {}
}
