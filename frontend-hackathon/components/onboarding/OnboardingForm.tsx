'use client'

import { FavoriteMoviesPicker } from '@/components/onboarding/FavoriteMoviesPicker'
import { MultiSelectChips } from '@/components/onboarding/MultiSelectChips'
import { PreferenceSelectors } from '@/components/onboarding/PreferenceSelectors'
import {
  LANGUAGE_OPTIONS,
  THEME_OPTIONS,
} from '@/features/onboarding/constants'
import { useOnboardingForm } from '@/features/onboarding/useOnboardingForm'

export function OnboardingForm() {
  const {
    values,
    setValues,
    canSubmit,
    submit,
    submitting,
    error,
    genreOptions,
    loadingOptions,
    movieOptions,
    movieSearchQuery,
    setMovieSearchQuery,
    loadingMovies,
    movieError,
    canBrowseMovies,
    addFavoriteMovie,
    removeFavoriteMovie,
  } = useOnboardingForm()

  return (
    <main className="min-h-screen bg-black px-4 py-10 text-white">
      <div className="mx-auto max-w-3xl space-y-8 rounded-2xl border border-gray-800 bg-gray-950 p-8">
        <div className="space-y-2">
          <h1 className="text-3xl font-semibold">Set up your movie taste</h1>
          <p className="text-gray-400">
            Pick a few things you like so we can personalize your first recommendations.
          </p>
        </div>

        <MultiSelectChips
          label="Pick exactly 3 favorite genres"
          options={genreOptions}
          selected={values.selectedGenres}
          max={3}
          onChange={selectedGenres => setValues(current => ({ ...current, selectedGenres }))}
        />

        {loadingOptions ? <p className="text-sm text-gray-400">Loading genres...</p> : null}

        <MultiSelectChips
          label="Pick 3 to 5 moods or themes"
          options={THEME_OPTIONS}
          selected={values.selectedThemes}
          max={5}
          onChange={selectedThemes => setValues(current => ({ ...current, selectedThemes }))}
        />

        <FavoriteMoviesPicker
          searchQuery={movieSearchQuery}
          onSearchQueryChange={setMovieSearchQuery}
          selectedMovies={values.favoriteMovies}
          options={movieOptions}
          loading={loadingMovies}
          error={movieError}
          canSearch={canBrowseMovies}
          onAddMovie={addFavoriteMovie}
          onRemoveMovie={removeFavoriteMovie}
        />

        <MultiSelectChips
          label="Pick genres to avoid"
          options={genreOptions}
          selected={values.avoidedGenres}
          max={3}
          onChange={avoidedGenres => setValues(current => ({ ...current, avoidedGenres }))}
        />

        <PreferenceSelectors
          preferredLanguages={values.preferredLanguages}
          preferredEra={values.preferredEra}
          preferredPace={values.preferredPace}
          onLanguagesChange={preferredLanguages => setValues(current => ({ ...current, preferredLanguages }))}
          onEraChange={preferredEra => setValues(current => ({ ...current, preferredEra }))}
          onPaceChange={preferredPace => setValues(current => ({ ...current, preferredPace }))}
          languageOptions={LANGUAGE_OPTIONS}
        />

        <div className="space-y-2">
          <label className="text-sm font-medium text-white">Tell us what kind of movies you enjoy</label>
          <textarea
            value={values.freeTextTasteSummary}
            onChange={event => setValues(current => ({ ...current, freeTextTasteSummary: event.target.value }))}
            placeholder="Thoughtful emotional sci-fi, strong characters, twisty thrillers..."
            className="min-h-32 w-full rounded-md border border-gray-700 bg-gray-900 px-3 py-2 text-white"
          />
        </div>

        {error ? <p className="text-sm text-red-400">{error}</p> : null}

        <button
          type="button"
          disabled={!canSubmit || submitting}
          onClick={submit}
          className="w-full rounded-md bg-red-600 px-4 py-3 font-medium text-white disabled:cursor-not-allowed disabled:bg-gray-700"
        >
          {submitting ? 'Saving your taste profile...' : 'Continue to recommendations'}
        </button>
      </div>
    </main>
  )
}
