import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

export interface ViewTag {
  path: string
  fullPath: string
  name?: string
  title: string
  icon?: string
  affix: boolean
}

export const useTagsViewStore = defineStore('tags-view', () => {
  const visitedViews = ref<ViewTag[]>([])
  const refreshKey = ref(0)

  function add(route: RouteLocationNormalizedLoaded) {
    if (!route.name || route.meta.noTagsView) return
    if (visitedViews.value.some((item) => item.fullPath === route.fullPath)) return
    visitedViews.value.push({
      path: route.path,
      fullPath: route.fullPath,
      name: String(route.name),
      title: String(route.meta.title ?? route.name),
      icon: route.meta.icon,
      affix: Boolean(route.meta.affix),
    })
  }

  function close(fullPath: string) {
    visitedViews.value = visitedViews.value.filter((item) => item.affix || item.fullPath !== fullPath)
  }

  function closeOthers(fullPath: string) {
    visitedViews.value = visitedViews.value.filter((item) => item.affix || item.fullPath === fullPath)
  }

  function closeAll() {
    visitedViews.value = visitedViews.value.filter((item) => item.affix)
  }

  function refresh() {
    refreshKey.value += 1
  }

  return { visitedViews, refreshKey, add, close, closeOthers, closeAll, refresh }
})
