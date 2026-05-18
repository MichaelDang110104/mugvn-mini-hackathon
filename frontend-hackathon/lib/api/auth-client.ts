import { getAuthToken } from '../session/session-store'

export async function loginWithBackend(email: string, password: string = 'any') {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email, password })
    })

    if (!response.ok) {
      throw new Error('Login failed')
    }

    const data = await response.json()
    return data // { token, userId, email }
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
