/**
 * Every API path in one place, relative to env.apiBaseUrl (which already
 * carries the /api prefix).
 *
 * Paths are here rather than inline at call sites so that a backend route
 * change is a one-file edit — products moved from /api/products to
 * /api/admin/products once already, when they went behind the ADMIN role.
 */
export const endpoints = {
  health: {
    check: '/health/check',
  },

  auth: {
    signup: '/auth/signup',
    login: '/auth/login',
  },

  /**
   * ADMIN role required — SecurityConfig restricts /api/admin/**. An EMPLOYEE
   * token gets 403 here, no token gets 401.
   */
  adminProducts: {
    /** Create when the payload has no id, update when it does. */
    set: '/admin/products/set',
    /** POST, despite being a read: the filters travel in the body. */
    list: '/admin/products/list',
    get: (productId: number) => `/admin/products/get/${productId}`,
    delete: (productId: number) => `/admin/products/delete/${productId}`,
  },
} as const
