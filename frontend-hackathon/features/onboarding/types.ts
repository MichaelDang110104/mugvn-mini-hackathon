export interface OnboardingFormValues {
  selectedGenres: string[]
  selectedThemes: string[]
  favoriteTitles: string[]
  avoidedGenres: string[]
  avoidedThemes: string[]
  preferredLanguages: string[]
  preferredEra: 'classic' | 'modern' | 'no_preference'
  preferredPace: 'fast' | 'balanced' | 'slow'
  freeTextTasteSummary: string
}

export interface SubmitOnboardingInput extends OnboardingFormValues {
  sessionId: string
}
