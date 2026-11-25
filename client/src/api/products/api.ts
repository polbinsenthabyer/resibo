import { endpoints } from '@/api/endpoints'
import { http } from '@/api/http'
import type { PageDto } from '@/api/types'

import type { Product, ProductListRequest, ProductUpsertRequest } from './types'

/**
 * Products, administrator side. Every call needs an ADMIN token — without one
 * these throw an ApiError with `isUnauthenticated` or `isForbidden` set, and
 * never reach the controller.
 */
export const productApi = {
  /** Creates when `request.id` is absent, updates when it is present. */
  save(request: ProductUpsertRequest): Promise<Product> {
    return http.post<Product>(endpoints.adminProducts.set, request)
  },

  /** `{}` is a valid request and returns the first page unfiltered. */
  list(request: ProductListRequest = {}, signal?: AbortSignal): Promise<PageDto<Product>> {
    return http.post<PageDto<Product>>(endpoints.adminProducts.list, request, {
      feedback: 'errors',
      ...(signal ? { signal } : {}),
    })
  },

  getById(productId: number, signal?: AbortSignal): Promise<Product> {
    return http.get<Product>(endpoints.adminProducts.get(productId), signal ? { signal } : {})
  },

  /** Resolves on success; the API returns no body for a delete. */
  async remove(productId: number): Promise<void> {
    await http.delete<null>(endpoints.adminProducts.delete(productId))
  },
}
