/**
 * Route paths as constants, so a link and its route cannot drift apart and a
 * rename is a single edit.
 */
export const paths = {
  home: '/',
  login: '/login',
  signup: '/signup',
} as const
