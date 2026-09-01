<script setup lang="ts">
import type { InventoryQuota, LoginUser } from '../api'
defineProps<{ user: LoginUser; quotas: InventoryQuota[]; loading: boolean }>()
defineEmits<{ refresh: [] }>()
</script>

<template>
  <main>
    <header><div><span class="eyebrow">INVENTORY CONTROL</span><h1>库存视图</h1><p>查看当前门店各车型的可售配额与预占数量。</p></div><el-button @click="$emit('refresh')">刷新库存</el-button></header>
    <section class="metric-grid metric-grid-three">
      <article><span>门店</span><strong class="metric-text">{{ user.storeId }}</strong><small>当前账号数据范围</small></article>
      <article><span>可售总量</span><strong>{{ quotas.reduce((sum, item) => sum + item.available, 0) }}</strong><small>数据库事实库存</small></article>
      <article><span>已预占</span><strong>{{ quotas.reduce((sum, item) => sum + item.reserved, 0) }}</strong><small>等待支付或交付</small></article>
    </section>
    <section class="table-card">
      <div class="section-title"><div><h2>车型配额</h2><p>Redisson 降低热点竞争，MySQL 条件更新负责最终防超卖</p></div><span class="live"><i></i> SOURCE OF TRUTH</span></div>
      <el-table :data="quotas" v-loading="loading" empty-text="当前门店暂无配额数据">
        <el-table-column prop="modelCode" label="车型编码" min-width="180" /><el-table-column prop="storeId" label="门店" min-width="180" />
        <el-table-column prop="available" label="可售数量" min-width="120" /><el-table-column prop="reserved" label="预占数量" min-width="120" />
        <el-table-column prop="version" label="乐观锁版本" min-width="130" /><el-table-column prop="updatedAt" label="更新时间" min-width="190" />
      </el-table>
    </section>
  </main>
</template>
