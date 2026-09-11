<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ElMessage from 'element-plus/es/components/message/index'
import {
  fetchPlatformDirectory,
  createPlatformOrganization,
  createPlatformRole,
  createPlatformUser,
  setPlatformEnabled,
  type PlatformOrganization,
  type PlatformRole,
  type PlatformUser,
} from './api-client'
import { usePermissionStore } from './permission-store'

const permissionStore = usePermissionStore()
const activeTab = ref('users')
const loading = ref(false)
const organizations = ref<PlatformOrganization[]>([])
const users = ref<PlatformUser[]>([])
const roles = ref<PlatformRole[]>([])
const canUpdate = computed(() => permissionStore.can('platform-directory', 'UPDATE'))
const createVisible = ref(false)
const createType = ref<'organization' | 'user' | 'role'>('organization')
const draft = ref<Record<string, string>>({})

async function load() {
  loading.value = true
  try {
    const subject = import.meta.env.DEV ? permissionStore.demoSubject || undefined : undefined
    const directory = await fetchPlatformDirectory(subject)
    organizations.value = directory.organizations
    users.value = directory.users
    roles.value = directory.roles
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `平台目录读取失败：${reason.message}` : '平台目录读取失败')
  } finally {
    loading.value = false
  }
}

function openCreate(type: 'organization' | 'user' | 'role') {
  createType.value = type
  draft.value = {}
  createVisible.value = true
}

async function create() {
  loading.value = true
  try {
    const subject = import.meta.env.DEV ? permissionStore.demoSubject || undefined : undefined
    if (createType.value === 'organization') await createPlatformOrganization(subject, { ...draft.value, sortOrder: Number(draft.value.sortOrder || 0) })
    if (createType.value === 'user') await createPlatformUser(subject, { ...draft.value, roleCodes: (draft.value.roleCodes || '').split(',').map((role) => role.trim()).filter(Boolean) })
    if (createType.value === 'role') await createPlatformRole(subject, draft.value)
    createVisible.value = false
    ElMessage.success('创建成功')
    await load()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `创建失败：${reason.message}` : '创建失败')
  } finally {
    loading.value = false
  }
}

async function toggle(entity: 'organization' | 'user' | 'role', id: string, enabled: boolean) {
  try {
    const subject = import.meta.env.DEV ? permissionStore.demoSubject || undefined : undefined
    await setPlatformEnabled(subject, entity, id, !enabled)
    ElMessage.success('状态已更新')
    await load()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `状态更新失败：${reason.message}` : '状态更新失败')
  }
}

onMounted(load)
</script>

<template>
  <section class="page-view">
    <div class="page-heading">
      <div>
        <span class="eyebrow">PLATFORM DIRECTORY</span>
        <h1>平台目录</h1>
        <p>统一查看组织、用户和角色。当前阶段提供只读查询，后续写操作将使用独立命令接口和审计链。</p>
      </div>
        <div class="directory-actions"><el-button v-if="canUpdate" @click="openCreate('organization')">新增组织</el-button><el-button v-if="canUpdate" @click="openCreate('user')">新增用户</el-button><el-button v-if="canUpdate" @click="openCreate('role')">新增角色</el-button><el-button :loading="loading" @click="load">刷新</el-button></div>
    </div>

    <section class="surface-panel directory-summary">
      <div><span>组织</span><strong>{{ organizations.length }}</strong></div>
      <div><span>用户</span><strong>{{ users.length }}</strong></div>
      <div><span>角色</span><strong>{{ roles.length }}</strong></div>
    </section>

    <section class="surface-panel directory-content" v-loading="loading">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="用户" name="users">
          <el-table :data="users" stripe>
            <el-table-column prop="displayName" label="姓名" min-width="140" />
            <el-table-column prop="loginName" label="登录名" min-width="160" />
            <el-table-column prop="organizationName" label="所属组织" min-width="150" />
            <el-table-column label="角色" min-width="220">
              <template #default="scope"><el-tag v-for="role in scope.row.roleCodes" :key="role" size="small" effect="plain" class="table-tag">{{ role }}</el-tag></template>
            </el-table-column>
            <el-table-column label="状态" width="150"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'" effect="plain">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag><el-button v-if="canUpdate" link size="small" @click="toggle('user', scope.row.id, scope.row.enabled)">{{ scope.row.enabled ? '停用' : '启用' }}</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="组织" name="organizations">
          <el-table :data="organizations" stripe>
            <el-table-column prop="displayName" label="组织名称" min-width="180" />
            <el-table-column prop="code" label="编码" min-width="140" />
            <el-table-column prop="type" label="类型" min-width="120" />
            <el-table-column prop="parentId" label="上级标识" min-width="160" />
            <el-table-column label="状态" width="150"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'" effect="plain">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag><el-button v-if="canUpdate" link size="small" @click="toggle('organization', scope.row.id, scope.row.enabled)">{{ scope.row.enabled ? '停用' : '启用' }}</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="角色" name="roles">
          <el-table :data="roles" stripe>
            <el-table-column prop="displayName" label="角色名称" min-width="170" />
            <el-table-column prop="code" label="角色编码" min-width="190" />
            <el-table-column prop="description" label="说明" min-width="260" />
            <el-table-column label="状态" width="150"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'" effect="plain">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag><el-button v-if="canUpdate" link size="small" @click="toggle('role', scope.row.code, scope.row.enabled)">{{ scope.row.enabled ? '停用' : '启用' }}</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
    <el-dialog v-model="createVisible" :title="`新增${createType === 'organization' ? '组织' : createType === 'user' ? '用户' : '角色'}`" width="520px">
      <el-form label-width="96px" @submit.prevent="create">
        <template v-if="createType === 'organization'"><el-form-item label="标识"><el-input v-model="draft.id" /></el-form-item><el-form-item label="编码"><el-input v-model="draft.code" /></el-form-item><el-form-item label="名称"><el-input v-model="draft.displayName" /></el-form-item><el-form-item label="类型"><el-input v-model="draft.type" placeholder="DEPARTMENT" /></el-form-item><el-form-item label="上级标识"><el-input v-model="draft.parentId" /></el-form-item><el-form-item label="排序"><el-input v-model="draft.sortOrder" /></el-form-item></template>
        <template v-else-if="createType === 'user'"><el-form-item label="标识"><el-input v-model="draft.id" /></el-form-item><el-form-item label="登录名"><el-input v-model="draft.loginName" /></el-form-item><el-form-item label="姓名"><el-input v-model="draft.displayName" /></el-form-item><el-form-item label="组织标识"><el-input v-model="draft.organizationId" /></el-form-item><el-form-item label="角色编码"><el-input v-model="draft.roleCodes" placeholder="nurse,head-nurse" /></el-form-item></template>
        <template v-else><el-form-item label="角色编码"><el-input v-model="draft.code" /></el-form-item><el-form-item label="名称"><el-input v-model="draft.displayName" /></el-form-item><el-form-item label="说明"><el-input v-model="draft.description" type="textarea" /></el-form-item></template>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" :loading="loading" @click="create">创建</el-button></template>
    </el-dialog>
  </section>
</template>
