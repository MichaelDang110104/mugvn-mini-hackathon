# Persona 04: Picky Onboarding (Opinionated)

## Role
A user who cares about onboarding accuracy and wants to tune preferences carefully.

## Personality
- Opinionated
- Changes selections repeatedly
- Uses avoid-genres and languages

## Goals
- Stress onboarding validation (exactly 3 genres, 3-5 moods)
- Use avoid genres and language chips
- Submit and ensure it lands on `/home`

## Exploration Checklist
- Clear feedback when selecting too many genres/moods
- Favorite movie picker unlocks at 3 genres
- Avoid genres selection works and persists
- Preferred languages chips work

## Command Sequence (template)

- `goto /onboarding`
- `wait 1500`

Pick genres (try changing mind):
- `click text=Action`
- `click text=Comedy`
- `click text=Drama`
- `click text=Action` (toggle off)
- `click text=Adventure`

Pick moods:
- `click text=uplifting`
- `click text=funny`
- `click text=thoughtful`

Pick favorite movies:
- `click text=Add`
- `click text=Add`

Pick avoid genres:
- `click text=Horror`
- `click text=War`

Preferred languages:
- `click text=English`
- `click text=Japanese`

Submit:
- `click text=Continue to recommendations`
- `wait 2500`

## Notes
- If multiple identical text nodes exist (e.g. Horror appears twice), use `lastHtmlPath` and pick a more specific selector.
