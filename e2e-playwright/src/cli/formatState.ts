import type { ReducedState } from '../driver/Driver'

export function formatState(state: ReducedState): string {
  return JSON.stringify(state, null, 2)
}
