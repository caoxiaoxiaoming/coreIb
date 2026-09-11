<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchHealth, fetchSystemInfo, type SystemInfo } from './api-client'
const info = ref<SystemInfo | null>(null)
const health = ref('检查中')
const loading = ref(true)
const error = ref('')
async function load() { loading.value = true; error.value = ''; try { const [systemInfo, healthInfo] = await Promise.all([fetchSystemInfo(), fetchHealth()]); info.value = systemInfo; health.value = healthInfo.status } catch (reason) { error.value = reason instanceof Error ? reason.message : '系统信息读取失败' } finally { loading.value = false } }
onMounted(load)
</script>
<template>
  <section class="page-view"><div class="page-heading"><div><span class="eyebrow">RUNTIME INFORMATION</span><h1>系统信息</h1><p>查看当前 coreIb 服务端运行时和数据库适配能力。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" type="error" :closable="false" show-icon :title="error" />
    <section v-if="info" class="surface-panel system-grid"><div><span>平台名称</span><strong>{{ info.name }}</strong></div><div><span>平台版本</span><strong>{{ info.version }}</strong></div><div><span>Java 运行时</span><strong>{{ info.javaVersion }}</strong></div><div><span>健康状态</span><el-tag :type="health === 'UP' ? 'success' : 'danger'" effect="plain">{{ health }}</el-tag></div><div class="wide"><span>支持数据库</span><div><el-tag v-for="database in info.supportedDatabases" :key="database" effect="plain" class="table-tag">{{ database }}</el-tag></div></div><div class="wide"><span>当前数据库</span><strong>{{ info.activeDatabase }}</strong></div></section>
  </section>
</template>
