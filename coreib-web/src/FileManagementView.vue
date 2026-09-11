<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ElMessage from 'element-plus/es/components/message/index'
import ElMessageBox from 'element-plus/es/components/message-box/index'
import { deleteFile, downloadFile, fetchFiles, uploadFile, type StoredFile } from './api-client'
import { usePermissionStore } from './permission-store'
const permissions=usePermissionStore();const loading=ref(false);const files=ref<StoredFile[]>([]);const search=ref('')
const subject=computed(()=>import.meta.env.DEV?permissions.demoSubject||undefined:undefined)
const canCreate=computed(()=>permissions.can('file-management','CREATE'));const canDelete=computed(()=>permissions.can('file-management','DELETE'))
const rows=computed(()=>{const key=search.value.toLowerCase().trim();return key?files.value.filter(file=>file.originalName.toLowerCase().includes(key)):files.value})
async function load(){loading.value=true;try{files.value=await fetchFiles(subject.value)}catch(reason){ElMessage.error(error(reason,'文件列表读取失败'))}finally{loading.value=false}}
async function performUpload(options:{file:File}){loading.value=true;try{await uploadFile(subject.value,options.file);ElMessage.success('上传成功');await load()}catch(reason){ElMessage.error(error(reason,'上传失败'))}finally{loading.value=false}}
async function download(file:StoredFile){try{await downloadFile(subject.value,file)}catch(reason){ElMessage.error(error(reason,'下载失败'))}}
async function remove(file:StoredFile){try{await ElMessageBox.confirm(`确认删除“${file.originalName}”？`,'删除确认',{type:'warning'});await deleteFile(subject.value,file.id);ElMessage.success('删除成功');await load()}catch(reason){if(reason!=='cancel'&&reason!=='close')ElMessage.error(error(reason,'删除失败'))}}
function size(value:number){if(value<1024)return`${value} B`;if(value<1024*1024)return`${(value/1024).toFixed(1)} KB`;return`${(value/1024/1024).toFixed(1)} MB`}
function time(value:string){return new Date(value).toLocaleString('zh-CN')}function error(reason:unknown,fallback:string){return reason instanceof Error?`${fallback}：${reason.message}`:fallback}
onMounted(load)
</script>
<template><section class="page-view"><div class="page-heading compact-heading"><div><h1>文件管理</h1><p>上传、下载和清理平台文件；二进制内容存储在受控目录，元数据随业务数据库持久化。</p></div><el-upload v-if="canCreate" :show-file-list="false" :http-request="performUpload"><el-button type="primary" :loading="loading">上传文件</el-button></el-upload></div><section class="surface-panel management-toolbar"><el-input v-model="search" clearable placeholder="按文件名搜索"/><el-button :loading="loading" @click="load">刷新</el-button></section><section class="surface-panel" v-loading="loading"><el-table :data="rows" stripe><el-table-column prop="originalName" label="文件名" min-width="240" show-overflow-tooltip/><el-table-column prop="contentType" label="类型" min-width="180"/><el-table-column label="大小" width="110"><template #default="scope">{{size(scope.row.sizeBytes)}}</template></el-table-column><el-table-column prop="uploaderId" label="上传人" min-width="150"/><el-table-column label="上传时间" min-width="180"><template #default="scope">{{time(scope.row.createdAt)}}</template></el-table-column><el-table-column label="操作" width="130" fixed="right"><template #default="scope"><el-button link type="primary" @click="download(scope.row)">下载</el-button><el-button v-if="canDelete" link type="danger" @click="remove(scope.row)">删除</el-button></template></el-table-column></el-table></section></section></template>
