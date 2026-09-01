<script setup lang="ts">
import type { LoginUser } from '../api'
defineProps<{ user: LoginUser }>()
</script>

<template>
  <main>
    <header><div><span class="eyebrow">RUNTIME PROFILE</span><h1>系统设置</h1><p>查看当前演示环境、账号范围和关键服务连接信息。</p></div></header>
    <section class="settings-grid">
      <article class="settings-card"><h2>当前账号</h2><dl><dt>用户</dt><dd>{{ user.displayName }}</dd><dt>角色</dt><dd>{{ user.role }}</dd><dt>门店范围</dt><dd>{{ user.storeId }}</dd></dl></article>
      <article class="settings-card"><h2>服务入口</h2><dl><dt>API Gateway</dt><dd>http://localhost:8080</dd><dt>Nacos</dt><dd>http://localhost:8088</dd><dt>Prometheus</dt><dd>http://localhost:9090</dd><dt>Grafana</dt><dd>http://localhost:3000</dd></dl></article>
      <article class="settings-card"><h2>本地演示说明</h2><p>当前账号、JWT 密钥、数据库密码和 Nacos 免鉴权配置仅用于 Docker Compose 本地演示。生产环境必须改为外部密钥、启用鉴权并部署高可用基础设施。</p></article>
    </section>
  </main>
</template>
