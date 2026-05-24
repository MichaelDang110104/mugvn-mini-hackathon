# Backend API Controllers Documentation

This document provides a comprehensive overview of all controllers and endpoints found in the `backend/src/main/java/com/hackathon/backend/controllers` folder. It outlines the purpose of each controller and details the available REST endpoints.

## 1. CommentController (`/api/comments`)
**Purpose:** Manages movie comments. Allows querying comments by movie ID, email, username, or retrieving a paginated list of all comments.
*   `GET /api/comments` - Retrieve all comments (paginated, sorted by date descending).
*   `GET /api/comments/{id}` - Retrieve a specific comment by its ObjectId.
*   `GET /api/comments/movie/{movieId}` - Retrieve all comments associated with a specific movie.
*   `GET /api/comments/email/{email}` - Retrieve comments posted by a specific email address.
*   `GET /api/comments/user/{name}` - Retrieve comments posted by a specific username.

## 2. ContractMovieController (`/api/movies`)
**Purpose:** Public, contract-aligned movie endpoints designed specifically for frontend integration. It handles semantic searches with fallbacks and provides detailed movie views alongside recommendations.
*   `GET /api/movies/search` - Search movies using semantic vector search with deterministic fallback behavior (handles session tracking).
*   `GET /api/movies/{movieId}` - Retrieve detailed information for a single movie along with a list of "similar movie" recommendations.

## 3. EmbeddedMovieController (`/api/embedded-movies`)
**Purpose:** Dedicated controller for querying `EmbeddedMovie` documents (movies containing vector embeddings for AI similarity search).
*   `GET /api/embedded-movies` - Retrieve all embedded movies (paginated).
*   `GET /api/embedded-movies/{id}` - Retrieve a specific embedded movie by ObjectId.
*   `GET /api/embedded-movies/search` - Search embedded movies by title (contains, case-insensitive).
*   `GET /api/embedded-movies/genre/{genre}` - Retrieve embedded movies containing a specific genre.
*   `GET /api/embedded-movies/year/{year}` - Retrieve embedded movies released in a specific year.
*   `GET /api/embedded-movies/cast/{actor}` - Retrieve embedded movies featuring a specific actor.
*   `GET /api/embedded-movies/director/{director}` - Retrieve embedded movies directed by a specific person.

## 4. EmbeddingController (`/api/admin/embeddings`)
**Purpose:** Internal/Admin endpoints used strictly for generating and persisting vector embeddings into the database. Intended for development and setup purposes.
*   `POST /api/admin/embeddings/movies/{movieId}` - Generate and save an embedding for a specific movie.
*   `POST /api/admin/embeddings/movies/batch` - Batch-embed all movies that currently lack embeddings.

## 5. EventController (`/api/events`)
**Purpose:** Telemetry and analytics controller. Receives user events (e.g., clicks, views, searches) from the frontend for behavioral tracking.
*   `POST /api/events` - Submit a user event payload. Emits an internal `UserEventReceivedEvent` for asynchronous backend processing.

## 6. MovieController (`/api/internal/movies`)
**Purpose:** Internal controller for querying the raw `movies` collection. Offers granular search filters across various metadata fields.
*   `GET /api/internal/movies` - Retrieve all movies (paginated and sortable).
*   `GET /api/internal/movies/{id}` - Retrieve a single movie by ObjectId.
*   `GET /api/internal/movies/search` - Search movies by title.
*   `GET /api/internal/movies/genre/{genre}` - Retrieve movies containing a specific genre.
*   `GET /api/internal/movies/year` - Retrieve movies released within a specific year range (`from`, `to`).
*   `GET /api/internal/movies/cast/{actor}` - Retrieve movies featuring a specific actor.
*   `GET /api/internal/movies/director/{director}` - Retrieve movies directed by a specific person.
*   `GET /api/internal/movies/country/{country}` - Retrieve movies from a specific country.
*   `GET /api/internal/movies/language/{language}` - Retrieve movies available in a specific language.
*   `GET /api/internal/movies/rated/{rated}` - Retrieve movies by content rating (e.g., PG-13, R).
*   `GET /api/internal/movies/top-rated` - Retrieve top-rated movies based on a minimum IMDB rating threshold.

## 7. RecommendationController (`/api/recommendations`)
**Purpose:** Contract-aligned endpoint for retrieving context-aware or personalized movie recommendations based on user sessions.
*   `GET /api/recommendations` - Retrieve a list of recommended movies for the current session/context.

## 8. SearchController (`/api/search`)
**Purpose:** Secondary or legacy search endpoint for basic movie retrieval.
*   `GET /api/search/movies` - Perform a movie search based on query (`q`) and limit parameters.

## 9. SessionController (`/api/sessions`)
**Purpose:** Manages user sessions, primarily used for tracking user states, recommendations context, and telemetry.
*   `GET /api/sessions` - Retrieve all sessions (paginated).
*   `GET /api/sessions/{id}` - Retrieve a session by its ObjectId.
*   `GET /api/sessions/user/{userId}` - Retrieve the session associated with a specific user ID.

## 10. UserController (`/api/users`)
**Purpose:** Manages MflixUser documents from the dataset. Provides basic identity lookups.
*   `GET /api/users` - Retrieve all users (paginated).
*   `GET /api/users/{id}` - Retrieve a user by their ObjectId.
*   `GET /api/users/email/{email}` - Find a user by their email address.
*   `GET /api/users/name/{name}` - Find a user by their exact name.
