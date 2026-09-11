import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { configureCsrfProtection } from './auth-client'
import {
  closeAuthenticationSession,
  fetchAuthenticationSession,
  type AuthenticationSession,
  fetchManagedMenus,
  type ManagedMenu,
} from './api-client'
import {
  can as canPerform,
  fetchEffectivePermissions,
  fieldAccess as resolveFieldAccess,
  type ColumnAccess,
  type PermissionAction,
  type PermissionSnapshot,
} from './coreib-permissions'

export const usePermissionStore = defineStore('permission', () => {
  const snapshot = ref<PermissionSnapshot | null>(null)
  const loading = ref(false)
  const error = ref('')
  const sessionError = ref('')
  const authSession = ref<AuthenticationSession | null>(null)
  const managedMenus = ref<ManagedMenu[]>([])
  const demoSubject = ref(import.meta.env.DEV ? readDemoSubject() : '')
  let pendingLoad: Promise<void> | null = null
  let pendingInitialization: Promise<void> | null = null

  const authenticated = computed(() => snapshot.value?.authenticated ?? false)
  const subjectId = computed(() => snapshot.value?.subjectId ?? 'anonymous')
  const roleCodes = computed(() => snapshot.value?.roleCodes ?? [])

  function initialize(): Promise<void> {
    if (pendingInitialization) return pendingInitialization
    pendingInitialization = (async () => {
      await loadSession()
      if (authSession.value?.mode !== 'OIDC' || authSession.value.identityAuthenticated) {
        await load()
      }
    })().finally(() => {
      pendingInitialization = null
    })
    return pendingInitialization
  }

  async function loadSession() {
    sessionError.value = ''
    try {
      authSession.value = await fetchAuthenticationSession()
      configureCsrfProtection(authSession.value.csrfCookieName, authSession.value.csrfHeaderName)
    } catch (reason) {
      sessionError.value = reason instanceof Error ? reason.message : '认证会话读取失败'
      throw reason
    }
  }

  function load(): Promise<void> {
    if (pendingLoad) return pendingLoad
    pendingLoad = (async () => {
      loading.value = true
      error.value = ''
      try {
        const subject = import.meta.env.DEV ? demoSubject.value || undefined : undefined
        snapshot.value = await fetchEffectivePermissions(subject)
        managedMenus.value = canPerform(snapshot.value, 'platform-management', 'READ')
          ? await fetchManagedMenus(subject).catch(() => [])
          : []
      } catch (reason) {
        error.value = reason instanceof Error ? reason.message : '权限加载失败'
        snapshot.value = null
        managedMenus.value = []
      } finally {
        loading.value = false
        pendingLoad = null
      }
    })()
    return pendingLoad
  }

  async function setDemoSubject(subject: string) {
    demoSubject.value = subject
    if (import.meta.env.DEV && typeof localStorage !== 'undefined') localStorage.setItem('coreib-demo-subject', subject)
    await load()
  }

  function login() {
    if (authSession.value?.loginUrl && typeof window !== 'undefined') {
      window.location.assign(authSession.value.loginUrl)
    }
  }

  async function logout() {
    if (!authSession.value?.logoutUrl) return
    await closeAuthenticationSession(authSession.value.logoutUrl)
    snapshot.value = null
    const redirectUrl = authSession.value.postLogoutRedirectUrl ?? '/'
    await loadSession()
    if (typeof window !== 'undefined') window.location.assign(redirectUrl)
  }

  function can(resource: string, action: PermissionAction): boolean {
    return snapshot.value ? canPerform(snapshot.value, resource, action) : false
  }

  function fieldAccess(resource: string, field: string): ColumnAccess {
    return snapshot.value ? resolveFieldAccess(snapshot.value, resource, field) : 'HIDDEN'
  }

  return {
    snapshot,
    authSession,
    managedMenus,
    loading,
    error,
    sessionError,
    demoSubject,
    authenticated,
    subjectId,
    roleCodes,
    initialize,
    load,
    loadSession,
    login,
    logout,
    setDemoSubject,
    can,
    fieldAccess,
  }
})

function readDemoSubject(): string {
  if (typeof localStorage === 'undefined') return ''
  return localStorage.getItem('coreib-demo-subject') ?? 'platform-admin'
}
