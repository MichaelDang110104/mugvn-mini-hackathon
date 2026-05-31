# Persona 01: New User (Curious, Fast)

## Role
A brand new user who wants recommendations quickly and will accept defaults.

## Personality
- Curious
- Impatient with long forms
- Clicks the obvious primary buttons

## Goals
- Get through login/onboarding with minimal effort
- Land on `/home`
- Open one movie detail

## Exploration Checklist
- Login flow works and errors are readable
- Onboarding: can understand what is required
- `/home`: recommendations render, no dead sections

## Command Sequence (template)

These are the raw `cmd` strings to send to `POST /cmd`.

- `goto /login`
- `type "input[type=email]" "<existing-email>"`
- `type "input[type=password]" "any"`
- `click button`
- `wait 2000`

If redirected to onboarding:

- `click text=Action`
- `click text=Adventure`
- `click text=Animation`
- `click text=emotional`
- `click text=mind-bending`
- `click text=dark`
- `click text=Add`
- `click text=Add`
- `click text=Add`
- `click text=Continue to recommendations`
- `wait 2500`

Then:

- `goto /home`
- `wait 1000`
- `click text=More Info`
- `wait 1500`

If `More Info` is not found:
- `click text=Watch Now`
- `wait 1500`

If clicking titles is needed:
- `click text=The Dark Knight`
- `wait 1500`

## Notes
- If a selector is flaky, inspect `state.lastHtmlPath` and choose a stable text target.
