import { coreIbFetch } from './auth-client'

export type PermissionAction = 'READ' | 'CREATE' | 'UPDATE' | 'DELETE' | 'EXPORT'
export type RowScope =
  | 'ALL'
  | 'ORGANIZATION'
  | 'ORGANIZATION_TREE'
  | 'SELF'
  | 'MANAGED_USERS'
  | 'RELATED_RECORDS'
  | 'CUSTOM'
export type ColumnAccess = 'HIDDEN' | 'READ' | 'WRITE' | 'MASKED'

export interface ResourcePermission {
  actions: PermissionAction[]
  rowScope: RowScope
  fields: Record<string, ColumnAccess>
}

export interface PermissionSnapshot {
  authenticated: boolean
  subjectId: string
  roleCodes: string[]
  resources: Record<string, ResourcePermission>
}

export async function fetchEffectivePermissions(subject?: string): Promise<PermissionSnapshot> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (subject) headers['X-CoreIb-Demo-Subject'] = subject
  const response = await coreIbFetch('/api/v1/permissions/effective', { headers })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  return response.json() as Promise<PermissionSnapshot>
}

export function can(
  snapshot: PermissionSnapshot,
  resource: string,
  action: PermissionAction,
): boolean {
  return snapshot.resources[resource]?.actions.includes(action) ?? false
}

export function fieldAccess(
  snapshot: PermissionSnapshot,
  resource: string,
  field: string,
): ColumnAccess {
  return snapshot.resources[resource]?.fields[field] ?? 'HIDDEN'
}
