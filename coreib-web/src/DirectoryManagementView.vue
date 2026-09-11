<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index'
import ElMessageBox from 'element-plus/es/components/message-box/index'
import {
  createPlatformOrganization, createPlatformRole, createPlatformUser, deletePlatformDirectoryEntity,
  fetchPlatformDirectory, setPlatformEnabled, updatePlatformOrganization, updatePlatformRole,
  updatePlatformUser, type PlatformOrganization, type PlatformRole, type PlatformUser,
} from './api-client'
import { usePermissionStore } from './permission-store'

type DirectoryKind = 'users' | 'roles' | 'departments'
const route = useRoute()
const permissions = usePermissionStore()
const kind = computed(() => route.meta.directoryKind as DirectoryKind)
const title = computed(() => ({ users: '用户管理', roles: '角色管理', departments: '部门管理' })[kind.value])
const singular = computed(() => ({ users: '用户', roles: '角色', departments: '部门' })[kind.value])
const loading = ref(false)
const search = ref('')
const organizations = ref<PlatformOrganization[]>([])
const users = ref<PlatformUser[]>([])
const roles = ref<PlatformRole[]>([])
const dialogVisible = ref(false)
const editing = ref(false)
const draft = ref<Record<string, unknown>>({})
const canUpdate = computed(() => permissions.can('platform-directory', 'UPDATE'))
const canDelete = computed(() => permissions.can('platform-directory', 'DELETE'))
const subject = computed(() => import.meta.env.DEV ? permissions.demoSubject || undefined : undefined)
const rows = computed(() => {
  const source = kind.value === 'users' ? users.value : kind.value === 'roles' ? roles.value : organizations.value
  const keyword = search.value.trim().toLowerCase()
  return keyword ? source.filter((row) => JSON.stringify(row).toLowerCase().includes(keyword)) : source
})

async function load() {
  loading.value = true
  try {
    const result = await fetchPlatformDirectory(subject.value)
    organizations.value = result.organizations; users.value = result.users; roles.value = result.roles
  } catch (reason) { ElMessage.error(errorText(reason, '目录读取失败')) }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = false
  const id = crypto.randomUUID()
  draft.value = kind.value === 'users'
    ? { id, loginName: '', displayName: '', organizationId: '', externalIdentity: '', roleCodes: [] }
    : kind.value === 'roles'
      ? { code: `role-${Date.now()}`, displayName: '', description: '' }
      : { id, parentId: '', code: '', displayName: '', type: 'DEPARTMENT', sortOrder: 10 }
  dialogVisible.value = true
}

function openEdit(row: PlatformUser | PlatformRole | PlatformOrganization) {
  editing.value = true
  if (kind.value === 'users') draft.value = { ...(row as PlatformUser), roleCodes: [...(row as PlatformUser).roleCodes], externalIdentity: '' }
  else if (kind.value === 'roles') draft.value = { ...(row as PlatformRole) }
  else draft.value = { ...(row as PlatformOrganization), sortOrder: 10 }
  dialogVisible.value = true
}

async function save() {
  loading.value = true
  try {
    if (kind.value === 'users') {
      const id = String(draft.value.id)
      if (editing.value) await updatePlatformUser(subject.value, id, draft.value)
      else await createPlatformUser(subject.value, draft.value)
    } else if (kind.value === 'roles') {
      const code = String(draft.value.code)
      if (editing.value) await updatePlatformRole(subject.value, code, draft.value)
      else await createPlatformRole(subject.value, draft.value)
    } else {
      const id = String(draft.value.id)
      if (editing.value) await updatePlatformOrganization(subject.value, id, draft.value)
      else await createPlatformOrganization(subject.value, draft.value)
    }
    dialogVisible.value = false; ElMessage.success('保存成功'); await load()
  } catch (reason) { ElMessage.error(errorText(reason, '保存失败')) }
  finally { loading.value = false }
}

async function toggle(row: PlatformUser | PlatformRole | PlatformOrganization) {
  const entity = kind.value === 'users' ? 'user' : kind.value === 'roles' ? 'role' : 'organization'
  const id = kind.value === 'roles' ? (row as PlatformRole).code : (row as PlatformUser | PlatformOrganization).id
  try { await setPlatformEnabled(subject.value, entity, id, !row.enabled); await load() }
  catch (reason) { ElMessage.error(errorText(reason, '状态更新失败')) }
}

async function remove(row: PlatformUser | PlatformRole | PlatformOrganization) {
  const entity = kind.value === 'users' ? 'user' : kind.value === 'roles' ? 'role' : 'organization'
  const id = kind.value === 'roles' ? (row as PlatformRole).code : (row as PlatformUser | PlatformOrganization).id
  try {
    await ElMessageBox.confirm(`确认删除${singular.value}“${'displayName' in row ? row.displayName : id}”？`, '删除确认', { type: 'warning' })
    await deletePlatformDirectoryEntity(subject.value, entity, id); ElMessage.success('删除成功'); await load()
  } catch (reason) { if (reason !== 'cancel' && reason !== 'close') ElMessage.error(errorText(reason, '删除失败')) }
}

function errorText(reason: unknown, fallback: string) { return reason instanceof Error ? `${fallback}：${reason.message}` : fallback }
watch(kind, load)
onMounted(load)
</script>

<template>
  <section class="page-view">
    <div class="page-heading compact-heading">
      <div><h1>{{ title }}</h1><p>维护平台{{ singular }}资料、启停状态与组织角色关系。</p></div>
      <el-button v-if="canUpdate" type="primary" @click="openCreate">新增{{ singular }}</el-button>
    </div>
    <section class="surface-panel management-toolbar"><el-input v-model="search" clearable :placeholder="`搜索${singular}`" /><el-button :loading="loading" @click="load">刷新</el-button></section>
    <section class="surface-panel" v-loading="loading">
      <el-table v-if="kind === 'users'" :data="rows" stripe>
        <el-table-column prop="displayName" label="姓名" min-width="130" /><el-table-column prop="loginName" label="登录名" min-width="150" /><el-table-column prop="organizationName" label="部门" min-width="130" />
        <el-table-column label="角色" min-width="220"><template #default="scope"><el-tag v-for="role in scope.row.roleCodes" :key="role" size="small" effect="plain" class="table-tag">{{ role }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right"><template #default="scope"><el-button v-if="canUpdate" link type="primary" @click="openEdit(scope.row)">编辑</el-button><el-button v-if="canUpdate" link @click="toggle(scope.row)">{{ scope.row.enabled ? '停用' : '启用' }}</el-button><el-button v-if="canDelete" link type="danger" @click="remove(scope.row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-table v-else-if="kind === 'roles'" :data="rows" stripe>
        <el-table-column prop="displayName" label="角色名称" min-width="150" /><el-table-column prop="code" label="角色编码" min-width="180" /><el-table-column prop="description" label="说明" min-width="260" /><el-table-column label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right"><template #default="scope"><el-button v-if="canUpdate" link type="primary" @click="openEdit(scope.row)">编辑</el-button><el-button v-if="canUpdate" link @click="toggle(scope.row)">{{ scope.row.enabled ? '停用' : '启用' }}</el-button><el-button v-if="canDelete" link type="danger" @click="remove(scope.row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-table v-else :data="rows" stripe row-key="id">
        <el-table-column prop="displayName" label="部门名称" min-width="170" /><el-table-column prop="code" label="编码" min-width="130" /><el-table-column prop="type" label="类型" min-width="120" /><el-table-column prop="parentId" label="上级标识" min-width="150" /><el-table-column label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right"><template #default="scope"><el-button v-if="canUpdate" link type="primary" @click="openEdit(scope.row)">编辑</el-button><el-button v-if="canUpdate" link @click="toggle(scope.row)">{{ scope.row.enabled ? '停用' : '启用' }}</el-button><el-button v-if="canDelete" link type="danger" @click="remove(scope.row)">删除</el-button></template></el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="`${editing ? '编辑' : '新增'}${singular}`" width="560px">
      <el-form label-width="100px" @submit.prevent="save">
        <template v-if="kind === 'users'">
          <el-form-item label="用户标识"><el-input v-model="draft.id" :disabled="editing" /></el-form-item><el-form-item label="登录名"><el-input v-model="draft.loginName" /></el-form-item><el-form-item label="姓名"><el-input v-model="draft.displayName" /></el-form-item>
          <el-form-item label="所属部门"><el-select v-model="draft.organizationId" clearable filterable><el-option v-for="org in organizations" :key="org.id" :label="org.displayName" :value="org.id" /></el-select></el-form-item>
          <el-form-item label="角色"><el-select v-model="draft.roleCodes" multiple filterable><el-option v-for="role in roles" :key="role.code" :label="role.displayName" :value="role.code" /></el-select></el-form-item><el-form-item label="外部身份"><el-input v-model="draft.externalIdentity" /></el-form-item>
        </template>
        <template v-else-if="kind === 'roles'"><el-form-item label="角色编码"><el-input v-model="draft.code" :disabled="editing" /></el-form-item><el-form-item label="角色名称"><el-input v-model="draft.displayName" /></el-form-item><el-form-item label="说明"><el-input v-model="draft.description" type="textarea" :rows="3" /></el-form-item></template>
        <template v-else><el-form-item label="部门标识"><el-input v-model="draft.id" :disabled="editing" /></el-form-item><el-form-item label="部门编码"><el-input v-model="draft.code" /></el-form-item><el-form-item label="部门名称"><el-input v-model="draft.displayName" /></el-form-item><el-form-item label="上级部门"><el-select v-model="draft.parentId" clearable filterable><el-option v-for="org in organizations.filter((item) => item.id !== draft.id)" :key="org.id" :label="org.displayName" :value="org.id" /></el-select></el-form-item><el-form-item label="类型"><el-select v-model="draft.type"><el-option label="机构" value="ROOT" /><el-option label="部门" value="DEPARTMENT" /><el-option label="病区" value="WARD" /></el-select></el-form-item><el-form-item label="排序"><el-input v-model.number="draft.sortOrder" type="number" /></el-form-item></template>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="loading" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>
