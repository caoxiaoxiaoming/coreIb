<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PlatformIcon from './PlatformIcon.vue'

const route = useRoute()
const current = computed(() => ({
  title: String(route.meta.title ?? '工作台'),
  icon: route.meta.icon,
}))
</script>

<template>
  <el-breadcrumb separator="/" class="yudao-breadcrumb">
    <el-breadcrumb-item to="/dashboard">coreIb</el-breadcrumb-item>
    <el-breadcrumb-item>
      <span class="breadcrumb-current">
        <platform-icon v-if="current.icon" :name="current.icon" :size="14" />
        {{ current.title }}
      </span>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>
