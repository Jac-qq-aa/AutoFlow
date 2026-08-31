<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveOrder, cancelOrder, completeDelivery, createOrder, listOrders, login, logout, payOrder, type LoginUser, type SalesOrder } from './api'

const user = ref<LoginUser | null>(JSON.parse(localStorage.getItem('autoflow_user') || 'null'))
const orders = ref<SalesOrder[]>([])
const loading = ref(false)
const loginForm = reactive({ username: 'manager', password: 'demo123' })
const createVisible = ref(false)
const createForm = reactive({
  channel: 'STORE', channelOrderNo: '', storeId: 'STORE-SH-001', customerName: '', customerPhone: '',
  modelCode: 'AF-SUV-PRO', amount: 219800,
})

const metrics = computed(() => ({
  total: orders.value.length,
  pending: orders.value.filter(order => !['COMPLETED', 'CANCELLED', 'CLOSED'].includes(order.status)).length,
  delivery: orders.value.filter(order => order.status === 'PENDING_DELIVERY').length,
  completed: orders.value.filter(order => order.status === 'COMPLETED').length,
}))

async function signIn() {
  try { user.value = await login(loginForm.username, loginForm.password); await refresh(); ElMessage.success('登录成功') }
  catch { ElMessage.error('登录失败，请检查演示账号') }
}
function signOut() { logout(); user.value = null; orders.value = [] }
async function refresh() {
  if (!user.value) return
  loading.value = true
  try { orders.value = await listOrders() } catch { ElMessage.error('订单加载失败') } finally { loading.value = false }
}
async function submitCreate() {
  try { await createOrder(createForm); createVisible.value = false; ElMessage.success('订单已创建，等待经理审核'); await refresh() }
  catch (error: any) { ElMessage.error(error.response?.data?.message || '创建失败') }
}
async function act(action: 'approve' | 'pay' | 'cancel' | 'deliver', order: SalesOrder) {
  try {
    if (action === 'approve') await approveOrder(order.orderId)
    if (action === 'pay') {
      const { value } = await ElMessageBox.prompt('SUCCESS / FAILURE / TIMEOUT', '支付模拟场景', { inputValue: 'SUCCESS' })
      await payOrder(order.orderId, value)
    }
    if (action === 'cancel') {
      const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', { inputValue: '客户改变购买计划' })
      await cancelOrder(order.orderId, value)
    }
    if (action === 'deliver') await completeDelivery(order.orderId)
    ElMessage.success('操作已提交，事件正在异步流转'); setTimeout(refresh, 1200)
  } catch (error: any) { if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '操作失败') }
}
function statusType(status: string) {
  if (status === 'COMPLETED') return 'success'
  if (['CANCELLED', 'CLOSED'].includes(status)) return 'info'
  if (['REFUNDING', 'CANCELLING'].includes(status)) return 'warning'
  return 'primary'
}
onMounted(refresh)
</script>

<template>
  <div v-if="!user" class="login-shell">
    <section class="brand-panel">
      <span class="eyebrow">AUTOMOTIVE ORDER ORCHESTRATION</span>
      <h1>每一笔订单，<br />都有清晰的下一站。</h1>
      <p>AutoFlow 将多渠道销售、库存配额、VIN 分配、支付与交付组织成可追踪的履约链路。</p>
      <div class="signal"><i></i><span>事件驱动服务已就绪</span></div>
    </section>
    <el-card class="login-card" shadow="never">
      <div class="logo-mark">AF</div><h2>登录工作台</h2><p>使用内置账号体验门店业务流程</p>
      <el-form label-position="top" @submit.prevent="signIn">
        <el-form-item label="账号"><el-input v-model="loginForm.username" size="large" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="loginForm.password" type="password" size="large" show-password /></el-form-item>
        <el-button type="primary" size="large" native-type="submit" class="full">进入 AutoFlow</el-button>
      </el-form>
      <div class="demo-users">manager / sales / delivery / admin<br />统一密码：demo123</div>
    </el-card>
  </div>

  <div v-else class="app-shell">
    <aside>
      <div class="side-brand"><span>AF</span><strong>AutoFlow</strong></div>
      <nav><a class="active">订单履约</a><a>库存视图</a><a>事件监控</a><a>系统设置</a></nav>
      <div class="aside-foot"><small>{{ user.displayName }}</small><span>{{ user.role }} · {{ user.storeId }}</span><button @click="signOut">退出登录</button></div>
    </aside>
    <main>
      <header><div><span class="eyebrow">SALES OPERATIONS</span><h1>订单履约中心</h1><p>查看从渠道接单到车辆交付的完整状态。</p></div><div class="header-actions"><el-button @click="refresh">刷新</el-button><el-button type="primary" @click="createVisible = true">新建订单</el-button></div></header>
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
            <el-button v-if="scope.row.status === 'PENDING_REVIEW'" link type="primary" @click="act('approve', scope.row)">审核</el-button>
            <el-button v-if="scope.row.status === 'PENDING_PAYMENT'" link type="primary" @click="act('pay', scope.row)">支付模拟</el-button>
            <el-button v-if="scope.row.status === 'PENDING_DELIVERY'" link type="success" @click="act('deliver', scope.row)">完成交付</el-button>
            <el-button v-if="!['COMPLETED','CANCELLED','CLOSED'].includes(scope.row.status)" link type="danger" @click="act('cancel', scope.row)">取消</el-button>
          </template></el-table-column>
        </el-table>
      </section>
    </main>
  </div>

  <el-dialog v-model="createVisible" title="创建销售订单" width="560px">
    <el-form label-position="top">
      <div class="form-grid"><el-form-item label="销售渠道"><el-select v-model="createForm.channel"><el-option label="线下门店" value="STORE" /><el-option label="官网" value="WEBSITE" /><el-option label="小程序" value="MINI_PROGRAM" /></el-select></el-form-item><el-form-item label="门店"><el-select v-model="createForm.storeId"><el-option label="上海 001" value="STORE-SH-001" /><el-option label="北京 001" value="STORE-BJ-001" /><el-option label="深圳 001" value="STORE-SZ-001" /></el-select></el-form-item></div>
      <div class="form-grid"><el-form-item label="客户姓名"><el-input v-model="createForm.customerName" /></el-form-item><el-form-item label="联系电话"><el-input v-model="createForm.customerPhone" /></el-form-item></div>
      <div class="form-grid"><el-form-item label="车型"><el-select v-model="createForm.modelCode"><el-option label="AutoFlow SUV Pro" value="AF-SUV-PRO" /><el-option label="AutoFlow Sedan X" value="AF-SEDAN-X" /><el-option label="AutoFlow City EV" value="AF-CITY-EV" /></el-select></el-form-item><el-form-item label="成交金额"><el-input-number v-model="createForm.amount" :min="1" :step="1000" /></el-form-item></div>
    </el-form>
    <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" @click="submitCreate">提交订单</el-button></template>
  </el-dialog>
</template>

