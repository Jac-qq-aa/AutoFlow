<script setup lang="ts">
import { computed } from 'vue'
import type { SalesOrder } from '../api'

const props = defineProps<{ orders: SalesOrder[]; loading: boolean }>()
defineEmits<{
  refresh: []
  create: []
  action: [action: 'approve' | 'pay' | 'cancel' | 'deliver', order: SalesOrder]
}>()

const metrics = computed(() => ({
  total: props.orders.length,
  pending: props.orders.filter(order => !['COMPLETED', 'CANCELLED', 'CLOSED'].includes(order.status)).length,
  delivery: props.orders.filter(order => order.status === 'PENDING_DELIVERY').length,
  completed: props.orders.filter(order => order.status === 'COMPLETED').length,
}))

function statusType(status: string) {
  if (status === 'COMPLETED') return 'success'
  if (['CANCELLED', 'CLOSED'].includes(status)) return 'info'
  if (['REFUNDING', 'CANCELLING'].includes(status)) return 'warning'
  return 'primary'
}
</script>

<template>
  <main>
    <header><div><span class="eyebrow">SALES OPERATIONS</span><h1>订单履约中心</h1><p>查看从渠道接单到车辆交付的完整状态。</p></div><div class="header-actions"><el-button @click="$emit('refresh')">刷新</el-button><el-button type="primary" @click="$emit('create')">新建订单</el-button></div></header>
    <section class="metric-grid">
      <article><span>全部订单</span><strong>{{ metrics.total }}</strong><small>当前权限范围</small></article>
      <article><span>履约处理中</span><strong>{{ metrics.pending }}</strong><small>等待业务动作</small></article>
      <article><span>等待交付</span><strong>{{ metrics.delivery }}</strong><small>VIN 已分配</small></article>
      <article><span>本期已完成</span><strong>{{ metrics.completed }}</strong><small>闭环订单</small></article>
    </section>
    <section class="table-card">
      <div class="section-title"><div><h2>销售订单</h2><p>库存、支付和交付状态由领域事件异步更新</p></div><span class="live"><i></i> LIVE</span></div>
      <el-table :data="orders" v-loading="loading" row-key="orderId">
        <el-table-column prop="orderNo" label="订单编号" min-width="185" />
        <el-table-column label="客户 / 渠道" min-width="150"><template #default="scope"><strong>{{ scope.row.customerName }}</strong><br /><small>{{ scope.row.channel }} · {{ scope.row.storeId }}</small></template></el-table-column>
        <el-table-column prop="modelCode" label="车型" min-width="130" />
        <el-table-column label="金额" min-width="110"><template #default="scope">¥ {{ Number(scope.row.amount).toLocaleString() }}</template></el-table-column>
        <el-table-column label="主状态" min-width="135"><template #default="scope"><el-tag :type="statusType(scope.row.status) as any" effect="light">{{ scope.row.status }}</el-tag></template></el-table-column>
        <el-table-column label="库存 / 支付" min-width="160"><template #default="scope"><small>{{ scope.row.inventoryStatus }}<br />{{ scope.row.paymentStatus }}</small></template></el-table-column>
        <el-table-column label="操作" fixed="right" width="240"><template #default="scope">
          <el-button v-if="scope.row.status === 'PENDING_REVIEW'" link type="primary" @click="$emit('action', 'approve', scope.row)">审核</el-button>
          <el-button v-if="scope.row.status === 'PENDING_PAYMENT'" link type="primary" @click="$emit('action', 'pay', scope.row)">支付模拟</el-button>
          <el-button v-if="scope.row.status === 'PENDING_DELIVERY'" link type="success" @click="$emit('action', 'deliver', scope.row)">完成交付</el-button>
          <el-button v-if="!['COMPLETED','CANCELLED','CLOSED'].includes(scope.row.status)" link type="danger" @click="$emit('action', 'cancel', scope.row)">取消</el-button>
        </template></el-table-column>
      </el-table>
    </section>
  </main>
</template>
