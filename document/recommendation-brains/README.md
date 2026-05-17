# Recommendation Brains

This directory contains simple, detailed specifications for the three parts of the recommendation engine.

The recommendation engine is split into three "brains":

1. `brain-1-movie-meaning.md`
2. `brain-2-user-taste.md`
3. `brain-3-crowd-behavior.md`
4. `how-the-3-brains-fit-together.md`

## Why split it this way

This makes the system easier to understand.

- Brain 1 explains movie meaning.
- Brain 2 explains what the current user seems to like.
- Brain 3 explains what many users tend to like together.

## MVP priority

Phase 1 should rely mainly on:

- Brain 1
- Brain 2
- trending fallback

Brain 3 is useful, but it should be treated as phase 2 unless the simpler system is already stable.

## Reading order

If you are new to the system, read in this order:

1. `brain-1-movie-meaning.md`
2. `brain-2-user-taste.md`
3. `brain-3-crowd-behavior.md`
4. `how-the-3-brains-fit-together.md`

That order matters because:

- first understand the movies
- then understand the user
- then understand the crowd
- then understand how the backend combines them
