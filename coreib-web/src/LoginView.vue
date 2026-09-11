<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from './permission-store'

const route = useRoute()
const store = usePermissionStore()
const retrying = ref(false)
const target = computed(() => String(route.query.redirect ?? '/dashboard'))

async function retry() {
  retrying.value = true
  try {
    await store.initialize()
    if (store.authSession?.mode === 'OIDC' && !store.authSession.identityAuthenticated) store.login()
    else window.location.assign(target.value)
  } finally {
    retrying.value = false
  }
}
</script>

<template>
  <main class="login-view">
    <section class="login-panel">
      <div class="login-brand"><span class="logo-mark">cI</span><div><strong>coreIb</strong><small>业务系统研发基座</small></div></div>
      <h1>登录管理端</h1>
      <p>使用组织统一身份进入 coreIb。认证会话、角色和数据权限均由 coreIb 后端提供。</p>
      <el-alert v-if="store.sessionError" :title="`认证服务不可用：${store.sessionError}`" type="error" :closable="false" show-icon />
      <el-button type="primary" size="large" :loading="retrying" class="login-button" @click="store.authSession?.loginUrl ? store.login() : retry()">
        {{ store.authSession?.loginUrl ? '统一身份登录' : '重新连接' }}
      </el-button>
      <span class="login-mode">认证模式：{{ store.authSession?.mode ?? '等待服务' }}</span>
    </section>
  </main>
</template>
