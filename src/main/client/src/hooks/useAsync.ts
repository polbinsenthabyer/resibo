import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Runs an async function and tracks its state as a discriminated union, so a
 * component cannot read `data` while still loading or forget the error branch
 * — the type makes both impossible rather than merely discouraged.
 *
 * Deliberately small. This is not a data-fetching library: there is no cache,
 * no deduplication and no revalidation. When the app needs those, the answer
 * is TanStack Query (proposed in resibo-plan.md §5, not yet adopted), not a
 * larger version of this hook.
 */

export type AsyncState<TData> =
  | { status: 'idle'; data: null; error: null }
  | { status: 'loading'; data: null; error: null }
  | { status: 'success'; data: TData; error: null }
  | { status: 'error'; data: null; error: Error }

interface UseAsyncOptions {
  /** Run as soon as the component mounts. Default false. */
  immediate?: boolean
}

export function useAsync<TData>(
  task: (signal: AbortSignal) => Promise<TData>,
  options: UseAsyncOptions = {},
) {
  const { immediate = false } = options

  // When the task runs on mount, the first render is already a loading render.
  // Starting at 'loading' means the mount effect never has to setState
  // synchronously just to announce something the caller already asked for.
  const [state, setState] = useState<AsyncState<TData>>(
    immediate
      ? { status: 'loading', data: null, error: null }
      : { status: 'idle', data: null, error: null },
  )

  // The task is usually an inline arrow, so its identity changes every render.
  // Holding it in a ref keeps `run` stable — otherwise any effect depending on
  // `run` would re-fire on every render. The ref is synced in an effect rather
  // than during render, because a render must not have side effects.
  const taskRef = useRef(task)
  useEffect(() => {
    taskRef.current = task
  }, [task])

  const abortRef = useRef<AbortController | null>(null)

  /** Performs the work and records the outcome. Does not touch loading state. */
  const perform = useCallback(async (controller: AbortController) => {
    try {
      const data = await taskRef.current(controller.signal)
      if (!controller.signal.aborted) {
        setState({ status: 'success', data, error: null })
      }
    } catch (cause) {
      if (controller.signal.aborted) {
        // Superseded by a newer call, or the component unmounted. Not a failure.
        return
      }
      setState({
        status: 'error',
        data: null,
        error: cause instanceof Error ? cause : new Error(String(cause)),
      })
    }
  }, [])

  /** Starts the task, cancelling one already in flight. Safe to call from an event. */
  const run = useCallback(() => {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller
    setState({ status: 'loading', data: null, error: null })
    return perform(controller)
  }, [perform])

  useEffect(() => {
    if (!immediate) {
      return
    }
    const controller = new AbortController()
    abortRef.current = controller
    void perform(controller)
    return () => controller.abort()
  }, [immediate, perform])

  // Anything still in flight when the component goes away is abandoned, so a
  // resolved promise cannot call setState on an unmounted component.
  useEffect(() => () => abortRef.current?.abort(), [])

  return { ...state, run }
}
