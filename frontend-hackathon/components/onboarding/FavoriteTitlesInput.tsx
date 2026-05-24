'use client'

interface FavoriteTitlesInputProps {
  values: string[]
  onChange: (values: string[]) => void
}

export function FavoriteTitlesInput({ values, onChange }: FavoriteTitlesInputProps) {
  function updateAt(index: number, nextValue: string) {
    const next = [...values]
    next[index] = nextValue
    onChange(next)
  }

  function addField() {
    if (values.length >= 5) {
      return
    }
    onChange([...values, ''])
  }

  function removeField(index: number) {
    if (values.length === 1) {
      onChange([''])
      return
    }
    onChange(values.filter((_, currentIndex) => currentIndex !== index))
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-white">Favorite movies</p>
        <button type="button" onClick={addField} className="text-sm text-red-400" disabled={values.length >= 5}>
          Add title
        </button>
      </div>
      <div className="space-y-2">
        {values.map((value, index) => (
          <div key={index} className="flex gap-2">
            <input
              value={value}
              onChange={event => updateAt(index, event.target.value)}
              placeholder={`Movie title ${index + 1}`}
              className="w-full rounded-md border border-gray-700 bg-gray-900 px-3 py-2 text-white outline-none"
            />
            <button type="button" onClick={() => removeField(index)} className="rounded-md border border-gray-700 px-3 text-gray-300">
              Remove
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
