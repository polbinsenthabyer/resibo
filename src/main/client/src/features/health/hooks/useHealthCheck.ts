import { useAsync } from '@/hooks/useAsync'

import { healthApi } from '../api'

/** Calls the health endpoint on mount and exposes the result as async state. */
export function useHealthCheck() {
  return useAsync((signal) => healthApi.check(signal), { immediate: true })
}
