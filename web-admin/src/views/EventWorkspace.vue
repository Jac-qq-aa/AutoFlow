<script setup lang="ts">
import type { DeadLetterEvent } from '../api'
defineProps<{ events: DeadLetterEvent[]; loading: boolean }>()
defineEmits<{ refresh: []; replay: [event: DeadLetterEvent] }>()
</script>

<template>
  <main>
    <header><div><span class="eyebrow">EVENT RELIABILITY</span><h1>事件监控</h1><p>集中查看订单、库存和履约服务消费失败后记录的死信事件。</p></div><el-button @click="$emit('refresh')">刷新事件</el-button></header>
    <section class="metric-grid metric-grid-three">
      <article><span>异常事件</span><strong>{{ events.length }}</strong><small>最近最多 600 条</small></article>
      <article><span>待处理</span><strong>{{ events.filter(item => item.status === 'PENDING').length }}</strong><small>需要排查或补偿</small></article>
      <article><span>已重放</span><strong>{{ events.filter(item => item.status === 'REPLAYED').length }}</strong><small>完成补偿投递</small></article>
    </section>
    <section class="table-card">
      <div class="section-title"><div><h2>死信事件</h2><p>正常事件不会出现在这里；消费超过重试边界后才记录</p></div><span class="live"><i></i> DLQ</span></div>
      <el-table :data="events" v-loading="loading" empty-text="暂无死信，消息链路运行正常">
        <el-table-column prop="service" label="来源服务" min-width="120" /><el-table-column prop="event_type" label="事件类型" min-width="180" />
        <el-table-column prop="aggregate_id" label="业务聚合 ID" min-width="210" /><el-table-column prop="topic" label="Topic" min-width="210" />
        <el-table-column prop="reason" label="失败原因" min-width="260" show-overflow-tooltip /><el-table-column prop="status" label="状态" min-width="110" />
        <el-table-column prop="created_at" label="记录时间" min-width="190" />
        <el-table-column label="操作" width="100"><template #default="scope"><el-button v-if="scope.row.status === 'PENDING'" link type="primary" @click="$emit('replay', scope.row)">重放</el-button></template></el-table-column>
      </el-table>
    </section>
  </main>
</template>
