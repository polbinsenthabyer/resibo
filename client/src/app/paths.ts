/**
 * Route paths as constants, so a link and its route cannot drift apart and a
 * rename is a single edit.
 */
export const paths = {
  home: '/',
  status: '/status',
  login: '/login',
  signup: '/signup',
  admin: '/admin',
  adminProducts: '/admin/products',
  employee: '/employee',
} as const
