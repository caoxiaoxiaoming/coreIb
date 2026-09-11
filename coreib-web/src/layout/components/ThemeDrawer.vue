<script setup lang="ts">
import { useAppStore, type LayoutMode } from '../../stores/app-store'
import PlatformIcon from './PlatformIcon.vue'

const store = useAppStore()
const colors = ['#409eff', '#009688', '#536dfe', '#e74c3c', '#0096c7', '#9c27b0']

function setLayout(layoutMode: LayoutMode) {
  store.patchPreferences({ layoutMode })
}
</script>

<template>
  <el-drawer v-model="store.settingsVisible" title="项目配置" size="350px" append-to-body>
    <div class="theme-section">
      <h3>主题模式</h3>
      <el-switch
        :model-value="store.dark"
        inline-prompt
        active-text="深色"
        inactive-text="浅色"
        @change="store.patchPreferences({ dark: Boolean($event) })"
      />
    </div>
    <div class="theme-section">
      <h3>导航布局</h3>
      <div class="layout-options">
        <button type="button" class="layout-option" :class="{ selected: store.layoutMode === 'classic' }" @click="setLayout('classic')">
          <span class="layout-preview classic-preview"><i></i><b></b></span><span>经典侧边栏</span>
        </button>
        <button type="button" class="layout-option" :class="{ selected: store.layoutMode === 'top' }" @click="setLayout('top')">
          <span class="layout-preview top-preview"><i></i><b></b></span><span>顶部导航</span>
        </button>
      </div>
    </div>
    <div class="theme-section">
      <h3>系统主题色</h3>
      <div class="color-swatches">
        <button v-for="color in colors" :key="color" type="button" class="color-swatch" :style="{ backgroundColor: color }" :aria-label="`主题色 ${color}`" @click="store.patchPreferences({ primaryColor: color })">
          <platform-icon v-if="store.primaryColor === color" name="Check" :size="14" />
        </button>
      </div>
    </div>
    <div class="theme-section display-settings">
      <h3>界面显示</h3>
      <label><span>面包屑</span><el-switch :model-value="store.breadcrumb" @change="store.patchPreferences({ breadcrumb: Boolean($event) })" /></label>
      <label><span>标签页</span><el-switch :model-value="store.tagsView" @change="store.patchPreferences({ tagsView: Boolean($event) })" /></label>
    </div>
    <el-button class="reset-button" @click="store.reset">清除缓存并重置</el-button>
  </el-drawer>
</template>
