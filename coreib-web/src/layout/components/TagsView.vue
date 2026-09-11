<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore, type ViewTag } from '../../stores/tags-view-store'
import PlatformIcon from './PlatformIcon.vue'

const route = useRoute()
const router = useRouter()
const store = useTagsViewStore()
const tags = computed(() => store.visitedViews)

function go(tag: ViewTag) {
  router.push(tag.fullPath)
}

function close(tag: ViewTag) {
  const wasActive = route.fullPath === tag.fullPath
  const index = tags.value.findIndex((item) => item.fullPath === tag.fullPath)
  store.close(tag.fullPath)
  if (wasActive) {
    const next = store.visitedViews[Math.min(index, store.visitedViews.length - 1)]
    router.push(next?.fullPath ?? '/dashboard')
  }
}

function closeOthers(tag: ViewTag) {
  store.closeOthers(tag.fullPath)
  router.push(tag.fullPath)
}

function closeAll() {
  store.closeAll()
  router.push('/dashboard')
}

function onMiddleClick(event: MouseEvent, tag: ViewTag) {
  if (event.button === 1 && !tag.affix) close(tag)
}

watch(() => route.fullPath, () => store.add(route), { immediate: true })
</script>

<template>
  <div class="yudao-tags-view">
    <el-scrollbar class="tags-scrollbar">
      <div class="tags-list">
        <el-dropdown v-for="tag in tags" :key="tag.fullPath" trigger="contextmenu">
          <button
            type="button"
            class="route-tag"
            :class="{ 'is-active': route.fullPath === tag.fullPath }"
            @click="go(tag)"
            @auxclick="onMiddleClick($event, tag)"
          >
            <platform-icon v-if="tag.icon" :name="tag.icon" :size="13" />
            <span>{{ tag.title }}</span>
            <platform-icon v-if="!tag.affix" name="Close" :size="12" class="tag-close" @click.stop="close(tag)" />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :disabled="route.fullPath !== tag.fullPath" @click="store.refresh()">
                <platform-icon name="Refresh" /> 刷新页面
              </el-dropdown-item>
              <el-dropdown-item :disabled="tag.affix" @click="close(tag)">
                <platform-icon name="Close" /> 关闭当前
              </el-dropdown-item>
              <el-dropdown-item divided @click="closeOthers(tag)">关闭其他</el-dropdown-item>
              <el-dropdown-item @click="closeAll">关闭全部</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-scrollbar>
    <el-tooltip content="刷新当前页面" placement="bottom">
      <button type="button" class="tags-tool" aria-label="刷新当前页面" @click="store.refresh()">
        <platform-icon name="Refresh" :size="16" />
      </button>
    </el-tooltip>
    <el-dropdown trigger="click">
      <button type="button" class="tags-tool" aria-label="标签页操作"><platform-icon name="MoreFilled" :size="16" /></button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item @click="store.closeOthers(route.fullPath)">关闭其他</el-dropdown-item>
          <el-dropdown-item @click="closeAll">关闭全部</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>
