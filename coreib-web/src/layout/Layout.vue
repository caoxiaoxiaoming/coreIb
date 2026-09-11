<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import ElMessage from 'element-plus/es/components/message/index'
import ElMessageBox from 'element-plus/es/components/message-box/index'
import { useRoute, useRouter } from 'vue-router'
import { fetchHealth } from '../api-client'
import { visibleMenuItems } from '../navigation'
import { usePermissionStore } from '../permission-store'
import { useAppStore } from '../stores/app-store'
import { useTagsViewStore } from '../stores/tags-view-store'
import Breadcrumb from './components/Breadcrumb.vue'
import MenuItems from './components/MenuItems.vue'
import PlatformIcon from './components/PlatformIcon.vue'
import TagsView from './components/TagsView.vue'
import ThemeDrawer from './components/ThemeDrawer.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
const tagsStore = useTagsViewStore()
const mobileMenuVisible = ref(false)
const health = ref('CHECKING')

const menus = computed(() => visibleMenuItems(permissionStore.snapshot, permissionStore.managedMenus))
const activeMenu = computed(() => route.path)
const mainKey = computed(() => `${route.fullPath}:${tagsStore.refreshKey}`)
const showClassicSidebar = computed(() => appStore.layoutMode === 'classic' && !appStore.mobile)

function updateViewport() {
  appStore.mobile = window.innerWidth < 900
  if (!appStore.mobile) mobileMenuVisible.value = false
}

async function refreshHealth() {
  try {
    health.value = (await fetchHealth()).status
  } catch {
    health.value = 'OFFLINE'
  }
}

async function toggleFullscreen() {
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await document.documentElement.requestFullscreen()
  } catch {
    ElMessage.warning('当前浏览器不允许切换全屏')
  }
}

async function logout() {
  if (!permissionStore.authSession?.logoutUrl) return
  try {
    await ElMessageBox.confirm('确认退出当前登录会话？', '退出登录', { type: 'warning' })
    await permissionStore.logout()
  } catch (reason) {
    if (reason !== 'cancel' && reason !== 'close') ElMessage.error('退出登录失败')
  }
}

function selectMobileMenu() {
  mobileMenuVisible.value = false
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
  refreshHealth()
})
onBeforeUnmount(() => window.removeEventListener('resize', updateViewport))
</script>

<template>
  <div class="yudao-layout" :class="[`layout-${appStore.layoutMode}`, { 'is-collapsed': appStore.collapsed }]">
    <aside v-if="showClassicSidebar" class="yudao-sidebar" :style="{ width: appStore.sidebarWidth }">
      <router-link to="/dashboard" class="yudao-logo" :class="{ compact: appStore.collapsed }">
        <span class="logo-mark">cI</span>
        <span v-if="!appStore.collapsed" class="logo-copy"><strong>coreIb</strong><small>业务系统基座</small></span>
      </router-link>
      <el-scrollbar class="sidebar-scrollbar">
        <el-menu :default-active="activeMenu" :collapse="appStore.collapsed" :unique-opened="true" router class="yudao-menu">
          <menu-items :items="menus" />
        </el-menu>
      </el-scrollbar>
      <div v-if="!appStore.collapsed" class="sidebar-status">
        <i :class="{ online: health === 'UP' }"></i>
        <span>平台服务 {{ health === 'UP' ? '正常' : health === 'CHECKING' ? '检查中' : '离线' }}</span>
      </div>
    </aside>

    <section class="yudao-workspace">
      <header class="yudao-header" :class="{ 'top-navigation': appStore.layoutMode === 'top' && !appStore.mobile }">
        <div class="header-leading">
          <el-tooltip :content="appStore.mobile ? '打开导航' : appStore.collapsed ? '展开菜单' : '折叠菜单'" placement="bottom">
            <button type="button" class="tool-button" :aria-label="appStore.mobile ? '打开导航' : '折叠菜单'" @click="appStore.mobile ? mobileMenuVisible = true : appStore.patchPreferences({ collapsed: !appStore.collapsed })">
              <platform-icon :name="appStore.mobile ? 'Menu' : appStore.collapsed ? 'Expand' : 'Fold'" />
            </button>
          </el-tooltip>
          <router-link v-if="appStore.layoutMode === 'top' && !appStore.mobile" to="/dashboard" class="top-logo">
            <span class="logo-mark">cI</span><strong>coreIb</strong>
          </router-link>
          <breadcrumb v-if="appStore.breadcrumb && appStore.layoutMode === 'classic'" />
        </div>

        <el-menu v-if="appStore.layoutMode === 'top' && !appStore.mobile" :default-active="activeMenu" mode="horizontal" router class="top-menu">
          <menu-items :items="menus" />
        </el-menu>

        <div class="header-tools">
          <el-tooltip content="服务状态" placement="bottom">
            <button type="button" class="health-state" :aria-label="`服务状态 ${health}`" @click="refreshHealth"><i :class="{ online: health === 'UP' }"></i><span>{{ health }}</span></button>
          </el-tooltip>
          <el-dropdown trigger="click" @command="appStore.patchPreferences({ componentSize: $event })">
            <el-tooltip content="组件尺寸" placement="bottom"><button type="button" class="tool-button" aria-label="组件尺寸"><span class="size-symbol">A</span></button></el-tooltip>
            <template #dropdown><el-dropdown-menu><el-dropdown-item command="large">宽松</el-dropdown-item><el-dropdown-item command="default">默认</el-dropdown-item><el-dropdown-item command="small">紧凑</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
          <el-tooltip content="全屏" placement="bottom"><button type="button" class="tool-button" aria-label="全屏" @click="toggleFullscreen"><platform-icon name="FullScreen" /></button></el-tooltip>
          <el-tooltip content="项目配置" placement="bottom"><button type="button" class="tool-button" aria-label="项目配置" @click="appStore.settingsVisible = true"><platform-icon name="Setting" /></button></el-tooltip>
          <el-dropdown trigger="click">
            <button type="button" class="user-entry" aria-label="用户菜单"><el-avatar :size="30">{{ permissionStore.subjectId.slice(0, 1).toUpperCase() }}</el-avatar><span>{{ permissionStore.subjectId }}</span><platform-icon name="ArrowDown" :size="13" /></button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item @click="router.push('/permissions')"><platform-icon name="Key" /> 当前权限</el-dropdown-item><el-dropdown-item v-if="permissionStore.authSession?.loginUrl && !permissionStore.authSession.identityAuthenticated" @click="permissionStore.login">登录</el-dropdown-item><el-dropdown-item v-if="permissionStore.authSession?.logoutUrl && permissionStore.authSession.identityAuthenticated" divided @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </header>

      <tags-view v-if="appStore.tagsView" />
      <main class="yudao-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="mainKey" />
        </router-view>
      </main>
    </section>

    <el-drawer v-model="mobileMenuVisible" direction="ltr" size="260px" :with-header="false" class="mobile-navigation" append-to-body>
      <router-link to="/dashboard" class="yudao-logo" @click="mobileMenuVisible = false"><span class="logo-mark">cI</span><span class="logo-copy"><strong>coreIb</strong><small>业务系统基座</small></span></router-link>
      <el-menu :default-active="activeMenu" router class="yudao-menu" @select="selectMobileMenu"><menu-items :items="menus" /></el-menu>
    </el-drawer>
    <theme-drawer />
  </div>
</template>
