import { useEffect } from 'react'
import type { RefObject } from 'react'

/** Keeps keyboard focus inside an admin popup and restores page scrolling on close. */
export function useAdminModal(
  dialogRef: RefObject<HTMLElement | null>,
  onClose: () => void,
  isBusy: boolean,
) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.setProperty('overflow', 'hidden')
    dialogRef.current?.focus()
    return () => {
      if (previousOverflow) document.body.style.setProperty('overflow', previousOverflow)
      else document.body.style.removeProperty('overflow')
    }
  }, [dialogRef])

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      const dialog = dialogRef.current
      if (!dialog) return
      if (event.key === 'Escape') {
        event.preventDefault()
        if (!isBusy) onClose()
        return
      }
      if (event.key !== 'Tab') return

      const controls = Array.from(
        dialog.querySelectorAll<HTMLElement>(
          'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled])',
        ),
      )
      const first = controls[0]
      const last = controls.at(-1)
      if (!first || !last) {
        event.preventDefault()
        dialog.focus()
      } else if (
        event.shiftKey &&
        (document.activeElement === first ||
          document.activeElement === dialog ||
          !dialog.contains(document.activeElement))
      ) {
        event.preventDefault()
        last.focus()
      } else if (
        !event.shiftKey &&
        (document.activeElement === last ||
          document.activeElement === dialog ||
          !dialog.contains(document.activeElement))
      ) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [dialogRef, isBusy, onClose])
}
