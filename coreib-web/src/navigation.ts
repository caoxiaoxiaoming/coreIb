import type { PermissionAction } from './coreib-permissions'
import type { PermissionSnapshot } from './coreib-permissions'
import { can } from './coreib-permissions'
import type { ManagedMenu } from './api-client'

export interface MenuItem {
  key: string
  title: string
  path?: string
  icon: string
  children?: MenuItem[]
  platformOnly?: boolean
  resource?: string
  action?: PermissionAction
}

/**
 * The menu is intentionally data-driven. Business modules can append entries
 * here without changing the shell, as long as they expose a matching resource
 * and action in the effective permission snapshot.
 */
export const menuItems: MenuItem[] = [
  { key: 'dashboard', title: '工作台', path: '/dashboard', icon: 'HomeFilled', platformOnly: true },
  {
    key: 'security', title: '权限与安全', icon: 'Lock',
    children: [
      { key: 'permissions', title: '权限中心', path: '/permissions', icon: 'Key', platformOnly: true },
      { key: 'security-administration', title: '权限配置', path: '/security-administration', icon: 'SetUp', resource: 'security-administration', action: 'READ' },
    ],
  },
  {
    key: 'platform-info', title: '平台信息', icon: 'Monitor',
    children: [
      { key: 'platform-directory', title: '平台目录总览', path: '/platform-directory', icon: 'OfficeBuilding', resource: 'platform-directory', action: 'READ' },
      { key: 'system', title: '系统信息', path: '/system', icon: 'Monitor', platformOnly: true },
    ],
  },
]

export function isMenuVisible(item: MenuItem, snapshot: PermissionSnapshot | null): boolean {
  if (item.children) return item.children.some((child) => isMenuVisible(child, snapshot))
  if (item.platformOnly) return true
  if (!snapshot || !item.resource || !item.action) return false
  return can(snapshot, item.resource, item.action)
}

export function visibleMenuItems(snapshot: PermissionSnapshot | null, managedMenus: ManagedMenu[] = []): MenuItem[] {
  return [...menuItems, ...managedMenuTree(managedMenus)].flatMap((item) => {
    if (!isMenuVisible(item, snapshot)) return []
    if (!item.children) return [item]
    return [{ ...item, children: item.children.filter((child) => isMenuVisible(child, snapshot)) }]
  })
}

function managedMenuTree(entries: ManagedMenu[]): MenuItem[] {
  const active = entries.filter((entry) => entry.enabled && entry.visible && entry.menuType !== 'BUTTON')
  const convert = (entry: ManagedMenu): MenuItem => {
    const separator = entry.permission.lastIndexOf(':')
    const resource = separator > 0 ? entry.permission.slice(0, separator) : undefined
    const action = separator > 0 ? entry.permission.slice(separator + 1) as PermissionAction : undefined
    const children = active.filter((child) => child.parentId === entry.id).sort(bySort).map(convert)
    return {
      key: `managed-${entry.id}`,
      title: entry.name,
      path: entry.path || undefined,
      icon: entry.icon || 'Menu',
      resource,
      action,
      children: children.length ? children : undefined,
    }
  }
  return active.filter((entry) => !entry.parentId || !active.some((parent) => parent.id === entry.parentId)).sort(bySort).map(convert)
}

function bySort(left: ManagedMenu, right: ManagedMenu): number {
  return left.sortOrder - right.sortOrder || left.name.localeCompare(right.name)
}
