'use client'

interface PreferenceSelectorsProps {
  preferredLanguages: string[]
  preferredEra: 'classic' | 'modern' | 'no_preference'
  preferredPace: 'fast' | 'balanced' | 'slow'
  onLanguagesChange: (values: string[]) => void
  onEraChange: (value: 'classic' | 'modern' | 'no_preference') => void
  onPaceChange: (value: 'fast' | 'balanced' | 'slow') => void
  languageOptions: string[]
}

export function PreferenceSelectors({
  preferredLanguages,
  preferredEra,
  preferredPace,
  onLanguagesChange,
  onEraChange,
  onPaceChange,
  languageOptions,
}: PreferenceSelectorsProps) {
  function toggleLanguage(language: string) {
    if (preferredLanguages.includes(language)) {
      onLanguagesChange(preferredLanguages.filter(value => value !== language))
      return
    }
    onLanguagesChange([...preferredLanguages, language])
  }

  return (
    <div className="space-y-6">
      <div className="space-y-3">
        <p className="text-sm font-medium text-white">Preferred languages</p>
        <div className="flex flex-wrap gap-2">
          {languageOptions.map(language => {
            const active = preferredLanguages.includes(language)
            return (
              <button
                key={language}
                type="button"
                onClick={() => toggleLanguage(language)}
                className={`rounded-full border px-3 py-2 text-sm ${
                  active ? 'border-red-500 bg-red-500 text-white' : 'border-gray-700 bg-gray-900 text-gray-300'
                }`}
              >
                {language}
              </button>
            )
          })}
        </div>
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium text-white">Preferred era</label>
        <select
          value={preferredEra}
          onChange={event => onEraChange(event.target.value as 'classic' | 'modern' | 'no_preference')}
          className="w-full rounded-md border border-gray-700 bg-gray-900 px-3 py-2 text-white"
        >
          <option value="no_preference">No preference</option>
          <option value="classic">Classic</option>
          <option value="modern">Modern</option>
        </select>
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium text-white">Preferred pace</label>
        <select
          value={preferredPace}
          onChange={event => onPaceChange(event.target.value as 'fast' | 'balanced' | 'slow')}
          className="w-full rounded-md border border-gray-700 bg-gray-900 px-3 py-2 text-white"
        >
          <option value="fast">Fast</option>
          <option value="balanced">Balanced</option>
          <option value="slow">Slow</option>
        </select>
      </div>
    </div>
  )
}
