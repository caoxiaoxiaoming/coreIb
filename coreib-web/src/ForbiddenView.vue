<template>
  <section class="page-view forbidden-view">
    <div class="forbidden-code">403</div>
    <span class="eyebrow">ACCESS CONTROL</span>
    <h1>暂无访问权限</h1>
    <p>当前账号没有打开此业务模块所需的权限。请联系平台管理员调整角色、组织或数据范围。</p>
    <div class="forbidden-actions">
      <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
      <el-button @click="$router.push('/permissions')">查看当前权限</el-button>
    </div>
  </section>
</template>
