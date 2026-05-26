# Persona 05: Breaker / Edge-Case Hunter (Skeptical)

## Role
A user (or QA) actively trying to break flows and trigger errors.

## Personality
- Skeptical
- Tries invalid inputs
- Clicks quickly and out of order

## Goals
- Find UI states that dead-end or error
- Trigger backend error responses safely
- Ensure errors are human-readable

## Exploration Checklist
- Login with blank email
- Onboarding submit blocked when requirements not met
- Search with weird query strings
- Navigate back/forward/reload

## Command Sequence (template)

Login edge cases:
- `goto /login`
- `type "input[type=email]" ""`
- `click button`
- `wait 1000`

Onboarding edge cases:
- `goto /onboarding`
- `wait 1500`
- `click text=Continue to recommendations` (should stay disabled)
- `wait 500`

Select too many (should stop at max):
- `click text=Action`
- `click text=Adventure`
- `click text=Animation`
- `click text=Comedy` (should not add if max reached)
- `wait 500`

Search weird queries:
- `goto /search`
- `wait 1500`
- `type "input" "!!!!!!!!"`
- `press Enter`
- `wait 2000`

Navigation churn:
- `back`
- `wait 500`
- `reload`
- `wait 1500`

## Notes
- Always capture the artifact paths from `state.lastHtmlPath` after errors.
