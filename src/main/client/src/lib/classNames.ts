/**
 * Joins class names, dropping anything falsy.
 *
 * `cx(styles.button, isPrimary && styles.primary)` — avoids both the template
 * literal that leaves a stray space and a dependency for twelve lines of code.
 */
export function cx(...values: Array<string | false | null | undefined>): string {
  return values.filter(Boolean).join(' ')
}
