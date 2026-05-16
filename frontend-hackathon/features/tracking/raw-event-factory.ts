/**
 * Raw Event Factory
 * Creates frontend event objects before normalization
 */

export interface RawEvent {
  timestamp: number
  action: 'impression' | 'click' | 'search' | 'watch_start' | 'like' | 'save' | 'rating'
  screen: string
  component: string
  itemType: 'movie_card' | 'hero_banner' | 'header' | 'search_bar' | string
  payload: {
    movieId?: string
    query?: string
    resultCount?: number
    position?: number
    rowTitle?: string
    rating?: number
    [key: string]: string | number | boolean | undefined
  }
}

export function createImpressionEvent(
  movieId: string,
  screen: string,
  component: string,
  options?: {
    position?: number
    rowTitle?: string
    metadata?: Record<string, string | number | boolean | undefined>
  }
): RawEvent {
  return {
    timestamp: Date.now(),
    action: 'impression',
    screen,
    component,
    itemType: 'movie_card',
    payload: {
      movieId,
      position: options?.position,
      rowTitle: options?.rowTitle,
      ...options?.metadata,
    },
  }
}

export function createClickEvent(
  movieId: string,
  screen: string,
  component: string,
  options?: {
    position?: number
    rowTitle?: string
    metadata?: Record<string, string | number | boolean | undefined>
  }
): RawEvent {
  return {
    timestamp: Date.now(),
    action: 'click',
    screen,
    component,
    itemType: 'movie_card',
    payload: {
      movieId,
      position: options?.position,
      rowTitle: options?.rowTitle,
      ...options?.metadata,
    },
  }
}

export function createSearchEvent(query: string, resultCount: number): RawEvent {
  return {
    timestamp: Date.now(),
    action: 'search',
    screen: 'search',
    component: 'search_bar',
    itemType: 'search_bar',
    payload: {
      query,
      resultCount,
    },
  }
}

export function createWatchStartEvent(movieId: string): RawEvent {
  return {
    timestamp: Date.now(),
    action: 'watch_start',
    screen: 'movie_detail',
    component: 'action_bar',
    itemType: 'movie_card',
    payload: {
      movieId,
    },
  }
}

export function createLikeEvent(movieId: string): RawEvent {
  return {
    timestamp: Date.now(),
    action: 'like',
    screen: 'movie_detail',
    component: 'action_bar',
    itemType: 'movie_card',
    payload: {
      movieId,
    },
  }
}

export function createSaveEvent(movieId: string): RawEvent {
  return {
    timestamp: Date.now(),
    action: 'save',
    screen: 'movie_detail',
    component: 'action_bar',
    itemType: 'movie_card',
    payload: {
      movieId,
    },
  }
}

export function createRatingEvent(movieId: string, rating: number): RawEvent {
  return {
    timestamp: Date.now(),
    action: 'rating',
    screen: 'movie_detail',
    component: 'action_bar',
    itemType: 'movie_card',
    payload: {
      movieId,
      rating,
    },
  }
}
