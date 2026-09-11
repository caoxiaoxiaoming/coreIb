<script setup lang="ts">
import { onMounted } from 'vue'
import { usePermissionStore } from './permission-store'
const store = usePermissionStore()
const isDevelopment = import.meta.env.DEV
const demoSubjects = [
  { value: '', label: '匿名开发会话' },
  { value: 'platform-admin', label: '平台管理员 · 查看平台目录' },
  { value: 'nurse', label: '护士 · 仅本人负责患者' },
  { value: 'head-nurse', label: '护士长 · 管理护士及患者' },
  { value: 'information-clerk', label: '信息科普通科员 · 只读' },
  { value: 'information-leader', label: '信息科领导 · 可读写' },
]
onMounted(() => { if (!store.snapshot && !store.loading) store.load() })
</script>
<template>
  <section class="page-view">
    <div class="page-heading"><div><span class="eyebrow">AUTHORIZATION CENTER</span><h1>权限中心</h1><p>展示当前用户的有效权限。行条件不会下发到浏览器，最终校验由后端完成。</p></div><div class="permission-actions"><el-select v-if="isDevelopment" :model-value="store.demoSubject" class="dev-subject-select" aria-label="开发联调主体" @change="store.setDemoSubject"><el-option v-for="subject in demoSubjects" :key="subject.value" :value="subject.value" :label="subject.label" /></el-select><el-button :loading="store.loading" @click="store.load">刷新权限</el-button></div></div>
    <el-alert v-if="store.error" type="warning" :closable="false" show-icon :title="store.error" />
    <section class="surface-panel identity-panel"><div><span>认证状态</span><strong>{{ store.authenticated ? '已认证' : '匿名开发会话' }}</strong></div><div><span>主体标识</span><strong>{{ store.subjectId }}</strong></div><div><span>角色</span><strong>{{ store.roleCodes.length ? store.roleCodes.join(' / ') : '暂无角色' }}</strong></div></section>
    <section class="surface-panel"><div class="panel-heading"><div><span class="eyebrow">EFFECTIVE RESOURCES</span><h2>资源策略</h2></div><el-tag effect="plain">{{ Object.keys(store.snapshot?.resources ?? {}).length }} 项</el-tag></div>
      <el-empty v-if="!Object.keys(store.snapshot?.resources ?? {}).length" description="当前没有可展示的权限资源" />
      <el-table v-else :data="Object.entries(store.snapshot?.resources ?? {}).map(([resource, value]) => ({ resource, ...value }))" stripe><el-table-column prop="resource" label="资源" min-width="180" /><el-table-column label="动作" min-width="240"><template #default="scope"><el-tag v-for="action in scope.row.actions" :key="action" size="small" effect="plain" class="table-tag">{{ action }}</el-tag></template></el-table-column><el-table-column prop="rowScope" label="行范围" min-width="180" /><el-table-column label="字段策略" min-width="260"><template #default="scope"><span v-if="!Object.keys(scope.row.fields).length" class="muted">由业务模块定义</span><span v-for="(access, field) in scope.row.fields" :key="field" class="field-rule">{{ field }}: {{ access }}</span></template></el-table-column></el-table>
    </section>
    <section class="surface-panel scenario-panel"><div class="panel-heading"><div><span class="eyebrow">CONFIRMED SCENARIOS</span><h2>已确认业务场景</h2></div></div><el-table :data="[{ role: '护士', range: '本人负责的患者', action: 'READ', state: 'demo 适配器可联调' }, { role: '护士长', range: '管理护士及其患者', action: 'READ', state: '策略内核已支持' }, { role: '信息科普通科员', range: '组织内业务数据', action: 'READ', state: '策略内核已支持' }, { role: '信息科领导', range: '组织内业务数据', action: 'READ / UPDATE', state: '策略内核已支持' }]" stripe><el-table-column prop="role" label="角色" width="180" /><el-table-column prop="range" label="数据范围" min-width="260" /><el-table-column prop="action" label="动作" width="180" /><el-table-column prop="state" label="实现状态" min-width="220" /></el-table></section>
  </section>
</template>
