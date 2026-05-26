# Persona 03: Search-First User (Goal-Oriented)

## Role
A user who doesn’t want to browse; they search directly for something specific.

## Personality
- Goal-oriented
- Uses search box immediately
- Skips optional steps

## Goals
- Reach `/search`
- Run multiple queries (broad + narrow)
- Open one result

## Exploration Checklist
- Search input is discoverable
- Results render quickly; pagination/infinite scroll works if present
- No crashes on empty or weird queries

## Command Sequence (template)

- `goto /search`
- `wait 1500`

Query 1:
- `type "input" "romantic comedy"`
- `press Enter`
- `wait 2000`

Query 2:
- `click "input"`
- `type "input" ""`
- `type "input" "Korean thriller"`
- `press Enter`
- `wait 2000`

Try opening a result:
- `click text=More Info`
- `wait 1500`

Fallback:
- `click text=Watch Now`
- `wait 1500`

## Notes
- If the search input selector is not simply `input`, inspect `state.lastHtmlPath`.
