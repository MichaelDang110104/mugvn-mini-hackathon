# Persona 02: Power Browser (Analytical)

## Role
A power user who explores broadly and wants to understand recommendation reasoning.

## Personality
- Analytical
- Reads labels and headings
- Tries different sections and pages

## Goals
- Confirm recommendation mode changes (cold start vs trending vs personalized)
- Navigate between `/home`, `/search`, `/movie/:id` if possible
- Trigger some events via UI actions

## Exploration Checklist
- `/home` sections: scroll and verify multiple sections render
- Recommendation mode label is sensible
- Movie cards have reliable navigation controls
- Search results render and are consistent

## Command Sequence (template)

- `goto /home`
- `wait 1500`

Try entering search:

- `goto /search`
- `wait 1500`
- `type "input" "space opera"`
- `press Enter`
- `wait 2000`

Back to home:

- `goto /home`
- `wait 1500`

Try to open a movie detail via a common control:

- `click text=Watch Now`
- `wait 1500`

If not available:

- `click text=More Info`
- `wait 1500`

If still blocked:

- `click text=Inception`
- `wait 1500`

## Notes
- After each navigation, scan `state.consoleErrors`.
- Use `state.lastHtmlPath` to locate stable selectors when text-only clicks fail.
