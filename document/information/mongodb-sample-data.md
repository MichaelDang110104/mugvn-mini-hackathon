# MongoDB Sample Data

This document shows real example data for every table (collection) in the recommendation system, using popular IMDB movies so you can see exactly how data is stored and used.

---

## 1. `movies` — The Movie List

This is where all movie information is kept. Each movie is one document.

### What Each Field Means

| Field | What It Is | What It Does |
| --- | --- | --- |
| `_id` | A unique name for the movie | Other tables use this to point to a specific movie |
| `title` | The movie name | Shown on screen; also used when searching |
| `overview` | A short plot summary | Helps the system understand what the movie is about |
| `genres` | Broad categories like "Action" or "Drama" | Used to group movies and find similar ones |
| `tags` | Specific keywords like "mind bending" or "time travel" | Catches details that genres are too broad to cover |
| `cast` | Main actors | Helps find movies with the same actors you like |
| `directors` | Who directed the movie | Helps find movies by the same director |
| `releaseYear` | What year the movie came out | Can be used to filter by era |
| `language` | The main language of the movie | Used to filter (e.g., show only English movies) |
| `posterUrl` | Where the poster image is stored | The image shown on the movie card |
| `ratingAvg` | Average score out of 10 | Higher-rated movies get a small boost in recommendations |
| `popularityScore` | How popular the movie is (0 to 1) | Used for "trending" lists and as a tiebreaker |
| `availability` | Whether the movie can be shown in your region | Hides movies that are not available where you are |
| `embedding` | A long list of numbers that represent the movie's "meaning" | This is what makes semantic search work — movies with similar meanings have similar numbers |
| `embeddingVersion` | Which AI model created the embedding | Keeps track so we know if embeddings need to be regenerated |
| `embeddingUpdatedAt` | When the embedding was last updated | Helps spot stale data after model changes |

### What Is `embedding`?

Think of it as a "fingerprint" for what a movie is about. The AI reads the title, summary, genres, tags, cast, and directors, then turns all that text into a list of numbers. Movies about similar topics end up with similar number lists. When you search for "mind bending emotional sci-fi", the system converts your search into the same kind of number list and finds movies whose fingerprints are closest to yours.

The arrays below are shortened to 16 numbers for readability. In reality they would be 1,536 numbers long.

### Example: The Shawshank Redemption

```json
{
  "_id": "movie_shawshank",
  "title": "The Shawshank Redemption",
  "overview": "A banker convicted of uxoricide forms a transformative friendship with a fellow inmate as he navigates decades of life inside a state penitentiary, ultimately finding redemption through patience, hope, and quiet defiance.",
  "genres": ["Drama"],
  "tags": ["prison", "friendship", "hope", "wrongful conviction", "redemption", "escape"],
  "cast": ["Tim Robbins", "Morgan Freeman", "Bob Gunton", "William Sadler"],
  "directors": ["Frank Darabont"],
  "releaseYear": 1994,
  "language": "en",
  "posterUrl": "/posters/shawshank_redemption.jpg",
  "ratingAvg": 9.3,
  "popularityScore": 0.95,
  "availability": {
    "isAvailable": true,
    "region": "global"
  },
  "embedding": [0.012, -0.034, 0.089, 0.045, -0.067, 0.023, 0.101, -0.055, 0.078, 0.011, 0.042, -0.091, 0.063, 0.037, -0.028, 0.095],
  "embeddingVersion": "ada-002-v1",
  "embeddingUpdatedAt": "2026-05-16T10:00:00Z"
}
```

### Example: The Godfather

```json
{
  "_id": "movie_godfather",
  "title": "The Godfather",
  "overview": "The aging patriarch of an organized crime dynasty in postwar New York City transfers control of his clandestine empire to his reluctant youngest son, pulling him into a world of power, loyalty, and violence.",
  "genres": ["Crime", "Drama"],
  "tags": ["mafia", "family", "power", "organized crime", "loyalty", "succession", "italian-american"],
  "cast": ["Marlon Brando", "Al Pacino", "James Caan", "Robert Duvall"],
  "directors": ["Francis Ford Coppola"],
  "releaseYear": 1972,
  "language": "en",
  "posterUrl": "/posters/the_godfather.jpg",
  "ratingAvg": 9.2,
  "popularityScore": 0.92,
  "availability": {
    "isAvailable": true,
    "region": "global"
  },
  "embedding": [0.015, -0.041, 0.072, 0.038, -0.059, 0.031, 0.088, -0.047, 0.065, 0.019, 0.053, -0.082, 0.071, 0.029, -0.036, 0.094],
  "embeddingVersion": "ada-002-v1",
  "embeddingUpdatedAt": "2026-05-16T10:00:00Z"
}
```

### Example: The Dark Knight

```json
{
  "_id": "movie_dark_knight",
  "title": "The Dark Knight",
  "overview": "When a sadistic criminal mastermind known as the Joker wreaks havoc on Gotham City, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice and save the city from chaos.",
  "genres": ["Action", "Crime", "Drama"],
  "tags": ["superhero", "villain", "chaos", "moral dilemma", "gotham", "vigilante", "joker"],
  "cast": ["Christian Bale", "Heath Ledger", "Aaron Eckhart", "Maggie Gyllenhaal"],
  "directors": ["Christopher Nolan"],
  "releaseYear": 2008,
  "language": "en",
  "posterUrl": "/posters/the_dark_knight.jpg",
  "ratingAvg": 9.0,
  "popularityScore": 0.97,
  "availability": {
    "isAvailable": true,
    "region": "global"
  },
  "embedding": [0.028, -0.053, 0.091, 0.044, -0.072, 0.019, 0.105, -0.061, 0.083, 0.035, 0.047, -0.098, 0.056, 0.041, -0.033, 0.087],
  "embeddingVersion": "ada-002-v1",
  "embeddingUpdatedAt": "2026-05-16T10:00:00Z"
}
```

### Example: Inception

```json
{
  "_id": "movie_inception",
  "title": "Inception",
  "overview": "A skilled thief who steals secrets from within the subconscious during dream states is offered a chance to erase his criminal past by performing an impossible mission: planting an idea into someone's mind through layered dream infiltration.",
  "genres": ["Action", "Sci-Fi", "Thriller"],
  "tags": ["dreams", "heist", "subconscious", "mind bending", "layered reality", "time dilation", "extraction"],
  "cast": ["Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page", "Tom Hardy"],
  "directors": ["Christopher Nolan"],
  "releaseYear": 2010,
  "language": "en",
  "posterUrl": "/posters/inception.jpg",
  "ratingAvg": 8.8,
  "popularityScore": 0.93,
  "availability": {
    "isAvailable": true,
    "region": "global"
  },
  "embedding": [0.031, -0.047, 0.085, 0.052, -0.068, 0.027, 0.099, -0.058, 0.074, 0.043, 0.061, -0.089, 0.045, 0.036, -0.022, 0.092],
  "embeddingVersion": "ada-002-v1",
  "embeddingUpdatedAt": "2026-05-16T10:00:00Z"
}
```

### Example: The Matrix

```json
{
  "_id": "movie_matrix",
  "title": "The Matrix",
  "overview": "A computer programmer discovers that reality as he knows it is a simulated world created by machines, and joins a rebellion to overthrow them while grappling with his own identity as a prophesied liberator.",
  "genres": ["Action", "Sci-Fi"],
  "tags": ["simulation", "reality", "chosen one", "dystopia", "martial arts", "cyberpunk", "awakening"],
  "cast": ["Keanu Reeves", "Laurence Fishburne", "Carrie-Anne Moss", "Hugo Weaving"],
  "directors": ["Lana Wachowski", "Lilly Wachowski"],
  "releaseYear": 1999,
  "language": "en",
  "posterUrl": "/posters/the_matrix.jpg",
  "ratingAvg": 8.7,
  "popularityScore": 0.90,
  "availability": {
    "isAvailable": true,
    "region": "global"
  },
  "embedding": [0.025, -0.058, 0.079, 0.048, -0.063, 0.033, 0.094, -0.051, 0.071, 0.039, 0.055, -0.086, 0.062, 0.031, -0.027, 0.088],
  "embeddingVersion": "ada-002-v1",
  "embeddingUpdatedAt": "2026-05-16T10:00:00Z"
}
```

### Example: Parasite

```json
{
  "_id": "movie_parasite",
  "title": "Parasite",
  "overview": "A poor family schemes their way into the employ of a wealthy household by posing as unrelated, highly qualified individuals, but their comfortable arrangement unravels when an unexpected discovery exposes dark secrets from both homes.",
  "genres": ["Comedy", "Drama", "Thriller"],
  "tags": ["class divide", "dark comedy", "social commentary", "deception", "korean cinema", "wealth gap", "twist"],
  "cast": ["Song Kang-ho", "Lee Sun-kyun", "Cho Yeo-jeong", "Choi Woo-shik"],
  "directors": ["Bong Joon-ho"],
  "releaseYear": 2019,
  "language": "ko",
  "posterUrl": "/posters/parasite.jpg",
  "ratingAvg": 8.5,
  "popularityScore": 0.88,
  "availability": {
    "isAvailable": true,
    "region": "global"
  },
  "embedding": [0.019, -0.062, 0.076, 0.041, -0.055, 0.029, 0.091, -0.044, 0.068, 0.037, 0.058, -0.083, 0.053, 0.034, -0.025, 0.086],
  "embeddingVersion": "ada-002-v1",
  "embeddingUpdatedAt": "2026-05-16T10:00:00Z"
}
```

---

## 2. `users` — Who Is Visiting

This table tracks anonymous visitors. No login needed — the system creates a user record the first time someone visits.

### What Each Field Means

| Field | What It Is | What It Does |
| --- | --- | --- |
| `_id` | A unique name for this user | Other tables use this to know which user did what |
| `sessionId` | A token for the current visit | Sent to the browser so every request knows who it is from |
| `createdAt` | When this user first visited | Useful for knowing how long the user has been around |
| `lastSeenAt` | When the user last did anything | Helps detect if a session is no longer active |

### Example

```json
{
  "_id": "user_demo_001",
  "sessionId": "sess_demo_001",
  "createdAt": "2026-05-20T14:30:00Z",
  "lastSeenAt": "2026-05-20T14:45:00Z"
}
```

### How It Works

You open the website. The backend creates this record and sends `sessionId` back to your browser (as a cookie). Your browser attaches this session ID to every request from then on. No sign-up, no password.

---

## 3. `user_events` — What The User Did

Every click, view, like, search, and save is recorded here. Nothing is ever deleted — it is a permanent log of behavior.

### What Each Field Means

| Field | What It Is | What It Does |
| --- | --- | --- |
| `_id` | MongoDB's own ID | Internal use |
| `eventId` | A unique ID for this specific action | Prevents duplicates — if the same action is sent twice (e.g., slow internet), the system sees the same `eventId` and ignores the second one |
| `userId` | Which user did this | Points to `users._id` |
| `sessionId` | Which visit this happened in | Points to `users.sessionId` — lets the system know what you did *just now* |
| `eventType` | What kind of action | One of: `view`, `click`, `like`, `save`, `rate`, `search`. Likes and saves are strong signals. Views and clicks are weaker. Searches tell the system what you are looking for. |
| `movieId` | Which movie the action was about | Points to `movies._id`. Required for everything except `search`. |
| `queryText` | What the user typed in the search bar | Only filled in for `search` events |
| `eventValue` | A number, like a star rating | Only used for `rate` events (e.g., 5 for 5 stars). Empty for other types. |
| `metadata` | Extra details | Things like which page the action happened on, how long the user stayed, etc. |
| `timestamp` | When this happened | Recent actions matter more than old ones |

### Rules

- `search` events must have `queryText` and do NOT need `movieId`
- `view`, `click`, `like`, `save`, `rate` events must have `movieId`
- Every event must have an `eventId`

### Example: User Searches

```json
{
  "_id": ObjectId("..."),
  "eventId": "evt_001",
  "userId": "user_demo_001",
  "sessionId": "sess_demo_001",
  "eventType": "search",
  "movieId": null,
  "queryText": "mind bending emotional sci-fi",
  "eventValue": null,
  "metadata": { "page": "/search", "device": "desktop" },
  "timestamp": "2026-05-20T14:32:00Z"
}
```

### Example: User Views a Movie

```json
{
  "_id": ObjectId("..."),
  "eventId": "evt_002",
  "userId": "user_demo_001",
  "sessionId": "sess_demo_001",
  "eventType": "view",
  "movieId": "movie_inception",
  "queryText": null,
  "eventValue": null,
  "metadata": { "page": "/movies/movie_inception", "durationSeconds": 45 },
  "timestamp": "2026-05-20T14:33:00Z"
}
```

### Example: User Likes a Movie

```json
{
  "_id": ObjectId("..."),
  "eventId": "evt_003",
  "userId": "user_demo_001",
  "sessionId": "sess_demo_001",
  "eventType": "like",
  "movieId": "movie_inception",
  "queryText": null,
  "eventValue": null,
  "metadata": { "page": "/movies/movie_inception" },
  "timestamp": "2026-05-20T14:34:00Z"
}
```

### Example: User Likes Another Movie

```json
{
  "_id": ObjectId("..."),
  "eventId": "evt_004",
  "userId": "user_demo_001",
  "sessionId": "sess_demo_001",
  "eventType": "like",
  "movieId": "movie_matrix",
  "queryText": null,
  "eventValue": null,
  "metadata": { "page": "/movies/movie_matrix" },
  "timestamp": "2026-05-20T14:38:00Z"
}
```

### Example: User Clicks a Recommendation

```json
{
  "_id": ObjectId("..."),
  "eventId": "evt_005",
  "userId": "user_demo_001",
  "sessionId": "sess_demo_001",
  "eventType": "click",
  "movieId": "movie_dark_knight",
  "queryText": null,
  "eventValue": null,
  "metadata": { "page": "/recommendations", "position": 3 },
  "timestamp": "2026-05-20T14:42:00Z"
}
```

### What Happens After These Events

The system looks at all events for this session and sees: the user searched for "mind bending emotional sci-fi", then viewed and liked Inception, then liked The Matrix, then clicked The Dark Knight. From this it learns: the user likes sci-fi, mind-bending plots, and Christopher Nolan films. It then reshuffles the homepage to show more movies that match those interests.

---

### Why `sessionId` Matters (Not Just `timestamp`)

You might wonder: "We already have `userId` to know who the user is, and `timestamp` to know when they did it. Why do we also need `sessionId`?"

**Short answer: `timestamp` tells you *when*. `sessionId` tells you *what the user is trying to do right now*.**

Here is why this matters for the recommendation engine:

#### The Problem: A User Can Have Different Moods on Different Visits

Imagine this user's history:

- **Monday (Session 1)**: Searches "horror movies" → clicks The Conjuring → clicks Hereditary
- **Tuesday (Session 2)**: Searches "kids movies" → clicks Toy Story → clicks Finding Nemo

With `sessionId`: The system knows Monday was about horror and Tuesday was about kids movies. These are two separate moods.

With only `timestamp`: The system just sees a flat list of events: horror, horror, kids, kids. It cannot tell if the user switched topics or gradually changed their taste. It might recommend a horror-kids mix that nobody wants.

#### The "Lunch Break" Problem

A user searches for "action movies," clicks Mad Max, then **leaves for lunch (2 hours)**, comes back, and searches for "documentaries."

With `sessionId`: A new session starts when they come back. The system correctly treats the documentary search as a fresh mood.

With only `timestamp`: The system has to guess: "How long ago is too long ago?" 10 minutes? 30? 60? There is no right answer because **time does not equal mood**. The user could step away for 5 minutes or 5 hours — the clock alone does not tell the system what they want now.

#### How This Changes the Recommendation Engine

The engine uses two signals to pick movies:

1. **Long-term taste** (from `userId`): "This user generally likes sci-fi and mind-bending movies"
2. **Current mood** (from `sessionId`): "Right now, they are searching for romantic comedies"

The engine combines these: "Show romantic comedies first (current mood), but maybe include Inception too (long-term taste)."

Without `sessionId`, the engine cannot cleanly separate these two signals. It would have to guess: "events from the last 15 minutes = current mood." But what if the user was gone for 20 minutes? Their "current mood" events would be empty and the engine would fall back to long-term taste — showing sci-fi movies when the user actually wants romance.

#### Real-World Comparison

| Situation | With `sessionId` | With only `timestamp` |
| --- | --- | --- |
| User switches topics between visits | Knows each visit has its own focus | Sees a mixed list, gets confused |
| User steps away for a while | New session = new focus | Has to guess if old events are still relevant |
| Two browser tabs open at once | Each tab has its own focus | Events from both tabs mix together |
| "What does the user want right now?" | All events from this visit | Events from the last N minutes (arbitrary) |

**Bottom line:** `sessionId` lets the engine separate "who this user is" from "what this user wants right now." Without it, the engine has to guess where one ends and the other begins using an arbitrary time window.

---

## 4. `user_profiles` — What The System Knows About The User

This is a summary of the user's tastes, built from their events. The user does not write to this table directly — the system creates it by reading `user_events`.

### What Each Field Means

| Field | What It Is | What It Does |
| --- | --- | --- |
| `_id` | MongoDB's own ID | Internal use |
| `userId` | Which user this profile belongs to | Points to `users._id` |
| `topGenres` | The genres this user likes most | Found by counting genres from movies the user liked. Used to find similar movies. |
| `topThemes` | The themes this user likes most | Found by counting tags from movies the user liked. More specific than genres. |
| `likedMovieIds` | Movies the user has liked | Used to find other users with similar taste |
| `recentMovieIds` | Movies the user interacted with recently | Used to say "similar to what you just watched" |
| `profileEmbedding` | A number fingerprint of the user's taste | Made by averaging the embeddings of movies the user liked. Used to find movies that match their taste. |
| `lastComputedAt` | When this profile was last updated | Tells the system if the profile is out of date |
| `lastSignalsAppliedAt` | The timestamp of the last event used | Prevents the system from reprocessing the same events |

### Example

```json
{
  "_id": ObjectId("..."),
  "userId": "user_demo_001",
  "topGenres": ["Sci-Fi", "Action", "Thriller", "Drama"],
  "topThemes": ["mind bending", "layered reality", "simulation", "moral dilemma", "chosen one"],
  "likedMovieIds": ["movie_inception", "movie_matrix"],
  "recentMovieIds": ["movie_dark_knight", "movie_inception", "movie_matrix"],
  "profileEmbedding": [0.029, -0.051, 0.088, 0.047, -0.070, 0.025, 0.102, -0.059, 0.076, 0.041, 0.058, -0.093, 0.054, 0.038, -0.024, 0.090],
  "lastComputedAt": "2026-05-20T14:43:00Z",
  "lastSignalsAppliedAt": "2026-05-20T14:42:00Z"
}
```

### How This Is Built

After the 5 events above:

1. The system looks at the two `like` events (Inception and The Matrix)
2. It collects their genres: Inception has Action, Sci-Fi, Thriller. The Matrix has Action, Sci-Fi. So top genres are Sci-Fi and Action.
3. It collects their tags: Inception has "dreams", "mind bending", "layered reality". The Matrix has "simulation", "reality", "chosen one". So top themes include "mind bending", "layered reality", "simulation".
4. `profileEmbedding` is the average of Inception's and The Matrix's embeddings.
5. `lastSignalsAppliedAt` is set to the time of the most recent event (evt_005).

---

## 5. `recommendation_logs` — Why Each Movie Was Recommended

Every time the system gives recommendations, it writes down: which movies it considered, which ones it picked, the scores, and the reasons. This is for debugging and for showing explanation labels in the UI.

### What Each Field Means

| Field | What It Is | What It Does |
| --- | --- | --- |
| `_id` | MongoDB's own ID | Internal use |
| `userId` | Who got these recommendations | Points to `users._id` |
| `sessionId` | Which visit this was for | Points to `users.sessionId` |
| `mode` | How the recommendations were made | `personalized` (based on your taste), `cold_start` (new user), `semantic` (from search), or `fallback_text` (when AI search failed) |
| `context` | Where on the site the request came from | `homepage`, `movie_detail`, or `search_results` |
| `candidateMovieIds` | All movies that were considered | The full pool before ranking |
| `recommendedMovieIds` | The movies actually shown to the user | The top picks from the candidate pool |
| `scores` | The score for each recommended movie | Higher number = better match |
| `reasonCodes` | Why each movie was recommended | Shown as small labels under each movie card in the UI |
| `createdAt` | When the recommendation was made | For tracking and replay |

### Available Reason Codes

| Code | What It Means to the User | When It Is Used |
| --- | --- | --- |
| `liked_similar_theme` | "Similar to movies you liked" | The movie shares tags with movies the user liked |
| `semantic_match_to_search` | "Matches your search" | The movie is close to what the user searched for |
| `similar_users_liked` | "People like you liked this" | Other users with similar taste also liked this movie |
| `popular_in_preferred_genre` | "Popular in a genre you like" | The movie is popular and in a genre the user prefers |
| `similar_to_recently_viewed` | "Similar to what you just watched" | The movie is similar to ones the user recently clicked |
| `trending_now` | "Trending right now" | The movie is currently popular (used for new users) |
| `editorial_starter_pick` | "A great place to start" | A hand-picked movie for new users |
| `fallback_text_match` | "Matches your search words" | Found by text search when AI search failed |

### Example

```json
{
  "_id": ObjectId("..."),
  "userId": "user_demo_001",
  "sessionId": "sess_demo_001",
  "mode": "personalized",
  "context": "homepage",
  "candidateMovieIds": [
    "movie_shawshank",
    "movie_godfather",
    "movie_dark_knight",
    "movie_inception",
    "movie_matrix",
    "movie_parasite"
  ],
  "recommendedMovieIds": [
    "movie_dark_knight",
    "movie_parasite",
    "movie_shawshank"
  ],
  "scores": {
    "movie_dark_knight": 0.89,
    "movie_parasite": 0.76,
    "movie_shawshank": 0.71
  },
  "reasonCodes": {
    "movie_dark_knight": [
      "similar_to_recently_viewed",
      "popular_in_preferred_genre"
    ],
    "movie_parasite": [
      "liked_similar_theme",
      "similar_users_liked"
    ],
    "movie_shawshank": [
      "popular_in_preferred_genre"
    ]
  },
  "createdAt": "2026-05-20T14:45:00Z"
}
```

### Why The Dark Knight Got the Highest Score

- `similar_to_recently_viewed`: The user just clicked on The Dark Knight (evt_005), so it gets a boost
- `popular_in_preferred_genre`: The user's top genres include Action and Drama (from liking Inception and The Matrix), and The Dark Knight is Action/Crime/Drama with the highest popularity score (0.97)

### Why Parasite Was Recommended

- `liked_similar_theme`: Parasite has "twist" and "dark comedy" tags, which overlap with the thriller/mind-bending themes from the user's liked movies
- `similar_users_liked`: Other users who liked Inception and The Matrix also rated Parasite highly

---

## How It All Fits Together

```
User opens the website
  → A record is created in "users" with a session ID
  → The session ID is sent to the browser

User searches "mind bending emotional sci-fi"
  → A "search" event is saved in "user_events"
  → The system searches "movies" using the embedding and finds Inception, The Matrix

User clicks on Inception
  → A "view" event is saved in "user_events"

User likes Inception
  → A "like" event is saved in "user_events"
  → The "user_profiles" table is updated: Sci-Fi is now a top genre

User likes The Matrix
  → Another "like" event is saved in "user_events"
  → The "user_profiles" table is updated again with more themes and genres

User goes back to the homepage
  → The system reads "user_profiles" and recent events from "user_events"
  → It scores all candidate movies and picks the best ones
  → The decision is saved in "recommendation_logs"
  → The browser shows the movies with explanation labels like "Similar to what you just watched"
```
