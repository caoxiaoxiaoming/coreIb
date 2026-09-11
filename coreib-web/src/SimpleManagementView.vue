<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ElMessage from 'element-plus/es/components/message/index'
import ElMessageBox from 'element-plus/es/components/message-box/index'
import {
  deleteManagementRecord, fetchManagedConfigs, fetchManagedMenus, fetchManagedNotices,
  fetchManagedPosts, saveManagementRecord, type PlatformManagementKind,
  type PlatformManagementRecord,
} from './api-client'
import { usePermissionStore } from './permission-store'

type Kind = 'menus' | 'posts' | 'configs' | 'notices'
type Field = { key: string; label: string; type?: 'text' | 'textarea' | 'number' | 'boolean' | 'select'; options?: { label: string; value: string }[]; width?: number }
type Definition = { singular: string; description: string; fields: Field[]; defaults: () => Record<string, any>; entity: 'MENU' | 'POST' | 'CONFIG' | 'NOTICE' }
const definitions: Record<Kind, Definition> = {
  menus: { singular: '菜单', description: '配置目录、页面和按钮权限元数据；保存后侧栏会立即重新加载。', entity: 'MENU', defaults: () => ({ id: crypto.randomUUID(), parentId: '', name: '', path: '', component: '', icon: 'Menu', permission: '', menuType: 'MENU', sortOrder: 10, visible: true, enabled: true }), fields: [
    { key: 'name', label: '菜单名称' }, { key: 'parentId', label: '上级标识' }, { key: 'menuType', label: '类型', type: 'select', options: [{label:'目录',value:'DIRECTORY'},{label:'菜单',value:'MENU'},{label:'按钮',value:'BUTTON'}] }, { key: 'path', label: '路由地址' }, { key: 'component', label: '组件标识' }, { key: 'icon', label: '图标' }, { key: 'permission', label: '权限标识' }, { key: 'sortOrder', label: '排序', type: 'number' }, { key: 'visible', label: '显示', type: 'boolean' }, { key: 'enabled', label: '启用', type: 'boolean' },
  ] },
  posts: { singular: '岗位', description: '维护业务人员岗位编码及启停状态。', entity: 'POST', defaults: () => ({ id: crypto.randomUUID(), code: '', name: '', sortOrder: 10, enabled: true }), fields: [{key:'code',label:'岗位编码'},{key:'name',label:'岗位名称'},{key:'sortOrder',label:'排序',type:'number'},{key:'enabled',label:'启用',type:'boolean'}] },
  configs: { singular: '参数', description: '集中维护运行参数；敏感参数应关闭公开访问。', entity: 'CONFIG', defaults: () => ({ id: crypto.randomUUID(), configKey: '', name: '', value: '', publicVisible: false, remark: '' }), fields: [{key:'configKey',label:'参数键'},{key:'name',label:'参数名称'},{key:'value',label:'参数值'},{key:'publicVisible',label:'公开',type:'boolean'},{key:'remark',label:'说明',type:'textarea'}] },
  notices: { singular: '公告', description: '维护通知和公告内容及发布状态。', entity: 'NOTICE', defaults: () => ({ id: crypto.randomUUID(), title: '', noticeType: 'NOTICE', status: 'DRAFT', content: '', publishedAt: null }), fields: [{key:'title',label:'标题'},{key:'noticeType',label:'类型',type:'select',options:[{label:'通知',value:'NOTICE'},{label:'公告',value:'ANNOUNCEMENT'}]},{key:'status',label:'状态',type:'select',options:[{label:'草稿',value:'DRAFT'},{label:'已发布',value:'PUBLISHED'},{label:'已关闭',value:'CLOSED'}]},{key:'content',label:'内容',type:'textarea'},{key:'publishedAt',label:'发布时间'}] },
}

const route = useRoute(); const permissions = usePermissionStore()
const kind = computed(() => route.meta.managementKind as Kind)
const definition = computed(() => definitions[kind.value])
const loading = ref(false); const search = ref(''); const records = ref<PlatformManagementRecord[]>([])
const dialogVisible = ref(false); const editing = ref(false); const draft = ref<Record<string, any>>({})
const subject = computed(() => import.meta.env.DEV ? permissions.demoSubject || undefined : undefined)
const canUpdate = computed(() => permissions.can('platform-management','UPDATE'))
const canDelete = computed(() => permissions.can('platform-management','DELETE'))
const rows = computed(() => { const key=search.value.trim().toLowerCase(); return key ? records.value.filter(row=>JSON.stringify(row).toLowerCase().includes(key)) : records.value })

async function load() {
  loading.value=true
  try {
    records.value = kind.value==='menus' ? await fetchManagedMenus(subject.value) : kind.value==='posts' ? await fetchManagedPosts(subject.value) : kind.value==='configs' ? await fetchManagedConfigs(subject.value) : await fetchManagedNotices(subject.value)
  } catch(reason) { ElMessage.error(message(reason,'读取失败')) } finally { loading.value=false }
}
function create() { editing.value=false; draft.value=definition.value.defaults(); dialogVisible.value=true }
function edit(row: PlatformManagementRecord) { editing.value=true; draft.value={...row}; dialogVisible.value=true }
async function save() {
  loading.value=true
  try {
    if(kind.value==='notices' && draft.value.status==='PUBLISHED' && !draft.value.publishedAt) draft.value.publishedAt=new Date().toISOString()
    await saveManagementRecord(subject.value,kind.value as PlatformManagementKind,draft.value as PlatformManagementRecord)
    dialogVisible.value=false; ElMessage.success('保存成功'); await load(); if(kind.value==='menus') await permissions.load()
  } catch(reason) { ElMessage.error(message(reason,'保存失败')) } finally { loading.value=false }
}
async function remove(row: PlatformManagementRecord) {
  try { await ElMessageBox.confirm(`确认删除${definition.value.singular}？`,'删除确认',{type:'warning'}); await deleteManagementRecord(subject.value,definition.value.entity,row.id); ElMessage.success('删除成功'); await load(); if(kind.value==='menus') await permissions.load() }
  catch(reason) { if(reason!=='cancel'&&reason!=='close') ElMessage.error(message(reason,'删除失败')) }
}
function display(value: unknown, field: Field) { if(field.type==='boolean') return value ? '是' : '否'; if(value===null||value===undefined||value==='') return '-'; return String(value) }
function message(reason:unknown,fallback:string){return reason instanceof Error?`${fallback}：${reason.message}`:fallback}
watch(kind,load); onMounted(load)
</script>

<template>
  <section class="page-view">
    <div class="page-heading compact-heading"><div><h1>{{ route.meta.title }}</h1><p>{{ definition.description }}</p></div><el-button v-if="canUpdate" type="primary" @click="create">新增{{ definition.singular }}</el-button></div>
    <section class="surface-panel management-toolbar"><el-input v-model="search" clearable :placeholder="`搜索${definition.singular}`" /><el-button :loading="loading" @click="load">刷新</el-button></section>
    <section class="surface-panel" v-loading="loading">
      <el-table :data="rows" stripe row-key="id">
        <el-table-column v-for="field in definition.fields.filter(item=>!['content'].includes(item.key))" :key="field.key" :prop="field.key" :label="field.label" min-width="120"><template #default="scope"><el-tag v-if="field.type==='boolean'" :type="scope.row[field.key]?'success':'info'" effect="plain">{{ display(scope.row[field.key],field) }}</el-tag><span v-else class="cell-ellipsis">{{ display(scope.row[field.key],field) }}</span></template></el-table-column>
        <el-table-column label="操作" width="130" fixed="right"><template #default="scope"><el-button v-if="canUpdate" link type="primary" @click="edit(scope.row)">编辑</el-button><el-button v-if="canDelete" link type="danger" @click="remove(scope.row)">删除</el-button></template></el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="dialogVisible" :title="`${editing?'编辑':'新增'}${definition.singular}`" width="620px">
      <el-form label-width="100px" @submit.prevent="save">
        <el-form-item label="数据标识"><el-input v-model="draft.id" :disabled="editing" /></el-form-item>
        <el-form-item v-for="field in definition.fields" :key="field.key" :label="field.label">
          <el-switch v-if="field.type==='boolean'" v-model="draft[field.key]" />
          <el-select v-else-if="field.type==='select'" v-model="draft[field.key]"><el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" /></el-select>
          <el-input v-else v-model="draft[field.key]" :type="field.type==='textarea'?'textarea':field.type==='number'?'number':'text'" :rows="field.type==='textarea'?5:undefined" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="loading" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>
