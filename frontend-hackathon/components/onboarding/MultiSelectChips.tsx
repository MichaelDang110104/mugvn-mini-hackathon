'use client'

interface MultiSelectChipsProps {
  label: string
  options: string[]
  selected: string[]
  max?: number
  onChange: (values: string[]) => void
}

export function MultiSelectChips({ label, options, selected, max, onChange }: MultiSelectChipsProps) {
  function toggle(option: string) {
    if (selected.includes(option)) {
      onChange(selected.filter(value => value !== option))
      return
    }

    if (max && selected.length >= max) {
      return
    }

    onChange([...selected, option])
  }

  return (
    <div className="space-y-3">
      <p className="text-sm font-medium text-white">{label}</p>
      <div className="flex flex-wrap gap-2">
        {options.map(option => {
          const active = selected.includes(option)
          return (
            <button
              key={option}
              type="button"
              onClick={() => toggle(option)}
              className={`rounded-full border px-3 py-2 text-sm transition ${
                active ? 'border-red-500 bg-red-500 text-white' : 'border-gray-700 bg-gray-900 text-gray-300'
              }`}
            >
              {option}
            </button>
          )
        })}
      </div>
    </div>
  )
}
