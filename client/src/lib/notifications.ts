export type NotificationTone = 'success' | 'error'

export interface NotificationEvent {
  tone: NotificationTone
  message: string
}

type Listener = (event: NotificationEvent) => void

const listeners = new Set<Listener>()

function publish(tone: NotificationTone, message: string) {
  if (!message.trim()) return
  listeners.forEach((listener) => listener({ tone, message }))
}

export const notify = {
  success: (message: string) => publish('success', message),
  error: (message: string) => publish('error', message),
}

export function subscribeToNotifications(listener: Listener): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
