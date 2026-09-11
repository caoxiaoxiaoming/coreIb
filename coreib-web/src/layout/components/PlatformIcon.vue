<script setup lang="ts">
import { computed, type Component } from 'vue'
import {
  ArrowDown, Check, Close, Expand, Fold, FullScreen, HomeFilled, Key, Lock,
  Menu, Monitor, Moon, MoreFilled, OfficeBuilding, Refresh, SetUp, Setting,
  Sunny, User, WarningFilled,
} from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{ name?: string; size?: number }>(), { name: 'Menu', size: 18 })
const icons: Record<string, Component> = {
  ArrowDown, Check, Close, Expand, Fold, FullScreen, HomeFilled, Key, Lock,
  Menu, Monitor, Moon, MoreFilled, OfficeBuilding, Refresh, SetUp, Setting,
  Sunny, User, WarningFilled,
}
const component = computed(() => icons[props.name] ?? Menu)
</script>

<template>
  <el-icon :size="size"><component :is="component" /></el-icon>
</template>
