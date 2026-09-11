<script setup lang="ts">
import type { MenuItem } from '../../navigation'
import PlatformIcon from './PlatformIcon.vue'

defineOptions({ name: 'YudaoMenuItems' })
defineProps<{ items: MenuItem[] }>()
</script>

<template>
  <template v-for="item in items" :key="item.key">
    <el-sub-menu v-if="item.children?.length" :index="item.key">
      <template #title>
        <platform-icon :name="item.icon" />
        <span>{{ item.title }}</span>
      </template>
      <yudao-menu-items :items="item.children" />
    </el-sub-menu>
    <el-menu-item v-else :index="item.path">
      <platform-icon :name="item.icon" />
      <template #title>{{ item.title }}</template>
    </el-menu-item>
  </template>
</template>
