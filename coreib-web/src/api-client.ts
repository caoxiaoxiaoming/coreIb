import { coreIbFetch } from './auth-client'
import type { ColumnAccess, PermissionAction, RowScope } from './coreib-permissions'

export interface SystemInfo {
  name: string
  version: string
  javaVersion: string
  supportedDatabases: string[]
  activeDatabase: string
}

async function getJson<T>(url: string, headers: Record<string, string> = {}): Promise<T> {
  const response = await coreIbFetch(url, {
    headers: { Accept: 'application/json', ...headers },
  })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  return response.json() as Promise<T>
}

async function sendJson<T>(url: string, method: string, body: unknown, headers: Record<string, string> = {}): Promise<T> {
  const response = await coreIbFetch(url, {
    method,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}

async function deleteJson(url: string, headers: Record<string, string> = {}): Promise<void> {
  const response = await coreIbFetch(url, { method: 'DELETE', headers: { Accept: 'application/json', ...headers } })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
}

export type AuthenticationMode = 'DISABLED' | 'JWT' | 'OIDC'

export interface AuthenticationSession {
  mode: AuthenticationMode
  identityAuthenticated: boolean
  platformSubjectResolved: boolean
  identityName: string
  subjectId: string
  loginUrl: string | null
  logoutUrl: string | null
  postLogoutRedirectUrl: string | null
  csrfCookieName: string | null
  csrfHeaderName: string | null
}

export function fetchAuthenticationSession(): Promise<AuthenticationSession> {
  return getJson<AuthenticationSession>('/api/v1/auth/session')
}

export async function closeAuthenticationSession(logoutUrl: string): Promise<void> {
  const response = await coreIbFetch(logoutUrl, { method: 'POST' })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
}

export function fetchSystemInfo(): Promise<SystemInfo> {
  return getJson<SystemInfo>('/api/v1/system/info')
}

export function fetchHealth(): Promise<{ status: string }> {
  return getJson<{ status: string }>('/actuator/health')
}

export interface PlatformOrganization {
  id: string
  parentId: string | null
  code: string
  displayName: string
  type: string
  enabled: boolean
}

export interface PlatformUser {
  id: string
  loginName: string
  displayName: string
  organizationId: string | null
  organizationName: string | null
  enabled: boolean
  roleCodes: string[]
}

export interface PlatformRole {
  code: string
  displayName: string
  description: string | null
  enabled: boolean
}

export async function fetchPlatformDirectory(subject?: string) {
  const headers: Record<string, string> = {}
  if (subject) headers['X-CoreIb-Demo-Subject'] = subject
  const base = '/api/v1/platform/directory'
  const [organizations, users, roles] = await Promise.all([
    getJson<PlatformOrganization[]>(`${base}/organizations`, headers),
    getJson<PlatformUser[]>(`${base}/users`, headers),
    getJson<PlatformRole[]>(`${base}/roles`, headers),
  ])
  return { organizations, users, roles }
}

export function createPlatformOrganization(subject: string | undefined, draft: unknown): Promise<PlatformOrganization> {
  const headers: Record<string, string> = subject ? { 'X-CoreIb-Demo-Subject': subject } : {}
  return sendJson<PlatformOrganization>('/api/v1/platform/directory/organizations', 'POST', draft, headers)
}

export function createPlatformUser(subject: string | undefined, draft: unknown): Promise<PlatformUser> {
  const headers: Record<string, string> = subject ? { 'X-CoreIb-Demo-Subject': subject } : {}
  return sendJson<PlatformUser>('/api/v1/platform/directory/users', 'POST', draft, headers)
}

export function createPlatformRole(subject: string | undefined, draft: unknown): Promise<PlatformRole> {
  const headers: Record<string, string> = subject ? { 'X-CoreIb-Demo-Subject': subject } : {}
  return sendJson<PlatformRole>('/api/v1/platform/directory/roles', 'POST', draft, headers)
}

export function setPlatformEnabled(subject: string | undefined, entity: 'organization' | 'user' | 'role', id: string, enabled: boolean): Promise<void> {
  const headers: Record<string, string> = subject ? { 'X-CoreIb-Demo-Subject': subject } : {}
  return sendJson<void>(`/api/v1/platform/directory/${entity}/${encodeURIComponent(id)}/enabled`, 'PATCH', { enabled }, headers)
}

export function updatePlatformOrganization(subject: string | undefined, id: string, draft: unknown): Promise<PlatformOrganization> {
  return sendJson(`/api/v1/platform/directory/organizations/${encodeURIComponent(id)}`, 'PUT', draft, subjectHeaders(subject))
}

export function updatePlatformUser(subject: string | undefined, id: string, draft: unknown): Promise<PlatformUser> {
  return sendJson(`/api/v1/platform/directory/users/${encodeURIComponent(id)}`, 'PUT', draft, subjectHeaders(subject))
}

export function updatePlatformRole(subject: string | undefined, id: string, draft: unknown): Promise<PlatformRole> {
  return sendJson(`/api/v1/platform/directory/roles/${encodeURIComponent(id)}`, 'PUT', draft, subjectHeaders(subject))
}

export function deletePlatformDirectoryEntity(subject: string | undefined, entity: 'organization' | 'user' | 'role', id: string): Promise<void> {
  return deleteJson(`/api/v1/platform/directory/${entity}/${encodeURIComponent(id)}`, subjectHeaders(subject))
}

export interface ManagedMenu {
  id: string
  parentId: string | null
  name: string
  path: string
  component: string
  icon: string
  permission: string
  menuType: 'DIRECTORY' | 'MENU' | 'BUTTON'
  sortOrder: number
  visible: boolean
  enabled: boolean
}

export interface ManagedPost { id: string; code: string; name: string; sortOrder: number; enabled: boolean }
export interface ManagedDictionaryType { id: string; code: string; name: string; remark: string | null; enabled: boolean }
export interface ManagedDictionaryData { id: string; typeCode: string; label: string; value: string; sortOrder: number; enabled: boolean }
export interface ManagedConfig { id: string; configKey: string; name: string; value: string; publicVisible: boolean; remark: string | null }
export interface ManagedNotice { id: string; title: string; noticeType: string; status: string; content: string; publishedAt: string | null }
export interface AuditEvent { eventId: string; actorId: string; action: string; resource: string; targetId: string; outcome: string; details: string | null; occurredAt: string }
export interface LoginEvent { id: string; userId: string; identityName: string; sourceIp: string | null; outcome: string; message: string | null; occurredAt: string }

export type PlatformManagementKind = 'menus' | 'posts' | 'dictionary-types' | 'dictionary-data' | 'configs' | 'notices'
export type PlatformManagementRecord = ManagedMenu | ManagedPost | ManagedDictionaryType | ManagedDictionaryData | ManagedConfig | ManagedNotice

const managementBase = '/api/v1/platform/management'
export function fetchManagedMenus(subject?: string): Promise<ManagedMenu[]> { return getJson(`${managementBase}/menus`, subjectHeaders(subject)) }
export function fetchManagedPosts(subject?: string): Promise<ManagedPost[]> { return getJson(`${managementBase}/posts`, subjectHeaders(subject)) }
export function fetchDictionaryTypes(subject?: string): Promise<ManagedDictionaryType[]> { return getJson(`${managementBase}/dictionary-types`, subjectHeaders(subject)) }
export function fetchDictionaryData(subject?: string, typeCode?: string): Promise<ManagedDictionaryData[]> { return getJson(`${managementBase}/dictionary-data${typeCode ? `?typeCode=${encodeURIComponent(typeCode)}` : ''}`, subjectHeaders(subject)) }
export function fetchManagedConfigs(subject?: string): Promise<ManagedConfig[]> { return getJson(`${managementBase}/configs`, subjectHeaders(subject)) }
export function fetchManagedNotices(subject?: string): Promise<ManagedNotice[]> { return getJson(`${managementBase}/notices`, subjectHeaders(subject)) }
export function fetchAuditEvents(subject?: string): Promise<AuditEvent[]> { return getJson(`${managementBase}/audit-events`, subjectHeaders(subject)) }
export function fetchLoginEvents(subject?: string): Promise<LoginEvent[]> { return getJson(`${managementBase}/login-events`, subjectHeaders(subject)) }
export function saveManagementRecord<T extends PlatformManagementRecord>(subject: string | undefined, kind: PlatformManagementKind, record: T): Promise<T> {
  return sendJson(`${managementBase}/${kind}/${encodeURIComponent(record.id)}`, 'PUT', record, subjectHeaders(subject))
}
export function deleteManagementRecord(subject: string | undefined, entity: 'MENU' | 'POST' | 'DICTIONARY_TYPE' | 'DICTIONARY_DATA' | 'CONFIG' | 'NOTICE', id: string): Promise<void> {
  return deleteJson(`${managementBase}/${entity}/${encodeURIComponent(id)}`, subjectHeaders(subject))
}

export interface StoredFile { id: string; originalName: string; contentType: string | null; sizeBytes: number; uploaderId: string; createdAt: string }
export function fetchFiles(subject?: string): Promise<StoredFile[]> { return getJson('/api/v1/files', subjectHeaders(subject)) }
export async function uploadFile(subject: string | undefined, file: File): Promise<StoredFile> {
  const body = new FormData(); body.append('file', file)
  const response = await coreIbFetch('/api/v1/files', { method: 'POST', headers: subjectHeaders(subject), body })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  return response.json()
}
export function deleteFile(subject: string | undefined, id: string): Promise<void> { return deleteJson(`/api/v1/files/${encodeURIComponent(id)}`, subjectHeaders(subject)) }
export async function downloadFile(subject: string | undefined, file: StoredFile): Promise<void> {
  const response = await coreIbFetch(`/api/v1/files/${encodeURIComponent(file.id)}/content`, { headers: subjectHeaders(subject) })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  downloadBlob(await response.blob(), file.originalName)
}

export interface CodegenTable { schema: string | null; tableName: string; remarks: string | null; columnCount: number }
export interface CodegenColumn { name: string; jdbcType: string; javaType: string; nullable: boolean; primaryKey: boolean }
export interface GeneratedFile { path: string; language: string; content: string }
export interface CodePreview { tableName: string; moduleName: string; columns: CodegenColumn[]; files: GeneratedFile[] }
export function fetchCodegenTables(subject?: string): Promise<CodegenTable[]> { return getJson('/api/v1/code-generation/tables', subjectHeaders(subject)) }
export function fetchCodePreview(subject: string | undefined, tableName: string, moduleName: string): Promise<CodePreview> { return getJson(`/api/v1/code-generation/preview?tableName=${encodeURIComponent(tableName)}&moduleName=${encodeURIComponent(moduleName)}`, subjectHeaders(subject)) }
export async function downloadGeneratedCode(subject: string | undefined, tableName: string, moduleName: string): Promise<void> {
  const response = await coreIbFetch(`/api/v1/code-generation/download?tableName=${encodeURIComponent(tableName)}&moduleName=${encodeURIComponent(moduleName)}`, { headers: subjectHeaders(subject) })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  downloadBlob(await response.blob(), `${moduleName || tableName}.zip`)
}

function downloadBlob(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob); const link = document.createElement('a')
  link.href = url; link.download = name; link.click(); URL.revokeObjectURL(url)
}

export interface RolePermissionGrant {
  id: string
  roleCode: string
  resource: string
  action: PermissionAction
  rowScope: RowScope
  policyKey: string | null
  enabled: boolean
}

export interface FieldPermissionGrant {
  id: string
  roleCode: string
  resource: string
  fieldName: string
  accessMode: ColumnAccess
}

export interface UserRoleAssignment {
  userId: string
  roleCode: string
}

export interface SupervisorRelationship {
  id: string
  userId: string
  supervisorId: string
  relationType: string
  active: boolean
  effectiveFrom: string | null
  effectiveTo: string | null
}

export interface RecordAssignment {
  id: string
  resource: string
  recordId: string
  userId: string
  assignmentType: string
  active: boolean
  effectiveFrom: string | null
  effectiveTo: string | null
}

export interface SecurityConfiguration {
  permissions: RolePermissionGrant[]
  fields: FieldPermissionGrant[]
  userRoles: UserRoleAssignment[]
  supervisorRelationships: SupervisorRelationship[]
  recordAssignments: RecordAssignment[]
}

export interface PermissionGrantDraft {
  resource: string
  action: PermissionAction
  rowScope: RowScope
  policyKey: string | null
  enabled: boolean
}

export interface FieldGrantDraft {
  resource: string
  fieldName: string
  accessMode: ColumnAccess
}

function subjectHeaders(subject?: string): Record<string, string> {
  return subject ? { 'X-CoreIb-Demo-Subject': subject } : {}
}

export function fetchSecurityConfiguration(subject?: string): Promise<SecurityConfiguration> {
  return getJson<SecurityConfiguration>('/api/v1/platform/security', subjectHeaders(subject))
}

export function replaceRolePolicy(
  subject: string | undefined,
  roleCode: string,
  permissions: PermissionGrantDraft[],
  fields: FieldGrantDraft[],
): Promise<void> {
  return sendJson<void>(
    `/api/v1/platform/security/roles/${encodeURIComponent(roleCode)}/policy`,
    'PUT',
    { permissions, fields },
    subjectHeaders(subject),
  )
}

export function replaceUserRoles(subject: string | undefined, userId: string, roleCodes: string[]): Promise<void> {
  return sendJson<void>(
    `/api/v1/platform/security/users/${encodeURIComponent(userId)}/roles`,
    'PUT',
    { roleCodes },
    subjectHeaders(subject),
  )
}

export function saveSupervisorRelationship(subject: string | undefined, draft: unknown): Promise<SupervisorRelationship> {
  return sendJson<SupervisorRelationship>(
    '/api/v1/platform/security/supervisor-relationships', 'POST', draft, subjectHeaders(subject),
  )
}

export function setSupervisorRelationshipActive(subject: string | undefined, id: string, active: boolean): Promise<void> {
  return sendJson<void>(
    `/api/v1/platform/security/supervisor-relationships/${encodeURIComponent(id)}/active`,
    'PATCH', { active }, subjectHeaders(subject),
  )
}

export function saveRecordAssignment(subject: string | undefined, draft: unknown): Promise<RecordAssignment> {
  return sendJson<RecordAssignment>(
    '/api/v1/platform/security/record-assignments', 'POST', draft, subjectHeaders(subject),
  )
}

export function setRecordAssignmentActive(subject: string | undefined, id: string, active: boolean): Promise<void> {
  return sendJson<void>(
    `/api/v1/platform/security/record-assignments/${encodeURIComponent(id)}/active`,
    'PATCH', { active }, subjectHeaders(subject),
  )
}
