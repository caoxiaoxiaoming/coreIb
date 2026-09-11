<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ElMessage from 'element-plus/es/components/message/index'
import ElMessageBox from 'element-plus/es/components/message-box/index'
import { deleteManagementRecord, fetchDictionaryData, fetchDictionaryTypes, saveManagementRecord, type ManagedDictionaryData, type ManagedDictionaryType } from './api-client'
import { usePermissionStore } from './permission-store'

const permissions=usePermissionStore(); const loading=ref(false)
const types=ref<ManagedDictionaryType[]>([]); const data=ref<ManagedDictionaryData[]>([]); const selectedCode=ref('')
const dialogVisible=ref(false); const editing=ref(false); const dialogKind=ref<'type'|'data'>('type'); const draft=ref<Record<string,any>>({})
const subject=computed(()=>import.meta.env.DEV?permissions.demoSubject||undefined:undefined)
const canUpdate=computed(()=>permissions.can('platform-management','UPDATE')); const canDelete=computed(()=>permissions.can('platform-management','DELETE'))
async function load(){loading.value=true;try{types.value=await fetchDictionaryTypes(subject.value);if(!selectedCode.value&&types.value.length)selectedCode.value=types.value[0].code;data.value=await fetchDictionaryData(subject.value,selectedCode.value||undefined)}catch(reason){ElMessage.error(error(reason,'字典读取失败'))}finally{loading.value=false}}
async function select(code:string){selectedCode.value=code;data.value=await fetchDictionaryData(subject.value,code)}
function create(kind:'type'|'data'){dialogKind.value=kind;editing.value=false;draft.value=kind==='type'?{id:crypto.randomUUID(),code:'',name:'',remark:'',enabled:true}:{id:crypto.randomUUID(),typeCode:selectedCode.value,label:'',value:'',sortOrder:10,enabled:true};dialogVisible.value=true}
function edit(kind:'type'|'data',row:ManagedDictionaryType|ManagedDictionaryData){dialogKind.value=kind;editing.value=true;draft.value={...row};dialogVisible.value=true}
async function save(){loading.value=true;try{await saveManagementRecord(subject.value,dialogKind.value==='type'?'dictionary-types':'dictionary-data',draft.value as ManagedDictionaryType|ManagedDictionaryData);dialogVisible.value=false;ElMessage.success('保存成功');await load()}catch(reason){ElMessage.error(error(reason,'保存失败'))}finally{loading.value=false}}
async function remove(kind:'type'|'data',id:string){try{await ElMessageBox.confirm('删除字典类型时会一并删除其字典数据，是否继续？','删除确认',{type:'warning'});await deleteManagementRecord(subject.value,kind==='type'?'DICTIONARY_TYPE':'DICTIONARY_DATA',id);await load()}catch(reason){if(reason!=='cancel'&&reason!=='close')ElMessage.error(error(reason,'删除失败'))}}
function error(reason:unknown,fallback:string){return reason instanceof Error?`${fallback}：${reason.message}`:fallback}
onMounted(load)
</script>
<template>
  <section class="page-view">
    <div class="page-heading compact-heading"><div><h1>字典管理</h1><p>维护稳定的业务枚举和显示值，供前后端按字典编码统一消费。</p></div><el-button v-if="canUpdate" type="primary" @click="create('type')">新增字典类型</el-button></div>
    <div class="dictionary-layout">
      <section class="surface-panel dictionary-types" v-loading="loading">
        <div class="panel-title"><strong>字典类型</strong><el-button size="small" @click="load">刷新</el-button></div>
        <button v-for="item in types" :key="item.id" type="button" class="dictionary-type" :class="{active:selectedCode===item.code}" @click="select(item.code)"><span><strong>{{ item.name }}</strong><small>{{ item.code }}</small></span><el-tag :type="item.enabled?'success':'info'" size="small">{{ item.enabled?'启用':'停用' }}</el-tag></button>
      </section>
      <section class="surface-panel dictionary-data" v-loading="loading">
        <div class="panel-title"><strong>{{ selectedCode || '字典数据' }}</strong><div><el-button v-if="canUpdate&&selectedCode" type="primary" size="small" @click="create('data')">新增数据</el-button></div></div>
        <el-table :data="data" stripe><el-table-column prop="label" label="标签" min-width="150"/><el-table-column prop="value" label="值" min-width="150"/><el-table-column prop="sortOrder" label="排序" width="80"/><el-table-column label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.enabled?'success':'info'">{{scope.row.enabled?'启用':'停用'}}</el-tag></template></el-table-column><el-table-column label="操作" width="130"><template #default="scope"><el-button v-if="canUpdate" link type="primary" @click="edit('data',scope.row)">编辑</el-button><el-button v-if="canDelete" link type="danger" @click="remove('data',scope.row.id)">删除</el-button></template></el-table-column></el-table>
        <div class="dictionary-type-actions" v-if="selectedCode"><el-button v-if="canUpdate" @click="edit('type',types.find(item=>item.code===selectedCode)!)">编辑当前类型</el-button><el-button v-if="canDelete" type="danger" plain @click="remove('type',types.find(item=>item.code===selectedCode)!.id)">删除当前类型</el-button></div>
      </section>
    </div>
    <el-dialog v-model="dialogVisible" :title="`${editing?'编辑':'新增'}${dialogKind==='type'?'字典类型':'字典数据'}`" width="540px"><el-form label-width="100px" @submit.prevent="save"><el-form-item label="数据标识"><el-input v-model="draft.id" :disabled="editing"/></el-form-item><template v-if="dialogKind==='type'"><el-form-item label="类型编码"><el-input v-model="draft.code"/></el-form-item><el-form-item label="类型名称"><el-input v-model="draft.name"/></el-form-item><el-form-item label="说明"><el-input v-model="draft.remark" type="textarea"/></el-form-item></template><template v-else><el-form-item label="类型编码"><el-input v-model="draft.typeCode" disabled/></el-form-item><el-form-item label="显示标签"><el-input v-model="draft.label"/></el-form-item><el-form-item label="字典值"><el-input v-model="draft.value"/></el-form-item><el-form-item label="排序"><el-input v-model.number="draft.sortOrder" type="number"/></el-form-item></template><el-form-item label="启用"><el-switch v-model="draft.enabled"/></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="loading" @click="save">保存</el-button></template></el-dialog>
  </section>
</template>
