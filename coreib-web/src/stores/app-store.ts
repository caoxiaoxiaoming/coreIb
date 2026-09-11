import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { ComponentSize } from 'element-plus'

export type LayoutMode = 'classic' | 'top'

interface StoredPreferences {
  layoutMode?: LayoutMode
  collapsed?: boolean
  dark?: boolean
  tagsView?: boolean
  breadcrumb?: boolean
  componentSize?: ComponentSize
  primaryColor?: string
}

const STORAGE_KEY = 'coreib-web-preferences'

export const useAppStore = defineStore('app', () => {
  const layoutMode = ref<LayoutMode>('classic')
  const collapsed = ref(false)
  const dark = ref(false)
  const tagsView = ref(true)
  const breadcrumb = ref(true)
  const componentSize = ref<ComponentSize>('default')
  const primaryColor = ref('#409eff')
  const mobile = ref(false)
  const settingsVisible = ref(false)

  const sidebarWidth = computed(() => collapsed.value ? '64px' : '208px')

  function initialize() {
    if (typeof localStorage !== 'undefined') {
      try {
        const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}') as StoredPreferences
        if (stored.layoutMode === 'classic' || stored.layoutMode === 'top') layoutMode.value = stored.layoutMode
        if (typeof stored.collapsed === 'boolean') collapsed.value = stored.collapsed
        if (typeof stored.dark === 'boolean') dark.value = stored.dark
        if (typeof stored.tagsView === 'boolean') tagsView.value = stored.tagsView
        if (typeof stored.breadcrumb === 'boolean') breadcrumb.value = stored.breadcrumb
        if (stored.componentSize) componentSize.value = stored.componentSize
        if (stored.primaryColor) primaryColor.value = stored.primaryColor
      } catch {
        localStorage.removeItem(STORAGE_KEY)
      }
    }
    applyAppearance()
  }

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      layoutMode: layoutMode.value,
      collapsed: collapsed.value,
      dark: dark.value,
      tagsView: tagsView.value,
      breadcrumb: breadcrumb.value,
      componentSize: componentSize.value,
      primaryColor: primaryColor.value,
    }))
    applyAppearance()
  }

  function applyAppearance() {
    document.documentElement.classList.toggle('dark', dark.value)
    document.documentElement.style.setProperty('--el-color-primary', primaryColor.value)
    document.documentElement.style.setProperty('--left-menu-bg-active-color', primaryColor.value)
  }

  function patchPreferences(patch: Partial<StoredPreferences>) {
    if (patch.layoutMode) layoutMode.value = patch.layoutMode
    if (typeof patch.collapsed === 'boolean') collapsed.value = patch.collapsed
    if (typeof patch.dark === 'boolean') dark.value = patch.dark
    if (typeof patch.tagsView === 'boolean') tagsView.value = patch.tagsView
    if (typeof patch.breadcrumb === 'boolean') breadcrumb.value = patch.breadcrumb
    if (patch.componentSize) componentSize.value = patch.componentSize
    if (patch.primaryColor) primaryColor.value = patch.primaryColor
    persist()
  }

  function reset() {
    localStorage.removeItem(STORAGE_KEY)
    layoutMode.value = 'classic'
    collapsed.value = false
    dark.value = false
    tagsView.value = true
    breadcrumb.value = true
    componentSize.value = 'default'
    primaryColor.value = '#409eff'
    applyAppearance()
  }

  return {
    layoutMode, collapsed, dark, tagsView, breadcrumb, componentSize, primaryColor,
    mobile, settingsVisible, sidebarWidth, initialize, patchPreferences, reset,
  }
})
