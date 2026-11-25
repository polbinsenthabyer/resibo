export type ManagedUserRole = 'ADMIN' | 'EMPLOYEE'

/** Mirrors the current admin User entity response. Only the numeric role ID is returned. */
export interface ManagedUser {
  id: number
  username: string
  name: string | null
  userRoleId: number
  isActive: boolean
  lastLoginAt: string | null
  createdBy: string | null
  createdAt: string | null
  updatedBy: string | null
  updatedAt: string | null
}

export interface UserUpsertRequest {
  id?: number
  username: string
  /** Required on create; omit on update to leave unchanged. */
  password?: string
  name?: string | null
  /** Create defaults to EMPLOYEE; omit on update to leave unchanged. */
  role?: ManagedUserRole
}

export type UserSortBy = 'id' | 'username' | 'name' | 'createdAt' | 'updatedAt' | 'lastLoginAt'

export interface UserListRequest {
  username?: string
  isExactMatch?: boolean
  role?: ManagedUserRole
  isActive?: boolean
  sortBy?: UserSortBy
  sortDirection?: 'ASC' | 'DESC'
  page?: number
  size?: number
}
