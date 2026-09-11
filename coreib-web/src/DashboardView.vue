<script setup lang="ts">
import { computed } from 'vue'
import { usePermissionStore } from './permission-store'
const permissions = usePermissionStore()
const accessSummary = computed(() => permissions.snapshot?.resources ?? {})
</script>

<template>
  <section class="page-view">
    <div class="page-heading"><div><span class="eyebrow">PLATFORM OVERVIEW</span><h1>业务系统工作台</h1><p>芋道 Vue3 管理端风格的前端基座，业务模块可以从这里按权限接入。</p></div><el-tag type="success" effect="plain">开发基座</el-tag></div>
    <div class="metric-grid">
      <article class="metric-card"><span>权限资源</span><strong>{{ Object.keys(accessSummary).length }}</strong><small>由后端权限快照提供</small></article>
      <article class="metric-card"><span>当前主体</span><strong>{{ permissions.subjectId }}</strong><small>{{ permissions.authenticated ? '已认证' : '匿名开发会话' }}</small></article>
      <article class="metric-card"><span>角色数量</span><strong>{{ permissions.roleCodes.length }}</strong><small>角色可映射多组策略</small></article>
      <article class="metric-card accent"><span>安全边界</span><strong>后端</strong><small>前端隐藏不替代接口校验</small></article>
    </div>
    <div class="content-grid">
      <section class="surface-panel"><div class="panel-heading"><div><span class="eyebrow">PLATFORM MODULES</span><h2>基座模块</h2></div><el-tag effect="plain">可扩展</el-tag></div><div class="module-list">
        <div class="module-row"><span class="module-code">AUTH</span><div><strong>认证与会话</strong><small>支持 JWT、OIDC 或院内统一身份适配</small></div><el-tag type="success" effect="plain">基础已接入</el-tag></div>
        <div class="module-row"><span class="module-code">RBAC</span><div><strong>角色与权限</strong><small>动作、行范围、字段访问和审计</small></div><el-tag type="success" effect="plain">已接入</el-tag></div>
        <div class="module-row"><span class="module-code">DATA</span><div><strong>多数据库适配</strong><small>SQL Server、Oracle、PostgreSQL</small></div><el-tag type="success" effect="plain">已接入</el-tag></div>
        <div class="module-row"><span class="module-code">BIZ</span><div><strong>业务模块</strong><small>按领域新增患者、住院、财务等工作区</small></div><el-tag effect="plain">扩展点</el-tag></div>
      </div></section>
      <section class="surface-panel security-note"><span class="eyebrow">ACCESS MODEL</span><h2>权限执行链</h2><ol><li><b>身份</b><span>认证模块解析当前用户和组织</span></li><li><b>策略</b><span>角色合并动作、行范围和字段策略</span></li><li><b>数据</b><span>业务仓储参数化过滤并执行后端校验</span></li><li><b>审计</b><span>记录查看、修改、导出和拒绝原因</span></li></ol></section>
    </div>
  </section>
</template>
