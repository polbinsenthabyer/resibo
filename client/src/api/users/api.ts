import { endpoints } from '@/api/endpoints'
import { http } from '@/api/http'
import type { PageDto } from '@/api/types'

import type { ManagedUser, UserListRequest, UserUpsertRequest } from './types'

/** ADMIN-only account management. Activation is separate from profile edits. */
export const userApi = {
  save(request: UserUpsertRequest): Promise<ManagedUser> {
    return http.post<ManagedUser>(endpoints.adminUsers.set, request)
  },

  list(request: UserListRequest = {}, signal?: AbortSignal): Promise<PageDto<ManagedUser>> {
    return http.post<PageDto<ManagedUser>>(endpoints.adminUsers.list, request, {
      feedback: 'errors',
      ...(signal ? { signal } : {}),
    })
  },

  getById(userId: number, signal?: AbortSignal): Promise<ManagedUser> {
    return http.get<ManagedUser>(endpoints.adminUsers.get(userId), signal ? { signal } : {})
  },

  async activate(userId: number): Promise<void> {
    await http.post<null>(endpoints.adminUsers.activate(userId))
  },

  async deactivate(userId: number): Promise<void> {
    await http.post<null>(endpoints.adminUsers.deactivate(userId))
  },
}
