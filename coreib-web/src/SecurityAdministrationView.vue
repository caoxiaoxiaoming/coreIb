<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ElMessage from 'element-plus/es/components/message/index'
import {
  fetchPlatformDirectory,
  fetchSecurityConfiguration,
  replaceRolePolicy,
  replaceUserRoles,
  saveRecordAssignment,
  saveSupervisorRelationship,
  setRecordAssignmentActive,
  setSupervisorRelationshipActive,
  type FieldGrantDraft,
  type PermissionGrantDraft,
  type PlatformRole,
  type PlatformUser,
  type RecordAssignment,
  type SecurityConfiguration,
  type SupervisorRelationship,
} from './api-client'
import type { ColumnAccess, PermissionAction, RowScope } from './coreib-permissions'
import { usePermissionStore } from './permission-store'

const permissionStore = usePermissionStore()
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('role-policy')
const roles = ref<PlatformRole[]>([])
const users = ref<PlatformUser[]>([])
const configuration = ref<SecurityConfiguration>(emptyConfiguration())
const selectedRole = ref('')
const permissionDrafts = ref<PermissionGrantDraft[]>([])
const fieldDrafts = ref<FieldGrantDraft[]>([])
const userRoleSelections = ref<Record<string, string[]>>({})
const supervisorDraft = ref({ userId: '', supervisorId: '', relationType: 'DIRECT' })
const assignmentDraft = ref({ resource: 'patient', recordId: '', userId: '', assignmentType: 'PRIMARY' })

const canUpdate = computed(() => permissionStore.can('security-administration', 'UPDATE'))
const selectedRoleName = computed(() => roles.value.find((role) => role.code === selectedRole.value)?.displayName ?? selectedRole.value)
const permissionResources = computed(() => [...new Set(permissionDrafts.value.map((item) => item.resource).filter(Boolean))])
const actions: PermissionAction[] = ['READ', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT']
const rowScopes: RowScope[] = ['ALL', 'ORGANIZATION', 'ORGANIZATION_TREE', 'SELF', 'MANAGED_USERS', 'RELATED_RECORDS', 'CUSTOM']
const columnAccessModes: ColumnAccess[] = ['HIDDEN', 'MASKED', 'READ', 'WRITE']

function demoSubject(): string | undefined {
  return import.meta.env.DEV ? permissionStore.demoSubject || undefined : undefined
}

async function load() {
  loading.value = true
  try {
    const subject = demoSubject()
    const [directory, security] = await Promise.all([
      fetchPlatformDirectory(subject),
      fetchSecurityConfiguration(subject),
    ])
    roles.value = directory.roles
    users.value = directory.users
    configuration.value = security
    userRoleSelections.value = Object.fromEntries(users.value.map((user) => [
      user.id,
      security.userRoles.filter((assignment) => assignment.userId === user.id).map((assignment) => assignment.roleCode),
    ]))
    if (!roles.value.some((role) => role.code === selectedRole.value)) selectedRole.value = roles.value[0]?.code ?? ''
    syncRoleDraft()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `权限配置读取失败：${reason.message}` : '权限配置读取失败')
  } finally {
    loading.value = false
  }
}

function syncRoleDraft() {
  permissionDrafts.value = configuration.value.permissions
    .filter((permission) => permission.roleCode === selectedRole.value)
    .map(({ resource, action, rowScope, policyKey, enabled }) => ({ resource, action, rowScope, policyKey, enabled }))
  fieldDrafts.value = configuration.value.fields
    .filter((field) => field.roleCode === selectedRole.value)
    .map(({ resource, fieldName, accessMode }) => ({ resource, fieldName, accessMode }))
}

function addPermission() {
  permissionDrafts.value.push({ resource: '', action: 'READ', rowScope: 'ALL', policyKey: null, enabled: true })
}

function addField() {
  fieldDrafts.value.push({ resource: permissionResources.value[0] ?? '', fieldName: '', accessMode: 'READ' })
}

async function saveRolePolicy() {
  if (!selectedRole.value) return
  saving.value = true
  try {
    await replaceRolePolicy(demoSubject(), selectedRole.value, permissionDrafts.value, fieldDrafts.value)
    await permissionStore.load()
    ElMessage.success(`${selectedRoleName.value}的权限策略已保存`)
    await load()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `策略保存失败：${reason.message}` : '策略保存失败')
  } finally {
    saving.value = false
  }
}

async function saveUserRoles(user: PlatformUser) {
  saving.value = true
  try {
    await replaceUserRoles(demoSubject(), user.id, userRoleSelections.value[user.id] ?? [])
    if (user.id === permissionStore.subjectId) await permissionStore.load()
    ElMessage.success(`${user.displayName}的角色已更新`)
    await load()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `角色更新失败：${reason.message}` : '角色更新失败')
  } finally {
    saving.value = false
  }
}

async function addSupervisorRelationship() {
  saving.value = true
  try {
    await saveSupervisorRelationship(demoSubject(), { ...supervisorDraft.value, active: true, effectiveFrom: null, effectiveTo: null })
    supervisorDraft.value = { userId: '', supervisorId: '', relationType: 'DIRECT' }
    ElMessage.success('上下级关系已保存')
    await load()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `关系保存失败：${reason.message}` : '关系保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleSupervisor(relationship: SupervisorRelationship) {
  try {
    await setSupervisorRelationshipActive(demoSubject(), relationship.id, !relationship.active)
    await load()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `状态更新失败：${reason.message}` : '状态更新失败')
  }
}

async function addRecordAssignment() {
  saving.value = true
  try {
    await saveRecordAssignment(demoSubject(), { ...assignmentDraft.value, active: true, effectiveFrom: null, effectiveTo: null })
    assignmentDraft.value = { resource: assignmentDraft.value.resource, recordId: '', userId: '', assignmentType: 'PRIMARY' }
    ElMessage.success('记录归属已保存')
    await load()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `归属保存失败：${reason.message}` : '归属保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleAssignment(assignment: RecordAssignment) {
  try {
    await setRecordAssignmentActive(demoSubject(), assignment.id, !assignment.active)
    await load()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? `状态更新失败：${reason.message}` : '状态更新失败')
  }
}

function userName(id: string): string {
  return users.value.find((user) => user.id === id)?.displayName ?? id
}

function emptyConfiguration(): SecurityConfiguration {
  return { permissions: [], fields: [], userRoles: [], supervisorRelationships: [], recordAssignments: [] }
}

onMounted(load)
</script>

<template>
  <section class="page-view security-admin-view">
    <div class="page-heading">
      <div>
        <span class="eyebrow">SECURITY ADMINISTRATION</span>
        <h1>权限配置</h1>
        <p>配置角色动作、数据行范围、字段访问，以及用户之间和业务记录之间的授权关系。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <section class="surface-panel security-summary">
      <div><span>角色策略</span><strong>{{ configuration.permissions.length }}</strong></div>
      <div><span>字段规则</span><strong>{{ configuration.fields.length }}</strong></div>
      <div><span>上下级关系</span><strong>{{ configuration.supervisorRelationships.length }}</strong></div>
      <div><span>记录归属</span><strong>{{ configuration.recordAssignments.length }}</strong></div>
    </section>

    <section class="surface-panel security-workspace" v-loading="loading">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="角色策略" name="role-policy">
          <div class="workspace-toolbar">
            <el-select v-model="selectedRole" class="role-select" placeholder="选择角色" @change="syncRoleDraft">
              <el-option v-for="role in roles" :key="role.code" :label="`${role.displayName} (${role.code})`" :value="role.code" />
            </el-select>
            <div>
              <el-button :disabled="!canUpdate || !selectedRole" @click="addPermission">添加动作</el-button>
              <el-button :disabled="!canUpdate || !selectedRole" @click="addField">添加字段</el-button>
              <el-button type="primary" :disabled="!canUpdate || !selectedRole" :loading="saving" @click="saveRolePolicy">保存策略</el-button>
            </div>
          </div>

          <h3>资源动作与行范围</h3>
          <el-empty v-if="!permissionDrafts.length" description="当前角色没有资源权限" />
          <el-table v-else :data="permissionDrafts" stripe>
            <el-table-column label="资源" min-width="180"><template #default="scope"><el-input v-model="scope.row.resource" :disabled="!canUpdate" placeholder="例如 patient" /></template></el-table-column>
            <el-table-column label="动作" width="150"><template #default="scope"><el-select v-model="scope.row.action" :disabled="!canUpdate"><el-option v-for="action in actions" :key="action" :value="action" :label="action" /></el-select></template></el-table-column>
            <el-table-column label="行范围" min-width="190"><template #default="scope"><el-select v-model="scope.row.rowScope" :disabled="!canUpdate"><el-option v-for="scopeName in rowScopes" :key="scopeName" :value="scopeName" :label="scopeName" /></el-select></template></el-table-column>
            <el-table-column label="策略键" min-width="170"><template #default="scope"><el-input v-model="scope.row.policyKey" :disabled="!canUpdate" placeholder="可选" /></template></el-table-column>
            <el-table-column label="启用" width="86"><template #default="scope"><el-switch v-model="scope.row.enabled" :disabled="!canUpdate" /></template></el-table-column>
            <el-table-column v-if="canUpdate" label="" width="76"><template #default="scope"><el-button link type="danger" @click="permissionDrafts.splice(scope.$index, 1)">移除</el-button></template></el-table-column>
          </el-table>

          <h3 class="subsection-title">字段访问</h3>
          <el-empty v-if="!fieldDrafts.length" description="当前角色没有字段级规则" />
          <el-table v-else :data="fieldDrafts" stripe>
            <el-table-column label="资源" min-width="190"><template #default="scope"><el-select v-model="scope.row.resource" :disabled="!canUpdate" allow-create filterable><el-option v-for="resource in permissionResources" :key="resource" :value="resource" :label="resource" /></el-select></template></el-table-column>
            <el-table-column label="字段" min-width="200"><template #default="scope"><el-input v-model="scope.row.fieldName" :disabled="!canUpdate" placeholder="业务字段名" /></template></el-table-column>
            <el-table-column label="访问模式" min-width="170"><template #default="scope"><el-select v-model="scope.row.accessMode" :disabled="!canUpdate"><el-option v-for="mode in columnAccessModes" :key="mode" :value="mode" :label="mode" /></el-select></template></el-table-column>
            <el-table-column v-if="canUpdate" label="" width="76"><template #default="scope"><el-button link type="danger" @click="fieldDrafts.splice(scope.$index, 1)">移除</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="用户角色" name="user-roles">
          <el-table :data="users" stripe>
            <el-table-column prop="displayName" label="用户" min-width="150" />
            <el-table-column prop="loginName" label="登录名" min-width="160" />
            <el-table-column label="角色" min-width="360">
              <template #default="scope">
                <el-select v-model="userRoleSelections[scope.row.id]" multiple filterable :disabled="!canUpdate" placeholder="未分配角色">
                  <el-option v-for="role in roles" :key="role.code" :label="role.displayName" :value="role.code" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column v-if="canUpdate" label="操作" width="100"><template #default="scope"><el-button link type="primary" :loading="saving" @click="saveUserRoles(scope.row)">保存</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="上下级关系" name="supervisors">
          <el-form v-if="canUpdate" :inline="true" class="inline-command-form" @submit.prevent="addSupervisorRelationship">
            <el-form-item label="下属"><el-select v-model="supervisorDraft.userId" filterable><el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" /></el-select></el-form-item>
            <el-form-item label="主管"><el-select v-model="supervisorDraft.supervisorId" filterable><el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" /></el-select></el-form-item>
            <el-form-item label="关系类型"><el-input v-model="supervisorDraft.relationType" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="saving" @click="addSupervisorRelationship">保存关系</el-button></el-form-item>
          </el-form>
          <el-table :data="configuration.supervisorRelationships" stripe>
            <el-table-column label="下属" min-width="170"><template #default="scope">{{ userName(scope.row.userId) }}</template></el-table-column>
            <el-table-column label="主管" min-width="170"><template #default="scope">{{ userName(scope.row.supervisorId) }}</template></el-table-column>
            <el-table-column prop="relationType" label="关系类型" min-width="140" />
            <el-table-column label="状态" width="140"><template #default="scope"><el-tag :type="scope.row.active ? 'success' : 'info'" effect="plain">{{ scope.row.active ? '生效' : '停用' }}</el-tag><el-button v-if="canUpdate" link @click="toggleSupervisor(scope.row)">{{ scope.row.active ? '停用' : '启用' }}</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="记录归属" name="assignments">
          <el-form v-if="canUpdate" :inline="true" class="inline-command-form" @submit.prevent="addRecordAssignment">
            <el-form-item label="资源"><el-input v-model="assignmentDraft.resource" /></el-form-item>
            <el-form-item label="记录标识"><el-input v-model="assignmentDraft.recordId" /></el-form-item>
            <el-form-item label="负责人"><el-select v-model="assignmentDraft.userId" filterable><el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" /></el-select></el-form-item>
            <el-form-item label="归属类型"><el-input v-model="assignmentDraft.assignmentType" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="saving" @click="addRecordAssignment">保存归属</el-button></el-form-item>
          </el-form>
          <el-table :data="configuration.recordAssignments" stripe>
            <el-table-column prop="resource" label="资源" min-width="140" />
            <el-table-column prop="recordId" label="记录标识" min-width="180" />
            <el-table-column label="负责人" min-width="160"><template #default="scope">{{ userName(scope.row.userId) }}</template></el-table-column>
            <el-table-column prop="assignmentType" label="归属类型" min-width="140" />
            <el-table-column label="状态" width="140"><template #default="scope"><el-tag :type="scope.row.active ? 'success' : 'info'" effect="plain">{{ scope.row.active ? '生效' : '停用' }}</el-tag><el-button v-if="canUpdate" link @click="toggleAssignment(scope.row)">{{ scope.row.active ? '停用' : '启用' }}</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
  </section>
</template>
