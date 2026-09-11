import { createRouter, createWebHistory } from 'vue-router'
import type { PermissionAction } from './coreib-permissions'
import Layout from './layout/Layout.vue'
import { usePermissionStore } from './permission-store'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('./LoginView.vue'),
      meta: { title: '登录', public: true, noTagsView: true },
    },
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'Dashboard', component: () => import('./DashboardView.vue'), meta: { title: '工作台', icon: 'HomeFilled', platform: true, affix: true } },
        { path: 'permissions', name: 'Permissions', component: () => import('./PermissionView.vue'), meta: { title: '权限中心', icon: 'Key', platform: true } },
        { path: 'security-administration', name: 'SecurityAdministration', component: () => import('./SecurityAdministrationView.vue'), meta: { title: '权限配置', icon: 'SetUp', resource: 'security-administration', action: 'READ' } },
        { path: 'platform-directory', name: 'PlatformDirectory', component: () => import('./PlatformDirectoryView.vue'), meta: { title: '平台目录', icon: 'User', resource: 'platform-directory', action: 'READ' } },
        { path: 'system/users', name: 'UserManagement', component: () => import('./DirectoryManagementView.vue'), meta: { title: '用户管理', icon: 'User', resource: 'platform-directory', action: 'READ', directoryKind: 'users' } },
        { path: 'system/roles', name: 'RoleManagement', component: () => import('./DirectoryManagementView.vue'), meta: { title: '角色管理', icon: 'Avatar', resource: 'platform-directory', action: 'READ', directoryKind: 'roles' } },
        { path: 'system/departments', name: 'DepartmentManagement', component: () => import('./DirectoryManagementView.vue'), meta: { title: '部门管理', icon: 'OfficeBuilding', resource: 'platform-directory', action: 'READ', directoryKind: 'departments' } },
        { path: 'system/menus', name: 'MenuManagement', component: () => import('./SimpleManagementView.vue'), meta: { title: '菜单管理', icon: 'Menu', resource: 'platform-management', action: 'READ', managementKind: 'menus' } },
        { path: 'system/posts', name: 'PostManagement', component: () => import('./SimpleManagementView.vue'), meta: { title: '岗位管理', icon: 'Postcard', resource: 'platform-management', action: 'READ', managementKind: 'posts' } },
        { path: 'system/dictionaries', name: 'DictionaryManagement', component: () => import('./DictionaryManagementView.vue'), meta: { title: '字典管理', icon: 'Collection', resource: 'platform-management', action: 'READ' } },
        { path: 'system/configs', name: 'ConfigManagement', component: () => import('./SimpleManagementView.vue'), meta: { title: '参数配置', icon: 'Tools', resource: 'platform-management', action: 'READ', managementKind: 'configs' } },
        { path: 'system/notices', name: 'NoticeManagement', component: () => import('./SimpleManagementView.vue'), meta: { title: '通知公告', icon: 'Bell', resource: 'platform-management', action: 'READ', managementKind: 'notices' } },
        { path: 'infrastructure/audit-logs', name: 'AuditLogs', component: () => import('./LogManagementView.vue'), meta: { title: '操作日志', icon: 'Document', resource: 'platform-management', action: 'READ', logKind: 'audit' } },
        { path: 'infrastructure/login-logs', name: 'LoginLogs', component: () => import('./LogManagementView.vue'), meta: { title: '登录日志', icon: 'Tickets', resource: 'platform-management', action: 'READ', logKind: 'login' } },
        { path: 'infrastructure/files', name: 'FileManagement', component: () => import('./FileManagementView.vue'), meta: { title: '文件管理', icon: 'Folder', resource: 'file-management', action: 'READ' } },
        { path: 'infrastructure/codegen', name: 'CodeGeneration', component: () => import('./CodeGenerationView.vue'), meta: { title: '代码生成', icon: 'MagicStick', resource: 'code-generation', action: 'READ' } },
        { path: 'system', name: 'System', component: () => import('./SystemView.vue'), meta: { title: '系统信息', icon: 'Monitor', platform: true } },
        { path: 'forbidden', name: 'Forbidden', component: () => import('./ForbiddenView.vue'), meta: { title: '无权访问', icon: 'WarningFilled', platform: true, noTagsView: true } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard', meta: { platform: true } },
  ],
  scrollBehavior: () => ({ left: 0, top: 0 }),
})

router.beforeEach(async (to) => {
  const permissionStore = usePermissionStore()
  if (!permissionStore.authSession && !permissionStore.loading) {
    try {
      await permissionStore.initialize()
    } catch {
      if (to.meta.public) return true
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  const requiresOidcLogin = permissionStore.authSession?.mode === 'OIDC'
    && !permissionStore.authSession.identityAuthenticated
  if (to.meta.public) {
    if (to.path === '/login' && !requiresOidcLogin && permissionStore.authSession) {
      return String(to.query.redirect ?? '/dashboard')
    }
    return true
  }
  if (requiresOidcLogin) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.path === '/' || to.meta.platform) return true
  if (!to.meta.resource || !to.meta.action) return { path: '/forbidden', query: { from: to.fullPath } }

  return permissionStore.can(String(to.meta.resource), String(to.meta.action) as PermissionAction)
    ? true
    : { path: '/forbidden', query: { from: to.fullPath } }
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '管理端')} - coreIb`
})

export default router
